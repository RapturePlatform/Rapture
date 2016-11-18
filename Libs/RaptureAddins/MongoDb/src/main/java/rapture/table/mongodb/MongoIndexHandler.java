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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;

import rapture.common.LockHandle;
import rapture.common.TableColumnSort;
import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.TableSelect;
import rapture.common.model.DocumentMetadata;
import rapture.config.MultiValueConfigLoader;
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
import rapture.table.memory.RowComparatorFactory;

/**
 * @author amkimian
 */
public class MongoIndexHandler implements IndexHandler {
    private static Logger log = Logger.getLogger(MongoIndexHandler.class);
    private static final String PREFIX = "prefix";
    private static final String KEY = "key";
    private static final String EPOCH = "epoch";
    private static final String $SET = "$set";
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
        MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
        collection.createIndex(new BsonDocument(KEY, new BsonInt32(1)));
    }

    private String getKey(String rowId) {
        return AbstractMetaHandler.LATEST + "/" + rowId;
    }

    @Override
    public void deleteTable() {
        // drop it all
        MongoDBFactory.getCollection(instanceName, tableName).drop();
    }

    @Override
    public void removeAll(String rowId) {
        String key = getKey(rowId);
        MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
        Document query = new Document();
        query.put(KEY, key);
        collection.findOneAndDelete(query);
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
        String key = getKey(rowId); // stupid key is row id plus "l/" prepended
                                    // to it

        MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
        Document query = new Document();
        query.put(KEY, key);
        Document toPut = new Document();
        toPut.put(KEY, key);
        toPut.put(ROWID, rowId);
        toPut.put(EPOCH, EpochManager.nextEpoch(collection));
        toPut.putAll(recordValues);

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER);

        @SuppressWarnings("unused")
        Document ret = collection.findOneAndUpdate(query, new Document($SET, toPut), options);
    }

    @Override
    public Long getLatestEpoch() {
        return EpochManager.getLatestEpoch(MongoDBFactory.getCollection(instanceName, tableName));
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
        // create index if not exists... use index name too from indexDefinition
        // object
        if (indexDefinition == null) {
            return;
        }

        String indexName = IndexNameFactory.createIndexName(indexDefinition);
        MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);

        int limit = Integer.parseInt(
                MultiValueConfigLoader.getConfig("MONGODB-" + instanceName + ".limit", MultiValueConfigLoader.getConfig("MONGODB-default.limit", "250")));

        boolean indexExists = false;
        ListIndexesIterable<Document> indexInfo = collection.listIndexes();
        for (Document dbObject : indexInfo) {
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

    private void createIt(IndexDefinition indexDefinition, boolean force, String indexName, MongoCollection<Document> collection) {
        // bug in mongo driver: need to set ns explicitly
        // String ns = collection.getDB() + "." + collection.getName();

        Document index = new Document();
        for (FieldDefinition f : indexDefinition.getFields()) {
            index.put(f.getName(), 1);
        }
        IndexOptions options = new IndexOptions().name(indexName).background(!force);
        collection.createIndex(index, options);
    }

    @Override
    public List<TableRecord> queryTable(final TableQuery querySpec) {

        MongoRetryWrapper<List<TableRecord>> wrapper = new MongoRetryWrapper<List<TableRecord>>() {
            @Override
            public FindIterable<Document> makeCursor() {
                FindIterable<Document> ret;
                MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);

                // Convert the query into that for a mongodb collection, then
                // call find
                Document query = EpochManager.getNotEqualEpochQueryObject();
                if (querySpec.getFieldTests() != null) {
                    for (TableSelect sel : querySpec.getFieldTests()) {
                        switch (sel.getOper()) {
                        case "=":
                            query.append(sel.getFieldName(), sel.getTestValue());
                            break;
                        case ">":
                            Document gt = new Document();
                            gt.append("$gt", sel.getTestValue());
                            query.append(sel.getFieldName(), gt);
                            break;
                        case "<":
                            Document lt = new Document();
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
                List<String> projection = querySpec.getFieldReturns();
                if (projection != null) {
                    projection.add(KEY);
                }

                // Now we need to do the query, with a limit and skip applied if
                // necessary
                Document sort = new Document();
                if (querySpec.getSortFields() != null) {
                    for (TableColumnSort sortField : querySpec.getSortFields()) {
                        sort.put(sortField.getFieldName(), sortField.getAscending() ? 1 : -1);
                    }
                }

                ret = collection.find(query).projection(Projections.include((List<String>) ((projection == null) ? Collections.emptyList() : projection)));

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

            @Override
            public List<TableRecord> action(FindIterable<Document> cursor) {
                List<TableRecord> records = new ArrayList<>();
                if (cursor != null) {
                    for (Document obj : cursor) {
                        if (obj != null) {
                            TableRecord rec = new TableRecord();
                            rec.setKeyName(obj.getString(KEY));
                            rec.setFields(obj);
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
        final Document mongoQuery = getClause(indexQuery.getWhere());
        final MongoCollection<Document> collection = MongoDBFactory.getCollection(instanceName, tableName);
        List<List<Object>> rows = new ArrayList<>();

        List<String> fieldList = indexQuery.getSelect().getFieldList();
        // Mongo can't do distinct based on multiple fields for some reason

        if (!indexQuery.isDistinct()) {
            // What fields to return
            final Document fields = new Document();
            for (String fieldName : indexQuery.getSelect().getFieldList()) {
                log.debug("Adding return field " + fieldName);
                fields.put(fieldName, 1);
            }
            res.setColumnNames(indexQuery.getSelect().getFieldList());
            fields.put(KEY, 1);

            MongoRetryWrapper<List<List<Object>>> wrapper = new MongoRetryWrapper<List<List<Object>>>() {

                @Override
                public FindIterable<Document> makeCursor() {

                    FindIterable<Document> ret;
                    if (fields.isEmpty()) {
                        ret = collection.find(mongoQuery);
                    } else {
                        fields.put(KEY, 1);
                        ret = collection.find(mongoQuery).projection(fields);
                    }

                    if (indexQuery.getOrderBy().getFieldList().size() > 0) {
                        Document sort = new Document();
                        for (String field : indexQuery.getOrderBy().getFieldList()) {
                            sort.put(field, indexQuery.getDirection() != OrderDirection.DESC ? 1 : -1);

                        }
                        ret = ret.sort(sort);
                    }

                    int skip = indexQuery.getSkip();
                    if (skip > 0) {
                        ret = ret.skip(skip);
                    } else skip = 0;

                    int limit = indexQuery.getLimit();
                    if (limit > 0) {
                        // By specifying a negative limit we tell Mongo that it can close the cursor after returning a single batch.
                        ret = ret.limit(-(limit + skip));
                    }

                    return ret;
                }

                @Override
                public List<List<Object>> action(FindIterable<Document> cursor) {
                    List<List<Object>> rows = new ArrayList<>();
                    for (Document obj : cursor) {
                        List<Object> row = new ArrayList<>();
                        for (String field : indexQuery.getSelect().getFieldList()) {
                            row.add(obj.get(field));
                        }
                        rows.add(row);
                    }
                    return rows;
                }
            };

            res.setRows(wrapper.doAction());
            return res;
            // We are done.

        } else if (fieldList.size() > 1) {
            // What fields to return
            final Document fields = new Document();
            for (String fieldName : indexQuery.getSelect().getFieldList()) {
                log.debug("Adding return field " + fieldName);
                fields.put(fieldName, 1);
            }
            res.setColumnNames(indexQuery.getSelect().getFieldList());
            fields.put(KEY, 1);

            MongoRetryWrapper<List<List<Object>>> wrapper = new MongoRetryWrapper<List<List<Object>>>() {

                @Override
                public FindIterable<Document> makeCursor() {

                    FindIterable<Document> ret;
                    if (fields.isEmpty()) {
                        ret = collection.find(mongoQuery);
                    } else {
                        fields.put(KEY, 1);
                        ret = collection.find(mongoQuery).projection(fields);
                    }

                    if (indexQuery.getOrderBy().getFieldList().size() > 0) {
                        Document sort = new Document();
                        for (String field : indexQuery.getOrderBy().getFieldList()) {
                            sort.put(field, indexQuery.getDirection() != OrderDirection.DESC ? 1 : -1);

                        }
                        ret = ret.sort(sort);
                    }

                    // We can't apply SKIP and LIMIT here because we must drop the fields that aren't distinct;
                    // Mongo doesn't appear to support distinct on multiple keys
                    return ret;
                }

                @Override
                public List<List<Object>> action(FindIterable<Document> cursor) {

                    int limit = Math.abs(indexQuery.getSkip()) + Math.abs(indexQuery.getLimit());
                    if (limit == 0) limit = Integer.MAX_VALUE;

                    List<List<Object>> rows = new ArrayList<>();
                    for (Document obj : cursor) {
                        List<Object> row = new ArrayList<>();
                        for (String field : indexQuery.getSelect().getFieldList()) {
                            row.add(obj.get(field));
                        }
                        if (indexQuery.isDistinct() && rows.contains(row)) continue;
                        rows.add(row);
                        if (rows.size() > limit) break;
                    }
                    return rows;
                }
            };

            rows = wrapper.doAction();
            // We are not done - still need to apply skip and limit

        } else {
            String key = fieldList.get(0);
            DistinctIterable<String> values = collection.distinct(key, mongoQuery, String.class);
            for (String v : values) {
                rows.add(ImmutableList.of(v));
            }

            res.setColumnNames(ImmutableList.of(key));
            if (indexQuery.getOrderBy().getFieldList().size() > 0) {
                List<String> columnNames = indexQuery.getSelect().getFieldList();
                Collections.sort(rows, RowComparatorFactory.createComparator(indexQuery.getOrderBy().getFieldList(), columnNames, indexQuery.getDirection()));
                if (indexQuery.getDirection() == OrderDirection.DESC) {
                    Collections.reverse(rows);
                }
            }
        }

        int skip = Math.abs(indexQuery.getSkip());
        if (skip < rows.size()) {
            int limit = Math.abs(indexQuery.getLimit());
            if ((limit > 0) && (rows.size() - skip > limit)) {
                res.setRows(rows.subList(skip, skip + limit));
            } else res.setRows(rows);
        } // else all rows are skipped
        return res;
    }

    private Document getClause(WhereClause whereClause) {
        log.debug("Getting where clause");
        Document ret = EpochManager.getNotEqualEpochQueryObject();
        if (whereClause.getPrimary() != null) {
            // If there are multiple clauses we need to work out what to do
            // For each named field, add to the clause. If we get a repeated
            // named
            // field, add to the same Document for that name (which must be
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

    private void workWith(WhereStatement whereClause, Document ret) {
        if (whereClause.getOper() == WhereTest.EQUAL) {
            log.debug("Primary is = ");
            ret.append(whereClause.getField(), whereClause.getValue().getValue());
            log.debug("Query is " + ret.toString());
        } else {
            Document inner = (Document) ret.get(whereClause.getField());
            boolean addMe = false;
            if (inner == null) {
                addMe = true;
                inner = new Document();
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
            case LIKE:
            	inner.append("$regex", whereClause.getValue().getValue());
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
