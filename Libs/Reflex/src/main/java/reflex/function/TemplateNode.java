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
package reflex.function;

import java.util.Map;

import org.stringtemplate.v4.ST;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.KernelExecutor;
import reflex.node.ReflexNode;
import reflex.value.ReflexValue;

/**
 * Take a string template and a set of parameters, and call string template on
 * the former with the latter.
 * 
 * @author amkimian
 * 
 */
public class TemplateNode extends BaseNode {

    private ReflexNode templateNode;
    private ReflexNode paramsNode;

    public TemplateNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode templateNode, ReflexNode paramsNode) {
        super(lineNumber, handler, s);
        this.templateNode = templateNode;
        this.paramsNode = paramsNode;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);

        ReflexValue template = templateNode.evaluate(debugger, scope);
        ReflexValue params = paramsNode.evaluate(debugger, scope);

        if (template.isString() && params.isMap()) {
            ST t = new ST(template.asString());
            Map<String, Object> paramMap = KernelExecutor.convert(params.asMap());
            for (Map.Entry<String, Object> param : paramMap.entrySet()) {
                String key = param.getKey();
                Object value = param.getValue();
                t.add(key, value.toString());
            }
            String ret = t.render();
            ReflexValue retVal = new ReflexValue(ret);
            debugger.stepEnd(this, retVal, scope);
            return retVal;
        } else {
            throw new ReflexException(lineNumber, "Need a string and a param map");
        }
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("template(%s,%s)", templateNode, paramsNode);
    }
}
