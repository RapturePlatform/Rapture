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
package rapture.kernel.cache;

import rapture.common.RaptureURI;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * @author bardhi
 * @since 5/21/15.
 */
public class KernelCaches {
    private static final Logger log = Logger.getLogger(KernelCaches.class);

    public Cache<RaptureURI, Optional<String>> getObjectStorageCache() {
        return objectStorageCache;
    }

    private final Cache<RaptureURI, Optional<String>> objectStorageCache;

    public KernelCaches() {
        objectStorageCache = setupObjectStorageCache();
    }

    private static Cache<RaptureURI, Optional<String>> setupObjectStorageCache() {
        return CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES)
                .removalListener(new RemovalListener<RaptureURI, Optional<String>>() {
                    @Override
                    public void onRemoval(RemovalNotification<RaptureURI, Optional<String>> notification) {
                        if (notification.getCause() != RemovalCause.REPLACED) {
                            if (log.isTraceEnabled()) log.trace("Removed " + notification.getKey() + " from local cache because " + notification.getCause());
                        }
                    }
                }).build();
    }

}
