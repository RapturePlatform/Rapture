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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.LockHandle;

import com.github.fakemongo.Fongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;

public class MongoLockHandler2Test {

    private MongoLockHandler2 m;

    @Before
    public void setup() {
        Fongo fongo = new Fongo("mongoUnitTest");
        final DB db = fongo.getDB("mongoUnitTestDB");
        m = new MongoLockHandler2() {
            @Override protected DBCollection getLockCollection(String lockName) {
                return db.getCollection("ffoonnggoo" + lockName);
            }
        };
    }

    @After
    public void tearDown() {
        //getLockCollection.drop();
    }

    @Test
    public void testAcquireLock() {
        String lockHolder = "me";
        String lockName = "//repoIwantto/lock";
        int secondsToWait = 3;
        int secondsToHold = 30;
        LockHandle lockHandle = m.acquireLock(lockHolder, lockName, secondsToWait, secondsToHold);
        assertNotNull(lockHandle);
        assertEquals(lockHolder, lockHandle.getLockHolder());
        assertEquals(lockName, lockHandle.getLockName());
        assertNotNull(lockHandle.getHandle());
        assertTrue(lockExists(lockHandle));
        assertTrue(m.releaseLock(lockHandle));
        assertFalse(lockExists(lockHandle));
    }

    @Ignore
    @Test
    public void testAcquireLockMultipleThreads() throws InterruptedException {
        final String lockHolder = "me";
        final String lockName = "//repoThatWillBe/Locked";
        final int secondsToWait = 10;
        final int secondsToHold = 30;
        final int size = 10;

        final Set<LockHandle> handlesAcquired = new HashSet<LockHandle>();
        ExecutorService taskExecutor = Executors.newFixedThreadPool(size);
        for (int i = 0; i < size; i++) {
            final int counter = i;
            taskExecutor.execute(new Runnable() {
                public void run() {
                    //m = new MongoLockHandler2();
                    LockHandle lockHandle = m
                            .acquireLock(lockHolder + String.valueOf(counter), lockName + String.valueOf(counter), secondsToWait, secondsToHold);
                    assertNotNull(lockHandle);
                    assertTrue(lockExists(lockHandle));
                    handlesAcquired.add(lockHandle);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }

                    assertTrue(m.releaseLock(lockHandle));
                    assertFalse(lockExists(lockHandle));

                }
            });
        }
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(15, TimeUnit.SECONDS);
        assertEquals(size, handlesAcquired.size());
    }


    @Ignore
    @Test
    public void testAcquireLockShutout() throws InterruptedException {
        final String lockHolder = "me";
        final String lockName = "//anotherRepo/Locked";
        final int secondsToWait = 1;
        final int secondsToHold = 6;
        final int size = 5;
        final Set<LockHandle> handlesAcquired = new HashSet<LockHandle>();
        final Set<String> handlesDenied = new HashSet<String>();

        ExecutorService taskExecutor = Executors.newFixedThreadPool(size);
        for (int i = 0; i < size; i++) {
            final int counter = i;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            taskExecutor.execute(new Runnable() {
                public void run() {
                    LockHandle lockHandle = m.acquireLock(lockHolder + String.valueOf(counter), lockName, secondsToWait, secondsToHold);
                    if (lockHandle != null) {
                        handlesAcquired.add(lockHandle);
                        assertTrue(lockExists(lockHandle));
                    } else {
                        handlesDenied.add(lockHolder + String.valueOf(counter));
                        // could not get lock, make sure our entry is still not in there
                        assertFalse(lockExists(lockHandle));
                        return;
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }

                    assertTrue(m.releaseLock(lockHandle));
                    assertFalse(lockExists(lockHandle));
                }
            });
        }
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(15, TimeUnit.SECONDS);
        assertEquals(1, handlesAcquired.size());
        assertEquals(size - 1, handlesDenied.size());
    }

    private boolean lockExists(LockHandle lockHandle) {
        if (lockHandle == null) {
            return false;
        }
        LockHandle lock = m.getLockForName(lockHandle.getLockName());

        if (lock != null && lock.getHandle().equals(lockHandle.getHandle())) {
            return true;
        }

        return false;
    }

}
