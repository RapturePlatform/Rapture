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
package rapture.exchange;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.generated.EGenLexer;
import rapture.generated.EGenParser;

public final class ExchangeFactory {
    private static Logger log = Logger.getLogger(ExchangeFactory.class);
    private static final Map<Integer, String> implementationMap;
    static {
        Map<Integer, String> setupMap = new HashMap<>();
        setupMap.put(EGenLexer.RABBITMQ, "rapture.exchange.rabbitmq.RabbitExchangeHandler");
        setupMap.put(EGenLexer.MEMORY, "rapture.exchange.memory.MemoryExchangeHandler");
	// TODO EGenLexer.FILE ?
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    public static ExchangeHandler getHandler(String id, String config) {
        try {
            log.info("Creating exchange from config - " + config);
            EGenParser parser = getParsedForConfig(config);
            int implementationType = parser.getImplementationType();
            if (implementationMap.containsKey(implementationType)) {
                return getExchange(implementationMap.get(implementationType), id, parser.getInstance(), parser.getImplementionConfig());
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported log store - " + parser.getImplementationName());
            }
        } catch (RecognitionException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating exchange");
            log.error("Error parsing config " + config + ": " + RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;

        }
    }

    private static ExchangeHandler getExchange(String className, String id, String instance, Map<String, String> config) {

        Class<?> idgenClass;
        try {
            idgenClass = Class.forName(className);
            Object fStore;
            try {
                fStore = idgenClass.newInstance();
                if (fStore instanceof ExchangeHandler) {
                    ExchangeHandler ret = (ExchangeHandler) fStore;
                    ret.setInstanceName(instance);
                    ret.setConfig(config);
                    return ret;
                } else {
                    log.error(className + " is not an exchange, cannot instantiate");
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create audit log");
                }
            } catch (InstantiationException e) {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating exchange");
                log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
                throw raptException;
            } catch (IllegalAccessException e) {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating exchange");
                log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
                throw raptException;
            }
        } catch (ClassNotFoundException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating exchange with config ", e);
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }

    }

    private static EGenParser getParsedForConfig(String config) throws RecognitionException {
        EGenLexer lexer = new EGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        EGenParser parser = new EGenParser(tokens);
        parser.qinfo();
        return parser;
    }

    private ExchangeFactory() {
    }
}
