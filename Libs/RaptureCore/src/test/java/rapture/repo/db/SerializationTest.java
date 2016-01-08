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
package rapture.repo.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.repo.CommitObject;
import rapture.common.repo.DocumentObject;
import rapture.common.repo.TreeObject;

public class SerializationTest {

    public void generalTest(TreeObject t) throws Exception {
        String json = JacksonUtil.jsonFromObject(t);
        TreeObject t2 = JacksonUtil.objectFromJson(json, TreeObject.class);
        String json2 = JacksonUtil.jsonFromObject(t2);
        System.out.println("Two sides are " + json + ", " + json2);
        assertEquals(json, json);
    }

    @Test
    public void testCommitSerialization() throws Exception {
        CommitObject commitObj = new CommitObject();
        commitObj.setPreviousReference("123");
        commitObj.setTreeRef("treeRef");
        commitObj.setUser("amkimian");
        commitObj.setWhen(new Date());
        String toJson = JacksonUtil.jsonFromObject(commitObj);
        System.out.println("Json for document is " + toJson);

        CommitObject c2 = JacksonUtil.objectFromJson(toJson, CommitObject.class);
        String timeTwo = JacksonUtil.jsonFromObject(c2);

        assertEquals(timeTwo, toJson);
    }

    @Test
    public void testDocumentSerialization() throws Exception {
        DocumentObject dObj = new DocumentObject();
        dObj.setContent(new JsonContent("{\"alan\":5}"));
        dObj.setPreviousReference("1234");

        String toJson = JacksonUtil.jsonFromObject(dObj);
        System.out.println("Json for document is " + toJson);

        DocumentObject dRet = JacksonUtil.objectFromJson(toJson, DocumentObject.class);

        assertTrue(dRet.equals(dObj));
    }
}
