package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;
import org.junit.Test;

public class MathsTest extends AbstractReflexScriptTest {
	private static Logger log = Logger.getLogger(MathsTest.class);

	@Test
	public void testDivision1() throws RecognitionException {
		String program = "i = 20;\n" +
			"println(i/2); \n";

		String output = runScript(program, null);
		assertEquals("10", output.trim());
	}
	
	@Test
	public void testDivision2() throws RecognitionException {
		String program = "i = 21;\n" +
			"println(i/2); \n";

		String output = runScript(program, null);
		assertEquals("10.5", output.trim());
	}
	
	@Test
	public void testplusEquals() throws RecognitionException {
		String program = "i = 10;\n" +
				"i += 2; \n" +
				"j = 10.0;\n" +
				"j += 2; \n" +
				"k = 10;\n" +
				"k += 2.00; \n" +
				"println(i+','+j+','+k); \n";

		String output = runScript(program, null);
		assertEquals("12,12.0,12.00", output.trim());
	}
		
	@Test
	public void testminusEquals() throws RecognitionException {
		String program = "i = 10;\n" +
				"i -= 2; \n" +
				"j = 10.0;\n" +
				"j -= 2; \n" +
				"k = 10;\n" +
				"k -= 2.00; \n" +
				"println(i+','+j+','+k); \n";

		String output = runScript(program, null);
		assertEquals("8,8.0,8.00", output.trim());
	}
		
	@Test
	public void testEqual() throws RecognitionException {
		String program = "i = [];\n" +
			"j = size(i); \n" +
			"println(j);  \n" +
			"if (j == 0) do \n" +
			"println(true); \n" +
			"else do \n" +
			"println(false); \n" +
			"end\n";

		String output = runScript(program, null);
		assertEquals("0\ntrue", output.trim());
	}
	
	@Test
	public void testEqualsAndAssert() throws RecognitionException {
		String program = 
		"       //Number manipulation \n" +
		"       y = 2.65; //float \n" +
		"       x = 12; //integer \n" +

		"       z = x*y; \n" +
		"       println('value of z is :' + z); \n" +
		"       println('type of z is  :' + typeof(z)); \n" +

		"       assert('its all gone dave tong',z == 31.8); \n" +
		"       assert(z == 31.80); \n" +

		"       a=12.123; \n" +
		"       b=871938; \n" +
		"       c=a*b; \n" +
		"       println('value of c is :' + c); \n" +
		"       println('type of c is  :' + typeof(c)); \n" +
		"       //10570504.374 \n" +
		"       assert(c == 10570504.374); \n";
		String[] output = runScript(program, null).split("\n");
		assertEquals("value of z is :31.80", output[0]);
		assertEquals("type of z is  :number", output[1]);
		assertEquals("value of c is :10570504.374", output[2]);
		assertEquals("type of c is  :number", output[3]);
	}
	
	@Test
	public void testListCompare() throws RecognitionException {
		String program = 
		"       a = [1 ,2 ,3 ,4 ]; \n" +
		"       b = []; \n" +
		"       for c in a do \n" +
		"           b = b + (c * 2); \n" +
		"       end \n" +
		"       assert ('Compare list of Number to list of Integers', b == [2,4,6,8]); \n" +
		"       assert ('Compare list of Number to list of BigDecimals', b == [2.0,4.00,6.000,8.0000000]); \n" +
		"       assert ('Cannot compare list of Number to list of String', b != ['2','4','6','8']); \n" +
		"       assert ('Compare list of Number to list of mixed Numbers', b == [2,4.0,6,8]); \n";
		
		String output = runScript(program, null);
	}	
	
	@Test
	public void testThePower() throws RecognitionException {
		String program = 
		"       a = 3.0 ^ 4; \n" +
		"       b = 3 ^ 4.00000; \n" +
		"       c = 256 ^ 0.25; \n" +
		"       d = 2 ^ -1; \n" +
		"       println(a); \n" +
		"       println(b); \n" +
		"       println(c); \n" +
		"       println(d); \n" +
		"       assert (a == 81); \n" +
		"       assert (b == 81); \n" +
		"       assert (c == 4); \n" +
		"       assert (d == 0.5); \n";
		
		String output = runScript(program, null);
		System.out.println(output);
	}		
}
