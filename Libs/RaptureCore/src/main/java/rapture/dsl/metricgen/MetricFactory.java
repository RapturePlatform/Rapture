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
package rapture.dsl.metricgen;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.generated.MetricGenLexer;
import rapture.generated.MetricGenParser;
import rapture.stat.CounterStatType;
import rapture.stat.PresenceStatType;
import rapture.stat.StatType;
import rapture.stat.StringStatType;
import rapture.stat.ValueStatType;

public final class MetricFactory {
    private static Logger log = Logger.getLogger(MetricFactory.class);

    public static StatType getStatType(String config) {
        // Given a config that is formatted in the idgen command
        // string, create a RaptureIdGen
        // and assign the appropriate features and implementation to it.
        //
        try {
            MetricGenParser parser = parseConfig(config);

            switch (parser.getMetricToken().getType()) {
            case MetricGenLexer.MESSAGE:
                return new StringStatType(parser.getMetricConfig().getConfig());
            case MetricGenLexer.PRESENCE:
                return new PresenceStatType(parser.getMetricConfig().getConfig());
            case MetricGenLexer.VALUE:
                return new ValueStatType(parser.getMetricConfig().getConfig());
            case MetricGenLexer.COUNTER:
                return new CounterStatType(parser.getMetricConfig().getConfig());
            default:
                log.error("Unsupported metric " + config);
            }
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
        }
        return null;
    }

    private static MetricGenParser parseConfig(String config) throws RecognitionException {
        MetricGenLexer lexer = new MetricGenLexer();
        log.debug("Creating stat from config - " + config);
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MetricGenParser parser = new MetricGenParser(tokens);
        parser.minfo();
        return parser;
    }

    private MetricFactory() {
    }
}
