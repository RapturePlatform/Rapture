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
package rapture.audit;

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
import rapture.generated.AuditGenLexer;
import rapture.generated.AuditGenParser;

/**
 * This class is used to create idgen interfaces from config.
 * 
 */

public final class AuditLogFactory {
    private static Logger log = Logger.getLogger(AuditLogFactory.class);
    private static final Map<Integer, String> implementationMap;
    static {
        Map<Integer, String> setupMap = new HashMap<Integer, String>();
        setupMap.put(AuditGenLexer.MEMORY, "rapture.audit.memory.MemoryAudit"); //$NON-NLS-1$
	// TODO AuditGenLexer.FILE ?
        setupMap.put(AuditGenLexer.REDIS, "rapture.audit.redis.RedisAudit"); //$NON-NLS-1$
        setupMap.put(AuditGenLexer.MONGODB, "rapture.audit.mongodb.MongoDBAuditLog"); //$NON-NLS-1$
        setupMap.put(AuditGenLexer.LOG4J, "rapture.audit.log4j.Log4jAudit"); //$NON-NLS-1$
        setupMap.put(AuditGenLexer.BLOB, "rapture.audit.blob.BlobAudit");
        setupMap.put(AuditGenLexer.ELASTIC, "rapture.audit.es.ElasticSearchAuditLog");
        setupMap.put(AuditGenLexer.NOTHING, "rapture.audit.NothingLog");
        implementationMap = Collections.unmodifiableMap(setupMap);
    }

    public static AuditLog getLog(String id, String config) {
        try {
            log.info(Messages.getString("CreateAuditLog") + config); //$NON-NLS-1$
            AuditGenParser parser = getParsedForConfig(config);
            int implementationType = parser.getImplementationType();
            if (implementationMap.containsKey(implementationType)) {
                return getLog(implementationMap.get(implementationType), id, parser.getInstance(), parser.getImplementionConfig());
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("UnsupportedLogStore") //$NON-NLS-1$
                        + parser.getImplementationName());
            }
        } catch (RecognitionException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("ErrorParsingConfig"));
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }
    }

    private static AuditLog getLog(String className, String id, String instance, Map<String, String> config) {
        try {
            Class<?> idGenClass = Class.forName(className);
            Object fStore;
            fStore = idGenClass.newInstance();
            if (fStore instanceof AuditLog) {
                AuditLog ret = (AuditLog) fStore;
                ret.setInstanceName(instance);
                ret.setConfig(id, config);
                return ret;
            } else {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("CouldNotCreate") + className + Messages.getString("NotAnAuditLog"));
                throw raptException;
            }
        } catch (Exception e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating audit log of type " + className);
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }
    }

    private static AuditGenParser getParsedForConfig(String config) throws RecognitionException {
        AuditGenLexer lexer = new AuditGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AuditGenParser parser = new AuditGenParser(tokens);
        parser.loginfo();
        return parser;
    }

    private AuditLogFactory() {
    }
}
