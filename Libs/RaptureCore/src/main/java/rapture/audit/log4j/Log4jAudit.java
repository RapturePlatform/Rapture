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
package rapture.audit.log4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rapture.audit.AuditUtil;
import rapture.audit.BaseAuditImplementation;
import rapture.common.RaptureURI;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.AuditLogEntry;
import rapture.common.model.audit.Log4jAuditConfig;
import rapture.common.model.audit.Log4jAuditConfigStorage;
import rapture.home.RaptureHomeRetriever;
import rapture.config.LocalConfigService;

public class Log4jAudit extends BaseAuditImplementation {

    private static final String LOG_DIR_PATH = "\\$\\{LOG_DIR_PATH\\}";
    private static final Logger log = Logger.getLogger(Log4jAudit.class);
    private Logger auditLog;

    private LinkedList<AuditLogEntry> entryCache = new LinkedList<AuditLogEntry>();
    private boolean cycleCache = false;
    private int sizeOfCache = 0;
    private static final int MAXCACHE = 500;

    @Override
    public List<AuditLogEntry> getRecentEntries(int count) {
        synchronized (entryCache) {
            List<AuditLogEntry> entries = new ArrayList<AuditLogEntry>();
            entries.addAll(entryCache);
            return entries;
        }
    }

    @Override
    public void setConfig(String logId, Map<String, String> ignore) {
        /*
         * Set up the log4j config, which is stored in Rapture
         */
        reloadConfig();
        setLogId(logId);
    }

    public void reloadConfig() {
        String serverName = LocalConfigService.getServerName();
        Log4jAuditConfig config = Log4jAuditConfigStorage.readByFields(serverName);
        if (config != null) {
            String xml = config.getXml();
            File logDirBase;
            if (config.getLogDirPath() == null) {
                logDirBase = null;
                log.info("Log base dir is not defined");
            } else {
                logDirBase = new File(config.getLogDirPath());
                log.info("Log base dir is " + logDirBase.getAbsolutePath());
            }

            File logDirPath;
            if (logDirBase != null && logDirBase.exists()) {
                String appName = RaptureHomeRetriever.getAppName();
                if (appName == null) {
                    try {
                        logDirPath = File.createTempFile("unknown", "audit", logDirBase);
                    } catch (IOException e) {
                        throw RaptureExceptionFactory.create("Unable to create temp path " + ExceptionToString.format(e));
                    }
                    log.warn("App name is null so will write audit to unknown dir " + logDirPath.getName());
                    logDirPath.delete();
                    logDirPath.mkdir();
                } else {
                    logDirPath = new File(logDirBase, appName);
                    log.info("will write audit to " + logDirPath.getAbsolutePath());
                }
            } else {
                logDirPath = new File("audit");
                log.info("Log dir base does not exist, so will write audit to " + logDirPath.getAbsolutePath());
            }
            log.info("Log4j audit dir is " + logDirPath.getAbsolutePath());
            xml = xml.replaceAll(LOG_DIR_PATH, logDirPath.getAbsolutePath());
            ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());
            try {
                DOMConfigurator configurator = new DOMConfigurator();
                configurator.doConfigure(inputStream, org.apache.log4j.LogManager.getLoggerRepository());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Cannot close input stream " + ExceptionToString.format(e));
                }
            }
        }
        auditLog = Logger.getLogger("rapture.AuditLog");
    }

    @Override
    public void setInstanceName(String instanceName) {

    }

    @Override
    public Boolean writeLog(String category, int level, String message, String user) {
        AuditLogEntry entry = createAuditEntry(category, level, message, user);

        if (level == 0) {
            auditLog.info(entry.toString());
        } else if (level == 1) {
            auditLog.warn(entry.toString());
        } else if (level == 2) {
            auditLog.error(entry.toString());
        } else {
            auditLog.info(entry.toString());
        }

        synchronized (entryCache) {
            entryCache.addFirst(entry);
            if (!cycleCache) {
                sizeOfCache++;
                if (sizeOfCache >= MAXCACHE) {
                    cycleCache = true;
                }
            } else {
                entryCache.removeLast();
            }
        }
        return true;
    }

    @Override
    public Boolean writeLogData(String category, int level, String message, String user, Map<String, Object> data) {
        return writeLog(category, level, message + " " + AuditUtil.getStringRepresentation(data), user );
    }
    
    @Override
    public List<AuditLogEntry> getEntriesSince(AuditLogEntry when) {
        synchronized (entryCache) {
            List<AuditLogEntry> entries = new ArrayList<AuditLogEntry>();
            entries.addAll(entryCache);
            return entries;
        }
    }

    @Override
    public void setContext(RaptureURI internalURI) {
    }

}
