/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.log;

import org.apache.log4j.Layout;

import uk.org.simonsite.log4j.appender.TimeAndSizeRollingAppender;

/**
 * @author bardhi
 * @since 9/16/14.
 */
public class Log4jAppenderFactory {
    /**
     * Creates a TimeAndSizeRollingAppender
     *
     * @param appenderName
     * @param fileName
     * @param maxFileSize
     * @param maxBackups
     * @param layout
     * @return
     */
    public static TimeAndSizeRollingAppender createTSRAppender(String appenderName, String fileName, String maxFileSize, int maxBackups, Layout layout) {
        TimeAndSizeRollingAppender appender = new TimeAndSizeRollingAppender();
        appender.setName(appenderName);
        appender.setLayout(layout);
        appender.setFile(fileName);

        appender.setMaxRollFileCount(maxBackups);
        appender.setMaxFileSize(maxFileSize);

        appender.setDatePattern(".yyy-MM-dd");
        appender.setDateRollEnforced(true);

        appender.activateOptions();
        return appender;
    }
}
