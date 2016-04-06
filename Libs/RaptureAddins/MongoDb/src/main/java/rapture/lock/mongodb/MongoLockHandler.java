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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.bson.BsonInt32;
import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

import rapture.common.LockHandle;
import rapture.lock.ILockingHandler;
import rapture.mongodb.MongoDBFactory;

/**
 * An implementation of a lock strategy on Mongo. The idea is that we need to
 * acquire a named lock, and then we release it later. If we fail to release it,
 * it will automatically become available after secondsToHold.
 * 
 * The lock in mongo is implemented by attempting to create a document that has
 * the following characteristics
 * 
 * { name = lockName, locked=true, freeTime=(some long time) }
 * 
 * @author amkimian
 * 
 */
public class MongoLockHandler implements ILockingHandler {
    private static final String RELEASETIME = "rt";
    private static final String DOLLARPUSH = "$push";
    private static final String DOLLARPULL = "$pull";
    private static final String NAME = "name";
    private static final String CTX = "ctx";
    private static final String LOCKS = "locks";
    private static final String RANDOM = "rnd";
    private static Logger log = Logger.getLogger(MongoLockHandler.class);
    private static final String tableName = "raplock";
    private String instanceName = "default";

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    private boolean waitAndShouldBail(long bailTime) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            //
        }
        if (System.currentTimeMillis() > bailTime) {
            return true;
        }
        return false;
    }

    @Override
    public LockHandle acquireLock(String lockHolder, String lockName, long secondsToWait, long secondsToHold) {
        log.trace("Mongo acquire lock");
        // create a random marker for each acquisition -- that way double-grabs
        // from the same session are rejected
        String random = makeRandom();
        Document query = getLockQuery(lockName);
        Document val = createLockVal(lockHolder, secondsToHold, random);
        Document update = createAddValObject(val);

        // First see if this exists

        MongoCollection<Document> coll = getLockCollection();

        long bailTime = System.currentTimeMillis() + secondsToWait * 1000;

        boolean gotLock = false;
        coll.updateOne(query, update, new UpdateOptions().upsert(true));

        while (!gotLock) {
            Document obj = coll.find(query).first();
            // Look for the locks field, and see if we are top
            if (obj != null) {
                log.trace("Locks are present");
                List<Object> locks = (List<Object>) obj.get(LOCKS);
                if (locks.size() > 0) {
                    Document first = (Document) locks.get(0);
                    log.trace("First lock is " + first.get(CTX));
                    if (first.get(CTX).toString().equals(lockHolder) && first.get(RANDOM).toString().equals(random)) {
                        log.trace(String.format("We have the lock  with name '%s'", lockName));
                        gotLock = true;
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace(String.format("name: [%s]\n" + "ctx: [%s], rnd: [%s]\n" + "ctx: [%s], rnd: [%s]", lockName, first.get(CTX).toString(),
                                    first.get(RANDOM).toString(), lockHolder, random));
                        }
                        if (expired(first)) {
                            releaseLockWithRandom(first.get(CTX).toString(), lockName, first.get(RANDOM).toString());
                        }
                        if (waitAndShouldBail(bailTime)) {
                            break;
                        }
                    }
                } else {
                    log.trace("Locks return list was zero size");
                    if (waitAndShouldBail(bailTime)) {
                        break;
                    }
                }
            } else {
                log.trace("No update, bailing with no lock");
                break;
            }
        }
        if (gotLock) {
            LockHandle lockHandle = new LockHandle();
            lockHandle.setLockName(lockName);
            lockHandle.setHandle(random);
            lockHandle.setLockHolder(lockHolder);
            return lockHandle;
        } else {
            log.info(String.format("Could not get lock for %s, bailing", lockName));
            // could not get the lock, so we should remove the lock with the random we saved earlier in this method, since the random is never accessible again
            releaseLockWithRandom(lockHolder, lockName, random);
            return null;
        }
    }

    // Add our lockHolder and timeToRelease to the master lock document
    // Then, if we are the top of the list, we're ready to go
    private String makeRandom() {
        return UUID.randomUUID().toString(); // this is overkill, but will do
    }

    private Document createAddValObject(Document val) {
        Document update = new Document();
        Document upVal = new Document();
        upVal.put(LOCKS, val);
        update.put(DOLLARPUSH, upVal);
        return update;
    }

    private Document createLockVal(String lockHolder, long secondsToHold, String random) {
        Document val = new Document();
        val.put(CTX, lockHolder);
        val.put(RANDOM, random);
        val.put(RELEASETIME, System.currentTimeMillis() + secondsToHold * 1000);
        return val;
    }

    private boolean expired(Document obj) {
        Object val = obj.get(RELEASETIME);
        if (val != null) {
            if (val instanceof Long) {
                if ((Long) val < System.currentTimeMillis()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected MongoCollection<Document> getLockCollection() {
        return MongoDBFactory.getCollection(instanceName, tableName);
    }

    Document getLockQuery(String lockName) {
        Document query = new Document();
        query.put(NAME, lockName);
        return query;
    }

    String getCtxKey() {
        return CTX;
    }

    String getLocksKey() {
        return LOCKS;
    }

    @Override
    public Boolean releaseLock(String lockHolder, String lockName, LockHandle lockHandle) {
        log.debug("Mongo release lock");

        if (lockHandle != null) {
            String random = lockHandle.getHandle();
            return releaseLockWithRandom(lockHolder, lockName, random);
        } else {
            log.error(String.format("Unable to release lock %s, because null lockHandle passed in.", lockName));
            return false;
        }
    }

    private Boolean releaseLockWithRandom(String lockHolder, String lockName, String random) {
        Document query = getLockQuery(lockName);
        Document toRemove = new Document();
        toRemove.put(CTX, lockHolder);
        toRemove.put(RANDOM, random);
        Document field = new Document();
        field.put(LOCKS, toRemove);
        Document oper = new Document();
        oper.put(DOLLARPULL, field);
        getLockCollection().updateOne(query, oper, new UpdateOptions().upsert(false));
        return true;
    }

    private Boolean breakLock(String lockHolder, String lockName) {
        Document query = getLockQuery(lockName);
        Document test = new Document();
        test.put(CTX, lockHolder);
        Document field = new Document();
        field.put(LOCKS, test);
        Document oper = new Document();
        oper.put(DOLLARPULL, field);
        getLockCollection().updateOne(query, oper, new UpdateOptions().upsert(false));
        return true;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        getLockCollection().createIndex(new Document(NAME, new BsonInt32(1)));
    }

    @Override
    public Boolean forceReleaseLock(String lockName) {
        log.debug("Mongo break lock");
        Document query = getLockQuery(lockName);
        Document current = getLockCollection().find(query).first();
        if (current != null) {
            BasicDBList locks = (BasicDBList) current.get(LOCKS);
            Document first = (Document) locks.get(0);
            String holder = first.get(CTX).toString();
            breakLock(holder, lockName);
            return true;
        }
        return false;
    }
}
