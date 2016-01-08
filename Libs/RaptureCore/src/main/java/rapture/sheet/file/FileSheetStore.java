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
package rapture.sheet.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureSheet;
import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetNote;
import rapture.common.RaptureSheetRange;
import rapture.common.RaptureSheetScript;
import rapture.common.RaptureSheetStyle;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.file.FileRepoUtils;
import rapture.sheet.SheetStore;

import com.google.common.collect.ImmutableList;

/**
 * A FileSheetStore is a simple directory/file implementation of a SheetStore.
 *  * 
 * @author dtong
 * 
 */
public class FileSheetStore implements SheetStore {

    private static Logger log = Logger.getLogger(FileSheetStore.class);
    private String authority;
    private Map<String, FileSheetStatus> sheetStatus = new HashMap<String, FileSheetStatus>();

    public static final String PREFIX = "prefix";
    private File parentDir = null;
    private FileMetaSheetStore metaStore;
    private FileSheetStatus nullSheetStatus = new FileSheetStatus();

    @Override
    public void setConfig(String authority, Map<String, String> config) {
        // What happens if this is called twice?
        if (parentDir != null) throw RaptureExceptionFactory.create("Calling setConfig twice is currently not supported");
        this.authority = authority;
        String prefix = config.get(FileRepoUtils.PREFIX);
        if (StringUtils.trimToNull(prefix) == null) throw RaptureExceptionFactory.create("prefix must be specified");
        parentDir = FileRepoUtils.ensureDirectory(prefix + "_sheet");
        metaStore = new FileMetaSheetStore();
        metaStore.setConfig(config);
    }

    /**
     * Epoch number implmentation is version dependent. Mongo generates it automatically; We have to set our own.
     */
    @Override
    public void setCell(String sheetName, int row, int column, String string, int dimension) {
        FileSheetStatus sheet = getSheetStatus(sheetName, 1L);
        if (sheet != this.nullSheetStatus) {
            long epoch = System.currentTimeMillis();
            RaptureSheetCell cell = new RaptureSheetCell(row, column, string, epoch);
            sheet.addCell(cell);
            sheet.setEpoch(epoch);
            writeSheet(sheetName, ImmutableList.of(cell), true);
        }
    }

    /**
     * Get from cache if possible
     * 
     * @param sheetName
     * @return
     */
    private FileSheetStatus getSheetStatus(String sheetName, Long epoch) {
        if (sheetStatus.containsKey(sheetName)) {
            FileSheetStatus ret = sheetStatus.get(sheetName);
            if (ret == null) {
                return nullSheetStatus;
            }
            return ret;
        }
        // Attempt to load
        return loadSheet(sheetName, epoch);
    }

    @Override
    public Boolean sheetExists(String docPath) {
        File sheetFile = FileRepoUtils.makeGenericFile(parentDir, docPath);
        return sheetFile.exists() && sheetFile.isFile();
    }
    
    /**
     * Load directly, do not try to read from cache.
     * @param sheetName
     * @return
     */
    private FileSheetStatus loadSheet(String sheetName, long epoch) {
        File sheetFile = FileRepoUtils.makeGenericFile(parentDir, sheetName);
        if (!sheetFile.exists() || !sheetFile.isFile()) return nullSheetStatus;
        try {
            List<String> lines = Files.readAllLines(sheetFile.toPath(), StandardCharsets.UTF_8);
            FileSheetStatus status = new FileSheetStatus();
            for (String line : lines) {
                String splitChar = line.substring(0, 1);
                String[] fields = line.substring(1).split(splitChar);
                
                RaptureSheetCell cell = new RaptureSheetCell();
                long tmpEpoch = Long.parseLong(fields[2]);
                if (tmpEpoch >= epoch) {
                    cell.setRow(Integer.parseInt(fields[0]));
                    cell.setColumn(Integer.parseInt(fields[1]));
                    cell.setEpoch(tmpEpoch);
                    cell.setData(fields[3]);
                    status.addCell(cell);
                }
            }
            sheetStatus.put(sheetName, status);
            return status;
        } catch (IOException | NumberFormatException e) {
            log.info("Cannot load sheet "+sheetName, e);
            log.debug(ExceptionToString.format(e));
        }
        return nullSheetStatus;
    }

