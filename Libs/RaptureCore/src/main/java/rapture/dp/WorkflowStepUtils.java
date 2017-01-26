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
package rapture.dp;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.Worker;
import rapture.common.dp.WorkerStorage;
import rapture.kernel.Kernel;

/**
 * Utility class for use in workflow steps to automatically log to the audit log. Also assists with passing values around between steps
 * 
 * @author dukenguyen
 *
 */
public class WorkflowStepUtils {

    private static final String INPUT_REF_KEY = "inputRef";
    private static final String URI_PREFIX_KEY = "uriPrefix";

    protected String workerUri;
    protected String stepName;
    private Logger log;

    public WorkflowStepUtils(String workerUri, String stepName) {
        this.workerUri = workerUri;
        this.stepName = stepName;
    }

    public Logger getLogger() {
        if (log == null) {
            log = Logger.getLogger(workerUri + "/" + stepName);
            Appender app = newAppender();
            // Remove any previous appender with the same name
            log.removeAppender(app.getName());
            log.addAppender(app);
        }
        return log;
    }

    public String getInputRef(CallingContext ctx) {
        return getBlobRef(ctx, INPUT_REF_KEY);
    }

    public void setInputRef(CallingContext ctx, String inputRef) {
        Kernel.getDecision().setContextLiteral(ctx, workerUri, INPUT_REF_KEY, inputRef);
    }

    public String getUriPrefix(CallingContext ctx) {
        return Kernel.getDecision().getContextValue(ctx, workerUri, URI_PREFIX_KEY);
    }

    public void setUriPrefix(CallingContext ctx, String uriPrefix) {
        Kernel.getDecision().setContextLiteral(ctx, workerUri, URI_PREFIX_KEY, uriPrefix);
    }

    public void putBlob(CallingContext ctx, String uri, String content, String contentType) {
        Kernel.getBlob().putBlob(ctx, uri, content.getBytes(StandardCharsets.UTF_8), contentType);
        log.info(String.format("Successfully stored blob at uri [%s] with contentType [%s]", uri, contentType));
    }

    String getAuditLogUri() {
        RaptureURI uri = new RaptureURI(workerUri, Scheme.WORKORDER);
        Worker worker = WorkerStorage.readByFields(uri.getShortPath(), uri.getElement());
        if (worker == null) {
            return InvocableUtils.getWorkflowAuditLog("unknown", uri.getShortPath(), stepName);
        }
        return InvocableUtils.getWorkflowAuditLog(worker.getAppStatusNameStack().get(0), worker.getWorkOrderURI(), stepName);
    }

    Appender newAppender() {
        return new AuditAppender(getAuditLogUri());
    }

    private String getBlobRef(CallingContext ctx, String key) {
        String ref = Kernel.getDecision().getContextValue(ctx, workerUri, key);
        if (StringUtils.isNotBlank(ref)) {
            BlobContainer blob = Kernel.getBlob().getBlob(ctx, ref);
            if (blob != null) {
                return new String(blob.getContent(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}