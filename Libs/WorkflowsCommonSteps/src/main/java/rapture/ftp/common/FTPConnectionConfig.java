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
package rapture.ftp.common;

import java.io.Serializable;

import org.apache.log4j.Logger;

import rapture.object.Debugable;

public class FTPConnectionConfig implements Serializable, Debugable {
    
    private static final Logger log = Logger.getLogger(FTPConnectionConfig.class);

    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_TIMEOUT = 60; // seconds
    public static final int MAX_RETRIES = 5;
    public static final int RETRY_WAIT = 15; // seconds

    private String loginId;
    private String password;
    private String privateKey;
    private String address;
    private boolean useSFTP = false;
    private int port = 23;
    private int retryCount = MAX_RETRIES;
    private int retryWait = RETRY_WAIT;
    private int timeout = DEFAULT_TIMEOUT;

    public String getLoginId() {
        return loginId;
    }

    public FTPConnectionConfig setLoginId(String loginId) {
        this.loginId = loginId;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public FTPConnectionConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public FTPConnectionConfig setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public FTPConnectionConfig setAddress(String address) {
        this.address = address;
        return this;
    }

    public int getPort() {
        return port;
    }

    public FTPConnectionConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isUseSFTP() {
        return useSFTP;
    }

    public FTPConnectionConfig setUseSFTP(boolean useSFTP) {
        this.useSFTP = useSFTP;
        return this;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public FTPConnectionConfig setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public int getRetryWait() {
        return retryWait;
    }

    public FTPConnectionConfig setRetryWait(int retryWait) {
        if (retryWait > 1000) {
            log.warn("retryWait should be specified in seconds. Is "+Math.abs(retryWait/60)+" minutes really correct?");
        }
        this.retryWait = retryWait * 1000;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public FTPConnectionConfig setTimeout(int timeout) {
        if (timeout > 1000) {
            log.warn("timeout should be specified in seconds. Is " + Math.abs(timeout / 60) + " minutes really correct?");
        }
        this.timeout = timeout * 1000;
        return this;
    }

    @Override
    public String debug() {
        return toString();
    }
}