    @Override
    public String getCell(String sheetName, int row, int column, int dimension) {
        // What is dimension? 
        FileSheetStatus status = getSheetStatus(sheetName, 1L);
        for (RaptureSheetCell cell : status.getCells()) {
            if ((cell.getRow() == row) && (cell.getColumn() == column)) {
                return cell.getData();
            }
        }
        return null;
    }

    @Override
    public RaptureSheet createSheet(String name) {
        // create an empty sheet
        writeSheet(name, new ArrayList<RaptureSheetCell>(0), false);
        RaptureSheet sheet = new RaptureSheet();
        sheet.setName(name);
        sheet.setAuthority(authority);
        return sheet;
    }

    private void writeSheet(String name, Collection<RaptureSheetCell> cells, boolean append) {
        File sheetFile = FileRepoUtils.makeGenericFile(parentDir, name);
        if (sheetFile.exists() && !sheetFile.isFile()) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot write to sheet "+sheetFile.getName() + " - not a file");
        try {
            sheetFile.getParentFile().mkdirs();
            try (PrintStream out = new PrintStream(new FileOutputStream(sheetFile, append))) {
                appendCells(out, cells);
            }
        } catch (SecurityException | IOException e) {
            // Very unlikely
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot write to sheet "+sheetFile.getName(), e);
        }
        return;
    }

    private void appendCells(PrintStream stream, Collection<RaptureSheetCell> cells) {
        for (RaptureSheetCell cell : cells) {
            char i = 32;
            String data = cell.getData();
            if (data == null) continue; // cell has been deleted
            // Find a delimeter that is available.
            // NOTE: if negative column numbers are allowed then we can't use minus sign
            while ((data.indexOf(i) >= 0) || Character.isDigit(i) || (i == '-')) i++;
            stream.append(i);
            stream.append((""+cell.getRow()));
            stream.append(i);
            stream.append((""+cell.getColumn()));
            stream.append(i);
            stream.append((""+cell.getEpoch()));
            stream.append(i);
            stream.append(data.replaceAll("\n", ""));   // newline not allowed
            stream.append('\n');
        }
    }

    @Override
    public RaptureSheet deleteSheet(String name) {
        File sheetFile = FileRepoUtils.makeGenericFile(parentDir, name);
        if (!sheetFile.exists() || !sheetFile.isFile()) return null;
        List<RaptureSheetNote> notelist = metaStore.getSheetNotes(name);
        for (RaptureSheetNote note : notelist) {
            metaStore.deleteSheetNote(name, note.getId());
        }
        sheetFile.delete();

        // Serves the purpose of DefaultFolderCleanupService for mongo implementation
        File parent = new File(sheetFile.getParent());
        if (parent.list().length == 0){
            parent.delete();
        }

        RaptureSheet sheet = new RaptureSheet();
        sheet.setName(name);
        sheet.setAuthority(authority);
        return sheet;
    }

    @Override
    public List<RaptureFolderInfo> listSheetByUriPrefix(String displayNamePart) {
        File file = FileRepoUtils.makeGenericFile(parentDir, displayNamePart);
        File[] children = file.listFiles();
        List<RaptureFolderInfo> info = new ArrayList<>((children == null) ? 0 : children.length);
        if ((children != null) && (children.length > 0)) {
            for (File kid : children) {
                RaptureFolderInfo inf = new RaptureFolderInfo();
                inf.setName(kid.getName());
                inf.setFolder(kid.isDirectory());
                info.add(inf);
            }
        }
        return info;
    }

    @Override
    public List<RaptureSheetCell> findCellsByEpoch(String name, int dimension, long epoch) {
        return getSheetStatus(name, epoch).getCells();
    }

    @Override
    public void cloneSheet(String srcName, String targetName) {
        FileSheetStatus sheet = getSheetStatus(srcName, 1L);
        writeSheet(targetName, sheet.getCells(), false);
    }

    @Override
    public Boolean deleteSheetColumn(String docPath, int column) {
        FileSheetStatus sheet = getSheetStatus(docPath, 1L);
        long epoch = System.currentTimeMillis();
        sheet.setEpoch(epoch);
        for (RaptureSheetCell cell : sheet.getCells()) {
            if (cell.getColumn() == column) cell.setData(null);
        }
        writeSheet(docPath, sheet.getCells(), false);
        return true;
    }

