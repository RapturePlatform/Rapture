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
package rapture.kernel.search;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.FTGenLexer;
import rapture.generated.FTGenParser;

/**
 * A factory that creates search store interfaces
 *
 * @author amkimian
 */
public final class SearchRepositoryFactory {
    private static Logger log = Logger.getLogger(SearchRepositoryFactory.class);
    private static final Map<Integer, String> implementationMap;

    static {
        Map<Integer, String> setupMap = new HashMap<Integer, String>();
        setupMap.put(FTGenLexer.ELASTIC, "rapture.elasticsearch.ElasticSearchRepository");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    public static SearchRepository createSearchStore(String config) {
        SearchRepository ret = null;
        try {
            log.info("Creating search store from config - " + config);
            FTGenParser parser = getParserForConfig(config);
            int implementationType = parser.getStoreType();
            if (implementationMap.containsKey(implementationType)) {
                ret = getStore(implementationMap.get(implementationType), parser.getInstance(), parser.getImplementionConfig());
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported blob store - " + parser.getImplementationName());
            }
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
        }
        return ret;
    }

    private static SearchRepository getStore(String className, String instanceName, Map<String, String> config) {
        try {
            Class<?> blobClass = Class.forName(className);
            Object fStore;
            fStore = blobClass.newInstance();
            if (fStore instanceof SearchRepository) {
                SearchRepository ret = (SearchRepository) fStore;
                ret.setConfig(config);
                ret.setInstanceName(instanceName);
                return ret;
            } else {
                log.error(className + " is not an search store, cannot instantiate");
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create blob store");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error retrieving blob", e);
        }
    }

    private static FTGenParser getParserForConfig(String config) throws RecognitionException {
        FTGenLexer lexer = new FTGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FTGenParser parser = new FTGenParser(tokens);
        parser.repinfo();
        return parser;
    }

    private SearchRepositoryFactory() {

    }
}
