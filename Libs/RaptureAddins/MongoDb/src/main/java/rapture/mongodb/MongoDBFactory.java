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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.ConnectionInfo;
import rapture.common.connection.ConnectionType;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import rapture.config.MultiValueConfigLoader;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public enum MongoDBFactory {
    INSTANCE;

    private static Logger log = Logger.getLogger(MongoDBFactory.class);
    private Map<String, Mongo> mongoInstances = new HashMap<String, Mongo>();
    private Map<String, DB> mongoDBs = new HashMap<String, DB>();
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
        if(Kernel.getSys() == null) {
            return getMongoFromLocalConfig(instanceName);
        // otherwise, get from sys.config
        } else {
            return getMongoFromSysConfig(instanceName);
        }
    }

    private Mongo getMongoFromLocalConfig(String instanceName) {
        String mongoHost = MultiValueConfigLoader.getConfig("MONGODB-" + instanceName);
        log.info("Host is " + mongoHost);
        if(StringUtils.isBlank(mongoHost)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Mongo host is not defined");
        }
        MongoClientURI uri = new MongoClientURI(mongoHost);
        log.info("Username is " + uri.getUsername());
        log.info("Host is " + uri.getHosts().toString());
        log.info("DBName is " + uri.getDatabase());
        log.info("Collection is " + uri.getCollection());

        try {
            Mongo mongo = new MongoClient(uri);
            DB db = mongo.getDB(uri.getDatabase());
            db.authenticate(uri.getUsername(), uri.getPassword());
            mongoDBs.put(instanceName, db);
            mongoInstances.put(instanceName, mongo);
            return mongo;
        } catch (MongoException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        } catch (UnknownHostException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        }
    }

    private Mongo getMongoFromSysConfig(String instanceName) {
        Map<String, ConnectionInfo> map = Kernel.getSys().getConnectionInfo(
                ContextFactory.getKernelUser(),
                ConnectionType.MONGODB.toString());
        if(!map.containsKey(instanceName)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR,
                    "Mongo instance is not defined: " + instanceName);
        }
        ConnectionInfo info = map.get(instanceName);
        log.info("Connection info = " + info);
        try {
            Mongo mongo = new MongoClient(info.getHost(), info.getPort());
            DB db = mongo.getDB(info.getDbName());
            db.authenticate(info.getUsername(), info.getPassword().toCharArray());
            mongoDBs.put(instanceName, db);
            mongoInstances.put(instanceName, mongo);
            return mongo;
        } catch (UnknownHostException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        }
    }

    public boolean canConnect() {
        return !mongoInstances.isEmpty();
    }

    public static DBCollection getCollection(String instanceName, String name) {
        return INSTANCE._getDB(instanceName).getCollection(name);
    }

    /**
     * get the database or throw a RaptureException -- never returns null
     */
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
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not initialized");
                throw raptException;
            }
        }
    }
}