    @Override
    public Boolean deleteSheetRow(String docPath, int row) {
        FileSheetStatus sheet = getSheetStatus(docPath, 1L);
        long epoch = System.currentTimeMillis();
        sheet.setEpoch(epoch);
        for (RaptureSheetCell cell : sheet.getCells()) {
            if (cell.getRow() == row) cell.setData(null);
        }
        writeSheet(docPath, sheet.getCells(), false);
        return true;
    }

    @Override
    public Boolean deleteSheetCell(String docPath, int row, int column, int dimension) {
        FileSheetStatus sheet = getSheetStatus(docPath, 1L);
        long epoch = System.currentTimeMillis();
        sheet.setEpoch(epoch);
        RaptureSheetCell cell = new RaptureSheetCell();
        cell.setColumn(column);
        cell.setRow(row);
        cell.setEpoch(epoch);
        cell.setData(null);
        sheet.addCell(cell);
        writeSheet(docPath, sheet.getCells(), false);
        return true;
    }

    @Override
    public Boolean setBlock(String docPath, int startRow, int startColumn, List<String> values, int width, int height,
            int dimension) {
        long epoch = System.currentTimeMillis();
        int currentRow = startRow;
        int currentColumn = startColumn;
        int columnCount = 0;
        // We can append the cells to the file. Don't need to rewrite all of them.
        Collection<RaptureSheetCell> cells = new ArrayList<>();
        FileSheetStatus sheet = getSheetStatus(docPath, 1L);
        sheet.setEpoch(epoch);
        for (String value : values) {
            RaptureSheetCell cell = new RaptureSheetCell();
            cell.setColumn(currentColumn);
            cell.setRow(currentRow);
            cell.setEpoch(epoch);
            cell.setData((value == null) ? "" : value);
            
            cells.add(cell);
            sheet.addCell(cell);
            currentColumn++;
            columnCount++;
            if (columnCount >= width) {
                currentRow++;
                currentColumn = startColumn;
                columnCount = 0;
            }
        }
        writeSheet(docPath, cells, true);
        return true;
    }

    @Override
    public void drop() {
        if (parentDir != null) {
            try {
                FileUtils.deleteDirectory(parentDir);
                parentDir = null;
            } catch (IOException e) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot drop sheet store", e);
            }
        }
    }

    @Override
    public List<RaptureSheetStyle> getSheetStyles(String name) {
        return metaStore.getSheetStyles(name);
    }

    @Override
    public Boolean deleteSheetStyle(String name, String styleName) {
       return metaStore.deleteSheetStyle(name, styleName);
    }

    @Override
    public RaptureSheetStyle putSheetStyle(String name, RaptureSheetStyle style) {
        return metaStore.putSheetStyle(name, style);
    }

    @Override
    public List<RaptureSheetScript> getSheetScripts(String name) {
        return metaStore.getSheetScripts(name);
    }

    @Override
    public Boolean deleteSheetScript(String name, String scriptName) {
       return metaStore.deleteSheetScript(name, scriptName);
    }

    @Override
    public RaptureSheetScript putSheetScript(String name, String scriptName, RaptureSheetScript script) {
       return metaStore.putSheetScript(name, scriptName, script);
    }

    @Override
    public List<RaptureSheetRange> getSheetNamedSelections(String name) {
        return metaStore.getSheetNamedSelections(name);
    }

    @Override
    public Boolean deleteSheetNamedSelection(String name, String rangeName) {
       return metaStore.deleteSheetNamedSelection(name, rangeName);
    }

    @Override
    public RaptureSheetRange putSheetNamedSelection(String name, String rangeName, RaptureSheetRange range) {
       return metaStore.putSheetNamedSelection(name, rangeName, range);
    }

    @Override
    public List<RaptureSheetNote> getSheetNotes(String name) {
        return metaStore.getSheetNotes(name);
    }

    @Override
    public Boolean deleteSheetNote(String name, String noteId) {
        return metaStore.deleteSheetNote(name, noteId);
    }

    @Override
    public RaptureSheetNote putSheetNote(String name, RaptureSheetNote note) {
       return metaStore.putSheetNote(name, note);
    }

    @Override
    public RaptureSheetScript getSheetScript(String name, String scriptName) {
        return metaStore.getSheetScript(name, scriptName);
    }

    @Override
    public RaptureSheetRange getSheetNamedSelection(String name, String rangeName) {
        return metaStore.getSheetNamedSelection(name, rangeName);
    }

}
