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
package rapture.relationship;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.RelationshipLexer;
import rapture.generated.RelationshipParser;

public class RelationshipStoreFactory {

    private static final Map<Integer, String> implementationMap;
    static {
        Map<Integer, String> setupMap = new HashMap<Integer, String>();
        // setupMap.put(RelationshipLexer.FILE, "rapture.relationship.file.FileRelationshipStore");
        setupMap.put(RelationshipLexer.MEMORY, "rapture.relationship.memory.MemoryRelationshipStore");
        setupMap.put(RelationshipLexer.CASSANDRA, "rapture.relationship.cassandra.CassandraRelationshipStore");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    public static RelationshipStore getStore(RaptureURI storeURI, String config) {
        try {
            RelationshipParser parser = getParsedForConfig(config);
            int implementationType = parser.getStoreType();
            if (implementationMap.containsKey(implementationType)) {
                return getRelationshipStore(storeURI, implementationMap.get(implementationType), parser.getInstance(), parser.getImplementionConfig());
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported relationship store - " + parser.getImplementationName());
            }
        } catch (RecognitionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported relationship store - " + config, e);
        }
    }

    private static RelationshipStore getRelationshipStore(RaptureURI storeURI, String className, String instanceName, Map<String, String> config) {
        try {
            Class<?> relationshipClass = Class.forName(className);
            Object fStore;
            fStore = relationshipClass.newInstance();
            if (fStore instanceof RelationshipStore) {
                RelationshipStore ret = (RelationshipStore) fStore;
                ret.setInstanceName(instanceName);
                ret.setStoreURI(storeURI);
                ret.setConfig(config);
                return ret;
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create relationship store of type " + className);
            }
        } catch (InstantiationException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create relationship store of type " + className, e);
        } catch (IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create relationship store of type " + className, e);
        } catch (ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create relationship store of type " + className, e);
        }
    }

    private static RelationshipParser getParsedForConfig(String config) throws RecognitionException {
        RelationshipLexer lexer = new RelationshipLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RelationshipParser parser = new RelationshipParser(tokens);
        parser.repinfo();
        return parser;
    }

}
