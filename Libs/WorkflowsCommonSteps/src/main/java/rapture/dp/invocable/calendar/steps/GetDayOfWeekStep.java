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
            String day = DayOfWeek.from(date).getDisplayName(TextStyle.FULL, locale);
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(), "Day of week is " + day, false);
            return day;
        } catch (Exception e) {
            decision.setContextLiteral(ctx, getWorkerURI(), getStepName(), "Exception in workflow : " + e.getLocalizedMessage());
            decision.setContextLiteral(ctx, getWorkerURI(), getErrName(), ExceptionToString.summary(e));
            decision.writeWorkflowAuditEntry(ctx, getWorkerURI(),
                    "Problem in " + getStepName() + ": " + ExceptionToString.getRootCause(e).getLocalizedMessage(), true);
            return getErrorTransition();
        }
    }

}
