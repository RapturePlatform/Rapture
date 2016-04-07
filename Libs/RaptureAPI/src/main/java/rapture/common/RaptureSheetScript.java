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
package rapture.common;

import java.util.Date;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
**/

@Deprecated
public class RaptureSheetScript {
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "RaptureSheetScript [name=" + name + ", description=" + description + ", script=" + script + ", autoRun=" + autoRun + ", intervalInSeconds="
                + intervalInSeconds + ", lastRun=" + lastRun + "]";
    }
    @Deprecated
    public boolean isAutoRun() {
        return autoRun;
    }
    @Deprecated
    public void setAutoRun(boolean autoRun) {
        this.autoRun = autoRun;
    }
    @Deprecated
    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }
    @Deprecated
    public void setIntervalInSeconds(int intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }
    @Deprecated
    public Date getLastRun() {
        return lastRun;
    }
    @Deprecated
    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }
    @Deprecated
    public String getName() {
        return name;
    }
    @Deprecated
    public void setName(String name) {
        this.name = name;
    }
    @Deprecated
    public String getDescription() {
        return description;
    }
    @Deprecated
    public void setDescription(String description) {
        this.description = description;
    }
    @Deprecated
    public String getScript() {
        return script;
    }
    @Deprecated
    public void setScript(String script) {
        this.script = script;
    }
    private String name;
    private String description;
    private String script;
    private boolean autoRun = false;
    private int intervalInSeconds = 0;
    private Date lastRun = null;
}
