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
package rapture.repo.cassandra;

import rapture.cassandra.AstyanaxCassandraBase;
import rapture.common.RaptureFolderInfo;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.repo.cassandra.key.KeyNormalizer;
import rapture.repo.cassandra.key.NormalizedKey;
import rapture.repo.cassandra.key.PathBuilder;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.util.RangeBuilder;

public class AstyanaxRepoConnection extends AstyanaxCassandraBase {
    private static Logger log = Logger.getLogger(AstyanaxRepoConnection.class);
    private final KeyNormalizer keyNormalizer;
    private Optional<String> pKeyPrefix; //partition key prefix

    public AstyanaxRepoConnection(String instance, Map<String, String> config) {
        super(instance, config);
        keyNormalizer = new KeyNormalizer(pKeyPrefix);
    }

    @Override
    protected void setupStorageDetails(Map<String, String> config) {
        ConfigParser configParser = new ConfigParser(config);
        configParser.parse();

        setKeyspaceName(configParser.getKeyspaceName());
        setColumnFamilyName(configParser.getCfName());
        pKeyPrefix = configParser.getpKeyPrefix();
    }

    public boolean drop() {
        return false;
    }

    public void dropRepo() {
        try {
            keyspace.dropColumnFamily(columnFamily);
        } catch (ConnectionException e) {
            log.error(e);
        }
    }

    public boolean deleteEntries(List<String> keys) {
        List<NormalizedKey> normalizedKeys = keyNormalizer.normalizeKeys(keys);
        MutationBatch mb = keyspace.prepareMutationBatch();
        for (NormalizedKey key : normalizedKeys) {
            mb.withRow(columnFamily, key.get()).delete();
        }
        try {
            mb.execute();
        } catch (ConnectionException ce) {
            log.error(ce);
            return false;
        }
        return true;

    }

    public boolean deleteEntry(String key) {
        NormalizedKey normalizedKey = keyNormalizer.normalizeKey(key);
        MutationBatch mb = keyspace.prepareMutationBatch();
        mb.withRow(columnFamily, normalizedKey.get()).delete();

        try {
            mb.execute();
        } catch (ConnectionException ce) {
            log.error(ce);
            return false;
        }
        return true;
    }

    private static final String DATA_COLUMN_NAME = "data";

