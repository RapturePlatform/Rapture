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
package com.incapture.apigen;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.incapture.apigen.slate.function.Function;
import com.incapture.apigen.slate.function.Parameter;

/**
 * @author bardhi
 * @since 6/3/15.
 */
public class SampleCodeFactoryTest {

    @Test
    public void testSampleCodeForJava() throws Exception {
        Function function = createGetDoc();
        assertEquals("HttpDocApi docApi = new HttpDocApi(loginApi);\n"
                + "SomeType retVal = docApi.getDoc(documentURI, isFlag);", SampleCodeFactory.INSTANCE.sampleCodeForJava("doc", function));
    }

    private Function createGetDoc() {
        Function function = new Function();

        function.setName("getDoc");
        function.setReturnType("SomeType", "desc");
        Parameter param1 = new Parameter();
        param1.setName("documentURI");
        param1.setDocumentation("this is the first param");
        param1.setType("String");
        param1.setPackageName("rapture.common");
        Parameter param2 = new Parameter();
        param2.setName("isFlag");
        param2.setDocumentation("some doc for param2");
        param2.setType("boolean");
        param2.setPackageName("rapture.common.other");
        function.setParameters(Arrays.asList(param1, param2));
        return function;
    }

    @Test
    public void testSampleCodeForPython() throws Exception {
        Function function = createGetDoc();
        assertEquals("retVal = baseAPI.doDoc_GetDoc(documentURI, isFlag);", SampleCodeFactory.INSTANCE.sampleCodeForPython("doc", function));

    }
}