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
package reflex.function;

import java.math.BigDecimal;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.structure.Structure;
import reflex.util.function.LanguageRegistry;
import reflex.util.function.StructureFactory;
import reflex.util.function.StructureKey;
import reflex.value.ReflexStructValue;
import reflex.value.ReflexValue;

/**
 * Cast one value type to another
 * 
 * @author amkimian
 * 
 */
public class CastNode extends BaseNode {

    private static final String BOOL = "bool";
    private static final String STRING = "string";
    private static final String NUMBER = "number";
    private static final String INTEGER = "integer";
    private ReflexNode source;
    private ReflexNode targetType;
    private LanguageRegistry registry;
    private String namespacePrefix;

    public CastNode(int lineNumber, IReflexHandler handler, Scope scope, ReflexNode src, ReflexNode targetType, LanguageRegistry registry, String namespacePrefix) {
        super(lineNumber, handler, scope);
        this.source = src;
        this.targetType = targetType;
        this.registry = registry;
        this.namespacePrefix = namespacePrefix;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue targType = targetType.evaluate(debugger, scope);
        ReflexValue src = source.evaluate(debugger, scope);
        ReflexValue retVal = src;
        if (targType.isString()) {
            
            // Currently we support casting to a number or a string
            String targ = targType.asString();
            if (targ.equalsIgnoreCase(NUMBER)) {
                if (src.isNumber()) {
                    retVal = new ReflexValue(src);
                } else if (src.isDate()) {
                    retVal = new ReflexValue(src.asDate().getEpoch());
                } else if (src.isTime()) {
                    retVal = new ReflexValue(src.asTime().getEpoch());
                } else {
                    String strVal = src.toString();
                    if (strVal.equals("NULL")) {
                        retVal = new ReflexValue(0.0);
                    } else {
                        retVal = new ReflexValue(new BigDecimal((strVal.startsWith("'") ? strVal.substring(1) : strVal)));
                    }
                }
            } else if (targ.equalsIgnoreCase(INTEGER)) {
            	if (src.isNumber()) {
            		retVal = new ReflexValue(src.asInt());
                } else {
                    String strVal = src.toString();
                    if (strVal.equals("NULL")) {
                        retVal = new ReflexValue(0);
                    } else {
                        retVal = new ReflexValue(Integer.valueOf((strVal.startsWith("'") ? strVal.substring(1) : strVal)));
                    }
                }
            } else if (targ.equalsIgnoreCase(STRING)) {
                if (src.isString()) {
                    retVal = new ReflexValue(src);
                } else {
                    retVal = new ReflexValue(src.toString());
                }
            } else if (targ.equalsIgnoreCase(BOOL)) {
                if (src.isBoolean()) {
                    retVal = new ReflexValue(src);
                } else {
                    retVal = new ReflexValue(Boolean.valueOf(src.toString()));
                }
            } else  {
                StructureKey key = namespacePrefix == null ? StructureFactory.createStructureKey(targ) : StructureFactory.createStructureKey(namespacePrefix, targ);
                
                Structure s = registry.getStructure(key);
                if (s != null) {
                    ReflexStructValue sVal = new ReflexStructValue();
                    sVal.setStructure(s);
                    sVal.attemptAssignFrom(src, false);
                    retVal = new ReflexValue(sVal);
                }
                
            }
        } else {
            throw new ReflexException(lineNumber, "Cannot cast without a string as the 2nd parameter");
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("cast(%s,%s)", source, targetType);
    }
}
