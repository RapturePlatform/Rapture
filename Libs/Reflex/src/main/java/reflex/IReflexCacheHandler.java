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
package reflex;

import reflex.value.ReflexValue;

/**
 * An interface used to expose a local in-process cache (for now) that Reflex
 * scripts can use to potentially share information (well, cache information,
 * it's not guaranteed that anything put into the cache can come out - if you
 * want that use a data repo)
 * 
 * @author amkimian
 * 
 */
public interface IReflexCacheHandler extends ICapability {
    /**
     * Expiry time can be 0/-1 which means "do the default"
     * 
     * @param value
     * @param name
     * @param expiryTimeInSeconds
     * @return
     */
    public boolean putIntoCache(ReflexValue value, String name, long expiryTimeInSeconds);

    /**
     * Can return new ReflexNullValue() which means that there wasn't a value of that
     * name in the cache.
     * 
     * @param name
     * @return
     */
    public ReflexValue retrieveFromCache(String name);

}
