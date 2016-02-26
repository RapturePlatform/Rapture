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

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.ReflexExecutor;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class IdentifierNode extends BaseNode {

    private String identifier;
    private String namespacePrefix;

    public IdentifierNode(int lineNumber, IReflexHandler handler, Scope scope, String id, String namespacePrefix) {
        super(lineNumber, handler, scope);
        identifier = id;
        this.namespacePrefix = namespacePrefix;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        // RAP-2872
        // First attempt to resolve the usual way (fast fix)
        // If not around, try to search from the first dotted part for a variable until we get
        // a resolution. If we resolve to something and that variable type is a map, use the remaining
        // part to resolve to a value using map lookup semantics.

        ReflexValue value = scope.resolve(identifier, namespacePrefix);
        if (value == null) {
            value = resolveMapDotter(scope);
            if (value == null) {
                value = new ReflexNullValue(lineNumber);
            }
        }
        debugger.stepEnd(this, value, scope);
        return value;
    }

    @Override
    public String toString() {
        return identifier;
    }

    private ReflexValue resolveMapDotter(Scope scope) {
        String[] allParts = identifier.split("\\.");
        StringBuilder currentResolve = new StringBuilder();
        for (int pos = 0; pos < allParts.length; pos++) {
            if (currentResolve.length() != 0) {
                currentResolve.append(".");
            }
            currentResolve.append(allParts[pos]);
            ReflexValue value = scope.resolve(currentResolve.toString(), namespacePrefix);
            if (value == null) {
                throw new ReflexException(lineNumber, "no such variable: " + currentResolve.toString());
            } else if (value.isMap()) {
                ReflexValue innerVal = value;
                int ender = pos + 1;
                for (; ender < allParts.length; ender++) {
                    if (!innerVal.asMap().containsKey(allParts[ender])) {
                        innerVal = null;
                        break;
                    }
                    Object v = innerVal.asMap().get(allParts[ender]);
                    innerVal = (v == null) 
                            ? new ReflexNullValue(lineNumber) 
                            : (v instanceof ReflexValue) 
                                ? (ReflexValue) v 
                                : new ReflexValue(v);
                    if (!innerVal.isMap()) break;
                }
                if (ender < allParts.length - 1) throw new ReflexException(lineNumber, "no such variable: " + this);
                return innerVal;
            }
        }
        return null;
    }
}
