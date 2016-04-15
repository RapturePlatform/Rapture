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

import java.util.Iterator;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;

/**
 * Useful tools for handling all things epoch related in Mongo
 * 
 * @author dukenguyen
 */
public class EpochManager {

    /*
     * mongo's ubiquitous _id key
     */
    private static final String _ID = "_id";

    /*
     * field for the increasing sequence number
     */
    private static final String SEQ = "seq";

    /*
     * mongo directive used to increment a value
     */
    private static final String DOLLARINC = "$inc";

    /*
     * field used to find the epoch document that stores the sequence number
     */
    private static final String EPOCH_DOCUMENT = "epochDocument";

    /**
     * Returns the next epoch available and advances the counter. Guaranteed to
     * be unique for the given collection. If the epoch document does not
     * already exist a new one is created and the first epoch returned will be
     * 1L.
     * 
     * @param collection
     *            - the MongoCollection to the get next epoch for
     * @return Long - a unique epoch value for this collection
     */
    public static Long nextEpoch(final MongoCollection<Document> collection) {
        final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER);

        MongoRetryWrapper<Long> wrapper = new MongoRetryWrapper<Long>() {
            public Long action(FindIterable<Document> cursor) {
                Document ret = collection.findOneAndUpdate(getEpochQueryObject(), getIncUpdateObject(getUpdateObject()), options);
                return (Long) ret.get(SEQ);
            }
        };
        return wrapper.doAction();
    }

    /**
     * Read-only operation that will return the latest epoch that is in use for
     * the given collection.
     * 
     * @param collection
     *            - the MongoCollection to interrogate
     * @return Long - the latest epoch currently in use
     */
    public static Long getLatestEpoch(final MongoCollection<Document> collection) {
        MongoRetryWrapper<Long> wrapper = new MongoRetryWrapper<Long>() {
            public FindIterable<Document> makeCursor() {
                return collection.find(getEpochQueryObject());
            }

            public Long action(FindIterable<Document> cursor) {
                Iterator<Document> iterator = cursor.iterator();
                return (iterator.hasNext()) ? (Long) iterator.next().get(SEQ) : 0L;
            }
        };
        return wrapper.doAction();
    }

    /**
     * Return the starting point for a query object in a collection that
     * contains epochs. We never want to return the epoch document in a query.
     * 
     * @return Document - starting query object for a collection with epochs
     */
    public static Document getNotEqualEpochQueryObject() {
        return new Document(_ID, new Document("$ne", EPOCH_DOCUMENT));
    }

    /**
     * Query by _id. Since _id is always indexed by Mongo, this is guaranteed to
     * be fast.
     * 
     * @return Document - query for the epoch document
     */
    private static Document getEpochQueryObject() {
        return new Document(_ID, EPOCH_DOCUMENT);
    }

    private static Document getIncUpdateObject(Document update) {
        return new Document(DOLLARINC, update);
    }

    private static Document getUpdateObject() {
        return new Document(SEQ, 1L);
    }
}
