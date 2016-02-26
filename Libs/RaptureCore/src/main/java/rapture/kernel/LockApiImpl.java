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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.LockHandle;
import rapture.common.RaptureLockConfig;
import rapture.common.RaptureLockConfigStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SemaphoreAcquireResponse;
import rapture.common.SemaphoreLock;
import rapture.common.SemaphoreLockStorage;
import rapture.common.api.LockApi;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JsonContent;
import rapture.config.ConfigLoader;
import rapture.dp.semaphore.URIGenerator;
import rapture.lock.ILockingHandler;
import rapture.lock.LockFactory;
import rapture.repo.RepoVisitor;

public class LockApiImpl extends KernelBase implements LockApi {
    private static Logger log = Logger.getLogger(LockApiImpl.class);

    public static final RaptureURI KERNEL_MANAGER_URI = new RaptureURI("//kernel", Scheme.LOCK);
    public static final RaptureURI SEMAPHORE_MANAGER_URI = new RaptureURI("//semaphore", Scheme.LOCK);
    public static final RaptureURI WORKFLOW_MANAGER_URI = new RaptureURI("//workflow", Scheme.LOCK);

    /**
     * Returns the Uri of the lock provider used by the Kernel functions. This should not be exposed as part of the API but instead it should be accessed using
     * the getTrusted() methods. This is because this should only ever be called from the Kernel code, not the API. This lock provider should never be used from
     * external APIs
     *
     * @return
     */
    public RaptureURI getKernelManagerUri() {
        return KERNEL_MANAGER_URI;
    }

    public LockApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override

     public LockHandle acquireLock(CallingContext context, String managerUri, String lockName, long secondsToWait, long secondsToKeep) {
        // TODO: Ben - lockname could perhaps become part of the Uri with a
        // 'bookmark' ie, # ?
        ILockingHandler handler = LockFactory.getLockHandler(managerUri);
        return handler.acquireLock(extractContext(context), lockName, secondsToWait, secondsToKeep);
    }

    private String extractContext(CallingContext context) {
        return nn(context.getUser()) + "__RaptureReserved__" + nn(context.getContext());
    }

    private String nn(String in) {
        return (in == null) ? "" : in;
    }

    @Override
    public RaptureLockConfig createLockManager(CallingContext context, String managerUri, String config, String pathPosition) {
        RaptureURI internalUri = new RaptureURI(managerUri, Scheme.LOCK);
        RaptureLockConfig newL = new RaptureLockConfig();
        newL.setName(internalUri.getDocPath());
        newL.setAuthority(internalUri.getAuthority());
        newL.setConfig(config);
        newL.setPathPosition(pathPosition);
        RaptureLockConfigStorage.add(newL, context.getUser(), "Created lock config");
        return newL;
    }

    @Override
    public void deleteLockManager(CallingContext context, String managerUri) {
        RaptureURI internalUri = new RaptureURI(managerUri, Scheme.LOCK);
        RaptureLockConfigStorage.deleteByAddress(internalUri, context.getUser(), "Remove lock provider");
    }

    @Override
    public Boolean lockManagerExists(CallingContext context, String managerUri) {
        return getLockManagerConfig(context, managerUri) != null;
    }

    @Override
    public RaptureLockConfig getLockManagerConfig(CallingContext context, String managerUri) {
        RaptureURI internalUri = new RaptureURI(managerUri, Scheme.LOCK);
        return RaptureLockConfigStorage.readByAddress(internalUri);
    }

