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

import java.util.ArrayList;
import java.util.List;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.importer.ImportHandler;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

public class SplitNode extends BaseNode {

    private ReflexNode str;
    private ReflexNode sep;
    private ReflexNode quoter;
    public SplitNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode str, ReflexNode sep, ReflexNode quoter, LanguageRegistry languageRegistry,
            ImportHandler importHandler) {
        super(lineNumber, handler, s);
        this.str = str;
        this.sep = sep;
        this.quoter = quoter;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        // A split splits on the separator but if quoter is true it is clever about quoted strings
        List<ReflexValue> r = new ArrayList<ReflexValue>();
        ReflexValue toSplitV = str.evaluate(debugger, scope);
        String toSplit = toSplitV.asString();
        ReflexValue quoterV = quoter.evaluate(debugger, scope);
        boolean shouldQuote = quoterV.asBoolean();
        ReflexValue sepV = sep.evaluate(debugger, scope);
        char sepChar = sepV.asString().charAt(0);
        char[] splitArr = toSplit.toCharArray();
        boolean quoting=false;
        StringBuilder currentVal = new StringBuilder();
        char quoteInUse = '"';
        for(char x : splitArr) {
        	if (x == sepChar && !quoting) {
        		r.add(new ReflexValue(currentVal.toString()));
        		currentVal = new StringBuilder();
        	} else if (x == '"' || x=='\'' && shouldQuote) {
        	    if (quoting && x == quoteInUse) {
        	        quoting = false;
        	    } else if (!quoting){
        	        quoting = true;
        	        quoteInUse = x;
        	    } else {
        	        currentVal.append(x);
        	    }
        	} else {
        		currentVal.append(x);
        	}
        }
        r.add(new ReflexValue(currentVal.toString()));
        ReflexValue ret = new ReflexValue(r);
        debugger.stepEnd(this, ret, scope);
        return ret;
    }

    @Override
    public String toString() {
    	return "split";
    }
}
