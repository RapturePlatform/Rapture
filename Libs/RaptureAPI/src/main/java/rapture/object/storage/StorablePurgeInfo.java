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
package rapture.object.storage;

import rapture.common.RaptureURI;
import rapture.object.Storable;

/**
 * @author bardhi
 * @since 11/14/14.
 */
public interface StorablePurgeInfo<T extends Storable> {
    /**
     * The class that extends {@link rapture.object.Storable} that belongs here
     *
     * @return
     */
    Class<T> getStorableClass();

    /**
     * The name of the sdk if this is an SDK object, or null for Rapture internal objects
     *
     * @return
     */
    String getSdkName();

    /**
     * The maximum time to keep an object before purging, in milliseconds
     *
     * @return
     */
    long getTTL();

    /**
     * An instance of the StorableIndexInfo class corresponding to this purge info class
     * @return
     */
    StorableIndexInfo getIndexInfo();

    /**
     * Return the base uri where objects of this type are stored
     * @return
     */
    RaptureURI getBaseURI();
}
