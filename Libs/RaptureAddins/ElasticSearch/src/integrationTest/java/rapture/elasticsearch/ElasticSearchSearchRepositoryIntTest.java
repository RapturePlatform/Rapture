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
package rapture.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.ConnectionInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.connection.ConnectionType;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class ElasticSearchSearchRepositoryIntTest {

    private ElasticSearchSearchRepository e;

    @Before
    public void setup() {
        Kernel.getKernel().restart();
        CallingContext context = ContextFactory.getKernelUser();
        ConnectionInfo info = new ConnectionInfo("localhost", 9300, "rapture", "rapture", "default", "default");
        Kernel.getSys().putConnectionInfo(context, ConnectionType.ES.toString(), info);
        e = new ElasticSearchSearchRepository();
        e.start();
    }

    @Test
    public void test() {
        String json = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2014-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";
        RaptureURI uri = new RaptureURI("document://testme/y", Scheme.DOCUMENT);
        e.put(uri, json);
        assertEquals(json, e.get(uri));
        System.out.println(e.search("kim*"));
    }

    @Test
    public void test2() {
        RaptureURI uri = new RaptureURI("document://testme/missing", Scheme.DOCUMENT);
        assertNull(e.get(uri));
    }

}
