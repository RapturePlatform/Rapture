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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import rapture.repo.StructuredRepo;

/**
 * Created by yanwang on 4/20/15.
 */
public abstract class TransactionManager {
    private static Logger log = Logger.getLogger(TransactionManager.class);

    private static Map<Long, String> threads;                  // thread id -> txId
    private static SetMultimap<String, StructuredRepo> repos; // txId -> Repo
    private static Cache<String, String> activeTransactions;   // txId -> txId
    private static Cache<String, String> failedTransactions;   // txId -> txId
    private static RemovalListener<String, String> transactionExpiryListener;

    static {
        threads = new ConcurrentHashMap<>();
        repos = Multimaps.synchronizedSetMultimap(HashMultimap.<String, StructuredRepo>create());
        transactionExpiryListener = new RemovalListener<String, String>() {
            @Override
            public void onRemoval(RemovalNotification<String, String> notification) {
                // rollback transaction on expire
                if(RemovalCause.EXPIRED == notification.getCause()) {
                    rollback(notification.getKey());
                }
            }
        };
        activeTransactions = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .removalListener(transactionExpiryListener)
                .build();
        failedTransactions = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build();
    }

    public static boolean begin(String txId) {
        if(isTransactionActive(txId)) {
            log.error("Transaction " + txId + " already started");
            return false;
        }
        log.info("start transaction " + txId);
        activeTransactions.put(txId, txId);
        return true;
    }

    //TODO use two phase commit
    public static boolean commit(String txId) {
        if(!isTransactionActive(txId)) {
            log.error("No active transaction " + txId);
            return false;
        }
        log.info("commit transaction " + txId);
        for(StructuredRepo repo : repos.get(txId)) {
            repo.commit(txId);
        }
        return transactionFinished(txId);
    }

    public static boolean rollback(String txId) {
        if(!isTransactionActive(txId)) {
            log.error("No active transaction " + txId);
            return false;
        }
        log.info("rollback transaction " + txId);
        for(StructuredRepo repo : repos.get(txId)) {
            repo.rollback(txId);
        }
        return transactionFinished(txId);
    }

    public static String getActiveTransaction() {
        return threads.get(Thread.currentThread().getId());
    }

    public static boolean isTransactionActive(String txId) {
        return activeTransactions.getIfPresent(txId) != null;
    }

    public static boolean isTransactionFailed(String txId) {
        return failedTransactions.getIfPresent(txId) != null;
    }

    public static void registerThread(String txId) {
        threads.put(Thread.currentThread().getId(), txId);
    }

    public static void registerRepo(String txId, StructuredRepo repo) {
        repos.put(txId, repo);
    }

    public static void transactionFailed(String txId) {
        if(isTransactionActive(txId)) {
            log.info("Transaction " + txId + " failed, rollback now");
            rollback(txId);
            failedTransactions.put(txId, txId);
        }
    }

    private static boolean transactionFinished(String txId) {
        activeTransactions.invalidate(txId);
        repos.removeAll(txId);
        threads.values().removeAll(Arrays.asList(txId));
        return true;
    }

    public static Set<String> getTransactions() {
        return activeTransactions.asMap().keySet();
    }
}
