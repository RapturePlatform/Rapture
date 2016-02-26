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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.impl.jackson.JacksonUtil;
import reflex.ReflexException;
import reflex.structure.ArrayStructureType;
import reflex.structure.InnerStructureType;
import reflex.structure.IntegerStructureType;
import reflex.structure.NumberStructureType;
import reflex.structure.StringStructureType;
import reflex.structure.Structure;
import reflex.structure.StructureType;

/**
 * An instance of a struct
 * 
 * @author amkimian
 *
 */
public class ReflexStructValue {
    private Structure structure;
    private Map<String, Object> fieldValues;
    private Map<String, Object> unmatchedValues;

    public ReflexStructValue() {
        fieldValues = new HashMap<String, Object>();
        unmatchedValues = new HashMap<String, Object>();
        structure = null;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    private boolean isOfCorrectType(Object val, StructureType sType) {
        if (sType instanceof IntegerStructureType) {
            if (val instanceof Integer) {
                return true;
            } else {
                System.err.println("Error " + val + " is not an integer");
            }
        } else if (sType instanceof NumberStructureType) {
            if (val instanceof Integer || val instanceof Double || val instanceof Float) {
                return true;
            } else {
                System.err.println("Error " + val + " is not an number, it is " + val.getClass().toString());
            }
        } else if (sType instanceof StringStructureType) {
            if (val instanceof String) {
                return true;
            } else {
                System.err.println("Error " + val + " is not a string");
            }
        } else if (sType instanceof ArrayStructureType) {
            if (val instanceof List<?>) {
                return true;
            } else {
                System.err.println("Error " + val + " is not a list");
            }
        } else if (sType instanceof InnerStructureType) {
            if (val instanceof Map) {
                return true;
            } else {
                System.err.println("Error " + val + " is not a map");
            }
        }
        return false;
    }

    public void attemptAssignFrom(ReflexValue value, boolean strict) {
        // If the value is a map, start assigning and coercing values into fieldValues. If not found, put in unmatchedValues
        // If strict, unmatched values throw except and incorrect types throw exception
        if (value.isMap()) {
            Map<String, Object> vals = value.asMap();
            attemptAssignFrom(vals, strict);
        } else if (value.isStruct()) {
            ReflexStructValue v = value.asStruct();
            attemptAssignFrom(v.fieldValues, strict);
        } else if (value.isString()) {
            // Attempt a json to map conversion
            Map<String, Object> vals = JacksonUtil.getMapFromJson(value.asString());
            attemptAssignFrom(vals, strict);
        } else {
            System.err.println("ReflexStructValue - Cannot assign from that! " + value.getTypeAsString());
        }
    }

    private void attemptAssignFrom(Map<String, Object> vals, boolean strict) {
        for (Map.Entry<String, StructureType> entry : structure.getMembers().entrySet()) {
            if (vals.containsKey(entry.getKey())) {
                Object val = vals.get(entry.getKey());
                if (val instanceof ReflexValue) {
                    val = ((ReflexValue) val).asObject();
                }
                StructureType sType = entry.getValue();
                if (isOfCorrectType(val, sType)) {
                    if (sType instanceof ArrayStructureType) {
                        ArrayStructureType aType = (ArrayStructureType) sType;
                        if (val instanceof List<?>) {
                            List<?> theList = (List<?>) val;
                            boolean ok = true;
                            for (Object x : theList) {
                                if (x instanceof ReflexValue) {
                                    x = ((ReflexValue) x).asObject();
                                }
                                if (!isOfCorrectType(x, aType.getArrayType())) {
                                    ok = false;
                                    break;
                                }
                            }
                            if (ok) {
                                // NB TODO: If the getArrayType is inner or array we really need to do more validation, but for now...
                                fieldValues.put(entry.getKey(), theList);
                            }
                        }
                    } else if (sType instanceof InnerStructureType) {
                        InnerStructureType iType = (InnerStructureType) sType;
                        if (val instanceof Map<?, ?>) {
                            Map<String, Object> inner = (Map<String, Object>) val;
                            ReflexStructValue v = new ReflexStructValue();
                            v.setStructure(iType.getStructure());
                            v.attemptAssignFrom(inner, strict);
                            fieldValues.put(entry.getKey(), v.fieldValues);
                        }
                    } else {
                        fieldValues.put(entry.getKey(), val);
                    }
                }
            }
        }
    }

    public String toString() {
        return fieldValues.toString();
    }

    public boolean dottedAssign(String[] parts, ReflexValue value) {
        // Navigate down the structure, throwing an exception if the path isn't correct according to the structure
        return dottedInnerAssign(parts, 1, value, structure, fieldValues);
    }

    private boolean dottedInnerAssign(String[] parts, int startIndex, ReflexValue value, Structure s, Map<String, Object> fVals) {
        String checkField = parts[startIndex];
        if (s.getMembers().containsKey(checkField)) {
            if (startIndex == parts.length - 1) {
                // We are at the end
                if (isOfCorrectType(value.asObject(), s.getMembers().get(checkField))) {
                    fVals.put(checkField, value.asObject());
                    return true;
                } else {
                    throw new ReflexException(0, "Value is not of correct type. Field name " + checkField + " value " + value.toString());
                }
            } else {
                if (s.getMembers().get(checkField) instanceof InnerStructureType) {
                    InnerStructureType iType = (InnerStructureType) s.getMembers().get(checkField);
                    if (!fVals.containsKey(checkField)) {
                        fVals.put(checkField, new HashMap<String, Object>());
                    }
                        
                        Map<String, Object> v = (Map<String, Object>)fVals.get(checkField);
                        dottedInnerAssign(parts, startIndex+1, value, iType.getStructure(), v);
                } else {
                    throw new ReflexException(0, "That field is not an inner structure");
                }
            }
        } else {
            throw new ReflexException(0, "No structure member named " + checkField);
        }
        return true;
    }

    public Map<String, Object> asMap() {
        return fieldValues;
    }
}
