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
package rapture.plugin.app;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Maps;

import rapture.common.PluginConfig;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.ScriptClient;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.plugin.install.PluginSandbox;
import rapture.plugin.install.PluginSandboxItem;
import rapture.util.ResourceLoader;

/**
 * Self install the plugin(s) defined in the resources of the built application. This entry point is linked from a secondary build that depends on this library
 * (and embeds this library)
 * 
 * @author amkimian
 * 
 */
public class SelfInstaller {

    public static final String ARG_HOST = "-host";
    public static final String ARG_USER = "-user";
    public static final String ARG_PASSWORD = "-password";
    public static final String ARG_AREA = "-area";
    public static final String ARG_OVERLAY = "-overlay";

    private String host = "http://localhost:8665/rapture";
    private String password = "rapture";
    private String username = "rapture";
    private String overlay = null;
    private boolean isFileBased = false;

    private void processArgs(String args[]) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (ARG_HOST.equals(args[i])) {
                    host = args[++i];
                } else if (ARG_USER.equals(args[i])) {
                    username = args[++i];
                } else if (ARG_PASSWORD.equals(args[i])) {
                    password = args[++i];
                } else if (ARG_AREA.equals(args[i])) {
                    pluginArea = args[++i];
                } else if (ARG_OVERLAY.equals(args[i])) {
                    overlay = args[++i];
                }
            }
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    private String pluginArea;
    private PluginSandbox sandbox;

    public SelfInstaller(String pluginArea) {
        this.pluginArea = pluginArea;
    }

    public void loadSandbox() throws Exception {
        sandbox = new PluginSandbox();
        loadSandboxFromResources(sandbox, "/" + pluginArea);
    }

    public void installSandbox() {
        Map<String, PluginTransportItem> payload = Maps.newHashMap();
        String thisVariant = overlay;
        for (PluginSandboxItem item : sandbox.getItems(thisVariant)) {
            try {
                System.out.println("Packaging " + item.getURI().toString());
                PluginTransportItem payloadItem = item.makeTransportItem();
                RaptureURI uri = item.getURI();
                if (isFileBased && item.getFullFilePath() != null) {
                    // set plugin item's local file path as uri element (separated by #)
                    // this element will be used by FileBlobStore and FileDataStore to create sym link
                    // the result uri's docPathWithElement will be docPath#element, eg.
                    // app/admin/doc.html#/Curtis/CurtisAdmin/PLUGIN/content/curtisweb/app/admin/blob.html
                    uri = RaptureURI.builder(uri).element(item.getFullFilePath()).build();
                    System.out.println("uri=" + uri);
                    payloadItem.setUri(uri.toString());
                }
                payload.put(uri.toString(), payloadItem);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        SimpleCredentialsProvider creds = new SimpleCredentialsProvider(username, password);
        HttpLoginApi login = new HttpLoginApi(host, creds);
        login.login();
        ScriptClient client = new ScriptClient(login);
        List<PluginConfig> list = client.getPlugin().getInstalledPlugins(ContextFactory.getKernelUser());
        for (PluginConfig cxt : list) {
            if (cxt.getPlugin() == sandbox.getPluginName()) {
                client.getPlugin().uninstallPlugin(sandbox.getPluginName());
            }
        }
        client.getPlugin().installPlugin(sandbox.makeManifest(thisVariant), payload);
    }

    public static void main(String[] args) throws Exception {
        SelfInstaller installer = new SelfInstaller("PLUGIN");
        installer.processArgs(args);
        installer.loadSandbox();
        installer.installSandbox();
    }

    private void loadSandboxFromResources(PluginSandbox sandbox, String resourcePath) throws Exception {
        String pluginContent = ResourceLoader.getResourceAsString(SelfInstaller.class, resourcePath + "/" + PluginSandbox.PLUGIN_TXT);
        System.out.println("Plugin content is " + pluginContent);
        PluginConfig fConfig = JacksonUtil.objectFromJson(pluginContent, PluginConfig.class);
        sandbox.setConfig(fConfig);

        String pluginIgnoreFile = ResourceLoader.getResourceAsString(SelfInstaller.class, resourcePath + "/" + PluginSandbox.IGNORE);
        if (!StringUtils.isBlank(pluginIgnoreFile)) {
            sandbox.processIgnoreFile(pluginIgnoreFile);
        }
        SandboxLoader loader;
        String rootPath = resourcePath + "/" + PluginSandbox.CONTENT;
        URL dirURL = SelfInstaller.class.getClass().getResource(rootPath);
        // Can happen if content directory is missing - only overlays in the plugin
        if (dirURL == null) return;
        if ("jar".equals(dirURL.getProtocol())) {
            loader = new JarBasedSandboxLoader();
        } else {
            isFileBased = true;
            loader = new FileBasedSandboxLoader();
        }
        loader.loadSandboxFromEntries(rootPath, overlay, sandbox);
    }
}
