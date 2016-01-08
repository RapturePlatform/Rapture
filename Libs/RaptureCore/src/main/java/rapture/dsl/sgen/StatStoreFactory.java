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
package rapture.dsl.sgen;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.SGenLexer;
import rapture.generated.SGenParser;
import rapture.stat.IRaptureStatApi;
import rapture.stat.IStatStore;
import rapture.stat.StandardStatImpl;

public final class StatStoreFactory {
    private static Logger log = Logger.getLogger(StatStoreFactory.class);
    private static final Map<Integer, String> implementationMap;
    static {
        Map<Integer, String> setupMap = new HashMap<Integer, String>();
	// TODO SGenLexer.FILE ? 
        setupMap.put(SGenLexer.MEMORY, "rapture.stat.memory.MemoryStatStore");
        setupMap.put(SGenLexer.REDIS, "rapture.stat.redis.RedisStatStore");
        setupMap.put(SGenLexer.DUMMY, "rapture.stat.dummy.DummyStatStore");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    private static IStatStore getStat(String className, Map<String, String> config) {
        try {
            Class<?> statStoreClass = Class.forName(className);
            Object fStore;
            fStore = statStoreClass.newInstance();
            if (fStore instanceof IStatStore) {
                IStatStore ret = (IStatStore) fStore;
                ret.setConfig(config);
                return ret;
            } else {
                String message = (className + " is not a statstore, cannot instantiate");
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message);

            }
        } catch (ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating stat store of type " + className, e);
        } catch (InstantiationException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating stat store of type " + className, e);
        } catch (IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating stat store of type " + className, e);
        }
    }

    private static IStatStore createStatStore(int storeToken, Map<String, String> config) {
        if (implementationMap.containsKey(storeToken)) {
            return getStat(implementationMap.get(storeToken), config);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported stat store type " + storeToken);
        }
    }

    public static IRaptureStatApi getStatImpl(String config) {
        // Given a config that is formatted in the idgen command
        // string, create a RaptureIdGen
        // and assign the appropriate features and implementation to it.
        //
        try {
            SGenLexer lexer = new SGenLexer();
            log.info("Creating store from config - " + config);
            lexer.setCharStream(new ANTLRStringStream(config));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            SGenParser parser = new SGenParser(tokens);
            parser.sinfo();

            StandardStatImpl ret = new StandardStatImpl();
            ret.setStatStore(createStatStore(parser.getStore().getType(), parser.getConfig().getConfig()));
            return ret;
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
        }
        return null;
    }

    private StatStoreFactory() {
    }
}
