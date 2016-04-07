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
package rapture.idgen.mongodb;

import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.Document;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.idgen.IdGenStore;
import rapture.mongodb.MongoDBFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;

/**
 * A idgen implemented on MongoDb
 */

public class IdGenMongoStore implements IdGenStore {
    private static Logger log = Logger.getLogger(IdGenMongoStore.class);

    private static final String DOLLARINC = "$inc";
    private static final String SEQ = "seq";
    private static final String IDGEN = "idgen";
    private static final String IDGEN_NAME = "idgenName";
    private static final String TABLE_NAME = "prefix";
    private static final String VALID = "valid";
    private String tableName;
    private String idGenName = "idgen";
    private String instanceName = "default";

    public IdGenMongoStore() {
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    private DBCollection getIdGenCollection() {
        return MongoDBFactory.getCollection(instanceName, tableName);
    }

    private BasicDBObject getIncUpdateObject(BasicDBObject update) {
        BasicDBObject realUpdate = new BasicDBObject();
        realUpdate.put(DOLLARINC, update);
        return realUpdate;
    }

    @Override
    public Long getNextIdGen(Long interval) {
        BasicDBObject realUpdate = getIncUpdateObject(getUpdateObject(interval));
         DBObject ret = getIdGenCollection().findAndModify(getQueryObject(), null, null, false, realUpdate, true,
                true);
        Boolean valid = (Boolean) ret.get(VALID);
        if (valid != null && !valid) {
            throw RaptureExceptionFactory.create("IdGenerator has been deleted");
        }
        return (Long) ret.get(SEQ);
    }

    private BasicDBObject getQueryObject() {
        BasicDBObject query = new BasicDBObject();
        query.put(IDGEN, idGenName);
        return query;
    }

    private BasicDBObject getUpdateObject(Long interval) {
        BasicDBObject update = new BasicDBObject();
        update.put(SEQ, interval);
        return update;
    }

    private BasicDBObject getInvalidator() {
        BasicDBObject invalidator = new BasicDBObject();
        invalidator.append("$set", new BasicDBObject().append(VALID, false));
        return invalidator;
    }
    
    private BasicDBObject getRevalidator() {
        BasicDBObject invalidator = new BasicDBObject();
        invalidator.append("$set", new BasicDBObject().append(VALID, true));
        return invalidator;
    }

    @Override
    public void resetIdGen(Long number) {
        // RAP-2109 Just use maths to change the value. Trying to replace the object does not work.
        Long currentValue = getNextIdGen(0L);
        Long decrement = number - currentValue;
        getNextIdGen(decrement);
    }

    @Override
    public void setConfig(Map<String, String> config) {
        log.info("Set config is " + config);
        tableName = config.get(TABLE_NAME);
        if (config.containsKey(IDGEN_NAME)) {
            idGenName = config.get(IDGEN_NAME);
        }
    }

    @Override
    public void invalidate() {
        BasicDBObject invalidator = getInvalidator();
        getIdGenCollection().findAndModify(getQueryObject(), null, null, false, invalidator, true, true);
    }
    
    public void makeValid() {
        BasicDBObject invalidator = getRevalidator();
        getIdGenCollection().findAndModify(getQueryObject(), null, null, false, invalidator, true, true);
    }

    @Override
    public void init() {

    }
}
