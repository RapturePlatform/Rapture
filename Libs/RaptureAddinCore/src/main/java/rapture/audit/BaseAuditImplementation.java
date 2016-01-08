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
package rapture.audit;

import java.util.Date;

import rapture.common.model.AuditLogEntry;
import rapture.util.IDGenerator;

public abstract class BaseAuditImplementation implements AuditLog {
    private String logId;

    protected BaseAuditImplementation() {
    }

    protected AuditLogEntry createAuditEntry(String category, int level, String message, String user) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setLevel(level);
        entry.setCategory(category);
        // entry.setSource(Kernel.getKernel().getAppStyle() + "."
        // + Kernel.getKernel().getAppId());
        entry.setLogId(getLogId());
        entry.setUser(user);
        entry.setMessage(message);
        entry.setWhen(new Date());
        entry.setEntryId(IDGenerator.getUUID());
        return entry;
    }

    protected String getLogId() {
        return logId;
    }

    protected void setLogId(String logId) {
        this.logId = logId;
    }
}
