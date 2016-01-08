/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.kernel;

import static rapture.common.Scheme.SHEET;

import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.EntitlementSet;
import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureSheet;
import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetDisplayCell;
import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetRow;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStyle;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SheetAndMeta;
import rapture.common.SheetRepoConfig;
import rapture.common.SheetRepoConfigStorage;
import rapture.common.api.SheetApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.shared.doc.GetDocPayload;
import rapture.kernel.context.ContextValidator;
import rapture.kernel.script.KernelScript;
import rapture.render.PDFSheetRenderer;
import rapture.repo.SheetRepo;
import rapture.script.IActivityInfo;
import rapture.script.IRaptureScript;
import rapture.script.ScriptFactory;
import rapture.series.config.ConfigValidatorService;
import rapture.series.config.InvalidConfigException;
import rapture.util.StringUtil;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.lowagie.text.DocumentException;

public class SheetApiImpl extends KernelBase implements SheetApi {
    private static Logger log = Logger.getLogger(SheetApiImpl.class);

    public SheetApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }
    
    @Override
    public void createSheetRepo(CallingContext context, String sheetRepoURI, String config) {
        checkParameter("Repository URI", sheetRepoURI);
        checkParameter("Config", config);

        RaptureURI parsedURI = new RaptureURI(sheetRepoURI, Scheme.SHEET);
        String authority = parsedURI.getAuthority();
        if ((authority == null) || authority.isEmpty()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoAuthority")); //$NON-NLS-1$
        }
        if (parsedURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", sheetRepoURI)); //$NON-NLS-1$
        }
        if (sheetRepoExists(context, parsedURI.getAuthority())) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("Exists", sheetRepoURI)); //$NON-NLS-1$
        }

        SheetRepoConfig repoConfig = new SheetRepoConfig();
        repoConfig.setAuthority(authority);
        repoConfig.setConfig(config);
        SheetRepoConfigStorage.add(repoConfig, context.getUser(), "Created sheet store"); //$NON-NLS-1$ //$NON-NLS-2$        // TODO Auto-generated method stub

        // Sheet repo will be cached when accessed the first time
        log.info("Creating Repository " + sheetRepoURI);
    }

    @Override
    public SheetRepoConfig getSheetRepoConfig(CallingContext context, String sheetURI) {
        RaptureURI authority = new RaptureURI(sheetURI, Scheme.RELATIONSHIP);
        if (authority.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", sheetURI)); //$NON-NLS-1$
        }
        return SheetRepoConfigStorage.readByAddress(authority);
    }

    @Override
    public String setSheetCell(CallingContext context, String sheetURI, int row, int column, String value, int tabId) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        // String.format("Setting cell (%d,%d) in sheet %s to %s", row, column,
        // sheetURI, value);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.setCell(parsedURI.getDocPath(), row, column, value, tabId);
        return value;
    }

    @Override
    public void setBlock(CallingContext context, String sheetURI, int startRow, int startColumn,
            List<String> values, int height, int width, int tabId) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.setBlock(parsedURI.getDocPath(), startRow, startColumn, values, height, width, tabId);
    }

    @Override
    public String getSheetCell(CallingContext context, String sheetURI, int row, int column, int tabId) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.getCell(parsedURI.getDocPath(), row, column, tabId);
    }

    @Override
    public Boolean sheetExists(CallingContext context, String sheetURI) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        if (sheetRepoExists(context, parsedURI.getAuthority())) {
            SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
            return repo.sheetExists(parsedURI.getDocPath());
        }
        return false;
    }

    @Override
    public RaptureSheet createSheet(CallingContext context, String sheetURI) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.createSheet(parsedURI.getDocPath());
    }

    @Override
    public RaptureSheet deleteSheet(CallingContext context, String sheetURI) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.deleteSheet(parsedURI.getDocPath());
    }

    @Override
    public List<RaptureSheetCell> findCellsByEpoch(CallingContext context, String sheetURI, int tabId, Long epoch) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.findCellsByEpoch(parsedURI.getDocPath(), tabId, epoch);
    }

    @Override
    public List<RaptureSheetStyle> getAllStyles(CallingContext context, String sheetURI) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.getSheetStyles(parsedURI.getDocPath());
    }

    @Override
    public void removeStyle(CallingContext context, String sheetURI, String styleName) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.deleteSheetStyle(parsedURI.getDocPath(), styleName);
    }

    @Override
    public RaptureSheetStyle createStyle(CallingContext context, String sheetURI, RaptureSheetStyle style) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.putSheetStyle(parsedURI.getDocPath(), style);
    }

    @Override
    public List<RaptureSheetScript> getAllScripts(CallingContext context, String sheetURI) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.getSheetScripts(parsedURI.getDocPath());
    }

    @Override
    public void removeScript(CallingContext context, String sheetURI, String scriptName) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.deleteSheetScript(parsedURI.getDocPath(), scriptName);
    }

    @Override
    public RaptureSheetScript createScript(CallingContext context, String sheetURI, String scriptName,
            RaptureSheetScript script) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.putSheetScript(parsedURI.getDocPath(), scriptName, script);
    }

    @Override
    public List<RaptureSheetRange> getSheetNamedSelections(CallingContext context, String sheetURI) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.getSheetNamedSelections(parsedURI.getDocPath());
    }

    @Override
    public void deleteSheetNamedSelection(CallingContext context, String sheetURI, String rangeName) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.deleteSheetNamedSelection(parsedURI.getDocPath(), rangeName);
    }

    @Override
    public RaptureSheetRange createSheetNamedSelection(CallingContext context, String sheetURI, String rangeName,
            RaptureSheetRange range) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.putSheetNamedSelection(parsedURI.getDocPath(), rangeName, range);
    }

    @Override
    public void runScriptOnSheet(CallingContext context, String sheetURI, final String scriptName) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        // Run the script embedded in this sheet, with an additional head which
        // sets the current authority, storeName, name
        RaptureSheetScript script = getRepoFromCache(parsedURI.getAuthority()).getSheetScript(parsedURI.getDocPath(), scriptName);
        StringBuilder prefix = new StringBuilder();
        prefix.append("import SheetManager as s;\n");
        prefix.append(String.format("$s.setContext('%s');\n", parsedURI.toString()));
        prefix.append(script.getScript());
        // Now run prefix

        RaptureScript mainScript = new RaptureScript();
        mainScript.setLanguage(RaptureScriptLanguage.REFLEX);
        mainScript.setName(scriptName);
        mainScript.setAuthority(parsedURI.getAuthority());
        mainScript.setPurpose(RaptureScriptPurpose.PROGRAM);
        mainScript.setScript(prefix.toString());

        IRaptureScript scriptContext = ScriptFactory.getScript(mainScript);
        Map<String, Object> extraVals = new HashMap<String, Object>();
        Kernel.writeComment(String.format(Messages.getString("Script.running"), parsedURI.getDocPath())); //$NON-NLS-1$
        final String activityId = Kernel.getActivity().createActivity(ContextFactory.getKernelUser(), parsedURI.getDocPath(), "Running script", 1L, 100L);
        scriptContext.runProgram(context, new IActivityInfo() {

            @Override
            public String getActivityId() {
                return activityId;
            }

            @Override
            public String getOtherId() {
                return scriptName;
            }

        }, mainScript, extraVals);
        script.setLastRun(new Date());
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.putSheetScript(parsedURI.getDocPath(), scriptName, script);
        Kernel.getActivity().finishActivity(ContextFactory.getKernelUser(), activityId, "Ran script");
    }

    @Override
    public void cloneSheet(CallingContext context, String srcURI, String targetURI) {

        RaptureURI parsedSrcURI = new RaptureURI(srcURI, Scheme.SHEET);
        RaptureURI parsedTargetURI = new RaptureURI(targetURI, Scheme.SHEET);

        // Clone sheet copies everything from one sheet to another. This is only
        // allowed within a store, as it happens within the store
        // implementation
        if (!parsedSrcURI.getAuthority().equals(parsedTargetURI.getAuthority())) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format(
                    "Source and target URIs not in the same authority & type: %s : %s ", srcURI, targetURI));
        }
        SheetRepo repo = getRepoFromCache(parsedSrcURI.getAuthority());
        repo.cloneSheet(parsedSrcURI.getDocPath(), parsedTargetURI.getDocPath());
    }

    @Override
    public RaptureSheetScript getSheetScript(CallingContext context, String sheetURI, String scriptName) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.getSheetScript(parsedURI.getDocPath(), scriptName);
    }

    @Override
    public List<RaptureSheetRow> getSheetNamedSelection(CallingContext context, String sheetURI, String rangeName) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        RaptureSheetRange range = getRepoFromCache(parsedURI.getAuthority()).getSheetNamedSelection(parsedURI.getDocPath(), rangeName);
        if (range == null) {
            return new ArrayList<RaptureSheetRow>();
        } else {
            List<RaptureSheetRow> rows = new ArrayList<RaptureSheetRow>();
            for (int row = range.getStartRow(); row <= range.getEndRow(); row++) {
                List<RaptureSheetCell> cells = new ArrayList<RaptureSheetCell>();
                for (int column = range.getStartColumn(); column <= range.getEndColumn(); column++) {
                    String value = getRepoFromCache(parsedURI.getAuthority()).getCell(parsedURI.getDocPath(), row, column, 0);
                    RaptureSheetCell cell = new RaptureSheetCell();
                    cell.setColumn(column);
                    cell.setRow(row);
                    cell.setData(value);
                    cells.add(cell);
                }
                RaptureSheetRow rowContainer = new RaptureSheetRow();
                rowContainer.setCells(cells);
                rows.add(rowContainer);
            }
            return rows;
        }
    }

    @Override
    public String exportSheetAsScript(CallingContext context, String sheetURI) {
        // Generate Reflex script that can recreate the given sheet.
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SHEET_URI = '%s';\n", sheetURI));
        sb.append("#sheet.createSheet(SHEET_URI);\n");
        // Set cells for each tabId
        Pattern p = Pattern.compile("[^0-9A-Za-z_ ,\\.]");
        int maxRow = 0;
        int maxColumn = 0;
        int minRow = 9999;
        int minColumn = 9999;
        for (int tabId = 0; tabId < 2; tabId++) {
            List<RaptureSheetCell> cells = findCellsByEpoch(context, sheetURI, tabId, 0L);
            sb.append("cellData = [ - ];\n");
            for (RaptureSheetCell cell : cells) {
                if (cell.getColumn() > maxColumn) {
                    maxColumn = cell.getColumn();
                }
                if (cell.getRow() > maxRow) {
                    maxRow = cell.getRow();
                }
                if (cell.getColumn() < minColumn) {
                    minColumn = cell.getColumn();
                }
                if (cell.getRow() < minRow) {
                    minRow = cell.getRow();
                }

                if (p.matcher(cell.getData()).find()) {
                    log.info("Special chars in data - " + cell.getData());
                    sb.append(String.format("denc=\"%s\";\n", StringUtil.base64Compress(cell.getData())));
                    sb.append(String.format("cellData[%d,%d] = bdecompress(denc);\n", cell.getRow(), cell.getColumn()));
                    // sb.append(String.format("#sheet.setSheetCell(SHEET_URI, %d, %d, bdecompress(denc), %d);\n",
                    // cell.getRow(), cell.getColumn(), tabId));
                } else {
                    sb.append(String.format("cellData[%d,%d] = '%s';\n", cell.getRow(), cell.getColumn(), cell.getData()));
                    // sb.append(String.format("#sheet.setSheetCell(SHEET_URI, %d, %d, '%s', %d);\n", cell.getRow(),
                    // cell.getColumn(), cell.getData(), tabId));
                }
            }
            // Now call bulk add
            sb.append("cellArray = [];\n");
            sb.append(String.format("for row = %d to %d do\n", minRow, maxRow));
            sb.append(String.format("for column = %d to %d do\n", minColumn, maxColumn));
            sb.append("     cellArray = cellArray + cellData[row,column];\n");
            sb.append("    end\n");
            sb.append(" end\n");
            sb.append(String.format("#sheet.setBlock(SHEET_URI,%d,%d,cellArray,%d,%d,%d);\n", minRow, minColumn, maxRow - minRow + 1,
                    maxColumn - minColumn + 1, tabId));
        }
        // Setup styles
        List<RaptureSheetStyle> styles = getAllStyles(context, sheetURI);
        for (RaptureSheetStyle style : styles) {
            sb.append(String.format("style = %s;\n", JacksonUtil.jsonFromObject(style)));
            sb.append(String.format("#sheet.createStyle(SHEET_URI, '%s', style);\n", style.getName()));
        }
        // Setup ranges
        List<RaptureSheetRange> ranges = getSheetNamedSelections(context, sheetURI);
        for (RaptureSheetRange range : ranges) {
            sb.append(String.format("range = %s;\n", JacksonUtil.jsonFromObject(range)));
            sb.append(String.format("#sheet.createSheetNamedSelection(SHEET_URI, '%s', range);\n", range.getName()));
        }
        // Setup scripts
        List<RaptureSheetScript> scripts = getAllScripts(context, sheetURI);
        for (RaptureSheetScript script : scripts) {
            sb.append(String.format("scriptContent='%s';\n", StringUtil.base64Compress(script.getScript())));
            script.setScript("");
            sb.append(String.format("script=%s;\n", JacksonUtil.jsonFromObject(script)));
            sb.append("script['script']=bdecompress(scriptContent);\n");
            sb.append(String.format("#sheet.createScript(SHEET_URI, '%s', script);\n", script.getName()));
        }
        return sb.toString();
    }

    @Override
    public List<RaptureSheetNote> getSheetNotes(CallingContext context, String sheetURI) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.getSheetNotes(parsedURI.getDocPath());

    }

    @Override
    public void deleteSheetNote(CallingContext context, String sheetURI, String noteId) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.deleteSheetNote(parsedURI.getDocPath(), noteId);
    }

    @Override
    public RaptureSheetNote createSheetNote(CallingContext context, String sheetURI, RaptureSheetNote note) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        note.setWho(context.getUser());
        note.setWhen(new Date());
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        return repo.putSheetNote(parsedURI.getDocPath(), note);
    }

    @Override
    public void deleteSheetColumn(CallingContext context, String sheetURI, int column) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        getRepoFromCache(parsedURI.getAuthority()).deleteSheetColumn(parsedURI.getDocPath(), column);
        // Now move ranges
        List<RaptureSheetRange> ranges = getSheetNamedSelections(context, sheetURI);
        for (RaptureSheetRange range : ranges) {
            if (range.getStartColumn() > column) {
                range.setStartColumn(range.getStartColumn() - 1);
                createSheetNamedSelection(context, sheetURI, range.getName(), range);
            }
        }
    }

    @Override
    public void deleteSheetRow(CallingContext context, String sheetURI, int row) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        getRepoFromCache(parsedURI.getAuthority()).deleteSheetRow(parsedURI.getDocPath(), row);
        // Now move ranges
        List<RaptureSheetRange> ranges = getSheetNamedSelections(context, sheetURI);
        for (RaptureSheetRange range : ranges) {
            if (range.getStartRow() > row) {
                range.setStartRow(range.getStartRow() - 1);
                createSheetNamedSelection(context, sheetURI, range.getName(), range);
            }
        }
    }

    @Override
    public void deleteSheetCell(CallingContext context, String sheetURI, int row, int column, int tabId) {
        RaptureURI parsedURI = new RaptureURI(sheetURI, Scheme.SHEET);
        SheetRepo repo = getRepoFromCache(parsedURI.getAuthority());
        repo.deleteSheetCell(parsedURI.getDocPath(), row, column, tabId);
    }

    @Override
    public List<SheetRepoConfig> getSheetRepoConfigs(CallingContext context) {
        return SheetRepoConfigStorage.readAll();
    }

    @Override
    public void deleteSheetRepo(CallingContext context, String repoURI) {
        RaptureURI uri = new RaptureURI(repoURI, Scheme.SHEET);
        try {
            SheetRepo store = getRepoFromCache(uri.getAuthority());
            if (store != null) store.drop();
        } catch (Exception e) {
            log.debug(e.getMessage());
        }

        SheetRepoConfigStorage.deleteByAddress(uri, context.getUser(), "Remove sheet repo");
        removeRepoFromCache(uri.getAuthority());
    }

    @Override
    public Boolean sheetRepoExists(CallingContext context, String repoURI) {
        return (getSheetRepoConfig(context, repoURI) != null);
    }

    /**
     * Return an expanded view of the sheet, filling in the blanks and setting up the formatting as well
     */
    @Override
    public SheetAndMeta getSheetAndMeta(CallingContext context, String sheetURI) {
        List<RaptureSheetCell> dataCells = findCellsByEpoch(context, sheetURI, 0, 0L);
        List<RaptureSheetCell> formatCells = findCellsByEpoch(context, sheetURI, 1, 0L);
        SheetAndMeta form = new SheetAndMeta();
        form.setStyles(getAllStyles(context, sheetURI));
        Map<String, RaptureSheetStyle> styleMap = new HashMap<String, RaptureSheetStyle>();
        for (RaptureSheetStyle s : form.getStyles()) {
            styleMap.put(s.getName(), s);
        }
        int maxRow = 0;
        int maxColumn = 0;
        Table<Integer, Integer, RaptureSheetDisplayCell> table = TreeBasedTable.create();

        for (RaptureSheetCell cell : dataCells) {
            if (cell.getRow() > maxRow) {
                maxRow = cell.getRow();
            }
            if (cell.getColumn() > maxColumn) {
                maxColumn = cell.getColumn();
            }
            RaptureSheetDisplayCell c = table.get(cell.getRow(), cell.getColumn());
            if (c == null) {
                c = new RaptureSheetDisplayCell();
                table.put(cell.getRow(), cell.getColumn(), c);
            }
            c.setData(cell.getData());
            c.setStyle("");
        }
        // Now apply formats
        for (RaptureSheetCell cell : formatCells) {
            RaptureSheetDisplayCell c = table.get(cell.getRow(), cell.getColumn());
            if (c != null) {
                c.setStyle(cell.getData());
            }
        }

        // Now relay out the form cells
        List<List<RaptureSheetDisplayCell>> dcells = new ArrayList<List<RaptureSheetDisplayCell>>();
        for (int row = 0; row <= maxRow; row++) {
            List<RaptureSheetDisplayCell> rowCells = new ArrayList<RaptureSheetDisplayCell>();
            for (int column = 0; column <= maxColumn; column++) {
                RaptureSheetDisplayCell c = table.get(row, column);
                if (c == null) {
                    c = new RaptureSheetDisplayCell();
                    c.setData("");
                    c.setStyle("");
                }
                if (!c.getStyle().isEmpty()) {
                    if (styleMap.containsKey(c.getStyle())) {
                        String numberFormat = styleMap.get(c.getStyle()).getNumberFormatString();
                        if (!numberFormat.isEmpty()) {
                            Double value = Double.parseDouble(c.getData());
                            NumberFormat nf = new DecimalFormat(numberFormat);
                            c.setData(nf.format(value));
                        }
                    }
                }
                rowCells.add(c);
            }
            dcells.add(rowCells);
        }
        // Now do any number formatting if specified in the style
        form.setCells(dcells);
        return form;
    }

    @Override
    public void exportSheetAsPdf(CallingContext context, String sheetURI, String blobURI) {
        PDFSheetRenderer renderer = new PDFSheetRenderer(sheetURI, blobURI);
        KernelScript api = new KernelScript();
        api.setCallingContext(context);
        try {
            renderer.renderAndSave(api);
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private SheetRepo getRepoFromCache(String authority) {
        return Kernel.getRepoCacheManager().getSheetRepo(authority);
    }

    private void removeRepoFromCache(String authority) {
        Kernel.getRepoCacheManager().removeRepo(Scheme.SHEET.toString(), authority);
    }
    
    @Override
    public Map<String, RaptureFolderInfo> listSheetsByUriPrefix(CallingContext context, String uriPrefix, int depth) {
        RaptureURI internalUri = new RaptureURI(uriPrefix, SHEET);
        String authority = internalUri.getAuthority();
        Map<String, RaptureFolderInfo> ret = new HashMap<String, RaptureFolderInfo>();
        
        // Schema level is special case.
        if (authority.isEmpty()) {
            --depth;
            try {
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                ContextValidator.validateContext(context, EntitlementSet.Sheet_getSheetRepoConfigs, requestObj); 

                List<SheetRepoConfig> configs = getSheetRepoConfigs(context);
                for (SheetRepoConfig config : configs) {
                     authority = config.getAuthority();
                     // NULL or empty string should not exist. 
                     if ((authority == null) || authority.isEmpty()) {
                         log.warn("Invalid authority (null or empty string) found for "+JacksonUtil.jsonFromObject(config));
                         continue;
                     }
                     String uri = SHEET+"://"+authority;
                     ret.put(uri, new RaptureFolderInfo(authority, true));
                     if (depth != 0) {
                         ret.putAll(listSheetsByUriPrefix(context, uri, depth));
                     }
                }

            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission for "+uriPrefix);
            }
            return ret;
        }

        SheetRepo repo = getRepoFromCache(internalUri.getAuthority());
        Boolean getAll = false;
        if (repo == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        String parentDocPath = internalUri.getDocPath() == null ? "" : internalUri.getDocPath();
        int startDepth = StringUtils.countMatches(parentDocPath, "/");

        if (log.isDebugEnabled()) {
            log.debug("Loading all children from repo " + internalUri.getAuthority() + " with " + internalUri.getDocPath());
        }
        if (depth <= 0) getAll = true;

        Stack<String> parentsStack = new Stack<String>();
        parentsStack.push(parentDocPath);

        while (!parentsStack.isEmpty()) {
            String currParentDocPath = parentsStack.pop();
            int currDepth = StringUtils.countMatches(currParentDocPath, "/") - startDepth;
            if (!getAll && currDepth >= depth) continue;
            boolean top = currParentDocPath.isEmpty();
            // Make sure that you have permission to read the folder.
            try {
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                requestObj.setDocUri(currParentDocPath);
                ContextValidator.validateContext(context, EntitlementSet.Sheet_getSheetCell, requestObj); 
    
                List<RaptureFolderInfo> children = repo.listSheetByUriPrefix(currParentDocPath);
    
                for (RaptureFolderInfo child : children) {
                    String childDocPath = currParentDocPath + (top ? "" : "/") + child.getName();
                    if (child.getName().isEmpty()) continue;
                    String childUri = Scheme.SHEET+"://" + authority + "/" + childDocPath + (child.isFolder() ? "/" : "");
                    ret.put(childUri, child);
                    if (child.isFolder()) {
                        parentsStack.push(childDocPath);
                    }
                }
            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission on folder "+currParentDocPath);
            }
            if (top) startDepth--; // special case
        }
        return ret;
    }

    @Override
    public List<String> deleteSheetsByUriPrefix(CallingContext context, String uriPrefix) {
        RaptureURI internalUri = new RaptureURI(uriPrefix, SHEET);
        String authority = internalUri.getAuthority();
        List<String> ret = new ArrayList<String>();

        // Schema level is special case.
        if (authority.isEmpty()) {
            try {
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                ContextValidator.validateContext(context, EntitlementSet.Sheet_getSheetRepoConfigs, requestObj);

                List<SheetRepoConfig> configs = getSheetRepoConfigs(context);
                for (SheetRepoConfig config : configs) {
                    authority = config.getAuthority();
                    // NULL or empty string should not exist.
                    if ((authority == null) || authority.isEmpty()) {
                        log.warn("Invalid authority (null or empty string) found for "+JacksonUtil.jsonFromObject(config));
                        continue;
                    }
                    String uri = SHEET+"://"+authority;
                    deleteSheetRepo(context, uri);
                    ret.add(uri);
                }

            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission for "+uriPrefix);
            }
            return ret;
        }

        SheetRepo repo = getRepoFromCache(internalUri.getAuthority());
        if (repo == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        String parentDocPath = internalUri.getDocPath() == null ? "" : internalUri.getDocPath();

        if (log.isDebugEnabled()) {
            log.debug("Loading all children from repo " + internalUri.getAuthority() + " with " + internalUri.getDocPath());
        }

        Stack<String> parentsStack = new Stack<String>();
        parentsStack.push(parentDocPath);

        while (!parentsStack.isEmpty()) {
            String currParentDocPath = parentsStack.pop();
            boolean top = currParentDocPath.isEmpty();
            // Make sure that you have permission to read the folder.
            try {
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                requestObj.setDocUri(currParentDocPath);
                ContextValidator.validateContext(context, EntitlementSet.Sheet_getSheetCell, requestObj);

                List<RaptureFolderInfo> children = repo.listSheetByUriPrefix(currParentDocPath);

                for (RaptureFolderInfo child : children) {
                    String childDocPath = currParentDocPath + (top ? "" : "/") + child.getName();
                    if (child.getName().isEmpty()) continue;
                    String childUri = Scheme.SHEET+"://" + authority + "/" + childDocPath + (child.isFolder() ? "/" : "");
                    deleteSheet(context, childUri);
                    ret.add(childUri);
                    if (child.isFolder()) {
                        parentsStack.push(childDocPath);
                    }
                }

                deleteSheet(context, Scheme.SHEET+"://" + authority + "/" + parentDocPath);
            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission on folder "+currParentDocPath);
            }
        }
        return ret;
    }

}