    @Override
    public List<RaptureLockConfig> getLockManagerConfigs(CallingContext context, final String managerUri) {
        final RaptureURI internalUri = new RaptureURI(managerUri, Scheme.LOCK);
        String prefix = RaptureLockConfigStorage.addressToStorageLocation(internalUri).getDocPath();
        final List<RaptureLockConfig> ret = new ArrayList<RaptureLockConfig>();
        getConfigRepo().visitAll(prefix, null, new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    log.info("Visiting " + name);
                    RaptureLockConfig lock;
                    try {
                        lock = RaptureLockConfigStorage.readFromJson(content);
                        if (lock.getAuthority().equals(internalUri.getAuthority())) {
                            ret.add(lock);
                        }
                    } catch (RaptureException e) {
                        log.error("Could not load document " + name + ", continuing anyway");
                    }
                }
                return true;
            }

        });
        return ret;
    }

    @Override
    public Boolean releaseLock(CallingContext context, String managerUri, String lockName, LockHandle lockHandle) {
        ILockingHandler handler = LockFactory.getLockHandler(managerUri);
        return handler.releaseLock(extractContext(context), lockName, lockHandle);
    }

    /**
     * Acquires a permit for given lockKey, blocking until one is available, or the waiting time elapsed.
     */
    public SemaphoreAcquireResponse acquirePermit(CallingContext callingContext, Integer maxAllowed, String lockKey, URIGenerator uriGenerator,
            long timeoutSeconds) {
        SemaphoreLock semaphoreLock = null;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() <= startTime + timeoutSeconds * 1000) {
            semaphoreLock = SemaphoreLockStorage.readByFields(lockKey);
            // semaphore unavailable, wait and retry
            if (semaphoreLock != null && semaphoreLock.getStakeholderURIs().size() >= maxAllowed) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Thread interrupted while waiting to acquire semaphore permit");
                    break;
                }
                continue;
            }
            // acquire lock
            LockHandle lockHandle = grabLock(callingContext, lockKey);
            if (lockHandle != null) {
                try {
                    // double check stakeholders
                    semaphoreLock = SemaphoreLockStorage.readByFields(lockKey);
                    if (semaphoreLock == null) {
                        semaphoreLock = new SemaphoreLock();
                        semaphoreLock.setLockKey(lockKey);
                    }
                    if (semaphoreLock.getStakeholderURIs().size() < maxAllowed) {
                        // add stakeholder
                        RaptureURI stakeholderUri = uriGenerator.generateStakeholderURI();
                        semaphoreLock.getStakeholderURIs().add(stakeholderUri.toString());
                        SemaphoreLockStorage.add(semaphoreLock, callingContext.getUser(), "Incrementing lock");
                        // return response
                        SemaphoreAcquireResponse response = new SemaphoreAcquireResponse();
                        response.setIsAcquired(true);
                        response.setAcquiredURI(stakeholderUri.toString());
                        return response;
                    }
                } finally {
                    releaseLock(callingContext, lockKey, lockHandle);
                }
            }
        }
        // unable to acquire semaphore
        SemaphoreAcquireResponse response = new SemaphoreAcquireResponse();
        if (semaphoreLock != null) {
            response.setExistingStakeholderURIs(semaphoreLock.getStakeholderURIs());
        }
        response.setIsAcquired(false);
        return response;
    }

    /**
     * Try to acquire a permit. returns true if successful, false if acquiring a permit was not possible (everything was locked)
     *
     * @return A unique Stakeholder URI you can use, or null if unable to acquire
     */
    public SemaphoreAcquireResponse tryAcquirePermit(CallingContext callingContext, Integer maxAllowed, String lockKey, URIGenerator uriGenerator) {
        LockHandle lockHandle = grabLock(callingContext, lockKey);
        if (lockHandle != null) {
            try {
                /*
                 * get count of existing locks with "lockKey"
                 */

                SemaphoreLock semaphoreLock = SemaphoreLockStorage.readByFields(lockKey);
                if (semaphoreLock == null) {
                    semaphoreLock = new SemaphoreLock();
                    semaphoreLock.setLockKey(lockKey);
                }

                Set<String> existingStakeholderUris = semaphoreLock.getStakeholderURIs();
                int count = existingStakeholderUris.size();

                SemaphoreAcquireResponse response = new SemaphoreAcquireResponse();
                response.setExistingStakeholderURIs(existingStakeholderUris);

                if (count < maxAllowed) {
                    RaptureURI stakeholderUri = uriGenerator.generateStakeholderURI();
                    existingStakeholderUris.add(stakeholderUri.toString());
                    SemaphoreLockStorage.add(semaphoreLock, callingContext.getUser(), "Incrementing lock");
                    response.setIsAcquired(true);
                    response.setAcquiredURI(stakeholderUri.toString());
                } else {
                    response.setIsAcquired(false);
                }
                return response;
            } finally {
                releaseLock(callingContext, lockKey, lockHandle);
            }
        } else {
            log.error(String.format("Unable to acquire lock %s, so unable to acquire semaphore permit", lockKey));
            SemaphoreAcquireResponse response = new SemaphoreAcquireResponse();
            response.setIsAcquired(false);
            return response;
        }
    }

    /**
     * Release a permit
     *
     * @param stakeholderUri the work order uri or other uri associated with the requestor
     * @return
     */
    public boolean releasePermit(CallingContext callingContext, String stakeholderUri, String lockKey) {
        LockHandle lockHandle = null;
        // try until grab the lock
        while ((lockHandle = grabLock(callingContext, lockKey)) == null) {
            log.debug(String.format("Unable to acquire lock %s, retry in 1s", lockKey));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Thread interrupted while trying to acquire lock");
                break;
            }
        }
        if (lockHandle != null) {
            try {
                SemaphoreLock lock = SemaphoreLockStorage.readByFields(lockKey);
                if (lock != null) {
                    Set<String> uris = lock.getStakeholderURIs();
                    uris.remove(stakeholderUri);
                    SemaphoreLockStorage.add(lock, callingContext.getUser(), "Deleting WorkOrder lock");
                    return true;
                } else {
                    log.warn(String.format("Attempting to remove non-existent lock for lockKey %s", lockKey));
                    return false;
                }
            } finally {
                releaseLock(callingContext, lockKey, lockHandle);
            }
        } else {
            log.error(String.format("Unable to acquire lock %s, so unable to release semaphore permit", lockKey));
            return false;
        }

    }

    private LockHandle grabLock(CallingContext callingContext, String lockKey) {
        long secondsToWait = 5;
        long secondsToKeep = 20;
        return acquireLock(callingContext, SEMAPHORE_MANAGER_URI.toString(), lockKey, secondsToWait, secondsToKeep);
    }

    private void releaseLock(CallingContext callingContext, String lockKey, LockHandle lockHandle) {
        String lockName = lockKey;
        releaseLock(callingContext, SEMAPHORE_MANAGER_URI.toString(), lockName, lockHandle);
    }

    @Override
    public void forceReleaseLock(CallingContext context, String managerUri, String lockName) {
        ILockingHandler handler = LockFactory.getLockHandler(managerUri);
        handler.forceReleaseLock(lockName);
    }
}
