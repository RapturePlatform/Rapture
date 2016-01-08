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

import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static com.google.common.base.Preconditions.checkArgument;

import rapture.common.Hose;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;
import rapture.common.exception.RaptureExceptionFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class StructureSeriesValueImpl implements StructureSeriesValue {
    private final Map<String, SeriesValue> name2field = Maps.newHashMap();
    private final String col;
    private String str;

    public StructureSeriesValueImpl(String col) {
        this.col = col;
    }

    /**
     * set a field in the structure
     *
     * @param name
     * @param val
     * @return the old value, or null if there was no prior value;
     */
    public SeriesValue setField(String name, SeriesValue val) {
        int dot = name.indexOf('.');
        if (dot >= 0) {
            String head = name.substring(0, dot);
            String tail = name.substring(dot + 1);
            SeriesValue raw = name2field.get(head);
            StructureSeriesValueImpl struct;
            if (raw == null) {
                struct = createSubtructure(head);
            } else {
                struct = (StructureSeriesValueImpl) raw.asStructure();
            }
            return struct.setField(tail, val);
        } else {
            return name2field.put(name, val);
        }
    }

    public StructureSeriesValueImpl createSubtructure(String name) {
        StructureSeriesValueImpl result = new StructureSeriesValueImpl(col);
        name2field.put(name, result);
        return result;
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

    public boolean asBoolean() {
        throw new UnsupportedOperationException("Attempt to use structure as boolean");
    }

    @Override
    public StructureSeriesValue asStructure() {
        return this;
    }

    @Override
    public Hose asStream() {
        throw new UnsupportedOperationException("Error: Attempt to use a structure as a function");
    }

    @Override
    public boolean isNumber() {
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
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isStructure() {
        return true;
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
        Preconditions.checkArgument(index == 0, "Error: Attempt to index a structure -- use myStruct.field, not myStruct[field]");
        return this;
    }

    @Override
    public SeriesValue getField(String name) {
        int dot = name.indexOf('.');
        if (dot >= 0) {
            String head = name.substring(0, dot);
            String tail = name.substring(dot + 1);
            SeriesValue raw = name2field.get(head);
            StructureSeriesValueImpl struct;
            if (raw == null) {
                return null;
            } else {
                struct = (StructureSeriesValueImpl) raw.asStructure();
            }
            return struct.getField(tail);
        } else {
            return name2field.get(name);
        }
    }

    private void prepStr() {
        if (str == null) {
            try {
                str = marshal();
            } catch (IOException e) {
                str = "<Error: " + e.getMessage() + ">";
            }
        }
    }

    public static StructureSeriesValue unmarshal(String jsonVal, String column) throws IOException {
        JsonParser parser = new JsonFactory().createJsonParser(jsonVal);
        checkArgument(parser.nextToken() == START_OBJECT); // dump the outermost
        // open bracket
        return unmarshal(parser, column);
    }

    static StructureSeriesValue unmarshal(JsonParser parser, String column) throws IOException {
        StructureSeriesValueImpl result = new StructureSeriesValueImpl(column);
        while (parser.nextToken() == FIELD_NAME) {
            String name = parser.getCurrentName();
            JsonToken token = parser.nextToken();
            switch (token) {
                case END_OBJECT:
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Malformed Json String for SeriesValue");
                case START_OBJECT:
                    result.setField(name, unmarshal(parser, column));
                    break;
                case VALUE_NUMBER_FLOAT:
                    result.setField(name, new DecimalSeriesValue(parser.getDoubleValue(), column));
                    break;
                case VALUE_NUMBER_INT:
                    result.setField(name, new LongSeriesValue(parser.getValueAsLong(), column));
                    break;
                case VALUE_STRING:
                    result.setField(name, new StringSeriesValue(parser.getText(), column));
                    break;
                case START_ARRAY:
                    result.setField(name, new ArraySeriesValue(parser, column));
                    break;
                case VALUE_FALSE:
                    result.setField(name, new BooleanSeriesValue(false, column));
                    break;
                case VALUE_TRUE:
                    result.setField(name, new BooleanSeriesValue(true, column));
                    break;
                case END_ARRAY:
                case FIELD_NAME:
                case NOT_AVAILABLE:
                case VALUE_EMBEDDED_OBJECT:
                case VALUE_NULL:
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unsupported Type encountered decoding SeriesValue: " + token);
            }
        }
        checkArgument(parser.getCurrentToken() == END_OBJECT, "Malformed Json Close for SeriesValue");
        return result;
    }

    private String marshal() throws IOException {
        StringWriter ss = new StringWriter();
        JsonGenerator gen = new JsonFactory().createJsonGenerator(ss);
        gen.writeStartObject();
        marshal(gen);
        gen.writeEndObject();
        return ss.toString();
    }

    void marshal(JsonGenerator gen) throws IOException {
        for (Map.Entry<String, SeriesValue> entry : name2field.entrySet()) {
            String name = entry.getKey();
            SeriesValue value = entry.getValue();
            if (value.isDouble()) {
                gen.writeNumberField(name, value.asDouble());
            } else if (value.isLong()) {
                gen.writeNumberField(name, value.asLong());
            } else if (value.isString()) {
                gen.writeStringField(name, value.asString());
            } else if (value.isStructure()) {
                gen.writeObjectFieldStart(name);
                ((StructureSeriesValueImpl) value).marshal(gen);
                gen.writeEndObject();
            } else if (value.isBoolean()) {
                gen.writeBoolean(value.asBoolean());
            } else if (value instanceof ArraySeriesValue) {
                gen.writeArrayFieldStart(name);
                ((ArraySeriesValue) value).marshal(gen);
                gen.writeEndArray();
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Unable to encode SeriesValue, non-data type");
            }
        }
    }

    @Override
    public long asLong() {
        throw new UnsupportedOperationException("Attempt to use structure as integer");
    }

    @Override
    public double asDouble() {
        return Double.NaN;
    }

    @Override
    public Collection<String> getFields() {
        return name2field.keySet();
    }

    public static List<SeriesValue> zip(List<String> columns, List<String> values) throws IOException {
        Iterator<String> iterC = columns.iterator();
        Iterator<String> iterD = values.iterator();
        List<SeriesValue> result = Lists.newArrayList();
        while (iterC.hasNext()) {
            result.add(StructureSeriesValueImpl.unmarshal(iterD.next(), iterC.next()));
        }
        return result;
    }
}
