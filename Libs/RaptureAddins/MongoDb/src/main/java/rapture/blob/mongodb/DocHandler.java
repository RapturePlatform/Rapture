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
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;

import rapture.common.CallingContext;
import rapture.common.exception.ExceptionToString;
import rapture.mongodb.MongoRetryWrapper;
import rapture.mongodb.MongoDBFactory;

public class DocHandler implements BlobHandler {
    private static Logger log = Logger.getLogger(DocHandler.class);
    private static final String CONTENT = "content";
    private static final String BLOB_NAME = "blobName";
    private String instanceName;
    private String bucket;
    private volatile MongoCollection<Document> collection;

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
            Document toStore = new Document();
            toStore.append(BLOB_NAME, docPath);
            toStore.append(CONTENT, toSave);
            try {
                getCollection().insertOne(toStore);
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

    private MongoCollection<Document> getCollection() {
        if (collection == null) {
            synchronized (collectionLock) {
                if (collection == null) {
                    collection = createCollection();
                }
            }
        }
        return collection;
    }

    private MongoCollection<Document> createCollection() {
        MongoDatabase db = MongoDBFactory.getDatabase(instanceName);
        MongoCollection<Document> tempCollection = db.getCollection(bucket);
        Document blobNameIndex = new Document(BLOB_NAME, 1).append("unique", false).append("sparse", true);
        tempCollection.createIndex(blobNameIndex);
        return tempCollection;
    }

    @Override
    public Boolean deleteBlob(CallingContext context, String docPath) {
        log.debug("Removing " + docPath);
        Document query = new Document();
        query.append(BLOB_NAME, docPath);
        try {
            DeleteResult res = getCollection().deleteOne(query);
            return res.getDeletedCount() != 0;
        } catch (MongoException e) {
            log.error("Could not delete " + docPath + ": " + e.getMessage());
            log.debug(ExceptionToString.format(e));
        }
        return false;
    }

    @Override
    public InputStream getBlob(CallingContext context, String docPath) {
        // For now get all of the binary contents and append them together
        final Document query = new Document(BLOB_NAME, docPath);
        final Document fields = new Document(CONTENT, "1");

        MongoRetryWrapper<ByteArrayInputStream> wrapper = new MongoRetryWrapper<ByteArrayInputStream>() {

            public FindIterable<Document> makeCursor() {
                return getCollection().find(query).projection(fields);
            }

            public ByteArrayInputStream action(FindIterable<Document> cursor) {
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                if (cursor != null) {
                    Iterator<Document> iterator = cursor.iterator();
                    if (!iterator.hasNext()){
                        // The result of the find is empty, so treat it as deleted.
                        return null;
                    }
                    for (Document rec : cursor) {
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
