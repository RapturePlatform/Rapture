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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetStatus;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * The MongoCellStore stores cells for sheet. Each cell is a document in the
 * repo. A given sheet also has some helper document(s) that provide meta
 * information such as the maximum row and column of the sheet.
 * 
 * @author alan
 * 
 */
public class MongoCellStore {
    private static Logger log = Logger.getLogger(MongoCellStore.class);
    private static final String PREFIX = "prefix";
    private static final String VALUE = "v";
    private static final String KEY = "key";
    private static final String ROW = "r";
    private static final String COL = "c";
    private static final String DIM = "d";
    private static final String EPOCH = "e";
    private String tableName;
    private String instanceName = "default";

    public MongoCellStore() {
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setConfig(Map<String, String> config) {
        tableName = config.get(PREFIX) + "_cells";
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.ensureIndex(KEY);
        collection.ensureIndex(ROW);
        collection.ensureIndex(COL);
    }

    public void drop() {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.drop();
        collection.dropIndex(KEY);
        collection.dropIndex(ROW);
        collection.dropIndex(COL);
    }

    public void setCell(String sheetName, int row, int column, String value, int dimension) {
        long epoch = getLatestEpoch(sheetName, dimension) + 1L;
        putCell(sheetName, row, column, value, dimension, epoch);
    }

    public Boolean setBulkCell(String sheetName, int startRow, int startColumn, List<List<String>> values, int dimension) {
        long epoch = getLatestEpoch(sheetName, dimension) + 1L;
        int currentRow = startRow;
        int currentColumn = startColumn;
        for (List<String> row : values) {
            for (String v : row) {
                putCell(sheetName, currentRow, currentColumn, v, dimension, epoch);
                currentColumn++;
            }
            currentRow++;
            currentColumn = startColumn;
        }
        return true;
    }

    public Boolean setBulkCell(String sheetName, int startRow, int startColumn, List<String> values, int height, int width, int dimension) {
        long epoch = getLatestEpoch(sheetName, dimension) + 1L;
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        List<DBObject> toWrite = new ArrayList<DBObject>();

        int currentRow = startRow;
        int currentColumn = startColumn;
        int columnCount = 0;
        for (String val : values) {
            if (val != null) {
                BasicDBObject toPut = new BasicDBObject();
                toPut.put(KEY, sheetName);
                toPut.put(ROW, currentRow);
                toPut.put(COL, currentColumn);
                toPut.put(DIM, dimension);
                toPut.put(VALUE, val);
                toPut.put(EPOCH, epoch);
                toWrite.add(toPut);
            }
            currentColumn++;
            columnCount++;
            if (columnCount >= width) {
                currentRow++;
                currentColumn = startColumn;
                columnCount = 0;
            }
        }
        collection.insert(toWrite);
        return true;
    }

    private void putCell(String sheetName, int row, int column, String value, int dimension, long epoch) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, sheetName);
        query.put(ROW, row);
        query.put(COL, column);
        query.put(DIM, dimension);

