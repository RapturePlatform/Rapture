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
package rapture.kernel.index;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class IndexDocumentTest {
	   private static CallingContext ctx = ContextFactory.getKernelUser();

	    @AfterClass
	    public static void cleanUp() {
            Kernel.getDoc().deleteDocRepo(ctx, "//docTest");
            Kernel.getIndex().deleteIndex(ctx, "//docTest");
	    }

	    @BeforeClass
	    public static void setup() {
	        System.setProperty("LOGSTASH-ISENABLED", "false");
	        Kernel.initBootstrap();
	        if (!Kernel.getDoc().docRepoExists(ctx, "//docTest"))
	            Kernel.getDoc().createDocRepo(ctx, "//docTest", "NREP {} USING MEMORY {}");
            Kernel.getIndex().createIndex(ctx, "//docTest", "field1($0) string, field2(test) string");
	    }
	    
	    @Test
	    public void writeADoc() {
	    	String doc = "//docTest/one/two";
	    	String content = "{ \"test\" : \"hello\" }";
	    	Kernel.getDoc().putDoc(ctx, doc, content);
	    }
	    
	    
}
