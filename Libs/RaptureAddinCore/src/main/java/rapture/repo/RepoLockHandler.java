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
package rapture.repo;

import rapture.common.LockHandle;
import rapture.lock.ILockingHandler;
import rapture.util.IDGenerator;

/**
 * @author bardhi
 * @since 5/11/15.
 */
public class RepoLockHandler {
    protected static final int SECONDS_TO_HOLD = 5;
    protected static final int SECONDS_TO_WAIT = 5;

    private boolean lockIsDummy;
    private final ILockingHandler lockHandler;
    private final String lockName;

    public RepoLockHandler(boolean lockIsDummy, ILockingHandler lockHandler, String storeId) {
        this.lockIsDummy = lockIsDummy;
        this.lockHandler = lockHandler;
        this.lockName = storeId;
    }

    public LockHandle acquireLock(String lockHolder) {
        return lockHandler.acquireLock(lockHolder, lockName, SECONDS_TO_WAIT, SECONDS_TO_HOLD);
    }

    public String generateLockHolder() {
        return lockIsDummy ? "" : IDGenerator.getUUID();
    }

    public Boolean releaseLock(String lockHolder, LockHandle lockHandle) {
        return lockHandler.releaseLock(lockHolder, lockName, lockHandle);
    }

    public LockHandle acquireLock(String lockHolder, String folderPath) {
        return this.acquireLock(lockHolder);
    }

    public Boolean releaseLock(String lockHolder, LockHandle lockHandle, String folderPath) {
        return releaseLock(lockHolder, lockHandle);
    }
}
