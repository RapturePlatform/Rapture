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
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * Put a document into a cache, with an optional expiry
 * 
 * @author amkimian
 * 
 */
public class PutCacheNode extends BaseNode {

    private ReflexNode variable;
    private ReflexNode cacheName;
    private ReflexNode expiry;

    public PutCacheNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode v, ReflexNode n, ReflexNode e) {
        super(lineNumber, handler, s);
        this.variable = v;
        this.cacheName = n;
        this.expiry = e;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        ReflexValue retVal = new ReflexVoidValue(lineNumber);
        debugger.stepStart(this, scope);
        ReflexValue var = variable.evaluate(debugger, scope);
        ReflexValue cache = cacheName.evaluate(debugger, scope);
        long expVal = 0L;
        if (expiry != null) {
            ReflexValue exp = expiry.evaluate(debugger, scope);
            if (exp.isNumber()) {
                expVal = exp.asLong();
            }
        }
        if (cache.isString()) {
            handler.getCacheHandler().putIntoCache(var, cache.asString(), expVal);
        } else {
            throw new ReflexException(lineNumber, "Cannot put a value into a non-string location in the cache");
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("putCache(%s,%s,%s)", variable, cacheName, expiry);
    }
}
