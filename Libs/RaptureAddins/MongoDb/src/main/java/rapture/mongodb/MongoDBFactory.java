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
package rapture.mongodb;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import rapture.common.ConnectionInfo;
import rapture.common.Messages;
import rapture.common.connection.ConnectionType;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.config.MultiValueConfigLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public enum MongoDBFactory {
    INSTANCE;

    private Messages mongoMsgCatalog = new Messages("Mongo");

    private static Logger log = Logger.getLogger(MongoDBFactory.class);
    private Map<String, Mongo> mongoInstances = new HashMap<>();

    // Legacy. Only still here because GridFS needs it.
    private Map<String, DB> mongoDBs = new HashMap<>();
    private Map<String, MongoDatabase> mongoDatabases = new HashMap<>();
    private static int retryCount = 3;

    public static int getRetryCount() {
        return retryCount;
    }

    public static void setRetryCount(int retryCount) {
        MongoDBFactory.retryCount = retryCount;
    }

    private Mongo getMongoForInstance(String instanceName) {
        if (instanceName == null) {
            instanceName = "default";
        }
        if (mongoInstances.containsKey(instanceName)) {
            return mongoInstances.get(instanceName);
        }
        // if bootstrap, get from local config file
        if (Kernel.getSys() == null) {
            return getMongoFromLocalConfig(instanceName);
            // otherwise, get from sys.config
        } else {
            return getMongoFromSysConfig(instanceName);
        }
    }

    private Mongo getMongoFromLocalConfig(String instanceName) {
        String mongoHost = MultiValueConfigLoader.getConfig("MONGODB-" + instanceName);
        log.info("Host is " + mongoHost);
        if (StringUtils.isBlank(mongoHost)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, mongoMsgCatalog.getMessage("NoHost"));
        }
        MongoClientURI uri = new MongoClientURI(mongoHost);
        log.info("Username is " + uri.getUsername());
        log.info("Host is " + uri.getHosts().toString());
        log.info("DBName is " + uri.getDatabase());
        log.info("Collection is " + uri.getCollection());

        try {
            MongoClient mongo = new MongoClient(uri);
            mongoDBs.put(instanceName, mongo.getDB(uri.getDatabase()));
            mongoDatabases.put(instanceName, mongo.getDatabase(uri.getDatabase()));
            mongoInstances.put(instanceName, mongo);
            return mongo;
        } catch (MongoException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, new ExceptionToString(e));
        }
    }

    private Mongo getMongoFromSysConfig(String instanceName) {
        Map<String, ConnectionInfo> map = Kernel.getSys().getConnectionInfo(ContextFactory.getKernelUser(), ConnectionType.MONGODB.toString());
        if (!map.containsKey(instanceName)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, mongoMsgCatalog.getMessage("NoInstance", instanceName));
        }
        ConnectionInfo info = map.get(instanceName);
        log.info("Connection info = " + info);
        try {
            MongoClient mongo = new MongoClient(info.getHost(), info.getPort());
            mongoDBs.put(instanceName, mongo.getDB(info.getDbName()));
            mongoDatabases.put(instanceName, mongo.getDatabase(info.getDbName()));
            mongoInstances.put(instanceName, mongo);
            return mongo;
        } catch (MongoException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, new ExceptionToString(e));
        }
    }

    public boolean canConnect() {
        return !mongoInstances.isEmpty();
    }

    /**
     * @deprecated Use getMongoDatabase
     * This is still needed because GridFS constructor requires a DB not a MongoDatabase.
     * Once GridFS is updated (Mongo 3.1) this can be eliminated. 
     */
    @Deprecated
    public static DB getDB(String instanceName) {
        return INSTANCE._getDB(instanceName);
    }

    private DB _getDB(String instanceName) {
        if (mongoDBs.containsKey(instanceName)) {
            return mongoDBs.get(instanceName);
        } else {
            getMongoForInstance(instanceName);
            if (mongoDBs.containsKey(instanceName)) {
                return mongoDBs.get(instanceName);
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, mongoMsgCatalog.getMessage("NotInitialized"));
            }
        }
    }

    public static MongoCollection<Document> getCollection(String instanceName, String name) {
        return INSTANCE._getMongoDatabase(instanceName).getCollection(name);
    }

    /**
     * get the database or throw a RaptureException -- never returns null
     */
    public static MongoDatabase getDatabase(String instanceName) {
        return INSTANCE._getMongoDatabase(instanceName);
    }

    private MongoDatabase _getMongoDatabase(String instanceName) {
        if (mongoDatabases.containsKey(instanceName)) {
            return mongoDatabases.get(instanceName);
        } else {
            getMongoForInstance(instanceName);
            if (mongoDatabases.containsKey(instanceName)) {
                return mongoDatabases.get(instanceName);
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, mongoMsgCatalog.getMessage("NotInitialized"));
            }
        }
    }
}