    public String get(String key) {
        NormalizedKey normalizedKey = keyNormalizer.normalizeKey(key);
        ColumnList<String> result;
        try {
            result = keyspace.prepareQuery(columnFamily).getKey(normalizedKey.get())
                    .withColumnRange(new RangeBuilder().setStart(DATA_COLUMN_NAME).setEnd(DATA_COLUMN_NAME).setLimit(1).build()).execute().getResult();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
        if (!result.isEmpty()) {
            return result.getColumnByName(DATA_COLUMN_NAME).getStringValue();
        }
        return null;

    }
    
    public long getRowNumber() {
        try {
            long count = 0;
            Rows<String, String> rows = keyspace.prepareQuery(columnFamily).getAllRows().execute().getResult();
            for (Row<String, String> row : rows) {
                count += row.getColumns().size();
            }
            return count;
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
    }
    
    public List<String> batchGet(List<String> rawKeys) {
        //really??? we should do better than this
        List<String> ret = new ArrayList<String>(rawKeys.size());
        for (String k : rawKeys) {
            ret.add(get(k));
        }
        return ret;
    }

    /**
     * Data is written to the row given by the key name, into the "data" column
     * cell.
     *
     * @param key
     * @param value
     */
    public void putData(String key, String value) {
        NormalizedKey normalizedKey = keyNormalizer.normalizeKey(key);
        try {
            keyspace.prepareColumnMutation(columnFamily, normalizedKey.get(), DATA_COLUMN_NAME).putValue(value, null).execute();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }

    }

    private int getFolderCount(String cf, String rawPrefix, String childName) {
        NormalizedKey prefix = keyNormalizer.normalizePrefix(rawPrefix);
        ColumnList<String> result;
        try {
            ColumnFamily<String, String> cFam = ColumnFamily.newColumnFamily(cf, StringSerializer.get(), StringSerializer.get());
            result = keyspace.prepareQuery(cFam).getKey(prefix.get())
                    .withColumnRange(new RangeBuilder().setStart(childName).setEnd(childName).setLimit(1).build())
                    .execute().getResult();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
        if (!result.isEmpty()) {
            return result.getColumnByName(childName).getIntegerValue();
        }
        return 0;
    }

    public void addFolderDocument(String cf, String prefix, String docName) {
        addPrefixValue(cf, prefix, docName, -1);
    }

    private void addPrefixValue(String cf, String rawPrefix, String childName, int folderCount) {
        NormalizedKey normalizedPrefix = keyNormalizer.normalizePrefix(rawPrefix);
        try {
            ColumnFamily<String, String> cFam = ColumnFamily.newColumnFamily(cf, StringSerializer.get(), StringSerializer.get());
            keyspace.prepareColumnMutation(cFam, normalizedPrefix.get(), childName).putValue(folderCount, null).execute();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }

    }

    public void addFolderFolder(String cf, String prefix, String childName) {
        int folderCount = getFolderCount(cf, prefix, childName);
        folderCount++;
        addPrefixValue(cf, prefix, childName, folderCount);
    }

    List<RaptureFolderInfo> getFolderChildren(String cf, String prefix) {
        return getFolderChildren(cf, keyNormalizer.normalizePrefix(prefix));
    }

    private List<RaptureFolderInfo> getFolderChildren(String cf, NormalizedKey prefix) {
        List<RaptureFolderInfo> ret = new ArrayList<RaptureFolderInfo>();
        ColumnList<String> result;
        try {
            ColumnFamily<String, String> cFam = ColumnFamily.newColumnFamily(cf, StringSerializer.get(), StringSerializer.get());
            result = keyspace.prepareQuery(cFam).getKey(prefix.get()).withColumnRange(new RangeBuilder().setLimit(1000).build()).execute().getResult();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }
        for (Column<String> column : result) {
            RaptureFolderInfo info = new RaptureFolderInfo();
            info.setName(column.getName());
            info.setFolder(column.getIntegerValue() != -1);
            ret.add(info);
        }
        return ret;

    }

    public List<String> getAllFolderChildren(String cf, String prefix) {
        NormalizedKey normalizedPrefix = keyNormalizer.normalizePrefix(prefix);
        // Start at prefix and then iterate down until we have no more folders
        // to iterate
        List<String> ret = new ArrayList<String>();
        List<RaptureFolderInfo> thisLevel = getFolderChildren(cf, normalizedPrefix);
        for (RaptureFolderInfo info : thisLevel) {
            if (info.isFolder()) {
                String lowerLevel = new PathBuilder(prefix).subPath(info.getName()).build();
                ret.addAll(getAllFolderChildren(cf, lowerLevel));
            } else {
                ret.add(new PathBuilder(prefix).subPath(info.getName()).build());
            }
        }
        return ret;
    }

    private void removePrefixValue(String cf, String rawPrefix, String docName) {
        NormalizedKey normalizedPrefix = keyNormalizer.normalizePrefix(rawPrefix);
        ColumnFamily<String, String> cFam = ColumnFamily.newColumnFamily(cf, StringSerializer.get(), StringSerializer.get());
        try {
            keyspace.prepareColumnMutation(cFam, normalizedPrefix.get(), docName).deleteColumn().execute();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }

    }

    public void removeFolderDocument(String cf, String prefix, String docName) {
        removePrefixValue(cf, prefix, docName);
    }

    public void removeFolderFolder(String cf, String prefix, String childName) {
        int folderCount = getFolderCount(cf, prefix, childName);
        if (folderCount != 0) {
            folderCount--;
            if (folderCount != 0) {
                addPrefixValue(cf, prefix, childName, folderCount);
            } else {
                removePrefixValue(cf, prefix, childName);
            }
        }
    }

    public void ensureStandardCF(String cf) {
        try {
            if (keyspace.describeKeyspace().getColumnFamily(cf) == null) {
                ColumnFamily<String, String> cFam = ColumnFamily.newColumnFamily(cf, StringSerializer.get(), StringSerializer.get());
                keyspace.createColumnFamily(cFam,
                        ImmutableMap.<String, Object>builder().put("key_validation_class", "UTF8Type").put("comparator_type", "UTF8Type").build());
            }
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }

    }

    public Boolean validate() {
        get("ignore"); //will throw an exception if invalid
        return true;
    }

    public String getUniqueId() {
        return String.format("%s-%s-%s", getKeyspaceName(), getColumnFamilyName(), pKeyPrefix.isPresent() ? pKeyPrefix.get() : "[]");
    }

    public Optional<String> getPKeyPrefix() {
        return pKeyPrefix;
    }
}
