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
package rapture.lock;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.RaptureLockConfig;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.config.MultiValueConfigLoader;
import rapture.generated.LGenLexer;
import rapture.generated.LGenParser;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * Generate iRepo instances from config strings
 * 
 * @author alan
 * 
 */
public final class LockFactory {
    private static Logger log = Logger.getLogger(LockFactory.class);

    private static Map<String, ILockingHandler> lockCache = new HashMap<String, ILockingHandler>();

    public static ILockingHandler createLock(String config) {
        try {
            LGenParser parser = parseConfig(config);
            switch (parser.getStore().getType()) {
            case LGenLexer.MONGODB:
                // MongoLockHandler2 may be dangerous in a multi-node system if the system clocks are not exactly in sync
                boolean useMongo2 = Boolean.parseBoolean(MultiValueConfigLoader.getConfig("MONGODB-lock.useVersion2", "false"));
                if (useMongo2) {
                    return getLockStore("rapture.lock.mongodb.MongoLockHandler2", parser.getConfig().getConfig());
                } else {
                    return getLockStore("rapture.lock.mongodb.MongoLockHandler", parser.getConfig().getConfig());
                }
            case LGenLexer.MEMORY:
                return getLockStore("rapture.lock.memory.MemoryLockingHandler", parser.getConfig().getConfig());
            case LGenLexer.DUMMY:
                return getLockStore("rapture.lock.dummy.DummyLockHandler", parser.getConfig().getConfig());
            case LGenLexer.REDIS:
                return getLockStore("rapture.lock.redis.RedisLockHandler", parser.getConfig().getConfig());
            case LGenLexer.ZOOKEEPER:
                return getLockStore("rapture.lock.zookeeper.ZooKeeperLockHandler", parser.getConfig().getConfig());
            case LGenLexer.ETCD:
                return getLockStore("rapture.lock.etcd.ETCDLockHandler", parser.getConfig().getConfig());
            // TODO case LGenLexer.FILE: ?
            }
        } catch (RecognitionException e) {
            log.error("Error parsing config - " + e.getMessage());
        }
        return null;
    }

    public static ILockingHandler getLockHandler(String providerURI) {
        if (lockCache.containsKey(providerURI)) {
            return lockCache.get(providerURI);
        }
        RaptureLockConfig config = Kernel.getLock().getLockManagerConfig(ContextFactory.getKernelUser(), providerURI);
        ILockingHandler ret = createLock(config.getConfig());
        lockCache.put(providerURI, ret);
        return ret;
    }

    private static ILockingHandler getLockStore(String className, Map<String, String> config) {
        Class<?> klass = null;
        try {
            klass = Class.forName(className);
            if (klass == null) {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Cannot obtain class for " + className);
                throw raptException;
            }
            Object fStore;
            fStore = klass.newInstance();
            if (fStore instanceof ILockingHandler) {
                ILockingHandler ret = (ILockingHandler) fStore;
                ret.setConfig(config);
                return ret;
            } else {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create lock handler");
                log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, className + " is not a lock implementation, cannot instantiate"));
                throw raptException;
            }
        } catch (Exception e) {
            log.error(ExceptionToString.format(e));
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "Could not create lock handler for " + className + " : " + e.getMessage(), e);
        }
    }

    private static LGenParser parseConfig(String config) throws RecognitionException {
        LGenLexer lexer = new LGenLexer();
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LGenParser parser = new LGenParser(tokens);
        parser.linfo();
        return parser;
    }

    private LockFactory() {
    }

}
