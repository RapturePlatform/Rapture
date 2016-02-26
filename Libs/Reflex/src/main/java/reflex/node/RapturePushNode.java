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

import java.util.List;
import java.util.Map;
import java.util.Set;

import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

import com.google.common.collect.Table.Cell;

public class RapturePushNode extends BaseNode {

	private ReflexNode uri;
	private ReflexNode value;
	private ReflexNode options;

	public RapturePushNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode uri, ReflexNode value, ReflexNode options) {
		super(lineNumber, handler, s);
		this.uri = uri;
		this.value = value;
		this.options = options;
	}

	@Override
	public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

		debugger.stepStart(this, scope);
		ReflexValue uriVal = uri.evaluate(debugger, scope);
		ReflexValue val = value.evaluate(debugger, scope);
		ReflexValue retVal = new ReflexVoidValue(lineNumber);
		RaptureURI uri = new RaptureURI(uriVal.asString());
		switch (uri.getScheme()) {
		case DOCUMENT:
			// retVal is a map, push the content
			handler.getApi().getDoc().putDoc(uriVal.asString(), JacksonUtil.jsonFromObject(val.asMap()));
		case SERIES:
			// Return all the elements of the series as a sparse matrix
			putSeriesMatrix(uriVal.asString(), val);
			break;
		case SHEET:
			// Return all of the cell values as a sparse matrix
			putSheetMatrix(uriVal.asString(), val, options != null ? options.evaluate(debugger, scope) : null);
		default:
			break;
		}

		debugger.stepEnd(this, retVal, scope);
		return retVal;
	}

	@SuppressWarnings("unchecked")
    private void putSheetMatrix(String displayName, ReflexValue value, ReflexValue optionsVal) {
	    // Need to handle the ordering here of rows and columns. Ideally first
	    // come first out
	    // Also, if the value has a [attr] in front of it, split that out - that's
	    // style.
	    // Do we really want row/column indexes, or ordered indexes and a start point?
	    
	    int startRow = 0;
	    int startColumn = 0;
		ReflexSparseMatrixValue smv = value.asMatrix();
		List<ReflexValue> columnOrder = smv.getColumnOrder();
		if (optionsVal != null) {
		    if (optionsVal.isMap()) {
		        Map<String, Object> v = optionsVal.asMap();
		        if (v.containsKey("columns")) {
		            Object x = v.get("columns");
		            columnOrder = (List<ReflexValue>) x;
		        }
		    }
		}
		for(ReflexValue row : smv.getRowOrder()) {
		    for(ReflexValue col : columnOrder) {
		        ReflexValue v = smv.get(row, col);
		        String val = v.asString();
		        if (val.startsWith("[")) {
		            int nextIndex = val.indexOf(']');
		            String style = val.substring(1,nextIndex);
		            System.out.println("Style is " + style);
		            val = val.substring(nextIndex+1);
	                handler.getApi().getSheet().setSheetCell(displayName, startRow, startColumn, style, 1);
		        }
		        handler.getApi().getSheet().setSheetCell(displayName, startRow, startColumn, val, 0);
		        startColumn++;
		    }
		    startColumn = 0;
		    startRow++;
		}
	}

	private void putSeriesMatrix(String displayName, ReflexValue value) {
		ReflexSparseMatrixValue smv = value.asMatrix();
		Set<Cell<ReflexValue, ReflexValue, ReflexValue>> cells = smv.getCells();
		for(Cell<ReflexValue, ReflexValue, ReflexValue> c : cells) {
			String col = c.getColumnKey().asString();
			// TODO: Series what about more complex types?
			String cellValue = c.getValue().asString();
			handler.getApi().getSeries().addStringToSeries(displayName, col, cellValue);
		}

	}

	@Override
	public String toString() {
		return String.format("rpush(%s,%s)", uri, value);
	}
}
