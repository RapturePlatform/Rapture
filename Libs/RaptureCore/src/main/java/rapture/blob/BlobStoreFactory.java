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
package rapture.blob;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.BlobGenLexer;
import rapture.generated.BlobGenParser;

/**
 * A factory that creates blob store interfaces
 *
 * @author amkimian
 */
public final class BlobStoreFactory {
    private static Logger log = Logger.getLogger(BlobStoreFactory.class);
    private static final Map<Integer, String> implementationMap;

    static {
        Map<Integer, String> setupMap = new HashMap<>();
        setupMap.put(BlobGenLexer.MEMORY, "rapture.blob.memory.MemoryBlobStore");
        setupMap.put(BlobGenLexer.FILE, "rapture.blob.file.FileBlobStore");
        setupMap.put(BlobGenLexer.CASSANDRA, "rapture.blob.cassandra.CassandraBlobStore");
        setupMap.put(BlobGenLexer.GSTORE, "rapture.blob.google.GoogleBlobStore");
        setupMap.put(BlobGenLexer.MONGODB, "rapture.blob.mongodb.MongoDBBlobStore");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    public static BlobStore createBlobStore(String config) {
        BlobStore ret = null;
        try {
            log.info("Creating blob store from config - " + config);
            BlobGenParser parser = getParserForConfig(config);
            int implementationType = parser.getStoreType();
            if (implementationMap.containsKey(implementationType)) {
                ret = getBlob(implementationMap.get(implementationType), parser.getInstance(), parser.getImplementionConfig());
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported blob store - " + parser.getImplementationName());
            }
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
        }
        return ret;
    }

    private static BlobStore getBlob(String className, String instanceName, Map<String, String> config) {
        try {
            Class<?> blobClass = Class.forName(className);
            Object fStore;
            fStore = blobClass.newInstance();
            if (fStore instanceof BlobStore) {
                BlobStore ret = (BlobStore) fStore;
                ret.setConfig(config);
                ret.setInstanceName(instanceName);
                return ret;
            } else {
                log.error(className + " is not an blob store, cannot instantiate");
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create blob store");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error retrieving blob", e);
        }
    }

    private static BlobGenParser getParserForConfig(String config) throws RecognitionException {
        BlobGenLexer lexer = new BlobGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BlobGenParser parser = new BlobGenParser(tokens);
        parser.repinfo();
        return parser;
    }

    private BlobStoreFactory() {

    }
}
