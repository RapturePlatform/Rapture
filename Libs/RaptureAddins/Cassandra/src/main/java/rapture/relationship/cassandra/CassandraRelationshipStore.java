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
package rapture.relationship.cassandra;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.cassandra.CassandraConstants;
import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureRelationship;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.MultiValueConfigLoader;
import rapture.relationship.RelationshipStore;
import rapture.repo.cassandra.AstyanaxRepoConnection;
import rapture.repo.cassandra.CassFolderHandler;

import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

public class CassandraRelationshipStore implements RelationshipStore {

    private static final Logger log = Logger.getLogger(CassandraRelationshipStore.class);

    private static final String RELATIONSHIP_JSON_FIELD = "json";

    private Map<String, String> config = new HashMap<String, String>();

    private Keyspace keyspace;
    private String instanceName;
    private RaptureURI storeURI;
    private CassFolderHandler folderHandler;
    private AstyanaxRepoConnection cass;

    private ColumnFamily<String, String> relationshipCF;
    private ColumnFamily<String, String> inboundCF;
    private ColumnFamily<String, String> outboundCF;
    private ColumnFamily<String, String> labelCF;
    private String instance = "default";
    public Messages messageCatalog;

    public CassandraRelationshipStore() {
        messageCatalog = new Messages("Cassandra");

    }

