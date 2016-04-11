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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bson.BsonInt32;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import com.github.fakemongo.Fongo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class EpochManagerTest {

    private MongoCollection<Document> collection;

    @Before
    public void setup() {
        Fongo fongo = new Fongo("mongoUnitTest");
        MongoDatabase db = fongo.getDatabase("mongoUnitTestDB");
        collection = db.getCollection("mongoUnitTestCollection");
    }

    @Test
    public void testNextEpoch() throws InterruptedException {
        Long ret = EpochManager.nextEpoch(collection);
        assertEquals(new Long(1L), ret);
        ret = EpochManager.nextEpoch(collection);
        assertEquals(new Long(2L), ret);

        final List<Long> longs = new ArrayList<Long>();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(78);
        for (int i = 0; i < 78; i++) {
            taskExecutor.execute(new Runnable() {
                public void run() {
                    longs.add(EpochManager.nextEpoch(collection));
                }
            });
        }
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(78, longs.size());
        assertFalse(hasDuplicate(longs));
        ret = EpochManager.nextEpoch(collection);
        assertEquals(new Long(81L), ret);
    }

    @Test
    public void testGetLatestEpoch() {
        Long ret = EpochManager.getLatestEpoch(collection);
        assertEquals(new Long(0L), ret);
        for (int i = 0; i < 234; i++) {
            EpochManager.nextEpoch(collection);
        }
        ret = EpochManager.getLatestEpoch(collection);
        assertEquals(new Long(234L), ret);
    }

    @Test
    public void testGetNotEqualEpochQueryObject() {
        int num = 100;
        String key = "key";
        for (int i = 0; i < num; i++) {
            collection.insertOne(new Document(key, EpochManager.nextEpoch(collection)));
        }
        assertEquals(num + 1, collection.count());
//        assertEquals(num + 1, collection.find().count());
        assertEquals(new Long(num), EpochManager.getLatestEpoch(collection));
        com.mongodb.client.FindIterable<Document> cursor = collection.find(EpochManager.getNotEqualEpochQueryObject()).sort(new Document(key, new BsonInt32(1)));
//        assertEquals(num, cursor.count());
        long count = 1L;
        Iterator<Document> iterator = cursor.iterator();
        while (iterator.hasNext()) {
            assertEquals(count++, ((Document) iterator.next()).get(key));
        }
    }

    private boolean hasDuplicate(List<Long> listContainingDuplicates) {
        final Set<Long> set = new HashSet<Long>();
        for (Long l : listContainingDuplicates) {
            if (!set.add(l)) {
                return true;
            }
        }
        return false;
    }
}