        BasicDBObject toPut = new BasicDBObject();
        toPut.put(KEY, sheetName);
        toPut.put(ROW, row);
        toPut.put(COL, column);
        toPut.put(DIM, dimension);
        toPut.put(VALUE, value);
        toPut.put(EPOCH, epoch);
        collection.findAndModify(query, null, null, false, toPut, false, true);
    }

    public String getCell(String sheetName, int row, int column, int dimension) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, sheetName);
        query.put(ROW, row);
        query.put(COL, column);
        query.put(DIM, dimension);
        BasicDBObject obj = (BasicDBObject) collection.findOne(query);
        if (obj != null) {
            Object v = obj.get(VALUE);
            return v.toString();
        }
        return null;
    }

    public List<RaptureSheetCell> findCellsByEpoch(final String sheetName, final int dimension, final long minEpochFilter) {

        MongoRetryWrapper<List<RaptureSheetCell>> wrapper = new MongoRetryWrapper<List<RaptureSheetCell>>() {

            public DBCursor makeCursor() {
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject query = new BasicDBObject();
                query.put(KEY, sheetName);
                query.put(DIM, dimension);
                return collection.find(query);
            }

            public List<RaptureSheetCell> action(DBCursor cursor) {
                List<RaptureSheetCell> ret = new ArrayList<RaptureSheetCell>();
                Long maxEpoch = 0L;

                while (cursor.hasNext()) {
                    BasicDBObject val = (BasicDBObject) cursor.next();
                    RaptureSheetCell cell = new RaptureSheetCell();
                    cell.setColumn(val.getInt(COL));
                    cell.setRow(val.getInt(ROW));
                    cell.setData(val.getString(VALUE));
                    boolean shouldAdd;
                    if (val.containsField(EPOCH)) {
                        long currEpoch = val.getLong(EPOCH);
                        shouldAdd = (currEpoch > minEpochFilter);
                        cell.setEpoch(currEpoch);
                        if (maxEpoch < currEpoch) {
                            maxEpoch = currEpoch;
                        }
                    } else {
                        shouldAdd = true;
                    }

                    if (shouldAdd) ret.add(cell);
                }
                return ret;
            }
        };
        return wrapper.doAction();
    }

    private Long knownEpoch = 0L;

    private Long getLatestEpoch(final String sheetName, final int dimension) {
        MongoRetryWrapper<Long> wrapper = new MongoRetryWrapper<Long>() {

            public DBCursor makeCursor() {
                BasicDBObject query = new BasicDBObject();
                query.put(KEY, sheetName);
                query.put(DIM, dimension);
                BasicDBObject gt = new BasicDBObject();
                gt.put("$gt", knownEpoch);
                query.put(EPOCH, gt);
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject vals = new BasicDBObject();
                vals.put(EPOCH, 1);
                return collection.find(query, vals);
            }

            public Long action(DBCursor cursor) {
                while (cursor.hasNext()) {
                    BasicDBObject val = (BasicDBObject) cursor.next();
                    long thisEpoch = val.getLong(EPOCH);
                    if (thisEpoch > knownEpoch) {
                        knownEpoch = thisEpoch;
                        log.debug(String.format("Epoch for %s(%d) is %d", sheetName, dimension, knownEpoch));
                    }
                }
                return knownEpoch;
            }
        };
        return wrapper.doAction();
    }

    public void cloneSheet(final String srcName, final String targetName) {
        final DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            public DBCursor makeCursor() {
                // Everything with the KEY being the srcName, modify the source
                // name to
                // targetName and store it back
                BasicDBObject query = new BasicDBObject();
                query.append(KEY, srcName);
                return collection.find(query);
            }

            public Object action(DBCursor cursor) {
                while (cursor.hasNext()) {
                    BasicDBObject object = (BasicDBObject) cursor.next();
                    object.put(KEY, targetName);
                    object.remove("_id");
                    collection.insert(object);
                }
                return null;
            }
        };
        @SuppressWarnings("unused")
        Object o = wrapper.doAction();
    }

    public void deleteColumn(final String sheetName, final int column) {
        final DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            public DBCursor makeCursor() {

                // Deleting a column is two things.
                // (a) find and remove all cells (in all dimensions) that have
                // this as a
                // column
                // (b) modify all cells that have a column > this column,
                // decreasing
                // their column by 1
                BasicDBObject query = new BasicDBObject();
                query.put(KEY, sheetName);
                query.put(COL, column);
                collection.findAndRemove(query);

                BasicDBObject changeQuery = new BasicDBObject();
                changeQuery.put(KEY, sheetName);
                BasicDBObject testQuery = new BasicDBObject();
                testQuery.put("$gt", column);
                changeQuery.put(COL, testQuery);

                return collection.find(changeQuery);
            }

            public Object action(DBCursor cursor) {
                while (cursor.hasNext()) {
                    BasicDBObject object = (BasicDBObject) cursor.next();
                    object.put(COL, object.getInt(COL) - 1);
                    object.put(EPOCH, getLatestEpoch(sheetName, object.getInt(DIM)));
                    collection.save(object);
                }

                return null;
            }
        };
        @SuppressWarnings("unused")
        Object o = wrapper.doAction();
    }

    public void deleteRow(final String sheetName, final int row) {
        final DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        MongoRetryWrapper<Object> wrapper = new MongoRetryWrapper<Object>() {

            public DBCursor makeCursor() {
                // Deleting a column is two things.
                // (a) find and remove all cells (in all dimensions) that have
                // this as a
                // column
                // (b) modify all cells that have a column > this column,
                // decreasing
                // their column by 1
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
                BasicDBObject query = new BasicDBObject();
                query.put(KEY, sheetName);
                query.put(ROW, row);
                collection.findAndRemove(query);

                BasicDBObject changeQuery = new BasicDBObject();
                changeQuery.put(KEY, sheetName);
                BasicDBObject testQuery = new BasicDBObject();
                testQuery.put("$gt", row);
                changeQuery.put(ROW, testQuery);
                return collection.find(changeQuery);
            }

            public Object action(DBCursor cursor) {

                while (cursor.hasNext()) {
                    BasicDBObject object = (BasicDBObject) cursor.next();
                    object.put(ROW, object.getInt(ROW) - 1);
                    object.put(EPOCH, getLatestEpoch(sheetName, object.getInt(DIM)));
                    collection.save(object);
                }

                return null;
            }
        };
        @SuppressWarnings("unused")
        Object o = wrapper.doAction();
    }

    public Boolean deleteCell(String sheetName, int row, int column, int dimension) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, sheetName);
        query.put(ROW, row);
        query.put(COL, column);
        query.put(DIM, dimension);
        collection.findAndRemove(query);
        return true;
    }

    public void removeAll(String sheetName) {
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, sheetName);
        collection.remove(query);
    }

}
