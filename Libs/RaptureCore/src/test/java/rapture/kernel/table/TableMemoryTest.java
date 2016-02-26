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
package rapture.kernel.table;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.util.ResourceLoader;

/**
 * General test of the in memory index. Shows up any issues with general flow.
 * 
 * @author amkimian
 * 
 */
public class TableMemoryTest {
    private static final String AUTHORITY = "test";
    private static final String TYPENAME = "testIndex" ;
    private static final String TABLE = "table";
    private static final String TYPE_URI = "//" + AUTHORITY + "/" + TYPENAME;
    private static final String TABLE_URI = "//" + AUTHORITY + "/" + TABLE;
    private static final String DOC_URI = "//" + AUTHORITY + "/" + TYPENAME + "/1";

    private static CallingContext ctx = ContextFactory.getKernelUser();

    @Before
    public void setup() {
        Kernel.initBootstrap();
        Kernel.getDoc().createDocRepo(ctx, TYPE_URI, "REP {} USING MEMORY {}");
        Kernel.getTable().createTable(ctx, TABLE_URI, "TABLE {} USING MEMORY {}");
    }

    @Ignore
    @Test
    public void testSingleDocument() {
        String document = ResourceLoader.getResourceAsString(this, "/testDoc.doc");
        Kernel.getDoc().putDoc(ctx, DOC_URI, document);
    }
}
