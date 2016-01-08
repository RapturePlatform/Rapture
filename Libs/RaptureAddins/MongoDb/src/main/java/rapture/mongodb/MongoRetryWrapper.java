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

import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;

import com.mongodb.DBCursor;
import com.mongodb.MongoException;

public abstract class MongoRetryWrapper<T> {
    private static Logger log = Logger.getLogger(MongoRetryWrapper.class);
    private int retryCount = MongoDBFactory.getRetryCount();
        
    public MongoRetryWrapper() {
    }
        
    /**
     * Handle the action.
     * @param cursor Database cursor returned by makeCursor (can be null)
     * @return
     * @throws MongoException
     */
    public abstract T action(DBCursor cursor) throws MongoException;

    /**
     * If you need a DBCursor, override this method.
     * @return
     */
    public DBCursor makeCursor() {
        return null;
    }

    public T doAction() {
        T object = null;
        DBCursor cursor = null;
        while (retryCount-- > 0) {
            try {
                cursor = makeCursor();
                object = action(cursor);
                retryCount = 0;
            } catch (com.mongodb.MongoException e) {
                log.info("Exception talking to Mongo: \n" + ExceptionToString.format(e));
                log.info("Remaining tries: " + retryCount);
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return object;
    }
}
