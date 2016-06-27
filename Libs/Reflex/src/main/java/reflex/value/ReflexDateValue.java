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
package reflex.value;

import java.util.Date;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import reflex.ReflexException;
import reflex.calendar.CalendarFactory;
import reflex.calendar.CalendarHandler;

/**
 * A wrapper around a Date/Time value that is a first class type in Reflex
 *
 * @author amkimian
 *
 */
public class ReflexDateValue {

    private LocalDate date;
    private String calendarString = "";
    private CalendarHandler calHandler;

    public ReflexDateValue() {
        this.date = new LocalDate(DateTimeZone.UTC);
        setupCalendarHandler();
    }

    public ReflexDateValue(Date initial, String calendar) {
        this.date = new LocalDate(initial.getTime(), DateTimeZone.UTC);
        this.calendarString = calendar;
        setupCalendarHandler();
    }

    public ReflexDateValue(ReflexDateValue other, String calendar) {
        this.date = other.date;
        this.calendarString = calendar;
        setupCalendarHandler();
    }

    public ReflexDateValue(LocalDate other, String calendar) {
        this.date = other;
        this.calendarString = calendar;
        setupCalendarHandler();
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyyMMdd").withZoneUTC();

    public ReflexDateValue(String yyyyMMdd, String calendar) {
        if (yyyyMMdd.isEmpty()) {
            date = LocalDate.now();
        } else {
            // Given a yyyyMMdd string, convert to a date
            try {
                date = new LocalDate(FORMATTER.parseDateTime(yyyyMMdd));
            } catch (IllegalArgumentException e) {
                throw new ReflexException(-1, String.format("Bad date format %s - %s", yyyyMMdd, e.getMessage()));
            } catch (UnsupportedOperationException e) {
                throw new ReflexException(-1, String.format("Bad date format %s - %s", yyyyMMdd, e.getMessage()));
            }
        }
        this.calendarString = calendar;
        setupCalendarHandler();
    }

    public ReflexDateValue add(Object increase) {
        if (increase instanceof Integer) {
            int amountToIncrease = ((Integer) increase).intValue();
            return new ReflexDateValue(calHandler.addDays(date.toDate(), amountToIncrease), calendarString);
        } else {
            throw new ReflexException(-1, "Cannot increase a date by something of type " + increase.getClass().toString());
        }
    }

    private void setupCalendarHandler() {
        calHandler = CalendarFactory.getHandler(calendarString);
    }

    public ReflexDateValue sub(Object decrease) {
        if (decrease instanceof Integer) {
            int amountToDecrease = ((Integer) decrease).intValue();
            return new ReflexDateValue(calHandler.addDays(date.toDate(), -1 * amountToDecrease), calendarString);
        } else {
            throw new ReflexException(-1, "Cannot decrease a date by something of type " + decrease.getClass().toString());
        }
    }

    @Override
    public String toString() {
        return FORMATTER.print(date);
    }

    public String toString(DateTimeFormatter formatter) {
        if (formatter == null) return toString();
        return formatter.print(date);
    }

    public Boolean greaterThanEquals(ReflexDateValue asDate) {
        if (date.equals(asDate.date)) {
            return true;
        }
        return greaterThan(asDate);
    }

    public Boolean greaterThan(ReflexDateValue other) {
        return this.date.isAfter(other.date);
    }

    public Boolean lessThanEquals(ReflexDateValue other) {
        return this.date.equals(other.date) || lessThan(other);
    }

    public Boolean lessThan(ReflexDateValue other) {
        return this.date.isBefore(other.date);
    }

    public long getEpoch() {
        return date.toDateMidnight(DateTimeZone.UTC).getMillis();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 7;
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        ReflexDateValue that = null;
        if (obj instanceof ReflexDateValue) {
            that = (ReflexDateValue) obj;
        } else if (obj instanceof ReflexValue) {
            try {
                that = ((ReflexValue) obj).asDate();
            } catch (ReflexException e) {
                return false;
            }
        } else {
            return false;
        }
        return this.date.equals(that.date);
    }
}
