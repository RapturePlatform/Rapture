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
package rapture.common.model;

import rapture.common.version.ApiVersion;

public class LoginLoginData {
    private String user;

    private String digest;

    private String context;

    private ApiVersion clientApiVersion;

    public ApiVersion getClientApiVersion() {
        return clientApiVersion;
    }

    public void setClientApiVersion(ApiVersion clientApiVersion) {
        this.clientApiVersion = clientApiVersion;
    }

    public String getContext() {
        return context;
    }

    public String getDigest() {
        return digest;
    }

    public String getUser() {
        return user;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
