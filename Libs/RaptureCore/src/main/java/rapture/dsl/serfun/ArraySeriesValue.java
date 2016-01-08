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
package rapture.dsl.serfun;

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;
import rapture.common.exception.RaptureExceptionFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.Lists;

public class ArraySeriesValue implements SeriesValue {
    private final String col;
    private String str = null;
    private List<SeriesValue> children = Lists.newArrayList();

    public ArraySeriesValue(JsonParser parser, String column) throws JsonParseException, IOException {
        col = column;
        while (true) {
            JsonToken token = parser.nextToken();
            switch (token) {
                case END_ARRAY:
                    return;
                case END_OBJECT:
                case FIELD_NAME:
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Malformed json array");
                case START_OBJECT:
                    children.add(StructureSeriesValueImpl.unmarshal(parser, column));
                    break;
                case VALUE_NUMBER_FLOAT:
                    children.add(new DecimalSeriesValue(parser.getDoubleValue(), column));
                    break;
                case VALUE_NUMBER_INT:
                    children.add(new LongSeriesValue(parser.getValueAsLong(), column));
                    break;
                case VALUE_STRING:
                    children.add(new StringSeriesValue(parser.getText(), column));
                    break;
                case START_ARRAY:
                    children.add(new ArraySeriesValue(parser, column));
                    break;
                case NOT_AVAILABLE:
                case VALUE_EMBEDDED_OBJECT:
                case VALUE_FALSE:
                case VALUE_NULL:
                case VALUE_TRUE:
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("Unsupported type %s in json array", token));
            }
        }
    }

    @Override
    public String getColumn() {
        return col;
    }

    @Override
    public String asString() {
        prepStr();
        return str;
    }

    private void prepStr() {
        if (str == null) {
            StringBuffer result = new StringBuffer();
            result.append("[");
            boolean first = true;
            for (SeriesValue sv : children) {
                if (first) first = false;
                else result.append(",");
                result.append(sv.asString());
            }
            result.append("]");
            str = result.toString();
        }
    }

    @Override
    public long asLong() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Attempt to use array as long");
    }

    @Override
    public boolean asBoolean() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Attempt to use array as boolean");
    }

    @Override
    public double asDouble() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Attempt to use array as decimal");
    }

    @Override
    public StructureSeriesValue asStructure() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Attempt to use array as structure");
    }

    @Override
    public Hose asStream() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Attempt to use array as stream");
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isLong() {
        return false;
    }

    @Override
    public boolean isDouble() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isStructure() {
        return false;
    }

    @Override
    public boolean isSeries() {
        return false;
    }

    @Override
    public SeriesValue pullValue() {
        return this;
    }

    @Override
    public SeriesValue pullValue(int index) {
        return this;
    }

    /**
     * generate the contents of the array -- do NOT emit the [ ]
     *
     * @param gen
     * @throws IOException
     */
    public void marshal(JsonGenerator gen) throws IOException {
        for (SeriesValue value : children) {
            if (value.isDouble()) {
                gen.writeNumber(value.asDouble());
            } else if (value.isLong()) {
                gen.writeNumber(value.asLong());
            } else if (value.isString()) {
                gen.writeString(value.asString());
            } else if (value.isStructure()) {
                gen.writeStartObject();
                ((StructureSeriesValueImpl) value).marshal(gen);
                gen.writeEndObject();
            } else if (value instanceof ArraySeriesValue) {
                gen.writeStartArray();
                ((ArraySeriesValue) value).marshal(gen);
                gen.writeEndArray();
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to encode SeriesValue, non-data type");
            }
        }
    }
}
