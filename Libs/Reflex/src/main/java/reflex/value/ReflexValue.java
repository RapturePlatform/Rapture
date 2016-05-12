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
package reflex.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import rapture.common.impl.jackson.JacksonUtil;
import reflex.ReflexException;

public class ReflexValue implements Comparable<ReflexValue> {

    private Object value;
    private ReflexValueType valueType;
    
    String NOTNULL = "Argument to ReflexValue cannot be null. Use ReflexNullValue";

    public enum Internal {
        NULL, VOID, BREAK, CONTINUE, SUSPEND, UNDEFINED;
        
        @Override
        public String toString() {
            return "__reserved__" + this.name();
        }
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        if (value instanceof List<?>) {
            value = ensureReflexValueList((List<?>) value);
        }
        this.value = value;
        setTypeBasedOnValue();
    }

    private Object ensureReflexValueList(List<?> value) {
        List<ReflexValue> ret = new ArrayList<ReflexValue>(value.size());
        for (Object x : value) {
            if (x instanceof ReflexValue) {
                ret.add((ReflexValue) x);
            } else {
                ret.add(new ReflexValue(x));
            }
        }
        return ret;
    }

    private void setTypeBasedOnValue() {
        if (isInternal()) {
            valueType = ReflexValueType.INTERNAL;
        } else if (value instanceof String) {
            valueType = ReflexValueType.STRING;
        } else if ((value instanceof Integer) || (value instanceof Long) || (value instanceof BigInteger)) {
            valueType = ReflexValueType.INTEGER;
        } else if (value instanceof Number) {
            valueType = ReflexValueType.NUMBER;
        } else if (value instanceof Map<?, ?>) {
            valueType = ReflexValueType.MAP;
        } else if (value instanceof Boolean) {
            valueType = ReflexValueType.BOOLEAN;
        } else if (value instanceof List<?>) {
            valueType = ReflexValueType.LIST;
        } else if (value instanceof ReflexDateValue) {
            valueType = ReflexValueType.DATE;
        } else if (value instanceof ReflexArchiveFileValue) {
            valueType = ReflexValueType.ARCHIVE;
        } else if (value instanceof ReflexFileValue) {
            valueType = ReflexValueType.FILE;
        } else if (value instanceof ReflexStringStreamValue) {
            valueType = ReflexValueType.STRINGSTREAM;
        } else if (value instanceof ReflexStreamValue) {
            valueType = ReflexValueType.STREAM;
        } else if (value instanceof ReflexLibValue) {
            valueType = ReflexValueType.LIB;
        } else if (value instanceof ReflexPortValue) {
            valueType = ReflexValueType.PORT;
        } else if (value instanceof ReflexProcessValue) {
            valueType = ReflexValueType.PROCESS;
        } else if (value instanceof ReflexTimeValue) {
            valueType = ReflexValueType.TIME;
        } else if (value instanceof ReflexTimerValue) {
            valueType = ReflexValueType.TIMER;
        } else if (value instanceof ReflexSparseMatrixValue) {
            valueType = ReflexValueType.SPARSEMATRIX;
        } else if (value instanceof ReflexByteArrayValue) {
            valueType = ReflexValueType.BYTEARRAY;
        } else if (value instanceof ReflexMimeValue) {
            valueType = ReflexValueType.MIME;
        } else if (value instanceof ReflexStructValue) {
            valueType = ReflexValueType.STRUCT;
        } else if (value instanceof Object[]) {
            // maybe we should have an Array type?
            Object[] array = (Object[]) value;
            List<ReflexValue> list = new ArrayList<ReflexValue>();
            for (Object obj : array)
                list.add(new ReflexValue(obj));
            value = list;
            valueType = ReflexValueType.LIST;
        } else {
            valueType = ReflexValueType.COMPLEX;
        }
    }

    private boolean isInternal() {
        return false;
    }

    private boolean isReturn = false;

    public ReflexValue(int lineNumber, List<ReflexValue> v) {
        if (v == null) {
            throw new ReflexException(lineNumber, NOTNULL);
        } else {
            setValue(v);
        }
    }

    public ReflexValue(int lineNumber, Object v) {
        if (v == null) {
            throw new ReflexException(lineNumber, NOTNULL);
        } else if (v instanceof ReflexValue) {
            // If we're including a value in a value, just include the value. If
            // that makes sense.
            setValue(((ReflexValue) v).value);
        } else {
            setValue(v);
        }
    }

