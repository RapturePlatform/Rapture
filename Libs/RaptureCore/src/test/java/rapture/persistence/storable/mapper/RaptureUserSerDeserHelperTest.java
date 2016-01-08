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
package rapture.persistence.storable.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.RaptureUser;
import rapture.common.model.RaptureUserStorage;

import org.junit.Test;

/**
 * @author bardhi
 * @since 7/22/15.
 */

public class RaptureUserSerDeserHelperTest {
    @Test
    public void teV1ToV2() throws Exception {
        RaptureUser user = new RaptureUser();
        String userId = "user1";
        user.setUsername(userId);
        assertEquals(userId, user.getUsername());
        assertNull(user.getUserId());

        String json = JacksonUtil.jsonFromObject(user);

        JsonContent jsonContent = new JsonContent(json);
        user = RaptureUserStorage.readFromJson(jsonContent);
        assertEquals(userId, user.getUsername());
        assertEquals(userId, user.getUserId());
    }
}
