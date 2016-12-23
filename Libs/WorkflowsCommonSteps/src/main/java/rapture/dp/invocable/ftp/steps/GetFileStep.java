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
package rapture.dp.invocable.ftp.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.api.DecisionApi;
import rapture.common.dp.AbstractInvocable;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.ftp.common.Connection;
import rapture.ftp.common.FTPRequest;
import rapture.ftp.common.FTPRequest.Action;
import rapture.ftp.common.FTPRequest.Status;
import rapture.ftp.common.SFTPConnection;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;

public class GetFileStep extends AbstractInvocable {
    private static final Logger log = Logger.getLogger(GetFileStep.class);

    DecisionApi decision;

    public GetFileStep(String workerUri, String stepName) {
        super(workerUri, stepName);
        decision = Kernel.getDecision();
    }

    @Override
    public String invoke(CallingContext ctx) {
        Connection connection = null;
        try {
            decision.setContextLiteral(ctx, getWorkerURI(), "STEPNAME", getStepName());

            String copy = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "GET_FILES"));
            if (copy == null) {
                // Try deprecated FETCH_FILES
                copy = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "FETCH_FILES"));
                if (copy != null) {
                    decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "FETCH_FILES parameter is deprecated - please use GET_FILES", true);
                }
            }
            if (copy == null) {
                decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "GET_FILES context variable is not set");
                decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), getStepName() + ": No files to copy - GET_FILES context variable is not set", false);
                return getNextTransition();
            }

            String workOrder = new RaptureURI(getWorkerURI()).toShortString();
            String json = ExecutionContextUtil.evalTemplateECF(ctx, workOrder, copy, new HashMap<>());
            Map<String, Object> map = JacksonUtil.getMapFromJson(json);

            String configUri = decision.getContextValue(ctx, getWorkerURI(), "FTP_CONFIGURATION");
            if (configUri == null) {
                decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "FTP_CONFIGURATION not set");
                decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in " + getStepName() + " - parameter FTP_CONFIGURATION is not set", true);
                return getErrorTransition();
            }

            if (!Kernel.getDoc().docExists(ctx, configUri)) {
                decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "Cannot load FTP_CONFIGURATION from " + configUri);
                decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Problem in " + getStepName() + " - Cannot load FTP_CONFIGURATION from " + configUri,
                        true);
                return getErrorTransition();
            }

            String retval = getNextTransition();
            int failCount = 0;
            int successCount = 0;
            List<FTPRequest> requests = new ArrayList<>();
            connection = new SFTPConnection(configUri).setContext(ctx);
            for (Entry<String, Object> e : map.entrySet()) {
                FTPRequest request = new FTPRequest(Action.READ).setRemoteName(e.getKey()).setLocalName(e.getValue().toString());
                connection.doAction(request);
                if (!request.getStatus().equals(Status.SUCCESS)) {
                    String errors = request.getErrors();
                    if (errors != null) {
                        decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), getStepName() + ": " + errors, true);
                    }
                    decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Unable to retrieve " + e.getKey() + " as " + e.getValue(), true);
                    retval = getFailTransition();
                    failCount++;
                } else {
                    decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Retrieved " + e.getKey(), false);
                    successCount++;
                }
                requests.add(request);
            }
            String retrieved = (failCount > 0) ? "Unable to retrieve " + failCount + " files" : successCount + " files retrieved";
            decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), retrieved);
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), retrieved, true);
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), retrieved);
            return retval;
        } catch (Exception e) {
            decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "Unable to retrieve files : " + e.getLocalizedMessage());
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), ExceptionToString.summary(e));
            log.error(ExceptionToString.format(ExceptionToString.getRootCause(e)));
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(),
                    "Problem in " + getStepName() + ": " + ExceptionToString.getRootCause(e).getLocalizedMessage(), true);
            return getErrorTransition();
        } finally {
            if (connection != null) {
                connection.logoffAndDisconnect();
            }
        }
    }

}
