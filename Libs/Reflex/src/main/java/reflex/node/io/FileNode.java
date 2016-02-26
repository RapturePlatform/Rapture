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
package reflex.node.io;

import java.util.List;

import rapture.common.BlobContainer;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexFileValue;
import reflex.value.ReflexStringStreamValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class FileNode extends BaseNode {

	private ReflexNode fileNameExpr;
	private ReflexNode fileType;
	private ReflexNode fileTypeExpr1;
	private ReflexNode fileTypeExpr2;
	private ReflexNode encodingNode;

	public FileNode(int lineNumber, IReflexHandler handler, Scope s, List<ReflexNode> fileNameParams) {
		super(lineNumber, handler, s);
		this.fileNameExpr = fileNameParams.get(0);
		if (fileNameParams.size() > 1) {
			fileType = fileNameParams.get(1);
		} else {
			fileType = null;
		}
		if (fileNameParams.size() > 2) {
			fileTypeExpr1 = fileNameParams.get(2);
		} else {
			fileTypeExpr1 = null;
		}
		if (fileNameParams.size() > 3) {
			fileTypeExpr2 = fileNameParams.get(3);
		} else {
			fileTypeExpr2 = null;
		}
		if (fileNameParams.size() > 4) {
			encodingNode = fileNameParams.get(4);
		} else {
			encodingNode = null;
		}
	}

	@Override
	public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
		debugger.stepStart(this, scope);
		// Check on whether the value is a blob or a file
		String name = fileNameExpr.evaluate(debugger, scope).asString();
		ReflexValue ret = new ReflexNullValue();
		if (!name.startsWith("blob://")) {
			ReflexFileValue fVal = new ReflexFileValue(name);
			ret = new ReflexValue(fVal);
		} else {
			BlobContainer container = handler.getApi().getBlob().getBlob(name);
			ReflexStringStreamValue sVal = new ReflexStringStreamValue();
			sVal.setContent(new String(container.getContent()));
			ret = new ReflexValue(sVal);
		}
		if (encodingNode != null) {
			ret.asFile().setEncoding(encodingNode.evaluate(debugger, scope).asString());
		}
		if (fileType != null) {
			ReflexValue fileTypeValue = fileType.evaluate(debugger, scope);
			ReflexValue fileTypeValue1 = fileTypeExpr1 != null ? fileTypeExpr1.evaluate(debugger, scope) : new ReflexNullValue();
			ReflexValue fileTypeValue2 = fileTypeExpr2 != null ? fileTypeExpr2.evaluate(debugger, scope) : new ReflexNullValue();

			if (!fileTypeValue.isNull()) {
				ret.asStream().setFileType(fileTypeValue, fileTypeValue1.isNull() ? null : fileTypeValue1.asString(), fileTypeValue2.isNull() ? null : fileTypeValue2.asString());
			}
		}
		debugger.stepEnd(this, ret, scope);
		return ret;
	}

	@Override
	public String toString() {
		return String.format("file(%s)", fileNameExpr);
	}
}
