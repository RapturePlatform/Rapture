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

import rapture.common.RaptureRunnerConfig;
import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class PropertyPlaceholderNode extends BaseNode {

    private String identifier;

    public PropertyPlaceholderNode(int lineNumber, IReflexHandler handler, Scope scope, String id) {
        super(lineNumber, handler, scope);
        identifier = id;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
      
        // Push this data to the queue
        List<ReflexNode> params = new ArrayList<ReflexNode>();
        KernelCallNode n = new KernelCallNode(lineNumber, handler, scope, "#runner.getRunnerConfig", params);
        ReflexValue reflexValue = n.evaluate(debugger, scope);

        RaptureRunnerConfig raptureConfig = reflexValue.asObjectOfType(RaptureRunnerConfig.class);
        
        ReflexValue value = new ReflexNullValue(lineNumber);
        
        if(raptureConfig.getConfig() != null && raptureConfig.getConfig().get(identifier) != null) {
            value = new ReflexValue(raptureConfig.getConfig().get(identifier));
        } else {
            value = scope.resolve(identifier);
        }
        debugger.stepEnd(this, value, scope);
        return value;
    }

    @Override
    public String toString() {
        return identifier;
    }
}
