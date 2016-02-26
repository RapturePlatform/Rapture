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
package rapture.common.model;

import rapture.common.RaptureTransferObject;

public class RaptureEventScript implements RaptureTransferObject {
    private String scriptURI;
    private Boolean runOnce;
    private Long fireCount;

    public Long getFireCount() {
        return fireCount;
    }

    public Boolean getRunOnce() {
        return runOnce;
    }

    public String getScriptURI() {
        return scriptURI;
    }
    
    /**
     * This is a temporary method for backwards compatibility.  Do not use
     */
    @Deprecated
    public void setScriptName(String uri) {
        scriptURI = uri;
    }

    public void setFireCount(Long fireCount) {
        this.fireCount = fireCount;
    }

    public void setRunOnce(Boolean runOnce) {
        this.runOnce = runOnce;
    }

    public void setScriptURI(String scriptName) {
        this.scriptURI = scriptName;
    }
}
