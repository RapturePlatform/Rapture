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
package reflex.node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import reflex.DebugLevel;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public abstract class BaseAssignmentNode extends BaseNode {

    protected String identifier;
    protected List<List<ReflexNode>> indexNodes;
    protected ReflexNode rhs;

    public BaseAssignmentNode(int lineNumber, IReflexHandler handler, Scope s, String i, List<List<ReflexNode>> e, ReflexNode n) {
        super(lineNumber, handler, s);
        identifier = i;
        indexNodes = (e == null) ? new ArrayList<List<ReflexNode>>() : e;
        rhs = n;
    }

    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope, Scope scopeToAssignIn, String namespacePrefix) {
        debugger.stepStart(this, scope);
        ReflexValue ret = new ReflexVoidValue(lineNumber);
        ReflexValue value = rhs.evaluate(debugger, scope);
        handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Assignment, rhs = " + value.toString());

        if (value .getValue() == ReflexValue.Internal.VOID) {
            throw new ReflexException(lineNumber, "can't assign VOID to " + identifier);
        }

        if (indexNodes.isEmpty()) { // a simple assignment
            handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "assign var into " + identifier);
            scopeToAssignIn.assign(identifier, value, namespacePrefix);
        } else { // a possible list-lookup and reassignment

            ReflexValue var = scope.resolve(identifier, namespacePrefix);
            if (var == null) {
                throw new ReflexException(lineNumber, "Null value for " + identifier);
            } else if (var.isList()) {
                handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Assign into list");
               setInList(debugger, value, var, scopeToAssignIn);
            } else if (var.isMap()) {
                handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Assign into map");
                setInMap(debugger, value, var, scopeToAssignIn);
            } else if (var.isSparseMatrix()) {
                handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Assign into sparse matrix");
                setInMatrix(debugger, value, var, scopeToAssignIn);
            } else {
                throw new ReflexException(lineNumber, "can't assign into a " + var.getTypeAsString() + " in that way");
            }
            ret = var;
        }

        debugger.stepEnd(this, ret, scope);
        return ret;
    }

    private void setInMatrix(IReflexDebugger debugger, ReflexValue value, ReflexValue var, Scope scope) {
    	// each indexNode should have a size of 1, and there should be two of them
    	ReflexValue rowIndex = indexNodes.get(0).get(0).evaluate(debugger, scope);
    	ReflexValue colIndex = indexNodes.get(0).get(1).evaluate(debugger, scope);
    	var.asMatrix().set(rowIndex, colIndex, value);
    }
    
    private void setInList(IReflexDebugger debugger, ReflexValue value, ReflexValue var, Scope scope) {
        // iterate up to `foo[x][y]` in case of `foo[x][y][z] = 42;`
    	List<ReflexNode> realVals = indexNodes.get(0);
        for (int i = 0; i < realVals.size() - 1 && var != null; i++) {
            ReflexValue index = realVals.get(i).evaluate(debugger, scope);

            if (!index.isNumber() || !var.isList()) { // sanity checks
                throw new RuntimeException("illegal statement: " + this);
            }

            int idx = index.asLong().intValue();
            var = var.asList().get(idx);
        }
        // list is now pointing to `foo[x][y]` in case of `foo[x][y][z]
        // = 42;`

        // get the value `z`: the last index, in `foo[x][y][z] = 42;`
        ReflexValue lastIndex = realVals.get(realVals.size() - 1).evaluate(debugger, scope);

        if (!lastIndex.isNumber() || !var.isList()) { // sanity checks
            throw new RuntimeException("illegal statement: " + this);
        }

        // re-assign `foo[x][y][z]`
        List<ReflexValue> existing = var.asList();
        existing.set(lastIndex.asLong().intValue(), value);
    }

    /**
     * Value is what to set, var references a map
     * 
     * @param value
     * @param var
     */
    @SuppressWarnings("unchecked")
	private void setInMap(IReflexDebugger debugger, ReflexValue value, ReflexValue var, Scope scope) {
        // We traverse down the map by using the indexNodes. When we get
        // to the last point that's the value we need to set.
        // If an indexNode is string based it's a lookup, if it's integer based
        // the value at this point will
        // be a list of maps, so we index from that point.
       List<ReflexNode> realVals = new LinkedList<ReflexNode>();
       for (List<ReflexNode> vals : indexNodes) {
           realVals.addAll(vals);
       }
       Map<String, Object> currentPoint = var.asMap();
        List<Object> currentArray = null;

        for (int i = 0; i < realVals.size() - 1; i++) {
            ReflexValue index = realVals.get(i).evaluate(debugger, scope);
            if (index.isString()) {
                if (currentPoint.containsKey(index.asString())) {
                    Object val = currentPoint.get(index.asString());
                    if (val instanceof List<?>) {
                        currentArray = (List<Object>) val;
                        currentPoint = null;
                    } else {
                        currentPoint = (Map<String, Object>) val;
                        currentArray = null;
                    }
                } else {
                    handler.getDebugHandler().statementReached(lineNumber, DebugLevel.SPAM, "Create new map at key " + index.asString());
                    Map<String, Object> newMap = new LinkedHashMap<String, Object>();
                    currentPoint.put(index.asString(), newMap);
                    currentPoint = newMap;
                    currentArray = null;
                }
            } else if (index.isNumber()) {
                if (currentArray != null) {
                    Object val = currentArray.get(index.asLong().intValue());
                    if (val instanceof List<?>) {
                        currentArray = (List<Object>) val;
                        currentPoint = null;
                    } else {
                        currentPoint = (Map<String, Object>) val;
                        currentArray = null;
                    }
                }
            }
        }
        // Now we are at the end with either a list or a map
        ReflexValue lastIndex = realVals.get(realVals.size() - 1).evaluate(debugger, scope);
        if (lastIndex.isNumber()) {
            currentArray.set(lastIndex.asLong().intValue(), value.asObject());
        } else {
            currentPoint.put(lastIndex.asString(), value.asObject());
        }
    }

    @Override
    public String toString() {
        return String.format("(%s[%s] = %s)", identifier, indexNodes, rhs);
    }
}
