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

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexSparseMatrixValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class TransposeNode extends BaseNode {

	private ReflexNode matrix;

	public TransposeNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode matrix) {
		super(lineNumber, handler, s);
		this.matrix = matrix;
	}

	@Override
	public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

		debugger.stepStart(this, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);
		ReflexValue matrixVal = matrix.evaluate(debugger, scope);
		if (matrixVal.isSparseMatrix()) {
		    ReflexSparseMatrixValue smv = matrixVal.asMatrix().transpose();
		    retVal = new ReflexValue(smv);
		}

		debugger.stepEnd(this, retVal, scope);
		return retVal;
	}


	@Override
	public String toString() {
		return String.format("transpose(%s)", matrix);
	}
}