    public ReflexValue(List<ReflexValue> v) {
        this(-1, v);
    }

    public ReflexValue(Object v) {
        this(-1, v);
    }

    // Only used by serializers
    public ReflexValue() {

    }

    private void standardThrow(ReflexValueType expectedType) throws ReflexException {
        String message = String.format("Expected %s, have %s", expectedType.toString(), valueType.toString());
        throw new ReflexException(-1, message);
    }

    public Boolean asBoolean() {
        switch (valueType) {
            case BOOLEAN:
                return (Boolean) value;
            default:
                standardThrow(ReflexValueType.BOOLEAN);
        }
        return null;
    }

    public byte[] asByteArray() {
        switch (valueType) {
            case BYTEARRAY:
                return ((ReflexByteArrayValue) value).getBytes();
            case STRING:
                return value.toString().getBytes();
            case MIME:
                return ((ReflexMimeValue) value).getData();
            default:
                standardThrow(ReflexValueType.BYTEARRAY);
        }
        return null;
    }

    public ReflexDateValue asDate() {
        switch (valueType) {
            case DATE:
                return (ReflexDateValue) value;
            default:
                standardThrow(ReflexValueType.DATE);
        }
        return null;
    }

    public Double asDouble() {
        switch (valueType) {
        case INTEGER:
        case NUMBER:
                return ((Number) value).doubleValue();
            case STRING:
                return Double.valueOf(value.toString());
            default:
                standardThrow(ReflexValueType.NUMBER);
        }
        return null;
    }

    public BigDecimal asBigDecimal() {
    	if (value instanceof BigDecimal) return (BigDecimal) value;
        switch (valueType) {
	        case INTEGER:
	            return new BigDecimal(((Number) value).longValue());
	        case NUMBER:
	            return new BigDecimal(((Number) value).doubleValue(), MathContext.DECIMAL64);
            case STRING:
                return new BigDecimal(value.toString());
            default:
                standardThrow(ReflexValueType.NUMBER);
        }
        return null;
    }

    public Float asFloat() {
        switch (valueType) {
            case NUMBER:
            case INTEGER:
                return ((Number) value).floatValue();
            default:
                standardThrow(ReflexValueType.NUMBER);
        }
        return null;
    }

    public ReflexStreamValue asStream() {
        switch (valueType) {
            case STREAM:
                return (ReflexStreamValue) value;
            case FILE:
                return (ReflexFileValue) value;
            case STRINGSTREAM:
                return (ReflexStringStreamValue) value;
            default:
                return null;
        }
    }

    public ReflexSparseMatrixValue asMatrix() {
        switch (valueType) {
            case SPARSEMATRIX:
                return (ReflexSparseMatrixValue) value;
            default:
                standardThrow(ReflexValueType.SPARSEMATRIX);
        }
        return null;
    }

    public ReflexFileValue asFile() {
        switch (valueType) {
            case FILE:
                return (ReflexFileValue) value;
            default:
                return null;
        }
    }

    public ReflexArchiveFileValue asArchive() {
        switch (valueType) {
            case ARCHIVE:
                return (ReflexArchiveFileValue) value;
            default:
                return null;
        }
    }

    public Integer asInt() {
        switch (valueType) {
        	case INTEGER:
            case NUMBER:
                return ((Number) value).intValue();
            case STRING:
                return Integer.valueOf(value.toString());
            default:
                return 0;
        }
    }

