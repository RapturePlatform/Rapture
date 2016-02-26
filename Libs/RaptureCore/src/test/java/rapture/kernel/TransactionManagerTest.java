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
package rapture.kernel;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import rapture.util.IDGenerator;

public class TransactionManagerTest {
    private String txId;

    @Before
    public void setup() {
        txId = IDGenerator.getUUID();
    }

    private class StartThread extends Thread {
        @Override
        public void run() {
            TransactionManager.begin(txId);
            assertTrue(TransactionManager.isTransactionActive(txId));
        }
    }

    private class CommitThread extends Thread {
        @Override
        public void run() {
            TransactionManager.commit(txId);
            assertFalse(TransactionManager.isTransactionActive(txId));
        }
    }

    private class RollbackThread extends Thread {
        @Override
        public void run() {
            TransactionManager.rollback(txId);
            assertFalse(TransactionManager.isTransactionActive(txId));
        }
    }

    @Test
    public void testInvalidTransaction() {
        assertFalse(TransactionManager.commit(txId));
        assertFalse(TransactionManager.rollback(txId));

        assertTrue(TransactionManager.begin(txId));
        assertFalse(TransactionManager.begin(txId));

        assertTrue(TransactionManager.commit(txId));
        assertFalse(TransactionManager.commit(txId));
    }

    @Test
    public void testGetActiveTransaction() throws InterruptedException {
        TransactionManager.registerThread(txId);
        assertEquals(txId, TransactionManager.getActiveTransaction());

        // register thread to a new transaction id
        String newTxId = "some other transaction id";
        TransactionManager.registerThread(newTxId);
        assertEquals(newTxId, TransactionManager.getActiveTransaction());

        // try a thread with no active transaction
        final String[] ids = new String[1];
        Thread thread = new Thread() {
            @Override
            public void run() {
                ids[0] = TransactionManager.getActiveTransaction();
            }
        };
        thread.start();
        thread.join();
        assertNull(ids[0]);
    }

    @Test
    public void testCommit() throws InterruptedException {
        assertFalse(TransactionManager.isTransactionActive(txId));

        Thread startThread = new StartThread();
        startThread.start();
        startThread.join();
        assertTrue(TransactionManager.isTransactionActive(txId));

        Thread commitThread = new CommitThread();
        commitThread.start();
        commitThread.join();
        assertFalse(TransactionManager.isTransactionActive(txId));
    }

    @Test
    public void testRollback() throws InterruptedException {
        assertFalse(TransactionManager.isTransactionActive(txId));

        Thread startThread = new StartThread();
        startThread.start();
        startThread.join();
        assertTrue(TransactionManager.isTransactionActive(txId));

        Thread rollbackThread = new RollbackThread();
        rollbackThread.start();
        rollbackThread.join();
        assertFalse(TransactionManager.isTransactionActive(txId));
    }

    @Test
    public void testCacheExpire() throws InterruptedException {
        int total = 10;
        final Map<String, String> expired = new HashMap<>();
        RemovalListener removalListener = new RemovalListener<String, String>() {
            @Override
            public void onRemoval(RemovalNotification<String, String> notification) {
                if(RemovalCause.EXPIRED == notification.getCause()) {
                    expired.put(notification.getKey(), notification.getValue());
                }
            }
        };
        Cache<String, String> myCache = CacheBuilder.newBuilder()
                .expireAfterWrite(2, TimeUnit.MILLISECONDS)
                .removalListener(removalListener)
                .build();
        for(int i = 0; i < total; i++) {
            myCache.put("key_" + i, "val_" + i);
        }
        Thread.sleep(10);
        myCache.cleanUp();
        assertEquals(total, expired.size());
    }
}