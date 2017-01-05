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
package rapture.mail;

public class SMTPConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String from;
    private String debug = null; // Logging level to write mail transcript out. See org.apache.log4j.Level

    private boolean authentication = false;
    private boolean tlsenable = false;
    private boolean tlsrequired = false;


    public String getUsername() {
        return username;
    }

    public boolean isAuthentication() {
        return authentication;
    }

    public SMTPConfig setAuthentication(boolean authentication) {
        this.authentication = authentication;
        return this;
    }

    public boolean isTlsenable() {
        return tlsenable;
    }

    public SMTPConfig setTlsenable(boolean tlsenable) {
        this.tlsenable = tlsenable;
        return this;
    }

    public boolean isTlsrequired() {
        return tlsrequired;
    }

    public SMTPConfig setTlsrequired(boolean tlsrequired) {
        this.tlsrequired = tlsrequired;
        return this;
    }

    @Override
    public String toString() {
        return "SMTPConfig [host=" + host + ", port=" + port + ", username=" + username + ", password=" + password + ", from=" + from + ", authentication="
                + authentication + ", tlsenable=" + tlsenable + ", tlsrequired=" + tlsrequired + "]";
    }

    public SMTPConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SMTPConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getHost() {
        return host;
    }

    public SMTPConfig setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public SMTPConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public SMTPConfig setFrom(String from) {
        this.from = from;
        return this;
    }

    public String getDebug() {
        return debug;
    }

    public SMTPConfig setDebug(String debug) {
        this.debug = debug;
        return this;
    }
}
