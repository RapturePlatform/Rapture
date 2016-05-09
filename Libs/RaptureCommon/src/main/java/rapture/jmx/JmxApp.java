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
package rapture.jmx;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class JmxApp {

    private static final Logger log = Logger.getLogger(JmxApp.class);

    private String url;
    private String name;
    private String host;
    private int port;

    public JmxApp(String url) {
        this.setUrl(url);
        URL urlObj;
        try {
            urlObj = new URL(url);
        } catch (MalformedURLException e) {
            log.error(String.format("Could not parse url string [%s]", url), e);
            return;
        }
        setName(stripLeadingAndTrailingSlash(urlObj.getPath()));
        setHost(urlObj.getHost());
        setPort(urlObj.getPort());
    }

    public JmxApp(String name, String host, int port) {
        setName(name);
        setHost(host);
        setPort(port);
        setUrl(String.format("http://%s:%d/%s/", host, port, name));
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    String stripLeadingAndTrailingSlash(String name) {
        if (StringUtils.isNotBlank(name)) {
            name = name.trim();
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
        }
        return name;
    }

    public String toString() {
        return String.format("%s@%s:%d", name, host, port);
    }
}