/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the \"Software\"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package reflex;

import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.RecognitionException;
import org.junit.Assert;
import org.junit.Test;

import rapture.common.impl.jackson.JacksonUtil;

public class JsonTest extends ResourceBasedTest {

    @Test
    public void testJson() throws RecognitionException {
        String result = runTestFor("/json.rfx");
        Assert.assertEquals("{\n  \"A\" : 1000000,\n" + "  \"B\" : 100000,\n" + "  \"C\" : 10000,\n" + "  \"D\" : 1000,\n" + "  \"E\" : 100,\n"
                + "  \"F\" : 10,\n" + "  \"G\" : 1,\n" + "  \"H\" : 0.1,\n" + "  \"I\" : 0.01,\n" + "  \"J\" : 0.001,\n" + "  \"K\" : 0.0001,\n"
                + "  \"L\" : 0.00001,\n" + "  \"M\" : 0.000001,\n" + "  \"N\" : 0.0000001\n" + "}\n--RETURNS--true", result);
    }

    @Test
    public void testJsonPerformance() {
        Map<String, Double> map = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            map.put("Val" + i, Math.random());
        }

        Long start = System.currentTimeMillis();
        String s1 = JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(map));
        Long mid = System.currentTimeMillis();
        String s2 = JacksonUtil.jsonFromObject(map, true);
        Long end = System.currentTimeMillis();
        String s3 = JacksonUtil.jsonFromObject(map, false);
        Long unformatted = System.currentTimeMillis() - end;

        Long oldWay = (mid - start);
        Long newWay = (end - mid);
        Assert.assertEquals(s1, s2);
        Assert.assertTrue(newWay < oldWay);
        System.out.println("Old way " + oldWay + "ms new way " + newWay + "ms Unformatted " + unformatted + "ms");

        System.out.println("New way is " + oldWay / newWay + " times faster");
    }

}
