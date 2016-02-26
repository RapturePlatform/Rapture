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
package rapture.object.storage;

import static org.junit.Assert.assertEquals;

import rapture.common.ActivityPathBuilder;
import rapture.common.CallingContextPathBuilder;
import rapture.common.RaptureConstants;
import rapture.common.RaptureScriptPathBuilder;

import org.junit.Test;

public class StorageRepoNamesTest {

    @Test
    public void test1() {
        String auth1 = RaptureScriptPathBuilder.getRepoName();
        assertEquals(RaptureConstants.CONFIG_REPO, auth1);
    }

    @Test
    public void test2() {
        String auth2 = CallingContextPathBuilder.getRepoName();
        assertEquals(RaptureConstants.EPHEMERAL_REPO, auth2);
    }

    @Test
    public void test3() {
        assertEquals("Activity", ActivityPathBuilder.getRepoName());
    }

}
