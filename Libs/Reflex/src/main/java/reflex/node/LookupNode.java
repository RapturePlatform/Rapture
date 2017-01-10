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
package reflex.node;

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexUndefinedValue;

public class LookupNode extends BaseNode {

	private ReflexNode expression;
	private List<List<ReflexNode>> indexes;

	public LookupNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode e, List<List<ReflexNode>> i) {
		super(lineNumber, handler, scope);
		expression = e;
		indexes = i;
	}

	private List<ReflexValue> getValuesFromList(List<ReflexNode> nodes, IReflexDebugger debugger, Scope scope) {
		List<ReflexValue> indexValues = new ArrayList<>(indexes.size());

		for (ReflexNode indexNode : nodes) {
			indexValues.add(indexNode.evaluate(debugger, scope));
		}
		return indexValues;
	}

	@Override
	public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
		debugger.stepStart(this, scope);
		ReflexValue value = expression.evaluate(debugger, scope);
		if (indexes.size() == 1) {
			value = getNewValueByIndex(value, getValuesFromList(indexes.get(0), debugger, scope));
		} else {
			List<List<ReflexValue>> indexValues = new ArrayList<>(indexes.size());

			for (List<ReflexNode> nodes : indexes) {
				indexValues.add(getValuesFromList(nodes, debugger, scope));
			}

			for (List<ReflexValue> index : indexValues) {

				value = getNewValueByIndex(value, index);
			}
		}

		debugger.stepEnd(this, value, scope);
		return value;
	}

	private ReflexValue getNewValueByIndex(ReflexValue value, List<ReflexValue> index) {
		if (value.isSparseMatrix()) {
			// TODO: Alan Reflex SparseMatrix
			ReflexSparseMatrixValue mValue = value.asMatrix();
			// The index values should be single values and there should be two of them
			if (index.size() != 2) {
				System.out.println("ERROR sparse index dim, what to do...?");
			}
			ReflexValue rowIndex = index.get(0);
			ReflexValue columnIndex = index.get(1);
			return mValue.get(rowIndex, columnIndex);
		} else {
			ReflexValue realIndex = index.get(0);
			if (value.isMap()) {
				Object v = value.asMap().get(realIndex.asString());
				if (v instanceof ReflexValue) {
					v = ((ReflexValue) v).asObject(); // Don't nest ReflexValues here
				}
                value = (v == null) ? new ReflexUndefinedValue(lineNumber) : new ReflexValue(lineNumber, v);
			} else {
				try {
                    int idx = realIndex.asLong().intValue();

                    if (value.isList()) {
                    	value = value.asList().get(idx);
                    } else if (value.isString()) {
                        value = new ReflexValue(lineNumber, String.valueOf(value.asString().charAt(idx)));
                    }
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    value = new ReflexUndefinedValue(lineNumber);
                }
			}
		}
		return value;
	}
}
