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
package rapture.persistence.storable.mapper;

import rapture.object.Storable;

/**
 * Interface for objects that are used to serialize and deserialize Storables. By default a Storable
 * does not need to have a helper and uses standard serialization where each Java field is mapped to
 * a json field. Sometimes we need to preserve backwards-compatibility however, and in those cases
 * we need complex logic to set some fields based on other fields. (e.g. if a field was renamed,
 * we want to set the new one based on the original)
 *
 * @author bardhi
 * @since 7/21/15.
 */
public interface StorableSerDeserHelper<STORABLE extends Storable, EXTENDED extends STORABLE> {
    Class<STORABLE> getStorableClass();

    Class<EXTENDED> getExtendedClass();

    void onSerialize(STORABLE storable);

    void onDeserialize(EXTENDED user);
}
