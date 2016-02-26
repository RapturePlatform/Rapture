/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
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
package rapture.table.mongodb;

import rapture.common.LockHandle;
import rapture.common.TableColumnSort;
import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.TableSelect;
import rapture.common.model.DocumentMetadata;
import rapture.dsl.idef.FieldDefinition;
import rapture.dsl.idef.IndexDefinition;
import rapture.dsl.iqry.IndexQuery;
import rapture.dsl.iqry.IndexQueryFactory;
import rapture.dsl.iqry.OrderDirection;
import rapture.dsl.iqry.WhereClause;
import rapture.dsl.iqry.WhereExtension;
import rapture.dsl.iqry.WhereStatement;
import rapture.dsl.iqry.WhereTest;
import rapture.index.IndexCreationLock;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.index.IndexRecord;
import rapture.mongodb.EpochManager;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;
import rapture.repo.meta.handler.AbstractMetaHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import rapture.config.MultiValueConfigLoader;

/**
 * @author amkimian
 */
public class MongoIndexHandler implements IndexHandler {
    private static Logger log = Logger.getLogger(MongoIndexHandler.class);
    private static final String PREFIX = "prefix";
    private static final String KEY = "key";
    private static final String EPOCH = "epoch";
    private String tableName;
    private String instanceName = "default";
    private IndexProducer indexProducer;

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        tableName = config.get(PREFIX);
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        collection.ensureIndex(KEY);
    }

    private String getKey(String rowId) {
        return AbstractMetaHandler.LATEST + "/" + rowId;
    }

    @Override
    public void deleteTable() {
        // drop it all
        MongoDBFactory.getDB(instanceName).getCollection(tableName).drop();
    }

    @Override
    public void removeAll(String rowId) {
        String key = getKey(rowId);
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, key);
        collection.findAndRemove(query);
    }

    @Override
    public void addedRecord(String key, String value, DocumentMetadata mdLatest) {
        List<IndexRecord> records = indexProducer.getIndexRecords(key, value, mdLatest);
        Map<String, Object> values = new HashMap<>();
        for (IndexRecord record : records) {
            values.putAll(record.getValues());
        }
        updateRow(key, values);
    }

    @Override
    public void updateRow(String rowId, Map<String, Object> recordValues) {
        String key = getKey(rowId); //stupid key is row id plus "l/" prepended to it

        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);
        BasicDBObject query = new BasicDBObject();
        query.put(KEY, key);
        BasicDBObject toPut = new BasicDBObject();
        toPut.put(KEY, key);
        toPut.put(ROWID, rowId);
        toPut.put(EPOCH, EpochManager.nextEpoch(collection));
        toPut.putAll(recordValues);
        collection.findAndModify(query, null, null, false, toPut, false, true);
    }

    @Override
    public Long getLatestEpoch() {
        return EpochManager.getLatestEpoch(MongoDBFactory.getDB(instanceName).getCollection(tableName));
    }

    @Override
    public void setIndexProducer(IndexProducer indexProducer) {
        this.indexProducer = indexProducer;
    }

    @Override
    public void initialize() {
        for (IndexDefinition indexDefinition : indexProducer.getIndexDefinitions()) {
            createIndex(indexDefinition, false);
        }
    }

    @Override
    public void ensureIndicesExist() {
        log.info(String.format("About to build indices for collection [%s]...", tableName));
        LockHandle lockHandle = null;
        try {
            lockHandle = IndexCreationLock.INSTANCE.grabLock();
            for (IndexDefinition indexDefinition : indexProducer.getIndexDefinitions()) {
                createIndex(indexDefinition, true);
            }
        } finally {
            if (lockHandle != null) {
                IndexCreationLock.INSTANCE.releaseLock(lockHandle);
            }
        }
        log.info(String.format("Done building indices for collection [%s]", tableName));
    }

    private void createIndex(IndexDefinition indexDefinition, boolean force) {
        //create index if not exists... use index name too from indexDefinition object
        if (indexDefinition == null) {
            return;
        }

        String indexName = IndexNameFactory.createIndexName(indexDefinition);
        DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);

        int limit = Integer.parseInt(MultiValueConfigLoader
                .getConfig("MONGODB-" + instanceName + ".limit", MultiValueConfigLoader.getConfig("MONGODB-default.limit", "250")));

        boolean indexExists = false;
        List<DBObject> indexInfo = collection.getIndexInfo();
        for (DBObject dbObject : indexInfo) {
            Object name = dbObject.get("name");
            if (name != null && name.toString().equals(indexName)) {
                indexExists = true;
                break;
            }
        }
        if (force || collection.count() < limit) {
            createIt(indexDefinition, force, indexName, collection);
        } else {
            if (!indexExists) {
                log.warn(tableName + " collection has more than " + limit + " items. please index manually");
            }
        }
    }

    private void createIt(IndexDefinition indexDefinition, boolean force, String indexName, DBCollection collection) {
        // bug in mongo driver: need to set ns explicitly
        String ns = collection.getDB() + "." + collection.getName();

        BasicDBObject index = new BasicDBObject();

        for (FieldDefinition f : indexDefinition.getFields()) {
            index.put(f.getName(), 1);
        }

        BasicDBObject options = new BasicDBObject("name", indexName).append("ns", ns);
        if (!force) { //if forcing, we want to lock while it builds
            options.append("background", true);
        }
        collection.createIndex(index, options);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TableRecord> queryTable(final TableQuery querySpec) {

        MongoRetryWrapper<List<TableRecord>> wrapper = new MongoRetryWrapper<List<TableRecord>>() {
            public DBCursor makeCursor() {
                DBCursor ret;
                DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);

                // Convert the query into that for a mongodb collection, then call find
                BasicDBObject query = EpochManager.getNotEqualEpochQueryObject();
                if (querySpec.getFieldTests() != null) {
                    for (TableSelect sel : querySpec.getFieldTests()) {
                        switch (sel.getOper()) {
                            case "=":
                                query.append(sel.getFieldName(), sel.getTestValue());
                                break;
                            case ">":
                                BasicDBObject gt = new BasicDBObject();
                                gt.append("$gt", sel.getTestValue());
                                query.append(sel.getFieldName(), gt);
                                break;
                            case "<":
                                BasicDBObject lt = new BasicDBObject();
                                lt.append("$lt", sel.getTestValue());
                                query.append(sel.getFieldName(), lt);
                                break;
                            case "LIKE":
                                query.append(sel.getFieldName(), java.util.regex.Pattern.compile(sel.getTestValue().toString()));
                                break;
                        }
                    }
                }

                // That's the query, now determine the return fields...
                BasicDBObject fields = new BasicDBObject();
                if (querySpec.getFieldReturns() != null) {
                    for (String fieldName : querySpec.getFieldReturns()) {
                        fields.put(fieldName, 1);
                    }
                }

                // Now we need to do the query, with a limit and skip applied if
                // necessary
                BasicDBObject sort = new BasicDBObject();
                if (querySpec.getSortFields() != null) {
                    for (TableColumnSort sortField : querySpec.getSortFields()) {
                        sort.put(sortField.getFieldName(), sortField.getAscending() ? 1 : -1);
                    }
                }

                if (fields.isEmpty()) {
                    ret = collection.find(query);
                } else {
                    fields.put(KEY, 1);
                    ret = collection.find(query, fields);
                }

                if (!sort.isEmpty()) {
                    ret = ret.sort(sort);
                }

                if (querySpec.getSkip() != 0) {
                    ret = ret.skip(querySpec.getSkip());
                }
                if (querySpec.getLimit() != 0) {
                    ret = ret.limit(querySpec.getLimit());
                }
                return ret;
            }

            public List<TableRecord> action(DBCursor cursor) {
                List<TableRecord> records = new ArrayList<TableRecord>();
                if (cursor != null) {
                    while (cursor.hasNext()) {
                        BasicDBObject obj = (BasicDBObject) cursor.next();
                        if (obj != null) {
                            TableRecord rec = new TableRecord();
                            rec.setKeyName(obj.getString(KEY));
                            rec.setFields(obj.toMap());
                            rec.setContent(obj.toString());
                            records.add(rec);
                        }
                    }
                }
                return records;
            }
        };

        return wrapper.doAction();
    }

    @Override
    public TableQueryResult query(String query) {
        // Evaluate this query, convert it to a query against this index, and
        // then return the results
        if (log.isDebugEnabled()) {
            log.debug("Performing query " + query);
        }
        IndexQuery parsedQuery = IndexQueryFactory.parseQuery(query);
        return query(parsedQuery);
    }

    public TableQueryResult query(final IndexQuery indexQuery) {
        if (log.isDebugEnabled()) {
            log.debug("Parsed query " + indexQuery);
        }
        TableQueryResult res = new TableQueryResult();
        final BasicDBObject mongoQuery = getClause(indexQuery.getWhere());
        final DBCollection collection = MongoDBFactory.getDB(instanceName).getCollection(tableName);

        if (!indexQuery.isDistinct()) {
            // What fields to return
            final BasicDBObject fields = new BasicDBObject();
            for (String fieldName : indexQuery.getSelect().getFieldList()) {
                log.debug("Adding return field " + fieldName);
                fields.put(fieldName, 1);
            }
            res.setColumnNames(indexQuery.getSelect().getFieldList());
            fields.put(KEY, 1);

            MongoRetryWrapper<List<List<Object>>> wrapper = new MongoRetryWrapper<List<List<Object>>>() {

                public DBCursor makeCursor() {

                    // Now we need to do the query, with a limit and skip applied if
                    // necessary
                    BasicDBObject sort = new BasicDBObject();
                    if (indexQuery.getOrderBy().getFieldList().size() > 0) {
                        for (String field : indexQuery.getOrderBy().getFieldList()) {
                            sort.put(field, indexQuery.getDirection() == OrderDirection.ASC ? 1 : -1);

                        }
                    }

                    DBCursor ret;
                    if (fields.isEmpty()) {
                        ret = collection.find(mongoQuery);
                    } else {
                        fields.put(KEY, 1);
                        ret = collection.find(mongoQuery, fields);
                    }
                    if (!sort.isEmpty()) {
                        ret = ret.sort(sort);
                    }

                    if (indexQuery.getLimit() != 0) {
                        ret = ret.limit(indexQuery.getLimit());
                    }

                    return ret;
                }

                public List<List<Object>> action(DBCursor cursor) {
                    List<List<Object>> rows = new ArrayList<List<Object>>();
                    while (cursor.hasNext()) {
                        BasicDBObject obj = (BasicDBObject) cursor.next();
                        List<Object> row = new ArrayList<Object>();
                        for (String field : indexQuery.getSelect().getFieldList()) {
                            row.add(obj.get(field));
                        }
                        rows.add(row);
                    }
                    return rows;
                }
            };
            res.setRows(wrapper.doAction());

        } else {
            String key = indexQuery.getSelect().getFieldList().get(0);
            List<?> values = collection.distinct(key, mongoQuery);
            List<List<Object>> rows = new ArrayList<List<Object>>();
            for (Object v : values) {
                List<Object> row = new ArrayList<Object>();
                row.add(v);
                rows.add(row);
            }
            res.setRows(rows);
            List<String> columnNames = new ArrayList<String>();
            columnNames.add(key);
            res.setColumnNames(columnNames);
        }
        return res;
    }

    private BasicDBObject getClause(WhereClause whereClause) {
        log.debug("Getting where clause");
        BasicDBObject ret = EpochManager.getNotEqualEpochQueryObject();
        if (whereClause.getPrimary() != null) {
            // If there are multiple clauses we need to work out what to do
            // For each named field, add to the clause. If we get a repeated named
            // field, add to the same BasicDBObject for that name (which must be
            // compound, right?
            workWith(whereClause.getPrimary(), ret);
            if (!whereClause.getExtensions().isEmpty()) {
                for (WhereExtension xt : whereClause.getExtensions()) {
                    // TODO: What about OR
                    // $or : [ array of tests ]
                    workWith(xt.getClause(), ret);
                }
            }
        }
        return ret;
    }

    private void workWith(WhereStatement whereClause, BasicDBObject ret) {
        if (whereClause.getOper() == WhereTest.EQUAL) {
            log.debug("Primary is = ");
            ret.append(whereClause.getField(), whereClause.getValue().getValue());
            log.debug("Query is " + ret.toString());
        } else {
            BasicDBObject inner = (BasicDBObject) ret.get(whereClause.getField());
            boolean addMe = false;
            if (inner == null) {
                addMe = true;
                inner = new BasicDBObject();
            }
            switch (whereClause.getOper()) {
                case GT:
                    inner.append("$gt", whereClause.getValue().getValue());
                    break;
                case LT:
                    inner.append("$lt", whereClause.getValue().getValue());
                    break;
                case NOTEQUAL:
                    inner.append("$ne", whereClause.getValue().getValue());
                    break;
                default:
                    break;
            }
            if (addMe) {
                ret.append(whereClause.getField(), inner);
            }
        }
    }

}
