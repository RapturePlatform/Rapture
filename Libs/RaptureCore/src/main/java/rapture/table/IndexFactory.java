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
package rapture.table;

import rapture.common.RaptureTableConfig;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.IndexConfig;
import rapture.dsl.idef.IndexDefinition;
import rapture.dsl.idef.IndexDefinitionFactory;
import rapture.generated.TGenLexer;
import rapture.generated.TGenParser;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

/**
 * Generate iRepo instances from config strings
 *
 * @author alan
 */
public class IndexFactory {
    private static final String INSTANCE_NAME = "instanceName";
    private static final String DEFINITION = "definition";
    private static Logger log = Logger.getLogger(IndexFactory.class);
    private static final Map<Integer, String> implementationMap;

    static {
        Map<Integer, String> setupMap = new HashMap<Integer, String>();
        // TODO setupMap.put(TGenLexer.FILE, "rapture.table.file.FileIndexHandler");
        setupMap.put(TGenLexer.MEMORY, "rapture.table.memory.MemoryIndexHandler");
        setupMap.put(TGenLexer.MONGODB, "rapture.table.mongodb.MongoIndexHandler");
        setupMap.put(TGenLexer.POSTGRES, "rapture.table.postgres.PostgresIndexHandler");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    private static IndexHandler createHandler(int storeToken, Map<String, String> config, Map<String, String> processConfig) {
        if (implementationMap.containsKey(storeToken)) {
            return getIndexStore(implementationMap.get(storeToken), config, processConfig);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported store type");
        }
    }

    public static IndexHandler createIndex(String config) {
        try {
            TGenLexer lexer = new TGenLexer();
            log.info("Creating table from config - " + config);
            lexer.setCharStream(new ANTLRStringStream(config));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TGenParser parser = new TGenParser(tokens);
            parser.iinfo();

            // parser should have everything we want now

            return createHandler(parser.getStoreType(), parser.getConfig().getConfig(), parser.getProcessorConfig().getConfig());
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + ExceptionToString.format(e));
        } catch (RaptureException e) {
            log.error("Error when initializing index - " + ExceptionToString.format(e));
        }
        return null;
    }

    private static IndexHandler getIndexStore(String className, Map<String, String> config, Map<String, String> processConfig) {
        try {
            Class<?> indexHandlerClass = Class.forName(className);
            Object indexHandlerObject;
            indexHandlerObject = indexHandlerClass.newInstance();
            if (indexHandlerObject instanceof IndexHandler) {
                IndexHandler indexHandler = (IndexHandler) indexHandlerObject;
                if (processConfig.containsKey(INSTANCE_NAME)) {
                    indexHandler.setInstanceName(processConfig.get(INSTANCE_NAME));
                } else {
                    indexHandler.setInstanceName("default");
                }
                if (processConfig.containsKey(DEFINITION)) {
                    IndexDefinition indexDefinition = IndexDefinitionFactory.getDefinition(processConfig.get(DEFINITION));
                    indexHandler.setIndexProducer(new IndexProducer(Collections.singletonList(indexDefinition)));
                }
                indexHandler.setConfig(config);
                return indexHandler;
            } else {
                String message = (className + " is not an index, cannot instantiate");
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message);
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating table store of type " + className, e);
        }
    }

    public static IndexHandler getIndex(String indexURI) {
        IndexConfig config = Kernel.getIndex().getIndex(ContextFactory.getKernelUser(), indexURI);
        RaptureURI internalURI = new RaptureURI(indexURI, Scheme.INDEX);
        log.debug("Table index for " + internalURI.getDocPath() + " is " + config.getConfig());
        return createIndex(config.getConfig());
    }
}
