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
package rapture.common.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.version.ApiVersion;

public class LoginLoginDataTest {

    @Test
    public void testJackson() {
        String params = "{\"user\":\"rapture\",\"digest\":\"12345\",\"context\":\"afejop-34024-fefw-123-12ref23\"}";

        LoginLoginData data = JacksonUtil.objectFromJson(params, LoginLoginData.class);
        assertNotNull(data);
    }

    @Test
    public void testJacksonClientApiVersion() {
        String params = "{\"user\":\"rapture\",\"digest\":\"12345\",\"context\":\"afejop-34024-fefw-123-12ref23\",\"clientApiVersion\": {\"major\": 1, \"minor\": 3, \"micro\": 2}}";

        LoginLoginData data = JacksonUtil.objectFromJson(params, LoginLoginData.class);
        ApiVersion clientApiVersion = data.getClientApiVersion();
        assertEquals(1, clientApiVersion.getMajor());
        assertEquals(3, clientApiVersion.getMinor());
        assertEquals(2, clientApiVersion.getMicro());
    }

    @Test
    public void testJacksonExtraFieldFail() {
        String params = "{\"user\":\"rapture\",\"digest\":\"12345\",\"context\":\"afejop-34024-fefw-123-12ref23\", \"test\": 0}";
        try {
            JacksonUtil.objectFromJson(params, LoginLoginData.class);
            fail("should have failed on extra field");
        } catch (Exception ignore) {
        }
    }
}
