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
package com.incapture.rapgen.persistence;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.object.Storable;
import rapture.persistence.storable.mapper.StorableSerDeserHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bardhi
 * @since 7/22/15.
 */
public class StorableSerDeserRepo {

    private final Map<Class<? extends Storable>, Class<? extends StorableSerDeserHelper>> storableToMapper;
    private final Map<Class<? extends Storable>, Class<? extends Storable>> storableToExtended;

    public StorableSerDeserRepo(Collection<Class<? extends StorableSerDeserHelper>> list) {
        storableToMapper = new HashMap<>();
        storableToExtended = new HashMap<>();

        for (Class<? extends StorableSerDeserHelper> mapper : list) {
            StorableSerDeserHelper<? extends Storable, ? extends Storable> instance;
            try {
                //noinspection unchecked
                instance = mapper.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw RaptureExceptionFactory.create("Error reading mapper", e);
            }
            storableToMapper.put(instance.getStorableClass(), mapper);
            storableToExtended.put(instance.getStorableClass(), instance.getExtendedClass());
        }
    }

    public Collection<Class<? extends StorableSerDeserHelper>> getAll() {
        return storableToMapper.values();
    }

    public Class<? extends Storable> getExtendedClass(Class<? extends Storable> storableClass) {
        return storableToExtended.get(storableClass);
    }

    public Class<? extends StorableSerDeserHelper> getSerDeserHelper(Class<? extends Storable> raptureUserClass) {
        return storableToMapper.get(raptureUserClass);
    }
}
