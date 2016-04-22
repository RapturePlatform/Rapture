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
package rapture.dp;

import org.custommonkey.xmlunit.XMLUnit;

import biz.c24.io.api.C24;
import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;

/**
 * Test class with referenecs to external jars c24 and xmlunit for ClassLoader test purposes in the RaptureCore project. See class WorkflowClassLoaderTest in
 * RaptureCore
 * 
 * @author dukenguyen
 *
 */
public class ClassLoaderTest1 extends AbstractInvocable<Object> {

    public ClassLoaderTest1(String workerURI, String stepName) {
        super(workerURI, stepName);
    }

    public static int x1;

    @Override
    public String invoke(CallingContext ctx) {
        try {
            XMLUnit.buildControlDocument("<x></x>");
        } catch (Exception e) {
            e.printStackTrace();
        }

        C24.init();

        return String.valueOf(++x1);
    }
}
