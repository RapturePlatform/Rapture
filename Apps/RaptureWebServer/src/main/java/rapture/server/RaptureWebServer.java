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
package rapture.server;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import rapture.api.hooks.impl.TopicPubHook;
import rapture.apiutil.StatusHelper;
import rapture.app.RaptureAppService;
import rapture.common.ApplicationCapability;
import rapture.common.MessageFormat;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.HttpStructuredApi;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.hooks.HookFactory;
import rapture.common.hooks.HookType;
import rapture.common.hooks.SingleHookConfig;
import rapture.common.impl.jackson.MD5Utils;
import rapture.config.ConfigLoader;
import rapture.config.MultiValueConfigLoader;
import rapture.home.RaptureHomeRetriever;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.cache.SysRepoCache;
import rapture.module.AddinLoader;
import rapture.server.web.servlet.ApiCapabilitiesService;
import rapture.util.IDGenerator;

public class RaptureWebServer {

    private static Logger logger = Logger.getLogger(RaptureWebServer.class);

    /**
     * @param args
     */
    public static void main(String[] args) {

        // Give -v flag or --version to print out version numbers and quit

        if (args.length > 0) {
            if (args[0].equals("-v") || args[0].equalsIgnoreCase("--version")) {
                System.out.println("Version : \n" + Kernel.versions());
                return;
            }
        }

        try {
            Kernel.initBootstrap(ImmutableMap.of("STD", ConfigLoader.getConf().StandardTemplate), RaptureWebServer.class, false);
            RaptureAppService.setupApp("RaptureWebServer");
            RaptureWebServer s = new RaptureWebServer();

            String categories = ConfigLoader.getConf().Categories;
            logger.info(String.format("Categories are %s", categories));
            String[] cats = categories.split(",");
            for (String cat : cats) {
                logger.info("Category " + cat);
                Kernel.setCategoryMembership(cat);
            }

            s.startApiWebServer();
            s.startWebServer();
            s.addHooks();

            Map<String, Object> capabilities = getCapabilities(categories);
            StatusHelper.setStatusAndCapability(ContextFactory.getKernelUser(), "RUNNING", capabilities, Kernel.getRunner());
            StatusHelper.startStatusUpdating(ContextFactory.getKernelUser(), Kernel.getRunner());
            logger.debug("Updated status");
            s.joinServer();
        } catch (RaptureException e) {
            logger.error("Error when running Rapture - " + ExceptionToString.format(e));
        }
        logger.info("Application Rapture exited");
    }

    private void addHooks() {
        TopicPubHook hook = new TopicPubHook();
        String id = TopicPubHook.getStandardId();
        ArrayList<HookType> hookTypes = Lists.newArrayList(HookType.PRE);
        ArrayList<String> includes = Lists.newArrayList();
        ArrayList<String> excludes = Lists.newArrayList("Pipeline.*", "Notification.*");
        SingleHookConfig hookConfig = HookFactory.INSTANCE.createConfig(hook, id, hookTypes, includes, excludes);
        // add TopicPubHook to the default hooks that come w/ the Rapture kernel
        Kernel.getApiHooksService().addSingleHookConfig(hookConfig);
    }

    private static Map<String, Object> getCapabilities(String categories) {
        Map<String, Object> capabilities = new HashMap<String, Object>();
        capabilities.put("PipelineHandler", categories);
        String localApiUrl = ApiCapabilitiesService.getApiUrlForThisHost();
        logger.info("Local api url is " + localApiUrl);
        capabilities.put(ApplicationCapability.APIURL, localApiUrl);
        return capabilities;
    }

    private Tomcat apiTomcat;
    private Tomcat webTomcat;

    public RaptureWebServer() {
    }

    public void joinServer() {
        try {
            while (true) {
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            logger.error("Error when running or starting RaptureServer - " + e.getMessage());
        }
    }

    public void startApiWebServer() {
        AddinLoader.loadAddins();
        logger.info("Configured as a web server");
        Kernel.getKernel().setAppStyle("webapp");
        Kernel.getKernel().setAppId(IDGenerator.getUUID());
        String portAsString = "8665";
        logger.info("Configuring server on port " + portAsString);
        Integer portAsInt = Integer.valueOf(portAsString);
        apiTomcat = new Tomcat();
        apiTomcat.setPort(portAsInt);
        logger.info("Starting ApiWebServer for Curtis");
        try {
            apiTomcat.addWebapp("/rapture", new File("apiwebapp").getAbsolutePath()).setJarScanner(new EmptyJarScanner());
            apiTomcat.start();
        } catch (Exception e) {
            logger.error("Error when running or starting RaptureServer - " + e.getMessage());
        }
    }

    public void startWebServer() {
        AddinLoader.loadAddins();
        logger.info("Configured as a web server");
        Kernel.getKernel().setAppStyle("webapp");
        Kernel.getKernel().setAppId(IDGenerator.getUUID());
        String portAsString = "8080";
        logger.info("Configuring server on port " + portAsString);
        Integer portAsInt = Integer.valueOf(portAsString);
        webTomcat = new Tomcat();
        webTomcat.setPort(portAsInt);
        logger.info("Starting WebServer");
        try {
            webTomcat.addWebapp("/", new File("webapp").getAbsolutePath()).setJarScanner(new EmptyJarScanner());
            webTomcat.start();
        } catch (Exception e) {
            logger.error("Error when running or starting RaptureServer - " + e.getMessage());
        }
    }

    public void stopServer() {
        try {
            apiTomcat.stop();
            webTomcat.stop();
        } catch (Exception e) {
            logger.error("Error when running or starting RaptureServer - " + e.getMessage());
        }
    }

    /**
     * No need to scan for TLDs since we do not use those. Greatly improves embedded tomcat start-up time.
     *
     * @author dukenguyen
     *
     */
    private class EmptyJarScanner implements JarScanner {
        @Override
        public void scan(JarScanType scanType, ServletContext context, JarScannerCallback callback) {
        }

        @Override
        public JarScanFilter getJarScanFilter() {
            return null;
        }

        @Override
        public void setJarScanFilter(JarScanFilter jarScanFilter) {
        }
    }
}
