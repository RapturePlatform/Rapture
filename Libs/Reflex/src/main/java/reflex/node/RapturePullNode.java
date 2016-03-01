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

import java.util.HashMap;
import java.util.List;

import rapture.common.RaptureSheetCell;
import rapture.common.RaptureSheetStatus;
import rapture.common.RaptureURI;
import rapture.common.SeriesPoint;
import rapture.common.impl.jackson.JacksonUtil;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class RapturePullNode extends BaseNode {

	private ReflexNode uri;
	private ReflexNode options;

	public RapturePullNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode uri, ReflexNode options) {
		super(lineNumber, handler, s);
		this.uri = uri;
		this.options = options;
	}

	@Override
	public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

		debugger.stepStart(this, scope);
		ReflexValue uriVal = uri.evaluate(debugger, scope);
		ReflexValue optionsVal = options == null ? new ReflexValue(new HashMap<String, Object>()) : options.evaluate(
				debugger, scope);
		ReflexValue retVal = new ReflexNullValue(lineNumber);
		RaptureURI uri = new RaptureURI(uriVal.asString());
		switch (uri.getScheme()) {
		case DOCUMENT:
			// Return a document as a map
			String content = handler.getApi().getDoc().getDoc(uri.getFullPath());
			retVal = new ReflexValue(JacksonUtil.getMapFromJson(content));
		case SERIES:
			// Return all the elements of the series as a sparse matrix
			retVal = getSeriesMatrix(uriVal.asString(), optionsVal);
			break;
		default:
			break;
		}

		debugger.stepEnd(this, retVal, scope);
		return retVal;
	}

	private ReflexValue getSeriesMatrix(String displayName, ReflexValue optionsVal) {
		ReflexSparseMatrixValue smv = new ReflexSparseMatrixValue(2);
		List<SeriesPoint> values = handler.getApi().getSeries().getPoints(displayName);
		System.out.println("Series value size is " + values.size());
		for(SeriesPoint v : values) {
			smv.set(new ReflexValue(v.getColumn()), new ReflexValue(1), new ReflexValue(v.getValue()));
		}
		return new ReflexValue(smv);
	}

	@Override
	public String toString() {
		return String.format("rpull(%s,%s)", uri, options);
	}
}
