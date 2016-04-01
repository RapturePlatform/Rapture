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

import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.LockHandle;
import rapture.lock.ILockingHandler;
import rapture.mongodb.MongoDBFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 *
 */
public class MongoLockHandler2 implements ILockingHandler {

    private static Logger log = Logger.getLogger(MongoLockHandler2.class);
    private static final String tableName = "raplock2_";
    private String instanceName = "default";

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public LockHandle acquireLock(String lockHolder, String lockName, long secondsToWait, long secondsToHold) {
        log.debug("Mongo acquire lock2  " + lockName + ":" + secondsToHold + ":" + secondsToWait);
        long start = System.currentTimeMillis();

        DBCollection coll = getLockCollection(lockName);
        log.debug("lock COLLECTION for " + lockName + "IS " + coll.getFullName());

        long bailTime = System.currentTimeMillis() + secondsToWait * 1000;
        long leaseTime = System.currentTimeMillis() + secondsToHold * 1000;

        DBObject lockFile = new BasicDBObject();
        lockFile.put("lockName", lockName);
        lockFile.put("lockHolder", lockHolder);
        lockFile.put("lease", leaseTime);

        long myLockID = System.currentTimeMillis();
        lockFile.put("_id", "" + myLockID);
        log.debug("id is " + myLockID);
        log.debug("bailtime " + bailTime);

        while (bailTime > System.currentTimeMillis()) {
            try {
                myLockID = System.currentTimeMillis();
                lockFile.put("_id", "" + myLockID);
                lockFile.put("lease", myLockID + secondsToHold * 1000);

                @SuppressWarnings("unused")
				WriteResult result = coll.insert(WriteConcern.ACKNOWLEDGED, lockFile);
                log.debug("inserted file" + lockFile);

                break;
            } catch (Exception e) {
                //              log.error(e.getCode());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        DBObject lock = null;

        // loop until we acquire lock or timeout
        while (bailTime > System.currentTimeMillis()) {
            // we have the lock if no lock file exists with a smaller number
            DBCursor results = coll.find(new BasicDBObject("lease", new BasicDBObject("$gt", System.currentTimeMillis()))).sort(new BasicDBObject("_id", 1))
                    .limit(1);

            try {
                if (results.hasNext()) {
                    lock = results.next();
                }

                if (lock != null && ((String) lock.get("_id")).equals("" + myLockID)) {
                    log.debug("* i have the lock" + lock.get("_id") + ":" + myLockID);
                    LockHandle lockHandle = new LockHandle();
                    lockHandle.setLockName(lockName);
                    lockHandle.setHandle("" + myLockID);
                    lockHandle.setLockHolder(lockHolder);
                    long end = System.currentTimeMillis();
                    log.debug("* NG acquired lock in " + (end - start));
                    return lockHandle;
                } else {
                    //log.info("* waiting for lock held by "+ lock.get("_id"));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                results.close();
            }

        }
        // ran out of time trying to get lock.
        log.debug("giving up " + myLockID);
        long end2 = System.currentTimeMillis();
        log.debug("denied lock in " + (end2 - start));
        return null;
    }

    @Override
    public Boolean releaseLock(String lockHolder, String lockName, LockHandle lockHandle) {

        return releaseLock(lockHandle);
    }

    protected DBCollection getLockCollection(String lockName) {
        // log.info("COLLECTION is " + tableName + lockName);
        return MongoDBFactory.getDB(instanceName).getCollection(tableName + lockName);

    }

    public Boolean releaseLock(LockHandle lockHandle) {
        log.debug("Mongo release lock");

        if (lockHandle != null) {
            String id = lockHandle.getHandle();
            String lockName = lockHandle.getLockName();
            return releaseLockWithID(lockName, id);
        } else {
            log.error("Unable to release lock %s, because null lockHandle passed in.");
            return false;
        }
    }

    private Boolean releaseLockWithID(String lockName, String id) {
        BasicDBObject lockFileQuery = new BasicDBObject();
        lockFileQuery.put("_id", id);
        DBCollection coll = getLockCollection(lockName);
        WriteResult res = coll.remove(lockFileQuery);

        return (res.getN() == 1);
    }

    @Override
    public Boolean forceReleaseLock(String lockName) {
        DBCollection coll = getLockCollection(lockName);
        if (coll != null) {
            coll.drop();
        }
        return true;
    }

    @Override
    public void setConfig(Map<String, String> config) {
    }

    public LockHandle getLockForName(String lockName) {
        //  a lock is the document with the smallest id number whose lease hasn't expired.
        DBCursor results = getLockCollection(lockName).find(new BasicDBObject("lease", new BasicDBObject("$gt", System.currentTimeMillis())))
                .sort(new BasicDBObject("_id", 1)).limit(1);

        DBObject lock = null;
        try {
            if (results.hasNext()) {
                lock = results.next();
            }
            if (lock != null) {
                LockHandle lockHandle = new LockHandle();
                lockHandle.setLockName((String) lock.get("lockName"));
                lockHandle.setHandle((String) lock.get("_id"));
                lockHandle.setLockHolder((String) lock.get("lockHolder"));
                return lockHandle;
            } else {
                return null;
            }
        } finally {
            results.close();
        }
    }
}