    public ReflexLibValue asLib() {
        switch (valueType) {
            case LIB:
                return (ReflexLibValue) value;
            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<ReflexValue> asList() {
        if (valueType != ReflexValueType.LIST) return null;
    
        // It's possible for objects other than ReflexValues to get into the list.
        List<ReflexValue> retList = (List<ReflexValue>) value;

        boolean isPure = true;
        for (Object o : retList) {
            if (!(o instanceof ReflexValue)) {
                isPure = false;
                break;
            }
        }

        if (!isPure) {
            retList = new ArrayList<>();
            for (Object o : (List<Object>) value) {
                retList.add((o instanceof ReflexValue) ? (ReflexValue) o : new ReflexValue(o));
            }
            value = retList;
        }
        return retList;
    }

    public Long asLong() {
        switch (valueType) {
        	case INTEGER:
            case NUMBER:
                return ((Number) value).longValue();
            case STRING:
                return Long.valueOf(value.toString());
            default:
                return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap() {
        switch (valueType) {
            case MAP:
                return (Map<String, Object>) value;
            default:
                return null;
        }
    }

    public ReflexStructValue asStruct() {
        switch (valueType) {
            case STRUCT:
                return (ReflexStructValue) value;
            default:
                return null;
        }
    }

    public Object asObject() {
        switch (valueType) {
            case NUMBER:
                if (value instanceof Double) {
                    Double v = (Double) value;
                    int iv = v.intValue();
                    Double bv = new Double(iv);
                    double diff = Math.abs(v - bv);
                    if (diff < 0.000000001) {
                        return Integer.valueOf(iv);
                    }
                }
                break;
            case INTERNAL:
                if (this.getValue() == ReflexValue.Internal.VOID) {
                    return "void";
                } else if (this.getValue() == ReflexValue.Internal.NULL) {
                    return "null";
                }
                break;
            default:
                break;
        }
        return value;
    }

    public ReflexPortValue asPort() {
        switch (valueType) {
            case PORT:
                return (ReflexPortValue) value;
            default:
                standardThrow(ReflexValueType.PORT);
        }
        return null;
    }

    public ReflexProcessValue asProcess() {
        switch (valueType) {
            case PROCESS:
                return (ReflexProcessValue) value;
            default:
                standardThrow(ReflexValueType.PROCESS);
        }
        return null;
    }

    public String asString() {
        switch (valueType) {
            case STRING:
                return (String) value;
            default:
                return value.toString();
        }
    }

    public ReflexTimeValue asTime() {
        switch (valueType) {
            case TIME:
                return (ReflexTimeValue) value;
            default:
                standardThrow(ReflexValueType.TIME);
        }
        return null;
    }

    public ReflexTimerValue asTimer() {
        switch (valueType) {
            case TIMER:
                return (ReflexTimerValue) value;
            default:
                standardThrow(ReflexValueType.TIMER);
        }
        return null;
    }

    public ReflexMimeValue asMime() {
        switch (valueType) {
            case MIME:
                return (ReflexMimeValue) value;
            default:
                standardThrow(ReflexValueType.MIME);

        }
        return null;
    }

    @Override
    public int compareTo(ReflexValue that) {
        if (this.isNumber() && that.isNumber()) {
            if (this.equals(that)) {
                return 0;
            } else {
                return this.asDouble().compareTo(that.asDouble());
            }
        } else if (this.isString() && that.isString()) {
            return this.asString().compareTo(that.asString());
        } else if (this.isList() && that.isList()) {
            return compareList(this.asList(), that.asList());
        } else {
            return this.asString().compareTo(that.asString());
            // throw new ReflexException(-1,
            // "illegal expression: can't compare `" + this + "` to `" + that +
            // "`");
        }
    }

    private int compareList(List<? extends ReflexValue> asList, List<? extends ReflexValue> asList2) {
        if (asList.size() != asList2.size()) {
            return asList.size() > asList2.size() ? -1 : 1;
        } else {
            return asList.toString().compareTo(asList2.toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this.getValue() == Internal.VOID) {
            throw new ReflexException(-1, "can't use VOID: " + this + " ==/!= " + o);
        }
        if (o == null) return false;
        if (this == o) return true;

        ReflexValue that = (o instanceof ReflexValue) ? (ReflexValue) o : new ReflexValue(o);

        // NULL and UNDEFINED are considered equivalent most of the time
        if (((this.getValue() == Internal.NULL) || (this.getValue() == Internal.UNDEFINED))
                && ((that.getValue() == Internal.NULL) || (that.getValue() == Internal.UNDEFINED))) {
            return true;
        }

        if (this.isInteger() && that.isInteger()) {
            return this.asLong().equals(that.asLong());
        } else if (this.isNumber() && that.isNumber()) {
            return this.asBigDecimal().compareTo(that.asBigDecimal()) == 0;
        } else if (this.isDate() && that.isDate()) {
            return this.asDate().equals(that);
        } else if (this.isTime() && that.isTime()) {
            return this.asTime().equals(that);
        } else if (this.isList() && that.isList()) {
            return compareTwoLists(this.asList(), that.asList());
        } else {
            return this.value.equals(that.value);
        }
    }

    private boolean compareTwoLists(List<? extends ReflexValue> a, List<? extends ReflexValue> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
        	ReflexValue one = a.get(i);
        	ReflexValue two = b.get(i);
        	// Must be of like types otherwise 2 == '2'
            if (one.isNumber() != two.isNumber()) return false;
            if (one.compareTo(two) != 0) return false;
        }
        return true;
    }

    /**
     * Return the type of this object
     *
     * @return
     */
    @JsonIgnore
    public String getTypeAsString() {
        switch (valueType) {
            case INTERNAL:
                if (isVoid()) {
                    return "void";
                } else if (isNull()) {
                    return "null";
                } else {
                    return "object";
                }
            default:
                return valueType.name().toLowerCase();
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @JsonIgnore
    public boolean isByteArray() {
        return valueType == ReflexValueType.BYTEARRAY;
    }

    @JsonIgnore
    public boolean isBoolean() {
        return valueType == ReflexValueType.BOOLEAN;
    }

    @JsonIgnore
    public boolean isMime() {
        return valueType == ReflexValueType.MIME;
    }

    @JsonIgnore
    public boolean isSparseMatrix() {
        return valueType == ReflexValueType.SPARSEMATRIX;
    }

    @JsonIgnore
    public boolean isComplex() {
        return valueType == ReflexValueType.COMPLEX;
    }

    @JsonIgnore
    public boolean isStruct() {
        return valueType == ReflexValueType.STRUCT;
    }

    @JsonIgnore
    public boolean isObject() {
        if (valueType == ReflexValueType.INTERNAL) {
            return !isVoid() && !isNull();
        }
        return valueType.isObject();
    }

    @JsonIgnore
    public boolean isDate() {
        boolean b = valueType == ReflexValueType.DATE;
        return b;
    }

    @JsonIgnore
    public boolean isFile() {
        return valueType == ReflexValueType.FILE;
    }

    @JsonIgnore
    public boolean isArchive() {
        return valueType == ReflexValueType.ARCHIVE;
    }

    @JsonIgnore
    public boolean isLib() {
        return valueType == ReflexValueType.LIB;
    }

    @JsonIgnore
    public boolean isList() {
        return valueType == ReflexValueType.LIST;
    }

    @JsonIgnore
    public boolean isMap() {
        return valueType == ReflexValueType.MAP;
    }

    @JsonIgnore
    public boolean isNull() {
        return false;
    }

    @JsonIgnore
    /**
     * Integers are Numbers
     * @return
     */
    public boolean isNumber() {
        return (valueType == ReflexValueType.NUMBER) || (valueType == ReflexValueType.INTEGER);
    }

    @JsonIgnore
    public boolean isInteger() {
        return valueType == ReflexValueType.INTEGER;
    }

    @JsonIgnore
    public boolean isPort() {
        return valueType == ReflexValueType.PORT;
    }

    @JsonIgnore
    public boolean isProcess() {
        return valueType == ReflexValueType.PROCESS;
    }

    @JsonIgnore
    public boolean isReturn() {
        return isReturn;
    }

    @JsonIgnore
    public boolean isString() {
        return valueType == ReflexValueType.STRING;
    }

    @JsonIgnore
    public boolean isTime() {
        return valueType == ReflexValueType.TIME;
    }

    @JsonIgnore
    public boolean isTimer() {
        return valueType == ReflexValueType.TIMER;
    }

    @JsonIgnore
    public boolean isVoid() {
        return false;
    }

    public void setReturn(boolean isReturn) {
        this.isReturn = isReturn;
    }

    @Override
    public String toString() {
        return isNull() ? "NULL" : isVoid() ? "VOID" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public <T> T asObjectOfType(Class<T> klass) {
        // TODO: Find a more efficient way to do this.
        if (isMap()) {
            if (asMap().containsKey("CLASS")) {
                Map<String, Object> mapCopy = new HashMap<String, Object>(asMap());
                mapCopy.remove("CLASS");
                return JacksonUtil.objectFromJson(JacksonUtil.jsonFromObject(mapCopy), klass);
            }
        }
        return (T) value;
    }

    @JsonIgnore
    public boolean isStreamBased() {
        return valueType == ReflexValueType.FILE || valueType == ReflexValueType.STREAM || valueType == ReflexValueType.STRINGSTREAM;
    }

    @JsonIgnore
    public String getValueTypeString() {
        return valueType.toString();
    }

    public void reassign(ReflexValue other) {
        // The idea here is that sometimes we'd like to use the type of the source to coerce the passed value into the current value
        // E.g. if the other is a string, and this is a structure, try to assign
        // If this is a number and the passed is a string, cast, etc.
        // Strings are the most easy
        //
    }

}
