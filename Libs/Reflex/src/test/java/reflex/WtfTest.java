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
package reflex;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.junit.Test;

public class WtfTest {
	
	@Test
	public void testSimple() throws Exception {
		testWith("Simple Assignment", "x=1;");
	}
	
	@Test
	public void testDef() throws Exception {
		testWith("Func Call", "def x()\n" + 
	                              "end\n" + 
	                              "x();");
	}
	
	@Test
	public void testPrintln() throws Exception {
		testWith("println", "println('hello');");
	}
	
	@Test
	public void testDebug() throws Exception {
		testWith("debug", "debug('hello');");
	}

	
	 public void testWith(String name, final String script) throws Exception {
	        long time1 = runWith(script, 1, 1000);
	        long time2 = runWith(script, 10, 100);
	        
	        System.out.println("Run of " + name + " - (1,1000) " + time1 + " (10,100) " + time2);

	 }
	 
	 public long runWith(final String script, int numThreads, final int runsPerThread) throws InterruptedException {
	        //Thread.sleep(10000);
	        //System.out.println("Starting");
	        List<Thread> threads = new ArrayList<Thread>();
	        long before = System.currentTimeMillis();
	        for (int j = 0; j < numThreads; j++) {
	            Thread t = new Thread() {
	                public void run() {
	                    for (int i = 0; i < runsPerThread; i++) {
	                        //String script = "x=1;";
	                        //String script = "def x()\n" + 
	                        //      "end\n" + 
	                        //      "x();";   
	                        //String script = "println(x);";
	                        
	                        ReflexLexer lexer = new ReflexLexer(new ANTLRStringStream(script));
	                        CommonTokenStream tokens = new CommonTokenStream(lexer);
	                        ReflexParser parser = new ReflexParser(tokens);
	                        try {
	                            @SuppressWarnings("unused")
                                CommonTree tree = (CommonTree) parser.parse().getTree();
	                        } catch (RecognitionException e) {
	                            e.printStackTrace();
	                        }
	                    }
	                    //System.out.println(Thread.currentThread().getName() + " is done");
	                }
	            };
	            t.start();
	            threads.add(t);
	        }

	        for (Thread tx : threads) {
	            tx.join();
	        }
	        long time = (System.currentTimeMillis() - before);
	        return time;
	    }

}
