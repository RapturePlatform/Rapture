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
package rapture.sheet.mongodb;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import rapture.common.*;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.repo.mongodb.MongoDbDataStore;
import rapture.sheet.SheetStore;

/**
 * A MongoDB sheet also uses a regular MongoRepo to handle names and simple storage of general sheet config
 * and then a separate class implementation for the sheet data.
 * 
 * @author amkimian
 *
 */
public class MongoDBSheet implements SheetStore {
    private MongoDbDataStore generalStore;
    private MongoCellStore cellStore;
    private MongoMetaSheetStore metaStore;
    private String authority;
//    private String storeName; // Never used
    private static final String DEFAULT_PREFIX_VALUE = "__sheet__";
    private static final String PREFIX = MongoDbDataStore.PREFIX;

    @Override
    public void setConfig(String authority, Map<String, String> config) {
        if (!config.containsKey(PREFIX)) {
            if(config.isEmpty()) {
                config.put(PREFIX, DEFAULT_PREFIX_VALUE + authority);
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Config contains keys other than 'prefix': " + config.keySet());
            }
        }
        generalStore = new MongoDbDataStore();
        generalStore.setConfig(config);
        cellStore = new MongoCellStore();
        cellStore.setConfig(config);
        metaStore = new MongoMetaSheetStore();
        metaStore.setConfig(config);
    }

    @Override
    public void drop() {
        generalStore.dropKeyStore();
        cellStore.drop();
        metaStore.drop();
    }

    @Override
    public void setCell(String sheetName, int row, int column, String value, int dimension) {
        cellStore.setCell(sheetName, row, column, value, dimension);
    }

    @Override
    public Boolean setBlock(String sheetName, int startRow, int startColumn, List<String> values, int width, int height,
            int dimension) {
        return cellStore.setBulkCell(sheetName, startRow, startColumn, values, width, height, dimension);
   }

    @Override
    public String getCell(String sheetName, int row, int column, int dimension) {
       return cellStore.getCell(sheetName, row, column, dimension);
    }


    @Override
    public RaptureSheet createSheet(String name) {
        RaptureSheet sheet = new RaptureSheet();
        sheet.setName(name);
        sheet.setAuthority(authority);
//        sheet.setSheetStore(storeName);
        generalStore.put(name, JacksonUtil.jsonFromObject(sheet));
        return sheet;
    }

    @Override
    public RaptureSheet deleteSheet(String name) {
        String jsonObject = generalStore.get(name);
        if (jsonObject != null) {
            RaptureSheet sheet = JacksonUtil.objectFromJson(jsonObject, RaptureSheet.class);
            generalStore.delete(name);
            cellStore.removeAll(name);
            metaStore.removeAll(name);
            return sheet;
        }
        RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Sheet does not exist, cannot delete");
        throw raptException;
    }

    @Override
    public List<RaptureFolderInfo> listSheetByUriPrefix(String displayNamePart) {
        return generalStore.getSubKeys(displayNamePart);
    }

    @Override
    public List<RaptureSheetCell> findCellsByEpoch(String name, int dimension, long epoch) {
        return cellStore.findCellsByEpoch(name, dimension, epoch);
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
    public void cloneSheet(String srcName, String targetName) {
        // A clone of a sheet needs to clone the cellStore and metaStore and then create a new RaptureSheet
        // in the general Store
        metaStore.cloneSheet(srcName, targetName);
        cellStore.cloneSheet(srcName, targetName);
        RaptureSheet sheet = new RaptureSheet();
        sheet.setName(targetName);
        sheet.setAuthority(authority);
//        sheet.setSheetStore(storeName);
        generalStore.put(targetName, JacksonUtil.jsonFromObject(sheet));
    }

    @Override
    public RaptureSheetRange getSheetNamedSelection(String name, String rangeName) {
       return metaStore.getSheetNamedSelection(name, rangeName);
    }

    @Override
    public Boolean deleteSheetColumn(String docPath, int column) {
        cellStore.deleteColumn(docPath, column);
        return true;
    }

    @Override
    public Boolean deleteSheetRow(String docPath, int row) {
        cellStore.deleteRow(docPath, row);
        return true;
    }

    @Override
    public Boolean deleteSheetCell(String docPath, int row, int column, int dimension) {
        return cellStore.deleteCell(docPath, row, column, dimension);
    }

    @Override
    public Boolean sheetExists(String docPath) {
        return generalStore.containsKey(docPath);
    }




}
