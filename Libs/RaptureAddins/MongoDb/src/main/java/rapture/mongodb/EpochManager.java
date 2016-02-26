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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

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
     *            - the DBCollection to the get next epoch for
     * @return Long - a unique epoch value for this collection
     */
    public static Long nextEpoch(final DBCollection collection) {
        MongoRetryWrapper<Long> wrapper = new MongoRetryWrapper<Long>() {
            public Long action(DBCursor cursor) {
                DBObject ret = collection.findAndModify(getEpochQueryObject(), null, null, false, getIncUpdateObject(getUpdateObject()), true, true);
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
     *            - the DBCollection to interrogate
     * @return Long - the latest epoch currently in use
     */
    public static Long getLatestEpoch(final DBCollection collection) {
        MongoRetryWrapper<Long> wrapper = new MongoRetryWrapper<Long>() {
            public DBCursor makeCursor() {
                return collection.find(getEpochQueryObject());
            }

            public Long action(DBCursor cursor) {
                return (cursor.hasNext()) ? (Long) cursor.next().get(SEQ) : 0L;
            }
        };
        return wrapper.doAction();
    }

    /**
     * Return the starting point for a query object in a collection that
     * contains epochs. We never want to return the epoch document in a query.
     * 
     * @return BasicDBObject - starting query object for a collection with
     *         epochs
     */
    public static BasicDBObject getNotEqualEpochQueryObject() {
        return new BasicDBObject(_ID, new BasicDBObject("$ne", EPOCH_DOCUMENT));
    }

    /**
     * Query by _id. Since _id is always indexed by Mongo, this is guaranteed to
     * be fast.
     * 
     * @return BasicDBObject - query for the epoch document
     */
    private static BasicDBObject getEpochQueryObject() {
        return new BasicDBObject(_ID, EPOCH_DOCUMENT);
    }

    private static BasicDBObject getIncUpdateObject(BasicDBObject update) {
        return new BasicDBObject(DOLLARINC, update);
    }

    private static BasicDBObject getUpdateObject() {
        return new BasicDBObject(SEQ, 1L);
    }
}
