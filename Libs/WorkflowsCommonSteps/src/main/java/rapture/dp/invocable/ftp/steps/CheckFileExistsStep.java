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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.api.DecisionApi;
import rapture.common.dp.Steps;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.AbstractStep;
import rapture.ftp.common.FTPConnection;
import rapture.ftp.common.FTPRequest;
import rapture.ftp.common.FTPRequest.Action;
import rapture.ftp.common.SFTPConnection;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;

public class CheckFileExistsStep extends AbstractStep {
    private static final Logger log = Logger.getLogger(CopyFileStep.class);

    DecisionApi decision;
    public CheckFileExistsStep(String workerUri, String stepName) {
        super(workerUri, stepName);
        decision = Kernel.getDecision();
    }

    static String wasNotWas(Boolean flag) {
        return (flag) ? " was " : " was not ";
    }

    static String wereNotWere(Boolean flag) {
        return (flag) ? " were " : " were not ";
    }

    /**
     * FTP_CONFIGURATION is optional. If not set the arguments are assumed to be local EXIST_FILENAMES is a map of file names to Booleans, indicating whether
     * the file is expected or not
     */
    @Override
    public String invoke(CallingContext ctx) {
        String workerUri = getWorkerURI();
        String workOrderUri = new RaptureURI(workerUri).toShortString();
        try {
            String configUri = StringUtils.stripToNull(decision.getContextValue(ctx, workOrderUri, "FTP_CONFIGURATION"));
            String filename = StringUtils.stripToNull(decision.getContextValue(ctx, workOrderUri, "EXIST_FILENAMES"));
            if (filename == null) {
                decision.setContextLiteral(ctx, workOrderUri, getStepName(), "No files to check");
                decision.setContextLiteral(ctx, workOrderUri, getErrName(), "");
                return getNextTransition();
            }

            Map<String, Object> files = JacksonUtil.objectFromJson(ExecutionContextUtil.evalTemplateECF(ctx, workOrderUri, filename, null), Map.class);

            FTPConnection connection = new SFTPConnection(configUri).setContext(ctx);
            String retval = getNextTransition();
            List<FTPRequest> requests = new ArrayList<>();
            int existsCount = 0;
            int failCount = 0;
            StringBuilder error = new StringBuilder();
            for (Entry<String, Object> e : files.entrySet()) {
                FTPRequest request = new FTPRequest(Action.EXISTS).setRemoteName(e.getKey());
                boolean exists = connection.doAction(request);
                if (!exists == ((Boolean) e.getValue())) {
                    retval = getFailTransition();
                    String target = e.getKey();
                    boolean plural = false;
                    if (exists) {
                        List l = (List) request.getResult();
                        if (l != null) {
                            if (l.size() > 1) {
                                target = l.size() + " files or directories matching " + e.getKey();
                                plural = true;
                            } else {
                                target = l.get(0).toString();
                            }
                        }
                    }
                    if (error.length() > 0) error.append("\n");
                    error.append(target).append((plural) ? wereNotWere(exists) : wasNotWas(exists)).append("found but")
                            .append(plural ? wereNotWere((Boolean) e.getValue()) : wasNotWas((Boolean) e.getValue())).append("expected");
                    failCount++;
                }
                requests.add(request);
            }
            decision.setContextLiteral(ctx, workOrderUri, getStepName(), "Located " + existsCount + " of " + files.size() + " files");
            String errMsg = error.toString();
            String audit_quiet = decision.getContextValue(ctx, workOrderUri, "AUDIT_QUIET");
            if (!StringUtils.isEmpty(errMsg)) {
                log.error(errMsg);
                if (audit_quiet == null) decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), getStepName() + ": " + errMsg, true);
            } else {
                if (audit_quiet == null) decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), getStepName() + ": all files successfully matched", false);
            }
            decision.setContextLiteral(ctx, workOrderUri, getErrName(), errMsg);
            return retval;
        } catch (Exception e) {
            decision.setContextLiteral(ctx, workOrderUri, getStepName(), "Unable to determine if files exist : " + e.getLocalizedMessage());
            decision.setContextLiteral(ctx, workOrderUri, getErrName(), ExceptionToString.summary(e));
            log.error(ExceptionToString.format(ExceptionToString.getRootCause(e)));
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(),
                    "Problem in " + getStepName() + ": " + ExceptionToString.getRootCause(e).getLocalizedMessage(), true);
            return getErrorTransition();
        }
    }

    public static String getFailTransition() {
        return Steps.WAIT;
    }

}
