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
package rapture.dsl.dparse;

import rapture.common.MessageFormat;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.repo.Messages;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class AsOfTimeDirectiveParser {
    public static final String AS_OF_TIME_FORMAT = "yyyyMMdd'T'HHmmss";
    public static final String AS_OF_TIME_FORMAT_MS = AS_OF_TIME_FORMAT + ".SSS";
    public static final String AS_OF_TIME_FORMAT_TZ = AS_OF_TIME_FORMAT + "Z";
    public static final String AS_OF_TIME_FORMAT_MS_TZ = AS_OF_TIME_FORMAT_MS + "Z";

    String asOfTime = "";
    boolean hasTimeZone = false;
    boolean hasMilliseconds = false;
    long timestamp = -1;

    public AsOfTimeDirectiveParser(String asOfTime) {
        this.asOfTime = asOfTime;

        hasTimeZone = asOfTime.indexOf('-') > -1;
        hasMilliseconds = asOfTime.indexOf('.') > -1;
        timestamp = getMillis();
    }

    public long getMillisTimestamp() {
        return timestamp;
    }

    private long getMillis() {
        if (asOfTime.matches("t\\d+")) {
            return parseTimestamp();
        }
        else {
            return parseDateString();
        }
    }

    private long parseTimestamp() {
        if (asOfTime.length() != 14) {
            String[] parameters = {asOfTime, "t + milliseconds since January 1, 1970 UTC"};
            throw RaptureExceptionFactory
                    .create(HttpURLConnection.HTTP_BAD_REQUEST, new MessageFormat(Messages.getString("UserInput.InvalidDatetimeFormat"), parameters));
        }

        return Long.parseLong(asOfTime.substring(1));
    }

    private long parseDateString() {
        try {
            SimpleDateFormat format = new SimpleDateFormat(getAsOfTimeFormat());
            return format.parse(asOfTime).getTime();
        }
        catch (ParseException e) {
            String[] parameters = {asOfTime, AS_OF_TIME_FORMAT_MS};
            throw RaptureExceptionFactory
                    .create(HttpURLConnection.HTTP_BAD_REQUEST, new MessageFormat(Messages.getString("UserInput.InvalidDatetimeFormat"), parameters));
        }
    }

    private String getAsOfTimeFormat() {
        String format = null;

        if (hasTimeZone) {
            if (hasMilliseconds) {
                format = AS_OF_TIME_FORMAT_MS_TZ;
            }
            else {
                format = AS_OF_TIME_FORMAT_TZ;
            }
        }
        else {
            if (hasMilliseconds) {
                format = AS_OF_TIME_FORMAT_MS;
            }
            else {
                format = AS_OF_TIME_FORMAT;
            }
        }

        return format;
    }
}
