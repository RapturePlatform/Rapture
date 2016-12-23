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
package rapture.kernel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import rapture.audit.AuditEvent;
import rapture.audit.AuditListener;
import rapture.audit.AuditLog;
import rapture.audit.log4j.Log4jAudit;
import rapture.common.AuditLogConfig;
import rapture.common.AuditLogConfigStorage;
import rapture.common.CallingContext;
import rapture.common.Messages;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.AuditApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.AuditLogEntry;
import rapture.common.model.audit.Log4jAuditConfig;
import rapture.common.model.audit.Log4jAuditConfigStorage;
import rapture.config.ConfigLoader;
import rapture.config.LocalConfigService;

/**
 * Admin is really dealing with the topLevel admin needs of a RaptureServer
 * 
 * @author amkimian
 * 
 */
public class AuditApiImpl extends KernelBase implements AuditApi {
    private static final RaptureURI KERNEL_URI = RaptureURI.builder(Scheme.LOG, "kernel").build();
    private static final Logger log = Logger.getLogger(AuditApiImpl.class);

    private Messages auditMsgCatalog;

    public AuditApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        auditMsgCatalog = new Messages("Audit");
    }

    @Override
    public void createAuditLog(CallingContext context, String name, String config) {
        AuditLogConfig cfg = new AuditLogConfig();
        cfg.setConfig(config);
        cfg.setName(name);
        AuditLogConfigStorage.add(cfg, context.getUser(), Messages.getString("Audit.CreateAuditLog")); //$NON-NLS-1$
        RaptureURI internalURI = new RaptureURI(name, Scheme.LOG);
        Kernel.getKernel().resetLogCache(internalURI);
    }

    @Override
    public void deleteAuditLog(CallingContext context, String logURI) {
        RaptureURI internalURI = new RaptureURI(logURI, Scheme.LOG);
        AuditLogConfigStorage.deleteByAddress(internalURI, context.getUser(), Messages.getString("Audit.RemoveAuditLog")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public Boolean doesAuditLogExist(CallingContext context, String logURI) {
        return getAuditLog(context, logURI) != null;
    }

    @Override
    public AuditLogConfig getAuditLog(CallingContext context, String logURI) {
        RaptureURI internalURI = new RaptureURI(logURI, Scheme.LOG);
        return AuditLogConfigStorage.readByAddress(internalURI);
    }

    @Override
    public List<AuditLogEntry> getRecentLogEntries(CallingContext context, String logURI, int count) {
        RaptureURI internalURI = new RaptureURI(logURI, Scheme.LOG);
        AuditLog ilog = Kernel.getKernel().getLog(context, internalURI);
        if (ilog != null) {
            return ilog.getRecentEntries(count);
        } else {
            return null;
        }
    }

    @Override
    public void writeAuditEntry(CallingContext context, String logURI, String category, int level, String message) {
        RaptureURI internalURI = new RaptureURI(logURI, Scheme.LOG);
        AuditLog ilog = Kernel.getKernel().getLog(context, internalURI);
        if (ilog != null) {
            ilog.writeLog(category, level, message, context.getUser());
        }
        notifyListeners(category, level, message);
    }

    @Override
    public void writeAuditEntryData(CallingContext context, String logURI, String category, int level, String message, Map<String, Object> data) {
        RaptureURI internalURI = new RaptureURI(logURI, Scheme.LOG);
        AuditLog ilog = Kernel.getKernel().getLog(context, internalURI);
        if (ilog != null) {
            ilog.writeLogData(category, level, message, context.getUser(), data);
        }
        notifyListeners(category, level, message);
    }

    @Override
    public List<AuditLogEntry> getEntriesSince(CallingContext context, String logURI, AuditLogEntry when) {
        RaptureURI internalURI = new RaptureURI(logURI, Scheme.LOG);
        AuditLog ilog = Kernel.getKernel().getLog(context, internalURI);
        if (ilog != null) {
            return ilog.getEntriesSince(when);
        } else {
            return null;
        }
    }

    @Override
    public List<AuditLogEntry> getRecentUserActivity(CallingContext context, String user, int count) {
        if (count == 0) {
            return new ArrayList<>();
        }
        RaptureURI internalURI = new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG);
        AuditLog ilog = Kernel.getKernel().getLog(context, internalURI);
        if (ilog == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, auditMsgCatalog.getMessage("KernelLogInvalid"));
        }
        return ilog.getRecentUserActivity(user, count);
    }

    @Override
    public void setup(CallingContext context, Boolean force) {
        /*
         * Create kernel logs config if it does not exist
         */
        String defaultAudit = ConfigLoader.getConf().DefaultAudit;
        if (force || !doesAuditLogExist(context, KERNEL_URI.toString())) {
            log.info("Creating default-based audit log for kernel");
            log.info("Default audit config is " + defaultAudit);
            createAuditLog(context, "kernel", defaultAudit);
        } else {
            log.info("Audit config exists for kernel");
        }

        /*
         * Set up the log4j config
         */
        String serverName = LocalConfigService.getServerName();
        Log4jAuditConfig log4jConfig = Log4jAuditConfigStorage.readByFields(serverName);
        if (force || log4jConfig == null) {
            log.info("Creating log4j audit config");
            log4jConfig = new Log4jAuditConfig();
            File optRapturePath = new File("/opt/rapture");
            File optRaptureAuditDir = new File(optRapturePath, "audit");
            File auditDir;
            auditDir = optRaptureAuditDir;

            log4jConfig.setLogDirPath(auditDir.getAbsolutePath());
            InputStream xmlIn = getClass().getClassLoader().getResourceAsStream("audit/auditLog4j.xml");

            String xml;
            try {
                xml = IOUtils.toString(xmlIn, CharEncoding.UTF_8);
            } catch (IOException e) {
                throw RaptureExceptionFactory.create("Unable to set up audit logger: " + ExceptionToString.format(e));
            } finally {
                try {
                    xmlIn.close();
                } catch (IOException e) {
                    log.error("Error closing input stream: " + ExceptionToString.format(e));
                }
            }
            log4jConfig.setXml(xml);
            log4jConfig.setServerName(LocalConfigService.getServerName());
            Log4jAuditConfigStorage.add(log4jConfig, context.getUser(), "Audit setup");

            AuditLog ilog = Kernel.getKernel().getLog(context, KERNEL_URI);
            if (ilog instanceof Log4jAudit) {
                Log4jAudit log4jLog = (Log4jAudit) ilog;
                log4jLog.reloadConfig();
            }
        } else {
            log.info("log4j audit config exists");
        }

        log.info("Done setting up audit");
    }

    @Override
    public List<RaptureFolderInfo> getChildren(CallingContext context, String prefix) {
        return AuditLogConfigStorage.getChildren(prefix);
    }

    private List<AuditListener> listeners = new CopyOnWriteArrayList<>();
    boolean auditAllExceptions = false;
    
    public void addAuditListener(AuditListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public boolean removeAuditListener(AuditListener listener) {
        boolean retVal = listeners.contains(listener);
        if (retVal) listeners.remove(listener);
        return retVal;
    }

    public void notifyListeners(String category, int level, String message) {
        AuditEvent aevent = new AuditEvent(category, level, message);
        for (AuditListener listener : listeners) listener.notify(aevent);
    }
    
    public void writeException(RaptureException re) {
        if (auditAllExceptions) {
            writeAuditEntry(ContextFactory.getKernelUser(), RaptureConstants.DEFAULT_AUDIT_URI, "exception", 2, re.getMessage());
        } else {
            notifyListeners("exception", 2, ExceptionToString.format(re));
        }
    }
}
