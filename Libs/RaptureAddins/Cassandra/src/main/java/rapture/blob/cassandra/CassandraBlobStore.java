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
package rapture.blob.cassandra;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import rapture.blob.BaseBlobStore;
import rapture.cassandra.CassandraConstants;

import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.storage.CassandraChunkedStorageProvider;
import com.netflix.astyanax.recipes.storage.ChunkedStorage;
import com.netflix.astyanax.recipes.storage.ObjectMetadata;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

import rapture.common.RaptureURI;
import rapture.config.MultiValueConfigLoader;
import rapture.common.CallingContext;

/**
 * Cassandra impmentation of the BlobStore interface.
 * 
 * @author ben
 * 
 */
public class CassandraBlobStore extends BaseBlobStore {

    private Map<String, String> config = new HashMap<String,String>();

    private Keyspace keyspace;
    private String blobCFName = "blobCF";
    private String blobKSName = "blobKS";

    private ColumnFamily<String, String> blobCF = ColumnFamily.newColumnFamily(blobCFName, StringSerializer.get(), StringSerializer.get());
    private CassandraChunkedStorageProvider chunkedProvider;

    public CassandraBlobStore() throws ConnectionException {
        reconfigure();
    }

    private void createSchema() throws ConnectionException {
        boolean keyspaceExists = false;
        try {
            if (keyspace.describeKeyspace() != null) {
                keyspaceExists = true;
            }
        } catch(BadRequestException e) {
            // do nothing, keyspace does not exist
        }

        if(!keyspaceExists) {
            keyspace.createKeyspace(ImmutableMap.<String, Object> builder()
                    .put("strategy_options", ImmutableMap.<String, Object> builder().put("replication_factor", "1").build())
                    .put("strategy_class", "SimpleStrategy").build());
        }

        if (keyspace.describeKeyspace().getColumnFamily(blobCFName) == null) {
            keyspace.createColumnFamily(blobCF,
                    ImmutableMap.<String, Object> builder().put("key_validation_class", "UTF8Type").put("comparator_type", "UTF8Type").build());
        }
    }

    @Override
    public Boolean storeBlob(CallingContext context, RaptureURI blobUri, Boolean append, InputStream content) {

        if (append == true) {
            throw new UnsupportedOperationException("Append is not currently supported for the Cassandra blob store");
        }
        try {
            ChunkedStorage.newWriter(chunkedProvider, blobUri.getDocPath(), content).withChunkSize(0x1000).withConcurrencyLevel(8).call();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    @Override
    public Long getBlobSize(CallingContext context, RaptureURI blobUri) {
        try {
            ObjectMetadata meta = ChunkedStorage.newInfoReader(chunkedProvider, blobUri.getDocPath()).call();
            return meta.getObjectSize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Boolean deleteBlob(CallingContext context, RaptureURI blobUri) {
        try {
            ChunkedStorage.newDeleter(chunkedProvider, blobUri.getDocPath()).call();
            return true;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream getBlob(CallingContext context, RaptureURI blobUri) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ChunkedStorage.newReader(chunkedProvider, blobUri.getDocPath(), baos).call();
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

    private void reconfigure() throws ConnectionException {

        String seeds = MultiValueConfigLoader.getConfig("CASSANDRA-" + getInstanceName() + ".seeds", "localhost");
        String clusterName = MultiValueConfigLoader.getConfig("CASSANDRA-" + getInstanceName() + ".clusterName", "Test Cluster");
        
        ConsistencyLevel readCL = ConsistencyLevel.CL_ONE;
        ConsistencyLevel writeCL = ConsistencyLevel.CL_ONE;
        if(config.containsKey(CassandraConstants.READ_CONSISTENCY)) {
            readCL = ConsistencyLevel.valueOf(config.get(CassandraConstants.READ_CONSISTENCY));
        }
        
        if(config.containsKey(CassandraConstants.WRITE_CONSISTENCY)) {
            writeCL = ConsistencyLevel.valueOf(config.get(CassandraConstants.WRITE_CONSISTENCY));
        }

        AstyanaxContext<Keyspace> context = new AstyanaxContext.Builder()
                .forCluster(clusterName)
                .forKeyspace(blobKSName)
                .withAstyanaxConfiguration(
                        new AstyanaxConfigurationImpl().setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE).setDefaultReadConsistencyLevel(readCL).setDefaultWriteConsistencyLevel(writeCL))
                .withConnectionPoolConfiguration(
                        new ConnectionPoolConfigurationImpl("astyanaxConnectionPool").setPort(9160).setMaxConnsPerHost(1).setSeeds(seeds))
                .withConnectionPoolMonitor(new CountingConnectionPoolMonitor()).buildKeyspace(ThriftFamilyFactory.getInstance());

        context.start();
        keyspace = context.getClient();

        createSchema();

        chunkedProvider = new CassandraChunkedStorageProvider(keyspace, blobCF);
    }

    public Keyspace getKeyspace() {
        return keyspace;
    }

    @Override
    public void init() {
        // TODO RAP-1582
    }

    @Override
    public Boolean deleteRepo() {
        return true;
    }


}
