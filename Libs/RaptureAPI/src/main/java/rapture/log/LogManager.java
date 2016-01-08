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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import rapture.config.MultiValueConfigLoader;
import net.logstash.log4j.JSONEventLayoutV1;
import uk.org.simonsite.log4j.appender.TimeAndSizeRollingAppender;

/**
 * A simple log manager to setup log file locations if we so desire.
 *
 * @author amkimian
 */

public class LogManager {

    private static final String DEFAULT_PATTERN = "%d{ABSOLUTE} %5p [%t] (%F:%L) <%X{" + MDCService.RFX_SCRIPT + "}> [%X{" + MDCService.WORKFLOW_FORMATTED + "}] - %m%n";

    public static void configureLogging() {
        String logDir = MultiValueConfigLoader.getConfig("LOGGER-logDir", "");
        String fileName = MultiValueConfigLoader.getConfig("LOGGER-file");
        String maxFileSize = MultiValueConfigLoader.getConfig("LOGGER-maxLogSize", "10MB");
        int maxBackups = Integer.parseInt(MultiValueConfigLoader.getConfig("LOGGER-maxBackups", "5"));
        String logLevel = MultiValueConfigLoader.getConfig("LOGGER-level", "DEBUG");
        String logPattern = MultiValueConfigLoader.getConfig("LOGGER-logPattern", DEFAULT_PATTERN);

        Appender stdoutAppender = Logger.getRootLogger().getAppender(LogAppenderNames.STDOUT);
        if (stdoutAppender != null) {
            PatternLayout layout = new PatternLayout();
            layout.setConversionPattern(logPattern);
            stdoutAppender.setLayout(layout);
        }

        setupTextFileLogger(fileName, maxFileSize, maxBackups, logLevel, logPattern);
        setupJsonFileLogger(logDir, fileName, maxFileSize, logLevel, logPattern);

    }

    /**
     * Set up an appender that will write rolling file logs. The logs will be human-readable text, which will follow a pattern specified in the layout. The
     * log file will exist in the same directory as the binary executable (we may want to eventually move this to a proper log dir)
     *
     * @param fileName
     * @param maxFileSize
     * @param maxBackups
     * @param logLevel
     * @param logPattern
     */
    private static void setupTextFileLogger(String fileName, String maxFileSize, int maxBackups, String logLevel, String logPattern) {
        if (!StringUtils.isBlank(fileName)) {
            PatternLayout patternLayout = new PatternLayout();
            patternLayout.setConversionPattern(logPattern);

            TimeAndSizeRollingAppender appender = Log4jAppenderFactory
                    .createTSRAppender(LogAppenderNames.FILE_TEXT, fileName, maxFileSize, maxBackups, patternLayout);

            Logger logger = Logger.getRootLogger();
            logger.addAppender(appender);
            logger.setLevel(Level.toLevel(logLevel));
        }
    }

    private static void setupJsonFileLogger(String logDir, String fileName, String maxFileSize, String logLevel, String logPattern) {
        if (!StringUtils.isBlank(fileName)) {
            JSONEventLayoutV1 layout = new JSONEventLayoutV1();
            String logFilePath = LogFilePathFactory.createLogFilePath(logDir, fileName + ".json");
            TimeAndSizeRollingAppender appender = Log4jAppenderFactory
                    .createTSRAppender(LogAppenderNames.FILE_JSON, logFilePath, maxFileSize, 0, layout);

            Logger logger = Logger.getRootLogger();
            logger.addAppender(appender);
            logger.setLevel(Level.toLevel(logLevel));
        }

    }
}
