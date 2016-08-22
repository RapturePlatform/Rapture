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
package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

/**
 * Some tests around Syntax Errors - do we get enough information to inform the
 * user?
 * 
 * @author amkimian
 * 
 */
public class SyntaxErrorChecksTest extends AbstractReflexScriptTest {

    @Test
    public void simpleError() {
        try {
            ReflexExecutor.runReflexProgram("var x;");
            fail("Should have thrown an exception");
        } catch (ReflexException e) {
            assertEquals("Unexpected identifier at token var  at line 1\n    1: var x;\n-------^^^^\n", e.getCause().getMessage());
        }
    }

    @Test
    public void testLTError() {
        try {
            ReflexExecutor.runReflexProgram("x = '12';\ny=1;\nz=x<y;println('Z is ' + z);\n");
            fail("Should have thrown an exception");
        } catch (ReflexException e) {
            assertEquals("Illegal arguments to expression - (x < y), both must be of same type: numeric, date, time or string; x is string and y is integer", e.getMessage());
        }
    }

    @Test
    public void testWalkError() {
        try {
            ReflexExecutor.runReflexProgram("x = '12';\ny=1;\nz=x-y;println('Z is ' + z);\n");
            fail("Should have thrown an exception");
        } catch (ReflexException e) {
            assertEquals("Illegal arguments to expression - (x - y), both sides must be numeric or the left side must be a list; x is string and y is integer", e.getMessage());
        }
    }

    @Test
    public void testRUI434() {
        try {
            ReflexExecutor.runReflexProgram("x = '12';\nprintln(x.y);\n");
            fail("Should have thrown an exception");
        } catch (ReflexException e) {
            assertEquals("no such variable: x.y", e.getMessage());
        }
    }

    @Test
    public void testRAP3895() throws RecognitionException, UnsupportedEncodingException {
    	String program = "x = 'abc';\n"+
    			"y1 = \"abc\";\n"+
    			"println(x == y1);\n"+
    			"y2 = \u201Cabc\u201D;\n"+
    			"println(x == y2);\n"+
    			"y3 = \u201Dabc\u201C;\n"+
    			"println(x == y3);\n"+
    			"y4 = \u201Dabc\";\n"+
    			"println(x == y4);\n";
		String output = runScript(program, null);
		assertEquals("true\ntrue\ntrue\ntrue", output.trim());
	}

    @Test
    public void testNonTerminatedString1() throws RecognitionException {
    	String program = "x = 'abc';\n"+
    			"y1 = \"xyz\"+\"abc;\n"+
    			"println(x == y1);\n"+
    			"";
		String output = this.runScriptCatchingExceptions(program, null);
		System.out.println(output);
		String split[] = output.split("\n");
        assertEquals("Found newline in string abc; at token \" at line 2", split[2]);
	}

    @Test
    public void testNonTerminatedString2() throws RecognitionException {
    	String program = "x = 'abc';\n"+
    			"y1 = 'xyz'+'abc;\n"+
    			"println(x == y1);\n"+
    			"";
		String output = this.runScriptCatchingExceptions(program, null);
		System.out.println(output);
		String split[] = output.split("\n");
        assertEquals("Found newline in string abc; at token ' at line 2", split[2]);
	}

    @Test
    public void testNonTerminatedString3() throws RecognitionException {
    	String program = "x = 'abc';\n"+
    			"y1 = \u201Dabc;\n"+
    			"println(x == y1);\n"+
    			"";
		String output = this.runScriptCatchingExceptions(program, null);
		System.out.println(output);
		String split[] = output.split("\n");
        assertEquals("Found newline in string abc; at token \u201D at line 2", split[2]);
	}

    @Test
    public void testNonTerminatedString4() throws RecognitionException {
    	String program = "x = 'abc';\n"+
    			"y1 = \u201Cabc;\n"+
    			"println(x == y1);\n"+
    			"";
		String output = this.runScriptCatchingExceptions(program, null);
		System.out.println(output);
		String split[] = output.split("\n");
        assertEquals("Found newline in string abc; at token \u201C at line 2", split[2]);
	}
}
