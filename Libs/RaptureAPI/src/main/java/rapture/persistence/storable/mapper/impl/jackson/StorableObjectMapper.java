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
package rapture.persistence.storable.mapper.impl.jackson;

import rapture.common.impl.jackson.MapperFactory;
import rapture.object.Storable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author bardhi
 * @since 7/23/15.
 */
public enum StorableObjectMapper {

    INSTANCE;

    /**
     * Register a serializer and deserializer for the specified {@link Storable}
     */
    public static <S> void addSerDes(Class<S> storableClass, JsonSerializer<S> ser, JsonDeserializer<S> deser) {
        SimpleModule m = MapperFactory.createModule(storableClass.getName());
        m.addSerializer(storableClass, ser);
        m.addDeserializer(storableClass, deser);
        INSTANCE.mapper.registerModule(m);
    }

    public static ObjectMapper getMapper() {
        return INSTANCE.mapper;
    }

    private final ObjectMapper mapper;

    StorableObjectMapper() {
        mapper = createMapper();
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = MapperFactory.createForStorables();

        return mapper;
    }

}