    @Override
    public RaptureURI createRelationship(RaptureRelationship relationship, String user) {

        String relationshipJSON = JacksonUtil.jsonFromObject(relationship);
        MutationBatch mb = keyspace.prepareMutationBatch();

        mb.withRow(relationshipCF, relationship.getURI().toString()).putColumn(RELATIONSHIP_JSON_FIELD, relationshipJSON);
        mb.withRow(inboundCF, relationship.getToURI().toString()).putColumn(relationship.getURI().toString(), relationshipJSON);
        mb.withRow(outboundCF, relationship.getFromURI().toString()).putColumn(relationship.getURI().toString(), relationshipJSON);
        mb.withRow(labelCF, relationship.getLabel()).putColumn(relationship.getURI().toString(), relationshipJSON);

        try {
            mb.execute();
        } catch (ConnectionException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("GetDataError"));
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }
        registerNode(relationship.getFromURI());
        registerNode(relationship.getToURI());
        return relationship.getURI();
    }

    private void registerNode(RaptureURI uri) {
        String k = uri.getScheme().toString() + "/" + uri.getAuthority() + "/" + uri.getDocPath();
        folderHandler.registerDocument(k);

    }

    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        return folderHandler.getChildren(prefix);
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String prefix, Boolean force) {
        return folderHandler.removeChildren(prefix, force);
    }

    @Override
    public RaptureRelationship getRelationship(RaptureURI relationshipURI) {
        RowQuery<String, String> query = keyspace.prepareQuery(relationshipCF).getKey(relationshipURI.toString()).withColumnSlice(RELATIONSHIP_JSON_FIELD);
        try {
            OperationResult<ColumnList<String>> result = query.execute();
            ColumnList<String> columns = result.getResult();
            Column<String> jsonColumn = columns.getColumnByName(RELATIONSHIP_JSON_FIELD);
            if (jsonColumn != null) {
                RaptureRelationship raptureRelationship = JacksonUtil.objectFromJson(jsonColumn.getStringValue(), RaptureRelationship.class);
                return raptureRelationship;
            } else {
                return null;
            }
        } catch (ConnectionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void deleteRelationship(RaptureURI relationshipURI, String user) {
        RaptureRelationship relationship = getRelationship(relationshipURI);

        MutationBatch mb = keyspace.prepareMutationBatch();

        mb.withRow(relationshipCF, relationship.getURI().toString()).delete();

        mb.withRow(inboundCF, relationship.getToURI().toString()).deleteColumn(relationship.getURI().toString());
        mb.withRow(outboundCF, relationship.getFromURI().toString()).deleteColumn(relationship.getURI().toString());
        mb.withRow(labelCF, relationship.getLabel()).deleteColumn(relationship.getURI().toString());

        try {
            mb.execute();
        } catch (ConnectionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e); //$NON-NLS-1$
        }

    }

    @Override
    public List<RaptureRelationship> getInboundRelationships(RaptureURI inboundRelationship) {
        RowQuery<String, String> query = keyspace.prepareQuery(inboundCF).getKey(inboundRelationship.toString());
        List<RaptureRelationship> retVal = queryForRelationships(query);
        return retVal;
    }

    @Override
    public List<RaptureRelationship> getOutboundRelationships(RaptureURI outboundRelationship) {
        RowQuery<String, String> query = keyspace.prepareQuery(outboundCF).getKey(outboundRelationship.toString());
        List<RaptureRelationship> retVal = queryForRelationships(query);
        return retVal;
    }

    @Override
    public List<RaptureRelationship> getLabeledRelationships(String label) {
        RowQuery<String, String> query = keyspace.prepareQuery(labelCF).getKey(label);
        List<RaptureRelationship> retVal = queryForRelationships(query);
        return retVal;
    }

    private List<RaptureRelationship> queryForRelationships(RowQuery<String, String> query) {
        List<RaptureRelationship> retVal = new ArrayList<RaptureRelationship>();
        try {
            OperationResult<ColumnList<String>> result = query.execute();
            for (Column<String> column : result.getResult()) {
                String json = column.getStringValue();
                RaptureRelationship raptureRelationship = JacksonUtil.objectFromJson(json, RaptureRelationship.class);
                retVal.add(raptureRelationship);
            }
        } catch (ConnectionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e); //$NON-NLS-1$
        }
        return retVal;
    }

    private void reconfigure() throws ConnectionException {

        String relationshipKSName = "relationshipKS";
        String relationshipCFName = "relationshipCF";
        String inboundCFName = "relationshipCF_inbound";
        String outboundCFName = "relationshipCF_outbound";
        String labelCFName = "relationshipCF_label";

        if (storeURI == null || storeURI.getAuthority() == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("StoreURI"));
        } else {
            relationshipKSName = storeURI.getAuthority() + "_relationshipKS";
            relationshipCFName = storeURI.getAuthority() + "_relationshipCF";
            inboundCFName = storeURI.getAuthority() + "_relationshipCF_inbound";
            outboundCFName = storeURI.getAuthority() + "_relationshipCF_outbound";
            labelCFName = storeURI.getAuthority() + "_relationshipCF_label";
        }

        String seeds = MultiValueConfigLoader.getConfig("CASSANDRA-" + instanceName + ".seeds", "localhost");
        String clusterName = MultiValueConfigLoader.getConfig("CASSANDRA-" + instanceName + ".clusterName", "Test Cluster");

        ConsistencyLevel readCL = ConsistencyLevel.CL_ONE;
        ConsistencyLevel writeCL = ConsistencyLevel.CL_ONE;
        if (config.containsKey(CassandraConstants.READ_CONSISTENCY)) {
            readCL = ConsistencyLevel.valueOf(config.get(CassandraConstants.READ_CONSISTENCY));
        }

        if (config.containsKey(CassandraConstants.WRITE_CONSISTENCY)) {
            writeCL = ConsistencyLevel.valueOf(config.get(CassandraConstants.WRITE_CONSISTENCY));
        }

        AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
                .forCluster(clusterName)
                .forKeyspace(relationshipKSName)
                .withAstyanaxConfiguration(
                        new AstyanaxConfigurationImpl().setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE).setDefaultReadConsistencyLevel(readCL)
                                .setDefaultWriteConsistencyLevel(writeCL))
                .withConnectionPoolConfiguration(
                        new ConnectionPoolConfigurationImpl("astyanaxConnectionPool").setPort(9160).setMaxConnsPerHost(1).setSeeds(seeds))
                .withConnectionPoolMonitor(new CountingConnectionPoolMonitor()).buildKeyspace(ThriftFamilyFactory.getInstance());

        context.start();
        keyspace = context.getClient();

        this.relationshipCF = ColumnFamily.newColumnFamily(relationshipCFName, StringSerializer.get(), StringSerializer.get());
        this.inboundCF = ColumnFamily.newColumnFamily(inboundCFName, StringSerializer.get(), StringSerializer.get());
        this.outboundCF = ColumnFamily.newColumnFamily(outboundCFName, StringSerializer.get(), StringSerializer.get());
        this.labelCF = ColumnFamily.newColumnFamily(labelCFName, StringSerializer.get(), StringSerializer.get());

        KeyspaceDefinition keyspaceDef = null;

        try {
            keyspaceDef = keyspace.describeKeyspace();
        } catch (Exception e) {
        }

        if (keyspaceDef == null) {
            System.out.println("AAAAAAA CREATING KEYSPACE");
            keyspace.createKeyspace(ImmutableMap.<String, Object> builder()
                    .put("strategy_options", ImmutableMap.<String, Object> builder().put("replication_factor", "1").build())
                    .put("strategy_class", "SimpleStrategy").build());
        }

        if (keyspace.describeKeyspace().getColumnFamily(relationshipCFName) == null) {
            keyspace.createColumnFamily(relationshipCF,
                    ImmutableMap.<String, Object> builder().put("key_validation_class", "UTF8Type").put("comparator_type", "UTF8Type").build());
        }

        if (keyspace.describeKeyspace().getColumnFamily(inboundCFName) == null) {
            keyspace.createColumnFamily(inboundCF,
                    ImmutableMap.<String, Object> builder().put("key_validation_class", "UTF8Type").put("comparator_type", "UTF8Type").build());
        }

        if (keyspace.describeKeyspace().getColumnFamily(outboundCFName) == null) {
            keyspace.createColumnFamily(outboundCF,
                    ImmutableMap.<String, Object> builder().put("key_validation_class", "UTF8Type").put("comparator_type", "UTF8Type").build());
        }

        if (keyspace.describeKeyspace().getColumnFamily(labelCFName) == null) {
            keyspace.createColumnFamily(labelCF,
                    ImmutableMap.<String, Object> builder().put("key_validation_class", "UTF8Type").put("comparator_type", "UTF8Type").build());
        }

        cass = new AstyanaxRepoConnection(instance, this.config);
        folderHandler = new CassFolderHandler(cass, cass.getColumnFamilyName());
    }

    public void drop() {
        try {
            keyspace.dropKeyspace();
        } catch (ConnectionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e); //$NON-NLS-1$
        }
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        this.config = config;
        try {
            reconfigure();
        } catch (ConnectionException e) {
            new IllegalStateException(e);
        }
    }

    @Override
    public void setStoreURI(RaptureURI storeURI) {
        this.storeURI = storeURI;
    }

    @Override
    public void dropStore() {
        cass.dropRepo();
        try {
            keyspace.dropColumnFamily(relationshipCF);
            keyspace.dropColumnFamily(inboundCF);
            keyspace.dropColumnFamily(outboundCF);
            keyspace.dropColumnFamily(labelCF);
        } catch (ConnectionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), e); //$NON-NLS-1$
        }
    }

}
