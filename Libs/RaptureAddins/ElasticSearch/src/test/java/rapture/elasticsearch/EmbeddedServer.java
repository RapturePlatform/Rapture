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

import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

/**
 * Used primarily for unit-testing, this is a fully in-memory instance of ElasticSearch
 * 
 * @author dukenguyen
 *
 */
public class EmbeddedServer {

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
        return node.client();
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
