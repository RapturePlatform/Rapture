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
package rapture.blob.mongodb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.exception.ExceptionToString;
import rapture.mongodb.MongoDBFactory;
import rapture.mongodb.MongoRetryWrapper;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class DocHandler implements BlobHandler {
    private static Logger log = Logger.getLogger(DocHandler.class);
    private static final String CONTENT = "content";
    private static final String BLOB_NAME = "blobName";
    private String instanceName;
    private String bucket;
    private volatile DBCollection collection;

    public DocHandler(String instanceName, String bucket) {
        this.instanceName = instanceName;
        this.bucket = bucket;
    }

    @Override
    public Boolean storeBlob(CallingContext context, String docPath, InputStream content, Boolean append) {
        log.debug("Saving " + docPath);
        byte[] toSave;
        try {
            toSave = IOUtils.toByteArray(content);
            BasicDBObject toStore = new BasicDBObject();
            toStore.append(BLOB_NAME, docPath);
            toStore.append(CONTENT, toSave);
            try {
                getCollection().insert(toStore);
                return true;
            } catch (MongoException e) {
                log.error("Could not store " + docPath + ": " + e.getMessage());
                log.debug(ExceptionToString.format(e));
            }
        } catch (IOException e1) {
            log.error("Could not read content gto store: " + e1.getMessage());
            log.debug(ExceptionToString.format(e1));
        }
        return false;
    }

    private static final Object collectionLock = new Object();

    private DBCollection getCollection() {
        if (collection == null) {
            synchronized (collectionLock) {
                if (collection == null) {
                    collection = createCollection();
                }
            }
        }
        return collection;
    }

    private DBCollection createCollection() {
        DB db = MongoDBFactory.getDB(instanceName);
        DBCollection tempCollection = db.getCollection(bucket);
        DBObject blobNameIndex = new BasicDBObject(BLOB_NAME, 1).append("unique", false).append("sparse", true);
        tempCollection.createIndex(blobNameIndex);
        return tempCollection;
    }

    @Override
    public Boolean deleteBlob(CallingContext context, String docPath) {
        log.debug("Removing " + docPath);
        BasicDBObject query = new BasicDBObject();
        query.append(BLOB_NAME, docPath);
        try {
            WriteResult res = getCollection().remove(query);
            return res.getN() != 0;
        } catch (MongoException e) {
            log.error("Could not delete " + docPath + ": " + e.getMessage());
            log.debug(ExceptionToString.format(e));
        }
        return false;
    }

    @Override
    public InputStream getBlob(CallingContext context, String docPath) {
        // For now get all of the binary contents and append them together
        final BasicDBObject query = new BasicDBObject();
        query.append(BLOB_NAME, docPath);

        final BasicDBObject fields = new BasicDBObject();
        fields.put(CONTENT, "1");

        MongoRetryWrapper<ByteArrayInputStream> wrapper = new MongoRetryWrapper<ByteArrayInputStream>() {

            public DBCursor makeCursor() {
                return getCollection().find(query, fields);
            }

            public ByteArrayInputStream action(DBCursor cursor) {
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                if (cursor != null) {
                    if (!cursor.hasNext()){
                        // The result of the find is empty, so treat it as deleted.
                        return null;
                    }
                    while (cursor.hasNext()) {
                        DBObject rec = cursor.next();
                        byte[] data = (byte[]) rec.get(CONTENT);
                        try {
                            bao.write(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                byte[] content = bao.toByteArray();
                return new ByteArrayInputStream(content);
            }
        };

        return wrapper.doAction();
    }
}
