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
package rapture.lock.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import rapture.common.LockHandle;

import com.github.fakemongo.Fongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

public class MongoLockHandlerTest {

    private DBCollection collection;
    private MongoLockHandler m;

    @Before
    public void setup() {
        Fongo fongo = new Fongo("mongoUnitTest");
        DB db = fongo.getDB("mongoUnitTestDB");
        collection = db.getCollection("mongoUnitTestCollection");
        m = new MongoLockHandler() {
            @Override
            protected DBCollection getLockCollection() {
                return collection;
            }
        };
    }

    @Test
    public void testAcquireLock() {
        String lockHolder = "me";
        String lockName = "//repoIwantto/lock";
        int secondsToWait = 10;
        int secondsToHold = 30;
        LockHandle lockHandle = m.acquireLock(lockHolder, lockName, secondsToWait, secondsToHold);
        assertNotNull(lockHandle);
        assertEquals(lockHolder, lockHandle.getLockHolder());
        assertEquals(lockName, lockHandle.getLockName());
        assertNotNull(lockHandle.getHandle());
        assertTrue(lockExists(lockName, lockHolder));
        assertTrue(m.releaseLock(lockHolder, lockName, lockHandle));
        assertFalse(lockExists(lockName, lockHolder));
    }

    @Test
    public void testAcquireLockMultipleThreads() throws InterruptedException {
        final String lockHolder = "me";
        final String lockName = "//repoThatWillBe/Locked";
        final int secondsToWait = 10;
        final int secondsToHold = 30;

        final Set<LockHandle> handlesAcquired = new HashSet<LockHandle>();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            final int counter = i;
            taskExecutor.execute(new Runnable() {
                public void run() {
                    LockHandle lockHandle = m.acquireLock(lockHolder + String.valueOf(counter), lockName, secondsToWait, secondsToHold);
                    assertNotNull(lockHandle);
                    assertTrue(lockExists(lockName, lockHolder + String.valueOf(counter)));
                    handlesAcquired.add(lockHandle);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                    assertTrue(m.releaseLock(lockHolder + String.valueOf(counter), lockName, lockHandle));
                    assertFalse(lockExists(lockName, lockHolder + String.valueOf(counter)));
                }
            });
        }
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(10, handlesAcquired.size());
    }

    @Test
    public void testAcquireLockShutout() throws InterruptedException {
        final String lockHolder = "me";
        final String lockName = "//anotherRepo/Locked";
        final int secondsToWait = 1;
        final int secondsToHold = 30;

        final Set<LockHandle> handlesAcquired = new HashSet<LockHandle>();
        final Set<String> handlesDenied = new HashSet<String>();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            final int counter = i;
            taskExecutor.execute(new Runnable() {
                public void run() {
                    LockHandle lockHandle = m.acquireLock(lockHolder + String.valueOf(counter), lockName, secondsToWait, secondsToHold);
                    if (lockHandle != null) {
                        handlesAcquired.add(lockHandle);
                        assertTrue(lockExists(lockName, lockHolder + String.valueOf(counter)));
                    } else {
                        handlesDenied.add(lockHolder + String.valueOf(counter));
                        // could not get lock, make sure our entry is still not in there
                        assertFalse(lockExists(lockName, lockHolder + String.valueOf(counter)));
                        return;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                    assertTrue(m.releaseLock(lockHolder + String.valueOf(counter), lockName, lockHandle));
                    assertFalse(lockExists(lockName, lockHolder + String.valueOf(counter)));
                }
            });
        }
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        assertEquals(1, handlesAcquired.size());
        assertEquals(9, handlesDenied.size());
    }

    private boolean lockExists(String lockName, String lockHolder) {
        BasicDBObject query = m.getLockQuery(lockName);
        BasicDBObject lock = new BasicDBObject(m.getCtxKey(), lockHolder);
        BasicDBObject match = new BasicDBObject("$elemMatch", lock);
        query.put(m.getLocksKey(), match);
        return collection.find(query).count() > 0;
    }

}
