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
package rapture.structured;

import java.net.HttpURLConnection;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.StructuredConfigLexer;
import rapture.generated.StructuredConfigParser;

import com.google.common.collect.ImmutableMap;

public class StructuredFactory {
    private static Logger log = Logger.getLogger(StructuredFactory.class);
    private static final Map<Integer, String> keyStoreImplementationMap = ImmutableMap.of(
            StructuredConfigLexer.POSTGRES, "rapture.repo.postgres.PostgresStructuredStore",
            StructuredConfigLexer.HSQLDB, "rapture.structured.hsqldb.HsqldbStructuredStore");

    public static StructuredStore getRepo(String config, String authority) {
        try {
            log.info("Creating repo from config - " + config);
            StructuredConfigParser parser = parseConfig(config);
            return makeStructuredStore(parser, authority);
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
            if (log.isDebugEnabled()) log.debug(ExceptionToString.format(e));
        } catch (RaptureException e) {
            log.error("Error when initializing repo - " + e.getMessage());
            if (log.isDebugEnabled()) log.debug(e.getFormattedMessage());
            throw e;
        }
        return null;
    }

    public static StructuredConfigParser parseConfig(String config) throws RecognitionException {
        StructuredConfigLexer lexer = new StructuredConfigLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StructuredConfigParser parser = new StructuredConfigParser(tokens);
        parser.repinfo();
        return parser;
    }

    private static StructuredStore makeStructuredStore(StructuredConfigParser parser, String authority) {
        int storeType = parser.getStoreType();
        if (keyStoreImplementationMap.containsKey(storeType)) {
            return getStructuredStore(keyStoreImplementationMap.get(storeType), parser.getInstance(), parser.getConfig().getConfig(), authority);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported structured store type");
        }
    }

    private static StructuredStore getStructuredStore(String className, String instance, Map<String, String> config, String authority) {
        try {
            Class<?> repoClass = Class.forName(className);
            Object fStore;
            fStore = repoClass.newInstance();
            if (fStore instanceof StructuredStore) {
                StructuredStore ret = (StructuredStore) fStore;
                ret.setInstance(instance);
                ret.setConfig(config, authority);
                return ret;
            } else {
                String message = (className + " is not a repo, cannot instantiate");
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message);
            }
        } catch (InstantiationException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "InstantiationException. Error creating structured store of type "
                    + className, e);
        } catch (IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "IllegalAccessException. Error creating structured store of type "
                    + className, e);
        } catch (ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "ClassNotFoundException. Error creating structured store of type "
                    + className + ": " + e.toString());
        }
    }

    private StructuredFactory() {
    }
}
