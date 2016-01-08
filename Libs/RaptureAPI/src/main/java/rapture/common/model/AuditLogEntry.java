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

import java.util.Date;

import rapture.common.RaptureTransferObject;

public class AuditLogEntry implements RaptureTransferObject {
    private String category;
    private String entryId = "";
    private int level; // 0=normal, 1=warning, 2=error
    private String logId;
    private String message;
    private String source;
    private String user;
    private Date when;

    public String getCategory() {
        return category;
    }

    public String getEntryId() {
        return entryId;
    }

    public int getLevel() {
        return level;
    }

    public String getLogId() {
        return logId;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public String getUser() {
        return user;
    }

    public Date getWhen() {
        return when;
    }

    public String retrieveLevelString() {
        switch (level) {
            case 0:
                return "info";
            case 1:
                return "warning";
            case 2:
                return "important";
            default:
                return "success";
        }
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    @Override
    public String toString() {
        return when + " " + "(user=" + user + ") " + " [" + logId + "] " + category + ": " + message;
    }
}
