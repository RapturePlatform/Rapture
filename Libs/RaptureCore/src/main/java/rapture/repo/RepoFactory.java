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
package rapture.repo;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.ConfigDef;
import rapture.generated.RapGenLexer;
import rapture.generated.RapGenParser;
import rapture.lock.ILockingHandler;
import rapture.lock.LockFactory;
import rapture.repo.db.BackedKeyStore;
import rapture.repo.meta.CachedRepo;
import rapture.repo.meta.ReadOnlyRepo;
import rapture.repo.meta.ShadowedRepo;

/**
 * Generate iRepo instances from config strings
 *
 * @author alan
 */
public class RepoFactory {
    private static Logger log = Logger.getLogger(RepoFactory.class);
    private static final Map<Integer, String> keyStoreImplementationMap;
    private static final Map<Integer, String> sqlStoreImplementationMap;

    static {
        keyStoreImplementationMap = createKeystoreMap();

        Map<Integer, String> sqlSetupMap = new HashMap<>();
        sqlSetupMap.put(RapGenLexer.JDBC, "rapture.repo.jdbc.JDBCSqlStore");
        sqlStoreImplementationMap = Collections.unmodifiableMap(sqlSetupMap);

        log.debug("Created implementation Map");
    }

    private static Map<Integer, String> createKeystoreMap() {
        Map<Integer, String> keyStoreSetupMap = new HashMap<>();
        keyStoreSetupMap.put(RapGenLexer.GCP_STORAGE, "rapture.repo.google.GoogleStorageKeyStore");
        keyStoreSetupMap.put(RapGenLexer.GCP_DATASTORE, "rapture.repo.google.GoogleDatastoreKeyStore");
        keyStoreSetupMap.put(RapGenLexer.MEMORY, "rapture.repo.mem.MemKeyStore");
        keyStoreSetupMap.put(RapGenLexer.REDIS, "rapture.repo.redis.RedisKeyStore");
        keyStoreSetupMap.put(RapGenLexer.AWS, "rapture.repo.aws.SimpleDbKeyStore");
        keyStoreSetupMap.put(RapGenLexer.MEMCACHED, "rapture.repo.memcache.MemCacheKeyStore");
        keyStoreSetupMap.put(RapGenLexer.FILE, "rapture.repo.file.FileDataStore");
        keyStoreSetupMap.put(RapGenLexer.MONGODB, "rapture.repo.mongodb.MongoDbDataStore");
        keyStoreSetupMap.put(RapGenLexer.EHCACHE, "rapture.repo.ehcache.EhCacheKeyStore");
        keyStoreSetupMap.put(RapGenLexer.CASSANDRA, "rapture.repo.cassandra.CassandraKeyStore");
        keyStoreSetupMap.put(RapGenLexer.CSV, "rapture.repo.file.CSVKeyStore");
        keyStoreSetupMap.put(RapGenLexer.POSTGRES, "rapture.repo.postgres.PostgresDataStore");
        return Collections.unmodifiableMap(keyStoreSetupMap);
    }

    private static SQLStore createSQLStore(int storeToken, String instanceName, Map<String, String> config) {
        if (sqlStoreImplementationMap.containsKey(storeToken)) {
            return getSQLStore(sqlStoreImplementationMap.get(storeToken), instanceName, config);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported sql store type");
        }
    }

    private static KeyStore createKeyStore(int storeToken, String instanceName, Map<String, String> config, boolean noFolderHandling) {
        if (keyStoreImplementationMap.containsKey(storeToken)) {
            return getKeyStore(keyStoreImplementationMap.get(storeToken), instanceName, config, noFolderHandling);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported key store type");
        }
    }

    private static ILockingHandler createLocker(int storeToken) {
        switch (storeToken) {
            //case RapGenLexer.MONGODB:
            //    return LockFactory.createLock("LOCKING USING MONGODB {}");
            default:
                return LockFactory.createLock("LOCKING USING DUMMY {}");
        }
    }

    private static Repository createKeyStoreBasedRepo(ConfigDef repConfig, KeyStore store, ILockingHandler locker) {
        if (repConfig.getName().equals("VREP")) {
            store.resetFolderHandling();
            return new VersionedRepo(repConfig.getConfig(), store, store.createRelatedKeyStore("cache"), locker);
        } else if (repConfig.getName().equals("REP")) {
            return new UnversionedRepo(repConfig.getConfig(), store, createMetaStore(store), createAttributeStore(store), locker);
        } else if (repConfig.getName().equals("NREP")) {
            log.debug("Creating NVersioned repo on " + store.getClass().toString());
            return new NVersionedRepo(repConfig.getConfig(), store, createVersionStore(store), createMetaStore(store),
                    createAttributeStore(store), locker);
        }
        return null;
    }

    private static KeyStore createVersionStore(KeyStore store) {
        return store.createRelatedKeyStore("version");
    }

    private static KeyStore createAttributeStore(KeyStore store) {
        return store.createRelatedKeyStore("attribute");
    }

    private static KeyStore createMetaStore(KeyStore store) {
        return store.createRelatedKeyStore("meta");
    }

