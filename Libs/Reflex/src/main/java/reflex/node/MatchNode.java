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
 * LIABILITY, WHETHER IN AN Match OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
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
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class MatchNode extends BaseNode {
    private static Logger log = Logger.getLogger(MatchNode.class);

	private Map<ReflexNode, ReflexNode> cases = new LinkedHashMap<>();
	private ReflexNode matchValue = null;
	
	public void setMatchValue(ReflexNode matchValue) {
		this.matchValue = matchValue;
	}

	public MatchNode(int lineNumber, IReflexHandler handler, Scope scope) {
        super(lineNumber, handler, scope);
    }
	
	public void addCase(ReflexNode value, ReflexNode block) {
		if (block == null) 
			throw new ReflexException(this.getLineNumber(), "+++ BLOCK IS NULL");
		ReflexNode oldblock = cases.put(value, block);
		if (oldblock != null) {
			if (value == null) {
				throw new ReflexException(this.getLineNumber(), "+++ Match MAY ONLY HAVE ONE DEFAULT");
			} else {
				throw new ReflexException(this.getLineNumber(), "+++ DUPLICATE CASE "+value);
			}
		}
	}

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        
        ReflexValue ret = matchValue.evaluate(debugger, scope);
        BlockNode match = null;
        
        for (Entry<ReflexNode, ReflexNode> entry : cases.entrySet()) {
        	ReflexNode node = entry.getKey();
        	if (node != null) {
        		ReflexValue value = entry.getKey().evaluate(debugger, scope);
        		if (value.asBoolean()) {
	        		log.debug("+++ MATCH "+value+" ");
	        		match = (BlockNode)entry.getValue();
	        		break;
	        	}
        	}
        }

        // execute block here
        if (match != null) {
        	ret = match.evaluate(debugger, scope);
        } else {
        	log.warn("Warning: Match had no matches and no default");
        }

        debugger.stepEnd(this, ret, scope);
        return ret;
    }
}
