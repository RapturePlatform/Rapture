package rapture.table.google;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DateTimeValue;
import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.LatLngValue;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.RawValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.common.base.Predicate;

import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.config.MultiValueConfigLoader;
import rapture.dsl.iqry.IndexQuery;
import rapture.dsl.iqry.IndexQueryFactory;
import rapture.dsl.iqry.OrderDirection;
import rapture.index.AbstractIndexHandler;
import rapture.index.IndexProducer;
import rapture.index.IndexRecord;
import rapture.repo.google.GoogleDatastoreKeyStore;
import rapture.table.memory.RowComparatorFactory;

public class GoogleIndexHandler extends AbstractIndexHandler
{
    private static Logger log = Logger.getLogger(GoogleIndexHandler.class);
    private static final String PREFIX = "prefix";
    private static final String KEY = "key";
    private static final String EPOCH = "epoch";
    private static final String $SET = "$set";
    private String kind;
    private String instanceName = "default";
    private IndexProducer indexProducer;
    private Datastore datastore = null;
    private String projectId;

    public GoogleIndexHandler() {
        projectId = MultiValueConfigLoader.getConfig("GOOGLE-projectId");
        if (projectId != null) datastore = DatastoreOptions.newBuilder().setProjectId(projectId).build().getService();
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        String projectId = StringUtils.trimToNull(config.get("projectid"));
        if (projectId != null) datastore = DatastoreOptions.newBuilder().setProjectId(projectId).build().getService();
        kind = StringUtils.stripToNull(config.get("prefix"));
        if (kind == null) throw new RuntimeException("Prefix not set in config " + JacksonUtil.formattedJsonFromObject(config));
    }

    @Override
    public void deleteTable() {
        if (datastore == null) throw RaptureExceptionFactory.create("Index is not configured");
        List<Key> keys = new ArrayList<>();
        QueryResults<Key> result = datastore.run(Query.newKeyQueryBuilder().setKind(kind).build());
        while (result.hasNext())
            keys.add(result.next());
        datastore.delete(keys.toArray(new Key[keys.size()]));
    }

    @Override
    public void removeAll(String rowId) {
        throw new RaptNotSupportedException("Not yet supported");
    }


    @Override
    public List<TableRecord> queryTable(TableQuery query) {
        throw new RaptNotSupportedException("Not yet supported");
    }


    @Override
    public TableQueryResult query(String query) {
        System.out.println("Performing query " + query);
        IndexQuery parsedQuery = IndexQueryFactory.parseQuery(query);
        return query(parsedQuery);
    }

    public TableQueryResult query(final IndexQuery indexQuery) {
        if (log.isDebugEnabled()) {
            log.debug("Parsed query " + indexQuery);
        }
        TableQueryResult res = new TableQueryResult();
        List<Key> keys = new ArrayList<>();
        List<String> fieldList = indexQuery.getSelect().getFieldList();
        res.setColumnNames(fieldList);
        List<List<Object>> rows = new ArrayList<>();
        List<Predicate<Map<String, Object>>> predicates = predicatesFromQuery(indexQuery);

        EntityQuery.Builder query = Query.newEntityQueryBuilder().setKind(kind);
        QueryResults<Entity> result = datastore.run(query.build());
        while (result.hasNext()) {
            Entity ent = result.next();
            List<Object> row = new LinkedList<>();
            boolean isGood = true;
            Map<String, Object> body = new HashMap<>();
            for (String name : ent.getNames()) {
                body.put(name, valueOf(ent.getValue(name)));
            }
            for (String field : fieldList) {
                if (ent.contains(field)) {
                    Object val = valueOf(ent.getValue(field));
                    for (Predicate<Map<String, Object>> predicate : predicates) {
                        isGood = predicate.apply(body);
                    }
                    if (!isGood) break;
                    row.add(val);
                }
            }
            if (!row.isEmpty()) {
                if (indexQuery.isDistinct() && rows.contains(row)) continue;
                rows.add(row);
            }
        }

        if (indexQuery.getOrderBy().getFieldList().size() > 0) {
            Collections.sort(rows, RowComparatorFactory.createComparator(indexQuery.getOrderBy().getFieldList(), fieldList, indexQuery.getDirection()));
            if (indexQuery.getDirection() == OrderDirection.DESC) {
                Collections.reverse(rows);
            }
        }

        int skip = indexQuery.getSkip();
        if (skip < 0) skip = 0;
        if (skip < rows.size()) {
            int limit = indexQuery.getLimit();

            if ((limit > 0) && (rows.size() - skip > limit)) {
                res.setRows(rows.subList(skip, skip + limit));
            } else {
                res.setRows(rows);
            }
        }

        return res;
    }

    private Object valueOf(Value<?> value) {
        switch (value.getType()) {
        case NULL:
            return null;

        case STRING:
            return ((StringValue) value).get();

        case ENTITY:
            Map<String, Object> map2 = new HashMap<>();
            FullEntity<?> fe = ((EntityValue) value).get();
            Set<String> names = fe.getNames();
            for (String nom : names) {
                GoogleDatastoreKeyStore.put(map2, nom, fe.getValue(nom));
            }
            return map2;

        case LIST:
            List<? extends Value<?>> list = ((ListValue) value).get();
            List<String> slist = new ArrayList<>();
            // TODO this may be an oversimplification
            for (Value<?> v : list) {
                if (v instanceof StringValue) slist.add(((StringValue) v).get());
            }
            return slist;
            
        case KEY:
            return((KeyValue) value).get();
        case LONG:
            return((LongValue) value).get();
        case DOUBLE:
            return((DoubleValue) value).get();
        case BOOLEAN:
            return ((BooleanValue) value).get();
        case DATE_TIME:
            return((DateTimeValue) value).get();
        case BLOB:
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                IOUtils.copy(((BlobValue) value).get().asInputStream(), baos);
                return(baos.toString());
            } catch (IOException e) {
                log.error("Cannot read blob: " + e.getMessage());
                break;
            }
        case RAW_VALUE:
            return((RawValue) value).get();
        case LAT_LNG:
            return((LatLngValue) value).get();
        default:
            throw new RuntimeException("Can't yet");
        }
        return null;
    }

    @Override
    public Long getLatestEpoch() {
        return 0L;
    }

    @Override
    public void setIndexProducer(IndexProducer indexProducer) {
        this.indexProducer = indexProducer;
    }

    @Override
    public void initialize() {
        throw new RaptNotSupportedException("Not yet supported");
    }

    @Override
    public void addedRecord(String key, String value, DocumentMetadata mdLatest) {
        List<IndexRecord> records = indexProducer.getIndexRecords(key, value, mdLatest);
        for (IndexRecord record : records) {
            Map<String, Object> values = record.getValues();
            if (values == null) values = new HashMap<>();
            values.put(ROWID, key);
            updateRow(key, values);
        }
    }

    @Override
    public void updateRow(String key, Map<String, Object> recordValues) {
        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(key);
        Builder builder = Entity.newBuilder(entityKey);
        for (Entry<String, Object> entry : recordValues.entrySet()) {
            builder.set(entry.getKey(), GoogleDatastoreKeyStore.valerie(entry.getKey(), entry.getValue()));
        }
        Entity entity = builder.build();

        try {
            datastore.put(entity);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void ensureIndicesExist() {
        throw new RaptNotSupportedException("Not yet supported");
    }

}
