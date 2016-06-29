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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import reflex.ReflexException;

/**
 * A wrapper around a Time value that is a first class type in Reflex
 * 
 * @author amkimian
 * 
 */
public class ReflexTimeValue {

    private DateTime dateTime;
    private DateTimeFormatter timeFormat = DateTimeFormat.forPattern("HH:mm:ss").withZoneUTC();

    public ReflexTimeValue() {
        this.dateTime = new DateTime(DateTimeZone.UTC);
    }

    public ReflexTimeValue(Date initial) {
        this.dateTime = new DateTime(initial.getTime(), DateTimeZone.UTC);
    }

    public ReflexTimeValue(long initial) {
        this.dateTime = new DateTime(initial, DateTimeZone.UTC);
    }

    public ReflexTimeValue(ReflexTimeValue other, DateTimeZone zone) {
        this.dateTime = other.dateTime.toDateTime(zone);
    }

    public ReflexTimeValue(String HHmmSS) {
        // Given a HH:mm:ss string, convert it to a local date time object
        dateTime = timeFormat.parseDateTime(HHmmSS);
    }

    @Override
    public String toString() {
        return dateTime.toString(timeFormat);
    }

    public String toString(DateTimeFormatter formatter, DateTimeZone zone) {
        if (formatter == null) return toString();
        if (zone == null) return formatter.print(dateTime.toLocalTime());
        return formatter.print(dateTime.toDateTime(zone));
    }

    public long getEpoch() {
        return dateTime.toDate().getTime();
    }

    public Boolean greaterThanEquals(ReflexTimeValue other) {
        return this.getEpoch() >= other.getEpoch();
    }

    public Boolean greaterThan(ReflexTimeValue other) {
        return this.getEpoch() > other.getEpoch();
    }

    public Boolean lessThanEquals(ReflexTimeValue other) {
        return this.getEpoch() <= other.getEpoch();
    }

    public Boolean lessThan(ReflexTimeValue other) {
        return this.getEpoch() < other.getEpoch();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 7;
        result = prime * result + ((dateTime == null) ? 0 : dateTime.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) return false;
        if (this == obj) return true;
        ReflexTimeValue that = null;

        if (obj instanceof ReflexTimeValue) {
            that = (ReflexTimeValue) obj;
        } else if (obj instanceof ReflexValue) {
            try {
                that = ((ReflexValue) obj).asTime();
            } catch (ReflexException e) {
                return false;
            }
        } else {
            return false;
        }
        // These are meant to be times, so the date component is irrelevant for comparison purposes.
        return this.toString().equals(that.toString());
    }
}