    private static KeyStore getKeyStore(String className, String instanceName, Map<String, String> config, boolean noFolderHandling) {
        try {
            Class<?> idgenClass = Class.forName(className);
            Object fStore;
            fStore = idgenClass.newInstance();
            if (fStore instanceof KeyStore) {
                KeyStore ret = (KeyStore) fStore;
                ret.setInstanceName(instanceName);
                if (noFolderHandling) {
                    ret.resetFolderHandling();
                }
                ret.setConfig(config);
                return ret;
            } else {
                String message = className + " is not a repo, cannot instantiate";
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, message);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create repo store of type " + className, e);
        }
    }

    private static SQLStore getSQLStore(String className, String instanceName, Map<String, String> config) {
        try {
            Class<?> idgenClass = Class.forName(className);
            Object fStore;
            fStore = idgenClass.newInstance();
            if (fStore instanceof SQLStore) {
                SQLStore ret = (SQLStore) fStore;
                ret.setInstanceName(instanceName);
                ret.setConfig(config);
                return ret;
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, className + " is not a repo, cannot instantiate");
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error instantiating sql store of type " + className, e);
        }
    }

    public static Repository getRepo(String config) {
        try {
            log.info("Creating repo from config - " + config);
            RapGenParser parser = parseConfig(config);
            String name = parser.getProcessorConfig().getName();
            if ((name != null) && name.equals("QREP")) {
                return createQRepo(parser);
            } else if ((name != null) && name.equals("RREP")){
            	return createRRepo(parser);
            } else {
                return createKeyRepo(parser);
            }

        } catch (RecognitionException e) {
            log.error(ExceptionToString.format(e));
            log.error("Error parsing config - " + e.getMessage());
        } catch (RaptureException e) {
            log.error(ExceptionToString.format(e));
            log.error("Error when initializing repo - " + e.getMessage());
        }
        return null;
    }

    private static Repository createQRepo(RapGenParser parser) {
        // Create a repo that uses a store that is inherently SQL based
        SQLStore baseSQLStore = createSQLStore(parser.getStoreType(), parser.getInstance(), parser.getImplementionConfig());
        return new QRepo(baseSQLStore, parser.getProcessorConfig().getConfig());
    }
    
    private static Repository createRRepo(RapGenParser parser) {
    	// Create a repo that uses a set of scripts to back the repo
    	return new RRepo(parser.getProcessorConfig().getConfig());
    }

    private static Repository createKeyRepo(RapGenParser parser) {
        // parser should have everything we want now

        int storeType = parser.getStoreType();
        ILockingHandler locker = createLocker(storeType);
        KeyStore baseKeyStore = createKeyStore(storeType, parser.getInstance(), parser.getImplementionConfig(),
                parser.getProcessorConfig().getName().equals("VREP"));
        if (parser.getCacheConfig() != null) {
            log.debug("Creating backed key store");
            KeyStore cacheStore = createKeyStore(parser.getCache().getType(), parser.getInstance(), parser.getCacheConfig().getConfig(), false);
            baseKeyStore = new BackedKeyStore(baseKeyStore, cacheStore);
        } else if (parser.getShadowConfig() != null) {
            log.debug("Creating shadowed key store");
            KeyStore shadowStore = createKeyStore(parser.getShadow().getType(), parser.getInstance(), parser.getShadowConfig().getConfig(), false);
            Repository shadowRepo = null;
            if (parser.getShadowType().getType() == RapGenLexer.SHADOW) {
                shadowRepo = new UnversionedRepo(null, shadowStore, createMetaStore(shadowStore), createAttributeStore(shadowStore), createLocker(
                        parser.getShadow().getType()));
            } else if (parser.getShadowType().getType() == RapGenLexer.VSHADOW) {
                shadowRepo = new VersionedRepo(null, shadowStore, shadowStore.createRelatedKeyStore("cache"), createLocker(parser.getShadow()
                        .getType()));
            } else {
                log.error("Really bad state when creating shadow repo");
            }
            if (parser.isGeneralCache()) {
                return new CachedRepo(createKeyStoreBasedRepo(parser.getProcessorConfig(), baseKeyStore, locker), shadowRepo);
            } else {
                return new ShadowedRepo(createKeyStoreBasedRepo(parser.getProcessorConfig(), baseKeyStore, locker), shadowRepo);
            }
        }
        if (parser.getReadOnly()) {
            return new ReadOnlyRepo(createKeyStoreBasedRepo(parser.getProcessorConfig(), baseKeyStore, locker));
        }
        return createKeyStoreBasedRepo(parser.getProcessorConfig(), baseKeyStore, locker);
    }

    private static RapGenParser parseConfig(String config) throws RecognitionException {
        try {
            RapGenLexer lexer = new RapGenLexer();
            lexer.setCharStream(new ANTLRStringStream(config));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            RapGenParser parser = new RapGenParser(tokens);
            parser.repinfo();
            return parser;
        } catch (Exception e) {
            String error = ExceptionToString.format(e);
            log.error(error);
            throw e;
        }
    }

    private RepoFactory() {
    }
}
