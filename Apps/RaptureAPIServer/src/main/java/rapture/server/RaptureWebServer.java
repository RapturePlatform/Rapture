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
package rapture.server;

import java.io.File;

import org.apache.catalina.startup.Tomcat;
import org.apache.log4j.Logger;

import rapture.app.RaptureAppService;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.kernel.Kernel;
import rapture.module.AddinLoader;
import rapture.util.IDGenerator;
import rapture.config.ConfigLoader;

/**
 * This is the entry point for the RaptureServer
 * 
 * @author alan
 * 
 */
public final class RaptureWebServer {

    private Tomcat tomcat;

    public void startWebServer() {
        // Heroku uses the PORT environment variable to define the port to use
        AddinLoader.loadAddins();
        log.info("Configured as a web server");
        Kernel.getKernel().setAppStyle("webapp");
        Kernel.getKernel().setAppId(IDGenerator.getUUID());
        String portAsString = System.getenv("PORT");
        if (portAsString == null) {
            portAsString = ConfigLoader.getConf().web.port;
        }
        if (portAsString == null || portAsString.isEmpty()) {
            portAsString = "8665";
        }
        log.info("Configuring server on port " + portAsString);
        Integer portAsInt = Integer.valueOf(portAsString);
        tomcat = new Tomcat();
        tomcat.setPort(portAsInt);
        log.info("Starting WebServer for Rapture");
        try {
            // tomcat.addWebapp("/rapture", new
            // File("src/main/webapp").getAbsolutePath());
            tomcat.addWebapp("/rapture", new File("webapp").getAbsolutePath());
            tomcat.start();
            log.info("Main thread joining server");
        } catch (Exception e) {
            log.error("Error when running or starting RaptureServer - " + e.getMessage());
        }
    }

    /*
     * public void startWebServer() throws URISyntaxException { // Heroku uses
     * the PORT environment variable to define the port to use
     * AddinLoader.loadAddins(); String portAsString = System.getenv("PORT"); if
     * (portAsString == null) { portAsString = ConfigLoader.getConf().web.port;
     * } if (portAsString == null || portAsString.isEmpty()) { portAsString =
     * "8665"; } log.info("Configuring server on port " + portAsString); Integer
     * portAsInt = Integer.valueOf(portAsString); raptureServer = new
     * Server(portAsInt.intValue()); // Bind Jetty to the web.xml in our
     * resources
     * 
     * File currentPath = new File(RaptureWebServer.class
     * .getProtectionDomain().getCodeSource().getLocation().toURI());
     * currentPath = currentPath.getParentFile(); WebAppContext webContext = new
     * WebAppContext(); webContext.setContextPath("/"); // The war file name,
     * particularly the version, comes from the RaptureApplication.cfg, value //
     * APIVersion
     * 
     * String warVersion =
     * MultiValueConfigLoader.getConfig("Application-APIVersion");
     * System.out.println("War version is " + warVersion); String warFile = new
     * File(currentPath, "RaptureWebAPI-" + warVersion + ".war")
     * .getAbsolutePath(); System.out.println("War file is " + warFile);
     * webContext.setWar(warFile); webContext.setContextPath("/rapture");
     * webContext.setClassLoader(ClassLoader.getSystemClassLoader());
     * webContext.setInitParameter(
     * "org.eclipse.jetty.servlet.Default.dirAllowed", "false");
     * 
     * raptureServer.setHandler(webContext);
     * log.info("Starting WebServer for Rapture"); try { raptureServer.start();
     * log.info("Main thread joining server"); } catch (Exception e) {
     * log.error("Error when running or starting RaptureServer - " +
     * e.getMessage()); } }
     */

    public void joinServer() {
        try {
            while (true) {
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            log.error("Error when running or starting RaptureServer - " + e.getMessage());
        }
    }

    public void stopServer() {
        try {
            tomcat.stop();
        } catch (Exception e) {
            log.error("Error when running or starting RaptureServer - " + e.getMessage());
        }
    }

    public RaptureWebServer() {
    }

    private static Logger log = Logger.getLogger(RaptureWebServer.class);

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
            RaptureAppService.setupApp("RaptureAPIServer");
            RaptureWebServer s = new RaptureWebServer();
            s.startWebServer();

            String categories = ConfigLoader.getConf().Categories;
            String[] cats = categories.split(",");
            log.info("Categories are: " + categories);
            for (String cat : cats) {
                Kernel.setCategoryMembership(cat);
            }
            s.joinServer();
        } catch (RaptureException e) {
            log.error("Error when running Rapture - " + ExceptionToString.format(e));
        }
        log.info("Application Rapture exited");
    }

}
