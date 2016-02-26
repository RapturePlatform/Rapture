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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import reflex.IReflexHandler;
import reflex.IReflexLineCallback;
import reflex.ReflexException;
import reflex.Scope;
import reflex.ThreadSafeScope;
import reflex.debug.IReflexDebugger;
import reflex.node.parallel.BlockEvaluator;
import reflex.node.parallel.CountingThreadPoolExecutor;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * Run the for loop in parallel
 * 
 * @author amkimian
 * 
 */
public class PForInStatementNode extends BaseNode {

    private String identifier;
    private ReflexNode listExpr;
    private ReflexNode block;

    public PForInStatementNode(int lineNumber, IReflexHandler handler, Scope s, String id, ReflexNode list, ReflexNode bl) {
        super(lineNumber, handler, s);
        identifier = id;
        listExpr = list;
        block = bl;
    }

    @Override
    public ReflexValue evaluate(final IReflexDebugger debugger, final Scope scope) {
        debugger.stepStart(this, scope);
        // listExpr must evaluate to a list or to a file
        ReflexValue list = listExpr.evaluate(debugger, scope);

        if (list.isList()) {
            CountingThreadPoolExecutor parallel = new CountingThreadPoolExecutor(10, 10, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
            int submitted = 0;
            List<?> vals = list.asList();
            for (Object v : vals) {
                ThreadSafeScope newScope = new ThreadSafeScope(scope);

                if (v instanceof ReflexValue) {
                    newScope.assign(identifier, (ReflexValue) v);
                } else {
                    newScope.assign(identifier, v == null ? new ReflexNullValue(lineNumber) : new ReflexValue(v));
                }

                BlockEvaluator be = new BlockEvaluator(block, newScope);
                submitted++;
                parallel.execute(be);
            }
            // Now wait for it to finish
            parallel.waitForExecCount(submitted);
            parallel.shutdown();
        } else if (list.isFile()) {
            // Pretend a list here
            ReflexValue retVal = handler.getIOHandler().forEachLine(list.asFile(), new IReflexLineCallback() {

                @Override
                public ReflexValue callback(String line) {
                    ReflexValue v = new ReflexValue(line);
                    scope.assign(identifier, v);
                    ReflexValue returnValue = block.evaluate(debugger, scope);
                    return returnValue;
                }

            });
            debugger.stepEnd(this, retVal, scope);
            return retVal;
        } else {
            throw new ReflexException(lineNumber, "The rhs of a for-in clause must be a list, we have a " + list.getTypeAsString());
        }

        return new ReflexVoidValue();
    }
}
