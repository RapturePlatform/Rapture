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

import java.net.HttpURLConnection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

import rapture.common.Messages;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.idgen.IdGenStore;
import rapture.mongodb.MongoDBFactory;

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
    private Messages mongoMsgCatalog = new Messages("Mongo");

    public IdGenMongoStore() {
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    private MongoCollection<Document> getIdGenCollection() {
        return MongoDBFactory.getCollection(instanceName, tableName);
    }

    private Document getIncUpdateObject(Document update) {
        Document realUpdate = new Document();
        realUpdate.put(DOLLARINC, update);
        return realUpdate;
    }

    @Override
    public Long getNextIdGen(Long interval) {
        Document realUpdate = getIncUpdateObject(getUpdateObject(interval));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER);

        Document ret = getIdGenCollection().findOneAndUpdate(getQueryObject(), realUpdate, options);
        if (ret == null) return null;
        
        Boolean valid = (Boolean) ret.get(VALID);
        if (valid != null && !valid) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    mongoMsgCatalog.getMessage("IdGenerator"));
        }
        return (Long) ret.get(SEQ);
    }

    private Document getQueryObject() {
        Document query = new Document();
        query.put(IDGEN, idGenName);
        return query;
    }

    private Document getUpdateObject(Long interval) {
        Document update = new Document();
        update.put(SEQ, interval);
        return update;
    }

    private Document getInvalidator() {
        Document invalidator = new Document();
        invalidator.append("$set", new Document().append(VALID, false));
        return invalidator;
    }

    private Document getRevalidator() {
        Document invalidator = new Document();
        invalidator.append("$set", new Document().append(VALID, true));
        return invalidator;
    }

    @Override
    public void resetIdGen(Long number) {
        // RAP-2109 Just use maths to change the value. Trying to replace the
        // object does not work.
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
        Document invalidator = getInvalidator();
        getIdGenCollection().findOneAndUpdate(getQueryObject(), invalidator, new FindOneAndUpdateOptions().upsert(true));
    }

    public void makeValid() {
        Document invalidator = getRevalidator();
        getIdGenCollection().findOneAndUpdate(getQueryObject(), invalidator, new FindOneAndUpdateOptions().upsert(true));
    }

    @Override
    public void init() {

    }
}
