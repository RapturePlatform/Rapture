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
package rapture.mongodb;

import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import rapture.config.MultiValueConfigLoader;

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
        String mongoHost = MultiValueConfigLoader.getConfig("MONGODB-" + instanceName);
        log.info("Host is " + mongoHost);
        Mongo mongo;
        if (mongoHost != null) {
            MongoClientURI uri = new MongoClientURI(mongoHost);
            log.info("Username is " + uri.getUsername());
            log.info("Host is " + uri.getHosts().toString());
            log.info("DBName is " + uri.getDatabase());
            log.info("Collection is " + uri.getCollection());

            try {
                mongo = new MongoClient(uri);
                DB db = mongo.getDB(uri.getDatabase());
                db.authenticate(uri.getUsername(), uri.getPassword());
                mongoDBs.put(instanceName, db);
            } catch (MongoException e) {
                mongo = null;
            } catch (UnknownHostException e) {
                mongo = null;
            }
        } else {

            mongoHost = "127.0.0.1:27017/Test";
            String[] parts = mongoHost.split("/");
            String[] hostParts = parts[0].split(":");

            try {
                mongo = new MongoClient(hostParts[0], Integer.parseInt(hostParts[1]));
            } catch (NumberFormatException e) {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error connecting to database");
                log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
                throw raptException;
            } catch (UnknownHostException e) {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error connecting to database");
                log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
                throw raptException;
            } catch (MongoException e) {
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error connecting to database");
                log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
                throw raptException;
            }
            mongoDBs.put(instanceName, mongo.getDB("Test"));
        }
        if (mongo != null) {
            mongoInstances.put(instanceName, mongo);
        }
        return mongo;
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
