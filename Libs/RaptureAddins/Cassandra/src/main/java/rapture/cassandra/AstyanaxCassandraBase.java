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
package rapture.cassandra;

import rapture.common.Messages;
import rapture.common.exception.RaptureExceptionFactory;

import java.net.HttpURLConnection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.retry.ExponentialBackoff;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

import rapture.config.MultiValueConfigLoader;

public class AstyanaxCassandraBase {
    public static final String ALPHA_NUM_UNDERSCRORE = "^[A-Za-z]([A-Za-z]|_|[0-9]){0,31}$";
    private static final int MAX_SCHEMA_CREATE_RETRIES = 1;

    private static Logger log = Logger.getLogger(AstyanaxCassandraBase.class);

    private String keyspaceName;
    private String columnFamilyName;

    protected Keyspace keyspace;
    protected ColumnFamily<String, String> columnFamily;

    private ConsistencyLevel readCL = ConsistencyLevel.CL_ONE;
    private ConsistencyLevel writeCL = ConsistencyLevel.CL_ONE;

    private String replicationFactor;

    private String strategyClass;

    private String keyValidationClass;

    private String comparitorType;

    private int retryDelay;

    private int numberOfRetries;

    private int connectionPoolSize;

    public Messages messageCatalog = new Messages("Cassandra");

    public AstyanaxCassandraBase(String instance, Map<String, String> config) {
        // The configuration has the following:
        // keyspace
        // columnParent
        // readConsitency (optional)
        // writeConsistency (optional)

        // The connection to Cassandra comes from RaptureCASSANDRA.cfg
        String seeds = MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".seeds", MultiValueConfigLoader.getConfig("CASSANDRA-default.seeds", "localhost"));
        String clusterName = MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".clusterName", MultiValueConfigLoader.getConfig("CASSANDRA-default.clusterName", "Test Cluster"));

        setupStorageDetails(config);

        replicationFactor = MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".replicationFactor", MultiValueConfigLoader.getConfig("CASSANDRA-default.replicationFactor", "1"));
        strategyClass = MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".strategyClass", MultiValueConfigLoader.getConfig("CASSANDRA-default.strategyClass", "SimpleStrategy"));
        keyValidationClass = MultiValueConfigLoader.getConfig("CASSANDRA-" + instance + ".keyValidationClass",
                MultiValueConfigLoader.getConfig("CASSANDRA-default.keyValidationClass", "UTF8Type"));
        comparitorType = MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".comparitorType", MultiValueConfigLoader.getConfig("CASSANDRA-default.comparitorType", "UTF8Type"));

        retryDelay = Integer.parseInt(MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".retryDelay", MultiValueConfigLoader.getConfig("CASSANDRA-default.retryDelay", "1000")));
        numberOfRetries = Integer.parseInt(MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".numberOfRetries", MultiValueConfigLoader.getConfig("CASSANDRA-default.numberOfRetries", "3")));
        connectionPoolSize = Integer.parseInt(MultiValueConfigLoader
                .getConfig("CASSANDRA-" + instance + ".connectionPoolSize", MultiValueConfigLoader.getConfig("CASSANDRA-default.connectionPoolSize", "8")));

        columnFamily = ColumnFamily.newColumnFamily(columnFamilyName, StringSerializer.get(), StringSerializer.get());
        log.info("Cassandra keyspace: " + keyspaceName);
        log.info("Cassandra columnFamily: " + columnFamilyName);

        if (config.containsKey(CassandraConstants.READ_CONSISTENCY)) {
            readCL = ConsistencyLevel.valueOf(config.get(CassandraConstants.READ_CONSISTENCY));
        }

        if (config.containsKey(CassandraConstants.WRITE_CONSISTENCY)) {
            writeCL = ConsistencyLevel.valueOf(config.get(CassandraConstants.WRITE_CONSISTENCY));
        }

        try {
            initConnection(clusterName, seeds);
            createSchema();
        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }

    }

    /**
     * Set up the keyspace, column family, and any other necessary variables related to storage
     *
     * @param config
     */
    protected void setupStorageDetails(Map<String, String> config) {
        String keyspace = config.get(CassandraConstants.KEYSPACECFG);
        setKeyspaceName(keyspace);
        String cfName = config.get(CassandraConstants.CFCFG);
        setColumnFamilyName(cfName);
    }

    protected void setColumnFamilyName(String cfName) {
        columnFamilyName = cfName;
        if (!columnFamilyName.matches(ALPHA_NUM_UNDERSCRORE) || !keyspaceName.matches(ALPHA_NUM_UNDERSCRORE)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    "Error keyspace or CF not alphanumeric + underscores and must be less than 32 chars: " + keyspaceName + " " + columnFamilyName);
        }
    }

    public String getColumnFamilyName() {
        return columnFamilyName;
    }

    protected void setKeyspaceName(String keyspace) {
        keyspaceName = keyspace;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    private void createSchema(int numRetries) throws ConnectionException {
        KeyspaceDefinition keyspaceDef = null;

        try {
            keyspaceDef = keyspace.describeKeyspace();
        } catch (Exception e) {
            log.info(String.format("Got exception [%s] during keyspace description, assuming it does not exist", e.getMessage()));
        }

        try {

            if (keyspaceDef == null) {
                keyspace.createKeyspace(ImmutableMap.<String, Object>builder()
                        .put("strategy_options", ImmutableMap.<String, Object>builder().put("replication_factor", replicationFactor).build())
                        .put("strategy_class", strategyClass).build());
            }

            if (keyspace.describeKeyspace().getColumnFamily(columnFamilyName) == null) {
                keyspace.createColumnFamily(columnFamily,
                        ImmutableMap.<String, Object>builder().put("key_validation_class", keyValidationClass).put("comparator_type", comparitorType).build());
            }

        } catch (BadRequestException e) {
            // We might get a bad request exception because of a race
            // condition; the keyspace or CF might already be created. in which
            // case we try again by recursively calling
            // this method.
            if (numRetries > MAX_SCHEMA_CREATE_RETRIES) {
                throw e;
            } else {
                createSchema(++numRetries);
            }
        }
    }

    private void createSchema() throws ConnectionException {
        createSchema(1);
    }

    private void initConnection(String clusterName, String seeds) {
        log.info(String.format("Connecting to Cassandra at %s:%s", clusterName, seeds));
        AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
                .forCluster(clusterName)
                .forKeyspace(keyspaceName)
                .withAstyanaxConfiguration(
                        new AstyanaxConfigurationImpl().setRetryPolicy(new ExponentialBackoff(retryDelay, numberOfRetries))
                                .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE).setDefaultReadConsistencyLevel(readCL)
                                .setDefaultWriteConsistencyLevel(writeCL))
                .withConnectionPoolConfiguration(
                        new ConnectionPoolConfigurationImpl("astyanaxConnectionPool").setPort(9160).setMaxConnsPerHost(connectionPoolSize).setSeeds(seeds))
                .withConnectionPoolMonitor(new CountingConnectionPoolMonitor()).buildKeyspace(ThriftFamilyFactory.getInstance());
        context.start();
        keyspace = context.getClient();
    }

    public ConsistencyLevel getReadCL() {
        return readCL;
    }

    public void setReadCL(ConsistencyLevel readCL) {
        this.readCL = readCL;
    }

    public ConsistencyLevel getWriteCL() {
        return writeCL;
    }

    public void setWriteCL(ConsistencyLevel writeCL) {
        this.writeCL = writeCL;
    }
}
