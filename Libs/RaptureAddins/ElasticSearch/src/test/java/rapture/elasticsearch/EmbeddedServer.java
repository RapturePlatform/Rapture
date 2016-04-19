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
package rapture.elasticsearch;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

/**
 * Used primarily for unit-testing, this is a fully in-memory instance of ElasticSearch
 * 
 * @author dukenguyen
 *
 */
public class EmbeddedServer {

    private static final Logger log = Logger.getLogger(EmbeddedServer.class);

    private static final String DEFAULT_DATA_DIRECTORY = "build/elasticsearch-data";

    private final Node node;
    private final String dataDirectory;

    public EmbeddedServer() {
        this(DEFAULT_DATA_DIRECTORY);
    }

    public EmbeddedServer(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        Settings.Builder settings = Settings.settingsBuilder()
                .put("node.name", "testnode")
                .put("cluster.name", "testcluster")
                .put("http.enabled", "false")
                .put("path.data", dataDirectory)
                .put("path.home", dataDirectory)
                .put("index.number_of_shards", "1")
                .put("index.number_of_replicas", "0")
                .put("discovery.zen.ping.multicast.enabled", "false");
        node = nodeBuilder()
                .local(true)
                .settings(settings.build())
                .node();
    }

    public Client getClient() {
        Client client = node.client();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(new Callable<Void>() {
            @Override
            public Void call() {
                ClusterAdminClient clusterAdmin = client.admin().cluster();
                ClusterHealthResponse res = clusterAdmin.health(new ClusterHealthRequest()).actionGet(1000);
                while (res.getStatus().equals(ClusterHealthStatus.RED)) {
                    res = clusterAdmin.health(new ClusterHealthRequest()).actionGet(1000);
                }
                return null;
            }
        });
        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Failed to wait for cluster to startup synchronously.  Unit tests may fail", e);
        }
        return client;
    }

    public void shutdown() {
        node.close();
        deleteDataDirectory();
    }

    private void deleteDataDirectory() {
        try {
            FileUtils.deleteDirectory(new File(dataDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Could not delete data directory of embedded elasticsearch server", e);
        }
    }
}
