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
package rapture.common.version;

import org.apache.log4j.Logger;

import rapture.server.ServerApiVersion;

public enum ApiVersionComparator {
    INSTANCE;

    private Logger log = Logger.getLogger(getClass());

    public boolean isCompatible(ApiVersion clientApiVersion) {
        ApiVersion min = ServerApiVersion.getMinimumVersion();
        log.info("isApiCompatible? : clientApiVersion=" + clientApiVersion + "; minimumVersion=" + min);
        if (clientApiVersion == null) {
            log.warn("Allowing client that does not report a version to connect");
            return true;
        }
        return atLeast(clientApiVersion, min);
    }

    public static boolean atLeast(ApiVersion jumper, ApiVersion bar) {        
        if (jumper.getMajor() > bar.getMajor()) return true;
        if (jumper.getMajor() < bar.getMajor()) return false;

        if (jumper.getMinor() > bar.getMinor()) return true;
        if (jumper.getMinor() < bar.getMinor()) return false;

        if (jumper.getMicro() < bar.getMicro()) return false;
        return true;
    }
}
