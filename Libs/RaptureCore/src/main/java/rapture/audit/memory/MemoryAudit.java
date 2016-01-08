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
package rapture.audit.memory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import rapture.audit.AuditUtil;
import rapture.audit.BaseAuditImplementation;
import rapture.common.RaptureURI;
import rapture.common.model.AuditLogEntry;

public class MemoryAudit extends BaseAuditImplementation {
    private static final int DEFAULT_MAX_ENTRIES = 100;
    private static final String MAX_ENTRIES = "maxEntries";

    private int maxEntries = DEFAULT_MAX_ENTRIES;
    // private String instanceName;

    // Recent entries are added to the head...
    private List<AuditLogEntry> entries = new LinkedList<AuditLogEntry>();

    public MemoryAudit() {
    }

    private synchronized void addAuditEntryAndTrim(AuditLogEntry entry) {
        entries.add(0, entry);
        if (entries.size() > maxEntries) {
            entries.subList(maxEntries, entries.size()).clear();
        }
    }

    @Override
    public void setInstanceName(String instanceName) {
        // this.instanceName = instanceName;
    }

    @Override
    public synchronized List<AuditLogEntry> getRecentEntries(int count) {
        if (entries.size() < count) {
            return ImmutableList.copyOf(entries);
        } else {
            return ImmutableList.copyOf(entries.subList(0, count));
        }
    }

    @Override
    public synchronized List<AuditLogEntry> getEntriesSince(AuditLogEntry when) {
        List<AuditLogEntry> ret = new ArrayList<AuditLogEntry>(20);
        boolean found = false;
        for (AuditLogEntry entry : entries) {
            if (!found) {
                if ((when == null) || entry.getEntryId().equals(when.getEntryId())) {
                    found = true;
                }
            } else {
                ret.add(entry);
            }
        }
        return ret;
    }

    @Override
    public void setConfig(String logId, Map<String, String> config) {
        setLogId(logId);
        setupFromConfig(config);
    }

    private void setupFromConfig(Map<String, String> config) {
        if (config.containsKey(MAX_ENTRIES)) {
            maxEntries = Integer.valueOf(config.get(MAX_ENTRIES));
        }
    }

    @Override
    public Boolean writeLog(String category, int level, String message, String user) {
        AuditLogEntry entry = createAuditEntry(category, level, message, user);
        addAuditEntryAndTrim(entry);
        return true;
    }
    
    @Override
    public Boolean writeLogData(String category, int level, String message, String user, Map<String, Object> data) {
        return writeLog(category, level, message + " " + AuditUtil.getStringRepresentation(data), user );
    }

    @Override
    public void setContext(RaptureURI internalURI) {
    }

}
