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
package rapture.json;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import rapture.util.JsonToMap;

@SuppressWarnings("deprecation")
public class JsonConvertTest {
	@Test
	public void testSimpleMap() {
		Map<String, Object> res = JsonToMap.convert("{ \"alan\" : 5 }");
		Assert.assertTrue(res.get("alan").equals(Integer.valueOf(5)));
	}
	
	@Test
	public void testComplexMap() {
	    Map<String, Object> mappedContent = JsonToMap.convert("{\"questionURI\":\"question://sys.questions/Callback_1391039327563_0.35279632\","
            + "\"mapping\":{},"
            + "\"progress\":\"UNSTARTED\","
            + "\"qtemplateURI\":\"qtemplate://1818186894/925484197\"}");
	    Assert.assertNotNull(mappedContent);
	}
}
