package rapture.dp.invocable.calendar.steps;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import rapture.common.CallingContext;
import rapture.common.api.DecisionApi;
import rapture.common.dp.AbstractInvocable;
import rapture.common.exception.ExceptionToString;
import rapture.kernel.Kernel;

public class GetDayOfWeekStep extends AbstractInvocable {

    public GetDayOfWeekStep(String workerUri, String stepName) {
        super(workerUri, stepName);
    }

    @Override
    public String invoke(CallingContext ctx) {
        DecisionApi decision = Kernel.getDecision();
        try {
            decision.setContextLiteral(ctx, getWorkerURI(), "STEPNAME", getStepName());

            String dateStr = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "DATE"));
            String languageTag = StringUtils.stripToNull(decision.getContextValue(ctx, getWorkerURI(), "LOCALE"));
            LocalDateTime date = (dateStr == null) ? LocalDateTime.now() : LocalDateTime.parse(dateStr);
            Locale locale = (languageTag == null) ? Locale.getDefault() : Locale.forLanguageTag(languageTag);
            return DayOfWeek.from(date).getDisplayName(TextStyle.FULL, locale);
        } catch (Exception e) {
            decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "Exception in workflow : " + e.getLocalizedMessage());
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), ExceptionToString.summary(e));
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(),
                    "Problem in GetDayOfWeekStep " + getStepName() + " - error is " + ExceptionToString.getRootCause(e).getLocalizedMessage(), true);
            return getErrorTransition();
        }
    }

}
