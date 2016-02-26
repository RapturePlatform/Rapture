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
package rapture.audit.blob;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import rapture.audit.AuditUtil;
import rapture.audit.BaseAuditImplementation;
import rapture.common.*;
import rapture.common.model.AuditLogEntry;
import rapture.kernel.BlobApiImpl;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class BlobAudit extends BaseAuditImplementation {
    private String blobUri;
    private String blobPrefix;
    final String pattern = "YYYY-MM-dd - HH:mm:ss.SS z";
    final DateTimeFormatter df = DateTimeFormat.forPattern(pattern);

    @Override
    public List<AuditLogEntry> getRecentEntries(int count) {
        String docPath = blobPrefix;
        RaptureURI uri = RaptureURI.builder(Scheme.BLOB, blobUri).docPath(docPath).build();
        CallingContext context = ContextFactory.getKernelUser();
        BlobApiImpl blobApi = Kernel.getBlob().getTrusted();

        ArrayList<AuditLogEntry> ret = new ArrayList<AuditLogEntry>();
        BlobContainer content = blobApi.getBlob(context, uri.toString());
        if (content != null) {
            addContentToList(content, ret);
            return (ret.size() > count) ? ret.subList(ret.size() - count, ret.size()) : ret;
        } else {
            // get children blobs, eg. get logs for all steps of a work order
            Map<String, RaptureFolderInfo> childrenMap = blobApi.listBlobsByUriPrefix(context, uri.toString(), 1);

            if (childrenMap != null) {
                for (RaptureFolderInfo child : childrenMap.values()) {
                    if (child.isFolder()) {
                        continue;
                    }
                    String childUri = uri.getFullPath() + "/" + child.getName();
                    content = blobApi.getBlob(context, childUri);
                    addContentToList(content, ret, true);
                    if (ret.size() >= count) {
                        break;
                    }
                }
            }
        }
        // sort audit log entries by time
        Collections.sort(ret, new Comparator<AuditLogEntry>() {
            @Override
            public int compare(AuditLogEntry o1, AuditLogEntry o2) {
                return o1.getWhen().compareTo(o2.getWhen());
            }
        });
        return (ret.size() > count) ? ret.subList(0, count) : ret;

    }


    private void addContentToList(BlobContainer content, List<AuditLogEntry> list) {
        addContentToList(content, list, false);
    }

    private void addContentToList(BlobContainer content, List<AuditLogEntry> list, boolean setTime) {
        if (content != null) {
            byte[] bytes = content.getContent();
            if (bytes != null) {
                String string = new String(bytes);
                List<String> data = Arrays.asList(string.split("\n"));
                for (String line : data) {
                    AuditLogEntry entry = new AuditLogEntry();
                    entry.setCategory("");
                    entry.setLevel(1);
                    entry.setMessage(line);
                    if (setTime) {
                        entry.setWhen(getTime(line));
                    }
                    list.add(entry);
                }
            }
        }
    }

    private Date getTime(String line) {
        try {
            String when = line.substring(0, 28); // eg. "2015-02-05 - 02:30:04.58 PST"
            return new SimpleDateFormat(pattern, Locale.getDefault()).parse(when);
        } catch (ParseException e) {
            return new Date();
        }
    }

    @Override
    public void setConfig(String logId, Map<String, String> config) {
        // blobRepo is the blobRepo to use
        // + the internalUri docPath in there
        blobUri = config.get("blobRepo");
    }

    @Override
    public void setInstanceName(String instanceName) {
    }

    @Override
    public Boolean writeLog(String category, int level, String message, String user) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(df.print(DateTime.now()));
        logMessage.append(" ");
        logMessage.append(message);
        logMessage.append("\n");

        String docPath = blobPrefix;
        RaptureURI uri = RaptureURI.builder(Scheme.BLOB, blobUri).docPath(docPath).build();
        Kernel.getBlob().getTrusted().appendToBlobLower(ContextFactory.getKernelUser(), uri.toString(), logMessage.toString().getBytes(), "text/plain");
        return true;
    }

    @Override
    public Boolean writeLogData(String category, int level, String message, String user, Map<String, Object> data) {
        return writeLog(category, level, message + " " + AuditUtil.getStringRepresentation(data), user);
    }


    @Override
    public List<AuditLogEntry> getEntriesSince(AuditLogEntry when) {
        return new ArrayList<AuditLogEntry>();
    }

    @Override
    public void setContext(RaptureURI internalURI) {
        blobPrefix = internalURI.getDocPath();
    }


}
