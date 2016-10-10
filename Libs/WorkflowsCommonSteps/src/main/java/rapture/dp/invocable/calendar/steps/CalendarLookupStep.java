package rapture.dp.invocable.calendar.steps;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.common.exception.ExceptionToString;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

public class CalendarLookupStep extends AbstractInvocable {

    public CalendarLookupStep(String workerUri, String stepName) {
        super(workerUri, stepName);
    }

    @Override
    public String invoke(CallingContext ctx) {
        try {
            Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), "STEPNAME", getStepName());

            String dateStr = StringUtils.stripToNull(Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "DATE"));
            String calendar = StringUtils.stripToNull(Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "CALENDAR"));
            String translator = StringUtils.stripToNull(Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "TRANSLATOR"));
            if (translator == null) translator = StringUtils.stripToNull(Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "DEFAULT_TRANSLATOR"));
            LocalDate date = (dateStr == null) ? LocalDate.now() : LocalDate.parse(dateStr);
            
            // Translate the date to a name - eg Good Friday, Yom Kippur, Thanksgiving
            // Expected format is a map of dates (with or without years) to names or lists of names
            // Example:
            // {
            // "31Dec" : ["New Tear's Eve", "Hogmanay"] ,
            // "05Sep2016" : "Labor Day",
            // "04Sep2017" : "Labor Day"
            // "2015-01-01" : "New Year's Day"
            // }

            Map<String, Object> calendarTable = new HashMap<>();
            if (calendar != null) {
                String calendarJson = StringUtils.stripToNull(Kernel.getDoc().getDoc(ctx, calendar));
                if (calendarJson != null) {
                    calendarTable = JacksonUtil.getMapFromJson(calendarJson);
                }
            }

            // Translate the date to a name - eg Good Friday, Yom Kippur, Thanksgiving
            Map<String, Object> translationTable = new HashMap<>();
            if (translator != null) {
                String translationJson = StringUtils.stripToNull(Kernel.getDoc().getDoc(ctx, translator));
                if (translationJson != null) {
                    translationTable = JacksonUtil.getMapFromJson(translationJson);
                }
            }

            List<String> lookup = new ArrayList<>();

            String languageTag = StringUtils.stripToNull(Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "LOCALE"));
            Locale locale = (languageTag == null) ? Locale.getDefault() : Locale.forLanguageTag(languageTag);

            for (DateTimeFormatter formatter : ImmutableList.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("ddMMMuuuu", locale),
                    DateTimeFormatter.ofPattern("ddMMM", locale), DateTimeFormatter.ofPattern("MMMdduuuu", locale), DateTimeFormatter.ofPattern("MMMdd", locale),
                    DateTimeFormatter.ofPattern("uuuuMMMdd", locale))) {

                String formattedDate = date.format(formatter);
                Object transList = translationTable.get(formattedDate);
                if (transList != null) {
                    if (transList instanceof Iterable) {
                        for (Object o : (Iterable) transList) {
                            lookup.add(o.toString());
                        }
                    } else lookup.add(transList.toString());
                }
                lookup.add(formattedDate);
            }
            lookup.add(DayOfWeek.from(date).getDisplayName(TextStyle.FULL, locale));
            
            Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), "DATE_TRANSLATIONS", JacksonUtil.jsonFromObject(lookup));

            // Calendar table defines the priority. getMapFromJson returns a LinkedHashMap so order is preserved.
            for (Entry<String, Object> calEntry : calendarTable.entrySet()) {
                if (lookup.contains(calEntry.getKey())) {
                    Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), "CALENDAR_LOOKUP_ENTRY", JacksonUtil.jsonFromObject(calEntry));
                    return calEntry.getValue().toString();
                }
            }

            return getNextTransition();
        } catch (Exception e) {
            Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), getStepName(), "Unable to access the calendar : " + e.getLocalizedMessage());
            Kernel.getDecision().setContextLiteral(ctx, getWorkerURI(), getStepName() + "Error", ExceptionToString.summary(e));
            return getErrorTransition();
        }
    }

}
