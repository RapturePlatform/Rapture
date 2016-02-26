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
package rapture.dp.invocable;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.util.HashMap;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dp.DecisionProcessExecutorTest;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class DecisionTestInvocable extends AbstractInvocable {

    public DecisionTestInvocable(String workerURI) {
        super(workerURI);
    }

    @Override
    public String invoke(CallingContext ctx) {
        CallingContext context = ContextFactory.getKernelUser();

        String var = "varName";
        String retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), var);
        assertNull(retVal);

        Kernel.getDecision().setContextLiteral(context, getWorkerURI(), var, "value");
        retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), var);
        assertEquals("value", retVal);

        Map<String, String> testMap = new HashMap<String, String>();
        String linkDataSecond = "secnod";
        testMap.put("first", linkDataSecond);
        String linkData = JacksonUtil.jsonFromObject(testMap);
        Kernel.getDoc().putDoc(context, DecisionProcessExecutorTest.CONTEXT_LINK, linkData);

        Kernel.getDecision().setContextLiteral(context, getWorkerURI(), var, DecisionProcessExecutorTest.CONTEXT_LINK);
        retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), var);
        assertEquals(DecisionProcessExecutorTest.CONTEXT_LINK, retVal);

        Kernel.getDecision().setContextLink(context, getWorkerURI(), var, DecisionProcessExecutorTest.CONTEXT_LINK);
        retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), var);
        assertEquals(linkDataSecond, retVal);

        // now test the things that are in the "view"
        retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), DecisionProcessExecutorTest.CONST_ALIAS);
        assertEquals(DecisionProcessExecutorTest.SOME_CONSTANT, retVal);

        retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), DecisionProcessExecutorTest.LINK_IN_VIEW);
        assertEquals(linkDataSecond, retVal);

        // test literal and template markers
        retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), "#literal");
        assertEquals("literal", retVal);

        Kernel.getDecision().setContextLiteral(context, getWorkerURI(), DecisionProcessExecutorTest.CONTEXT_VARIABLE_1, "value1");
        Kernel.getDecision().setContextLiteral(context, getWorkerURI(), DecisionProcessExecutorTest.CONTEXT_VARIABLE_2, "value2");

        retVal = Kernel.getDecision().getContextValue(context, getWorkerURI(), 
                "%template/${"+DecisionProcessExecutorTest.VARIABLE_ALIAS+"}/${"+DecisionProcessExecutorTest.CONTEXT_VARIABLE_2+"}");

        assertEquals("template/value1/value2", retVal);
        
        return "next";
    }
}
