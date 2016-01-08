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
package rapture.app;

import org.apache.log4j.Logger;

import rapture.jmx.RaptureMBeanServer;
import rapture.log.LogManager;

/**
 * @author bardhi
 * @since 9/16/14.
 */
public class RaptureAppService {
    private static final Logger log = Logger.getLogger(RaptureAppService.class);

    /**
     * Performs the setup steps that are common among all Rapture apps. Specifically, configuring the log appenders and setting up JMX
     *
     * @param appName The name of the app
     */
    public static void setupApp(String appName) {
        LogManager.configureLogging();
        log.info(String.format("Starting %s", appName));
        log.info("==================================");

        RaptureMBeanServer.initialise(appName);
    }



}
