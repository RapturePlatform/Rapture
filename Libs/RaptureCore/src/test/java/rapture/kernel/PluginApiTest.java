package rapture.kernel;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.api.IndexApi;
import rapture.common.api.PluginApi;

public class PluginApiTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetPluginItem() {
        IndexApi index = Kernel.getIndex();
        PluginApi plugin = Kernel.getPlugin();
        CallingContext context = ContextFactory.getKernelUser();
        String tableUri = "table://foo/bar";
        String indexUri = "index://baz";

        index.createTable(context, tableUri, "TABLE {} USING MEMORY {}");
        index.createIndex(context, indexUri, "field1($1) number");

        String pluginName = "Jeff";

        plugin.createManifest(context, pluginName);
        plugin.addManifestItem(context, pluginName, tableUri);
        plugin.addManifestItem(context, pluginName, indexUri);
        plugin.verifyPlugin(context, pluginName);

        PluginTransportItem item1 = plugin.getPluginItem(context, tableUri);
        System.out.println(new String(item1.getContent()));
        assertNotNull(item1);

        PluginTransportItem item2 = plugin.getPluginItem(context, indexUri);
        System.out.println(new String(item2.getContent()));
        assertNotNull(item2);
    }
}
