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

/**
 * This holds information about the lock.
 * 
 * @author alan
 * 
 */
public class MemoryLock {
    private String lockHolder;
    private String lockKey;
    private long timeToRelease;

    public Object getLockHolder() {
        return lockHolder;
    }

    public String getLockKey() {
        return lockKey;
    }

    public long getTimeToRelease() {
        return timeToRelease;
    }

    public boolean hasContext(String lockContext) {
        return lockContext.equals(lockHolder);
    }

    public boolean readyToRelease() {
        return getTimeToRelease() < System.currentTimeMillis();
    }

    public void setLockHolder(String lockHolder) {
        this.lockHolder = lockHolder;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public void setTimeToRelease(long timeToRelease) {
        this.timeToRelease = timeToRelease;
    }
}
