package rapture.dp.invocable.configuration.steps;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
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

    @Override
    public String invoke(CallingContext ctx) {
        try {
            Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), "STEPNAME", getStepName());

            String workOrderUri = new RaptureURI(getWorkerURI(), Scheme.WORKORDER).toShortString();
            String config = StringUtils.stripToNull(Kernel.getDecision().getContextValue(ctx, workOrderUri, "CONFIGURATION"));
            Map<String, String> view = new HashMap<>();
            DocApi docApi = Kernel.getDoc();
            if (docApi.docExists(ctx, config)) {
                String doc = docApi.getDoc(ctx, config);
                Map<String, Object> map = JacksonUtil.getMapFromJson(doc);
                for (Entry<String, Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    String value = StringUtils.stripToNull(entry.getValue().toString());
                    ContextValueType type = ContextValueType.getContextValueType(value.charAt(0));
                    if (type == ContextValueType.NULL) {
                        type = ContextValueType.LITERAL;
                    } else value = value.substring(1);
                    ExecutionContextUtil.setValueECF(ctx, workOrderUri, view, key, type, value);
                }
            }
            return Steps.NEXT.toString();
        } catch (Exception e) {
            Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), getStepName(), "Exception in workflow : " + e.getLocalizedMessage());
            Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), getStepName() + "Error", ExceptionToString.summary(e));
            return getErrorTransition();
        }
    }

}
