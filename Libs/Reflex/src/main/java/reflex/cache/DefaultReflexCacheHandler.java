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
package reflex.cache;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import reflex.IReflexCacheHandler;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class DefaultReflexCacheHandler implements IReflexCacheHandler {

    private static final int DEFAULT_EXPIRY = 100;
    private Cache<String, CacheElement<ReflexValue>> valueCache = CacheBuilder.newBuilder().expireAfterWrite(DEFAULT_EXPIRY, TimeUnit.SECONDS).build();

    @Override
    public boolean putIntoCache(ReflexValue value, String name, long expiryTimeInSeconds) {
        if (expiryTimeInSeconds == 0) {
            expiryTimeInSeconds = DEFAULT_EXPIRY;
        }
        valueCache.put(name, new CacheElement<ReflexValue>(value, expiryTimeInSeconds * 1000));
        return false;
    }

    @Override
    public ReflexValue retrieveFromCache(String name) {
        CacheElement<ReflexValue> ret = valueCache.getIfPresent(name);
        if (ret == null || ret.hasExpired()) {
            return new ReflexNullValue();
        } else {
            return ret.getContent();
        }
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

}
