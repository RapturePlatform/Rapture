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
package rapture.config;

import org.apache.log4j.Logger;

import rapture.util.NetworkUtil;

public class LocalConfigService {

    private static final Logger log = Logger.getLogger(LocalConfigService.class);

    /**
     * Returns the server name, first attempting to retrieve it from the
     * configuration file, and failing that it will use the name of this host
     * 
     * @return
     */
    public static String getServerName() {
        String serverName = MultiValueConfigLoader.getConfig("RUNNER-serverName");
        if (serverName == null) {
            log.debug("Will obtain server name from machine name");
            serverName = NetworkUtil.getServerName();
        } else {
            log.debug("This server is known as " + serverName);
        }
        return serverName;
    }
}
