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
package rapture.lock.memory;

import java.util.HashMap;
import java.util.Map;

import rapture.common.LockHandle;
import rapture.lock.ILockingHandler;

/*
 * The memory locking handler is basically a local synchronization point
 * 
 */
public class MemoryLockingHandler implements ILockingHandler {
    private Map<String, MemoryLock> lockMap;
    private Object syncLock;
    @SuppressWarnings("unused")
    private String instanceName;

    public MemoryLockingHandler() {
        lockMap = new HashMap<String, MemoryLock>();
        syncLock = new Object();
    }

    @Override
    public LockHandle acquireLock(String lockContext, String lockName, long secondsToWait, long secondsToHold) {
        if (getLockForMe(lockContext, lockName, secondsToHold)) {
            return new LockHandle();
        }

        if (secondsToWait != 0) {
            long secondsCountdown = secondsToWait;
            while (secondsCountdown > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                if (getLockForMe(lockContext, lockName, secondsToHold)) {
                    return new LockHandle();
                }
                secondsCountdown--;
            }
        }
        return null;
    }

    private boolean checkLock(String lockContext, String lockName, long secondsToHold) {
        if (!lockExists(lockName)) {
            createLock(lockContext, lockName, secondsToHold);
            return true;
        } else {
            MemoryLock lock = lockMap.get(lockName);
            if (lock.readyToRelease()) {
                // Replace lock as current lock has expired
                createLock(lockContext, lockName, secondsToHold);
                return true;
            }
        }
        return false;
    }

    private void createLock(String lockContext, String lockName, long secondsToHold) {
        MemoryLock lock = generateLock(lockContext, lockName, secondsToHold);
        lockMap.put(lockName, lock);
    }

    private MemoryLock generateLock(String lockHolder, String lockName, long secondsToHold) {
        MemoryLock lock = new MemoryLock();
        lock.setLockHolder(lockHolder);
        lock.setLockKey(lockName);
        lock.setTimeToRelease(getReleaseTime(secondsToHold));
        return lock;
    }

    private boolean getLockForMe(String lockContext, String lockName, long secondsToHold) {
        synchronized (syncLock) {
            if (checkLock(lockContext, lockName, secondsToHold)) {
                return true;
            }
        }
        return false;
    }

    private long getReleaseTime(long secondsToHold) {
        if (secondsToHold != 0) {
            return System.currentTimeMillis() + 1000 * secondsToHold;
        } else {
            return 0;
        }
    }

    private boolean lockExists(String lockName) {
        return lockMap.containsKey(lockName);
    }

    @Override
    public Boolean releaseLock(String lockContext, String lockName, LockHandle lockHandle) {
        synchronized (syncLock) {
            if (lockMap.containsKey(lockName) && lockContext.equals(lockMap.get(lockName).getLockHolder())) {
                lockMap.remove(lockName);
                return true;
            }
        }
        return false;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        lockMap = new HashMap<String, MemoryLock>();
        syncLock = new Object();
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public Boolean forceReleaseLock(String lockName) {
        synchronized (syncLock) {
            if (lockMap.containsKey(lockName)) {
                lockMap.remove(lockName);
                return true;
            }
        }
        return false;
    }
}
