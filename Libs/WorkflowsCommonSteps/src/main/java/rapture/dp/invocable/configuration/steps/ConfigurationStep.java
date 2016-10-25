package rapture.dp.invocable.configuration.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.DecisionApi;
import rapture.common.api.DocApi;
import rapture.common.dp.AbstractInvocable;
import rapture.common.dp.ContextValueType;
import rapture.common.dp.Steps;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;
import rapture.kernel.dp.ExecutionContextUtil;

public class ConfigurationStep extends AbstractInvocable {

    public ConfigurationStep(String workerUri, String stepName) {
        super(workerUri, stepName);
    }

    private static final String LOCALHOST = "localhost";
    private static final String DEFAULT_RIM_PORT = "8000";
    private static final String WORKORDER_DELIMETER = "&workorder=";
    public static final String EXTERNAL_RIM_WORKORDER_URL = "EXTERNALRIMWORKORDERURL";
    private static final Logger log = Logger.getLogger(ConfigurationStep.class);

    @Override
    public String invoke(CallingContext ctx) {
        DecisionApi decision = Kernel.getDecision();
        String workOrderUri = new RaptureURI(getWorkerURI(), Scheme.WORKORDER).toShortString();
        String config = StringUtils.stripToNull(decision.getContextValue(ctx, workOrderUri, "CONFIGURATION"));

        try {
            decision.setContextLiteral(ctx, getWorkerURI(), "STEPNAME", getStepName());

            String docPath = new RaptureURI(workOrderUri).getDocPath();
            int lio = docPath.lastIndexOf('/');

            StringBuilder externalUrl = new StringBuilder();
            String host = System.getenv("HOST");
            String port = System.getenv("PORT");
            externalUrl.append("http://").append((host != null) ? host : LOCALHOST).append(":").append((port != null) ? port : DEFAULT_RIM_PORT)
                    .append("/process/")
                    .append(docPath.substring(0, lio)).append(WORKORDER_DELIMETER).append(docPath.substring(lio + 1));
            decision.setContextLiteral(ctx, workOrderUri, EXTERNAL_RIM_WORKORDER_URL, externalUrl.toString());

            Map<String, String> view = new HashMap<>();
            DocApi docApi = Kernel.getDoc();

            if (config == null) {
                decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "No configuration document specified", false);
                return this.getNextTransition();
            }
            List<String> configs;
            try {
                // Could be a list or a single entry.
                configs = JacksonUtil.objectFromJson(config, ArrayList.class);
            } catch (Exception e) {
                configs = ImmutableList.of(config);
            }

            for (String conf : configs) {
                if (docApi.docExists(ctx, conf)) {
                    String doc = docApi.getDoc(ctx, conf);
                    Map<String, Object> map = JacksonUtil.getMapFromJson(doc);
                    for (Entry<String, Object> entry : map.entrySet()) {
                        String key = entry.getKey();
                        String value = StringUtils.stripToNull(entry.getValue().toString());
                        ContextValueType type = ContextValueType.getContextValueType(value.charAt(0));
                        if (type == ContextValueType.NULL) {
                            type = ContextValueType.LITERAL;
                        } else value = value.substring(1);
                        ExecutionContextUtil.setValueECF(ctx, workOrderUri, view, key, type, value);
                        decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), getStepName() + ": Read configuration data from " + conf, false);
                    }
                } else {
                    decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), getStepName() + ": Cannot locate configuration document " + conf, true);
                }
            }
            return Steps.NEXT.toString();
        } catch (Exception e) {
            decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "Exception in workflow : " + e.getLocalizedMessage());
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), ExceptionToString.summary(e));
            log.error(ExceptionToString.format(ExceptionToString.getRootCause(e)));
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(),
                    "Problem in " + getStepName() + ": unable to read the configuration document " + config, true);
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), ": " + ExceptionToString.getRootCause(e).getLocalizedMessage(), true);

            return getErrorTransition();
        }
    }

}
