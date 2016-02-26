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
package rapture.kernel.fields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureField;
import rapture.common.RaptureFieldPath;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureGroupingFn;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class FieldsTest {

    @Before
    public void setup() {
        CallingContext ctx = ContextFactory.getKernelUser();

        try {
            Kernel.INSTANCE.clearRepoCache(false);
            Kernel.initBootstrap(null, null, true);
            Kernel.getDoc().createDocRepo(ctx, RaptureURI.builder(Scheme.DOCUMENT, "order").asString(), "REP {} using MEMORY {}");
            Kernel.getDoc().createDocRepo(ctx, RaptureURI.builder(Scheme.DOCUMENT, "asset").asString(), "REP {} using MEMORY {}");

            // Create some fields

            RaptureField field = new RaptureField();
            field.setName("user");
            field.setAuthority("order");
            RaptureFieldPath fPath = new RaptureFieldPath();
            fPath.setPath("info.user");
            fPath.setTypeName("order");
            field.getFieldPaths().add(fPath);
            Kernel.getFields().putField(ctx, field);

            field = new RaptureField();
            field.setName("user2");
            field.setAuthority("order");
            RaptureFieldPath fPath2 = new RaptureFieldPath();
            fPath2.setPath("info.user2");
            fPath2.setTypeName("order");
            field.getFieldPaths().add(fPath2);
            Kernel.getFields().putField(ctx, field);

            field = new RaptureField();
            field.setName("asset");
            field.setAuthority("asset");
            RaptureFieldPath fPath3 = new RaptureFieldPath();
            fPath3.setPath("info.assetId");
            fPath3.setTypeName("asset");
            field.getFieldPaths().add(fPath3);
            field.setUnits("string");
            field.setGroupingFn(RaptureGroupingFn.NONE);
            Kernel.getFields().putField(ctx, field);

            field = new RaptureField();
            field.setName("currency");
            field.setAuthority("asset");
            RaptureFieldPath fPath4 = new RaptureFieldPath();
            fPath4.setPath("currency");
            fPath4.setTypeName("asset");
            field.getFieldPaths().add(fPath4);
            Kernel.getFields().putField(ctx, field);

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }
    
    @Test @Ignore
    public void testFindFieldsByUriPrefix() {
        CallingContext context = ContextFactory.getKernelUser();
        Map<String, RaptureFolderInfo> children = Kernel.getFields().listFieldsByUriPrefix(context, "//order/", 1);
        assertEquals(2, children.size());
        assertEquals("user", children.get(0).getName());
        assertEquals("user2", children.get(1).getName());
        children = Kernel.getFields().listFieldsByUriPrefix(ContextFactory.getKernelUser(), "//asset/", 1);
        assertEquals(2, children.size());
        assertEquals("asset", children.get(0).getName());
        assertEquals("currency", children.get(1).getName());
    }

    @Test @Ignore
    public void doubleReference() {
        CallingContext ctx = ContextFactory.getKernelUser();
        // Now add a document

        Kernel.getDoc().putDoc(ctx, "//order/2", "{ \"something\" : 5, \"info\" : { \"user\" : \"Alan\", \"assetId\" : \"asset001\" }}");

        // Add the asset
        Kernel.getDoc().putDoc(ctx, "//asset/asset001", "{ \"currency\" : \"USD\" }");

        // Now retrieve that field!

        List<String> fields = new ArrayList<String>();
        fields.add("user");
        fields.add("asset.currency");

        List<String> result = Kernel.getFields().getDocumentFields(ctx, "//order/2", fields);
        for (String r : result) {
            System.out.println(r);
        }
    }

    @Test @Ignore
    public void generalTest() {
        CallingContext ctx = ContextFactory.getKernelUser();
        // Now add a document

        Kernel.getDoc().putDoc(ctx, "//order/1", "{ \"something\" : 5, \"info\" : { \"user\" : \"Alan\" }}");

        // Now retrieve that field!

        List<String> fields = new ArrayList<String>();
        fields.add("user");

        List<String> result = Kernel.getFields().getDocumentFields(ctx, "//order/1", fields);
        for (String r : result) {
            System.out.println(r);
        }

    }



    @Test @Ignore
    public void testInvalidDocument() {
        CallingContext ctx = ContextFactory.getKernelUser();

        // Now add a document

        Kernel.getDoc().putDoc(ctx, "//order/2", "{ \"something\" : 5, \"info\" : { \"user\" : \"Alan\", \"assetId\" : \"asset001\" }}");

        // Add the asset
        Kernel.getDoc().putDoc(ctx, "//asset/asset001", "{ \"currency\" : \"USD\" }");

        // Now retrieve that field!

        List<String> fields = new ArrayList<String>();
        fields.add("user2");

        List<String> result = Kernel.getFields().getDocumentFields(ctx, "//order/2", fields);
        for (String r : result) {
            assertTrue(r.isEmpty());
        }
    }

    @Test @Ignore
    public void testInvalidFields() {
        CallingContext ctx = ContextFactory.getKernelUser();

        // Now add a document

        Kernel.getDoc().putDoc(ctx, "//order/2", "{ \"something\" : 5, \"info\" : { \"user\" : \"Alan\", \"assetId\" : \"asset001\" }}");

        // Add the asset
        Kernel.getDoc().putDoc(ctx, "//asset/asset001", "{ \"currency\" : \"USD\" }");

        // Now retrieve that field!

        List<String> fields = new ArrayList<String>();
        fields.add("userx");

        List<String> result = Kernel.getFields().getDocumentFields(ctx, "//order/2", fields);
        for (String r : result) {
            assertTrue(r.isEmpty());
        }
    }
}
