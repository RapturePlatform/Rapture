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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.debug.NullDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class SwitchNode extends BaseNode {
    private static Logger log = Logger.getLogger(SwitchNode.class);
    private static final IReflexDebugger debugger = new NullDebugger();
	private ReflexNode switchValue = null;
	private Map<ReflexValue, ReflexNode> cases = new LinkedHashMap<>();
	
    public ReflexNode getSwitchValue() {
		return switchValue;
	}

	public void setSwitchValue(ReflexNode switchValue) {
		this.switchValue = switchValue;
	}

	public SwitchNode(int lineNumber, IReflexHandler handler, Scope scope) {
        super(lineNumber, handler, scope);
    }
	
	public void addCase(ReflexNode value, ReflexNode block) {
		if (block == null) 
			throw new ReflexException(this.getLineNumber(), "+++ BLOCK IS NULL");
		ReflexValue rValue = (value == null) ? null : value.evaluate(debugger, getScope());
		ReflexNode oldblock = cases.put(rValue, block);
		if (oldblock != null) {
			if (value == null) {
				throw new ReflexException(this.getLineNumber(), "+++ SWITCH MAY ONLY HAVE ONE DEFAULT");
			} else {
				throw new ReflexException(this.getLineNumber(), "+++ DUPLICATE CASE "+value);
			}
		}
	}

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue ret = new ReflexVoidValue();
        
        // First off, what's the switch value?
        if (switchValue == null) {
        	throw new ReflexException(this.getLineNumber(), "+++ DIVIDE BY CUCUMBER ERROR. REINSTALL UNIVERSE AND REBOOT +++");
        }
        ReflexValue switchVal = switchValue.evaluate(debugger, scope);        
        BlockNode action = null;
        
        for (Entry<ReflexValue, ReflexNode> entry : cases.entrySet()) {
        	ReflexValue node = entry.getKey();
        	if (node != null) {
	        	if (switchVal.equals(node)) {
	        		log.debug("+++ MATCH "+node+" ");
	        		action = (BlockNode)entry.getValue();
	        		break;
	        	}
        	}
        }
        if (action == null) {
        	log.debug("+++ DEFAULT CASE ");
        	action = (BlockNode) cases.get(null);
        }
        
        // execute block here
        if (action != null) {
        	ret = action.evaluate(debugger, scope);
        } else {
        	log.warn("Warning: Switch had no matches and no default");
        }

        debugger.stepEnd(this, ret, scope);
        return ret;
    }
}
