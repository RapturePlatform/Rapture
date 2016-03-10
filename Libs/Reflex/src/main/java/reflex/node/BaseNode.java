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

import rapture.common.RaptureConstants;

import org.apache.log4j.Logger;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public abstract class BaseNode implements ReflexNode {

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lineNumber;
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseNode other = (BaseNode) obj;
		if (lineNumber != other.lineNumber)
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		return true;
	}

	private static final Logger log = Logger.getLogger(BaseNode.class);
    protected static boolean disableAudit = true;
    protected String nodeId;
    protected int lineNumber;
    protected IReflexHandler handler;
    private Scope scope;

    public BaseNode(int lineNumber, IReflexHandler handler, Scope scope) {
        this.lineNumber = lineNumber;
        this.handler = handler;
        this.scope = scope;
        if (handler != null) {
            this.nodeId = handler.getSuspendHandler().getNewNodeId();
        } else {
            this.nodeId = "x";
        }
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public ReflexValue evaluateWithoutScope(IReflexDebugger debugger) {
        return evaluate(debugger, this.scope);
    }

    @Override
    public ReflexValue evaluateWithResume(IReflexDebugger debugger, Scope scope) {
        // We need to skip to the resume point, then evaluate from that point
        // For nodes that don't care, we will just skip
        return new ReflexVoidValue();
    }

    protected ReflexValue writeAudit(IReflexDebugger debugger, String comment) {
        if (disableAudit) return new ReflexVoidValue();
        // scope.setPendingComment(comment);
        if (handler.getApi() == null) {
            String errorMessage = "API is null - " + comment;
            log.error(errorMessage);
            System.err.println(errorMessage);
            return new ReflexVoidValue();
        }
        debugger.stepStart(this, scope);
        handler.getApi().getAudit().writeAuditEntry(RaptureConstants.DEFAULT_AUDIT_URI, "reflex", 0, comment);
        ReflexValue reflexRetVal = new ReflexVoidValue();
        debugger.stepEnd(this, reflexRetVal, scope);
        return reflexRetVal;
    }

    protected void throwError(String msg, ReflexNode lhs, ReflexNode rhs, ReflexValue a, ReflexValue b) {
        String errorMessage = "Illegal arguments to expression - " + this + ", " + msg + twoValString(lhs, rhs, a, b);
        throw new ReflexException(lineNumber, errorMessage);
    }

    public String toString() {
        return "";
    }

    public String twoValString(ReflexNode lhs, ReflexNode rhs, ReflexValue a, ReflexValue b) {
        return "; " + lhs.toString() + " is " + a.getTypeAsString() + " and " + rhs.toString() + " is " + b.getTypeAsString();
    }
}
