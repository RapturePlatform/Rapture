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
package rapture.script;

import java.util.Date;

import rapture.common.impl.jackson.JsonContent;

/**
 * A RaptureDataContext is a convenient vehicle for storing information about
 * data - particularly content that has been rendered to a map structure, who
 * created it, the full displayname, when it was written etc.
 * 
 * This can be passed and used by scripts as needed.
 * 
 * @author alan
 * 
 */
public class RaptureDataContext {
    private String displayName;

    private String user;

    private Date when;

    private JsonContent jsonContent;

    public String getDisplayName() {
        return displayName;
    }

    public JsonContent getJsonContent() {
        return jsonContent;
    }

    public String getUser() {
        return user;
    }

    public Date getWhen() {
        return when;
    }

    public void putJsonContent(JsonContent content) {
        this.jsonContent = content;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setJsonContent(JsonContent content) {
        this.jsonContent = content;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setWhen(Date when) {
        this.when = when;
    }
}
