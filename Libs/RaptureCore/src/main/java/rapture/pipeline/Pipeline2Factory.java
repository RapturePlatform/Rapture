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
package rapture.pipeline;

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
import rapture.generated.P2GenLexer;
import rapture.generated.P2GenParser;

public final class Pipeline2Factory {
    private static Logger log = Logger.getLogger(Pipeline2Factory.class);
    private static final Map<Integer, String> implementationMap;
    static {
        Map<Integer, String> setupMap = new HashMap<>();
        // Pipeline2 not implemented for Rabbit. If we decide to implement it enable it here and eliminate old Pipeline API
        // setupMap.put(P2GenLexer.RABBITMQ, "rapture.pipeline2.rabbitmq.RabbitPipeline2Handler");
        setupMap.put(P2GenLexer.GCP_PUBSUB, "rapture.pipeline2.gcp.PubsubPipeline2Handler");
        setupMap.put(P2GenLexer.MEMORY, "rapture.pipeline2.memory.MemoryPipeline2Handler");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    public static Pipeline2Handler getHandler(String id, String config) {
        try {
            log.info("Creating exchange from config - " + config);
            P2GenParser parser = getParsedForConfig(config);
            int implementationType = parser.getImplementationType();
            if (implementationMap.containsKey(implementationType)) {
                return getPipeline2(implementationMap.get(implementationType), id, parser.getInstance(), parser.getImplementionConfig());
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported configuration - " + config);
            }
        } catch (RecognitionException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating exchange");
            log.error("Error parsing config " + config + ": " + RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;

        }
    }

    private static Pipeline2Handler getPipeline2(String className, String id, String instance, Map<String, String> config) {

        Class<?> idgenClass;
        try {
            idgenClass = Class.forName(className);
            Object fStore = idgenClass.newInstance();
            if (fStore instanceof Pipeline2Handler) {
                Pipeline2Handler ret = (Pipeline2Handler) fStore;
                ret.setInstanceName(instance);
                ret.setConfig(config);
                return ret;
            } else {
                log.error(className + " is not an exchange, cannot instantiate");
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create audit log");
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating exchange ", e);
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }

    }

    private static P2GenParser getParsedForConfig(String config) throws RecognitionException {
        P2GenLexer lexer = new P2GenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        P2GenParser parser = new P2GenParser(tokens);
        parser.qinfo();
        return parser;
    }

    private Pipeline2Factory() {
    }
}
