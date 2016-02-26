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
package rapture.kernel.user;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureCubeResult;
import rapture.common.RaptureField;
import rapture.common.RaptureFieldPath;
import rapture.common.RaptureGroupingFn;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * Test the cube view in context
 * 
 * @author alan
 * 
 */
public class CubeViewFullTest {
    private void createFields() {
        CallingContext ctx = ContextFactory.getKernelUser();
        RaptureField field = new RaptureField();
        field.setName("duration");
        field.setAuthority("order");
        RaptureFieldPath fPath = new RaptureFieldPath();
        fPath.setPath("duration");
        fPath.setTypeName("order");
        field.getFieldPaths().add(fPath);
        Kernel.getFields().putField(ctx, field);

        field = new RaptureField();
        field.setName("amount");
        field.setAuthority("order");
        RaptureFieldPath fPath2 = new RaptureFieldPath();
        fPath2.setPath("amount");
        fPath2.setTypeName("order");
        field.getFieldPaths().add(fPath2);
        field.setUnits("mn.");
        field.setGroupingFn(RaptureGroupingFn.AVERAGE);
        Kernel.getFields().putField(ctx, field);

        field = new RaptureField();
        field.setName("asset");
        field.setAuthority("order");
        RaptureFieldPath fPath3 = new RaptureFieldPath();
        fPath3.setPath("assetId");
        fPath3.setTypeName("order");
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

        field = new RaptureField();
        field.setName("sector");
        field.setAuthority("asset");
        RaptureFieldPath fPath5 = new RaptureFieldPath();
        fPath5.setPath("sector");
        fPath5.setTypeName("asset");
        field.getFieldPaths().add(fPath5);
        Kernel.getFields().putField(ctx, field);

    }

    @Before
    public void setup() {
        CallingContext ctx = ContextFactory.getKernelUser();

        try {
            Kernel.initBootstrap(null, null, true);
            if (!Kernel.getDoc().docRepoExists(ctx, RaptureURI.builder(Scheme.DOCUMENT,"order").asString())) {
                Kernel.getDoc().createDocRepo(ctx, RaptureURI.builder(Scheme.DOCUMENT,"order").asString(), "NREP {} using MEMORY {}");
            }
            if (!Kernel.getDoc().docRepoExists(ctx, RaptureURI.builder(Scheme.DOCUMENT,"asset").build().toString())) {
                Kernel.getDoc().createDocRepo(ctx, RaptureURI.builder(Scheme.DOCUMENT,"asset").asString(), "NREP {} using MEMORY {}");
            }

            // Create some fields

            createFields();

            Kernel.getDoc().putDoc(ctx, "//order/1",
                    "{ \"duration\" : 0.5, \"amount\" : 100.0, \"assetId\" : \"XYZ123\" }");
            Kernel.getDoc().putDoc(ctx, "//order/2",
                    "{ \"duration\" : 1.2, \"amount\" : 120.0, \"assetId\" : \"XYZ124\" }");
            Kernel.getDoc().putDoc(ctx, "//order/3",
                    "{ \"duration\" : 0.9, \"amount\" : 110.0, \"assetId\" : \"XYZ125\" }");
            Kernel.getDoc().putDoc(ctx, "//order/4",
                    "{ \"duration\" : 0.23, \"amount\" : 10.0, \"assetId\" : \"XYZ126\" }");
            Kernel.getDoc().putDoc(ctx, "//order/5",
                    "{ \"duration\" : 0.5, \"amount\" : 134.0, \"assetId\" : \"XYZ127\" }");

            Kernel.getDoc().putDoc(ctx, "//asset/XYZ123",  "{ \"currency\" : \"GBP\", \"sector\" : \"Technology\"}");

            Kernel.getDoc().putDoc(ctx, "//asset/XYZ124",  "{ \"currency\" : \"USD\", \"sector\" : \"Financial\"}");
            Kernel.getDoc().putDoc(ctx, "//asset/XYZ125",  "{ \"currency\" : \"USD\", \"sector\" : \"Technology\"}");
            Kernel.getDoc().putDoc(ctx, "//asset/XYZ126",  "{ \"currency\" : \"EUR\", \"sector\" : \"Industrial\"}");
            Kernel.getDoc().putDoc(ctx, "//asset/XYZ127",  "{ \"currency\" : \"USD\", \"sector\" : \"Financial\"}");

            String script = "return true;";

            Kernel.getScript().createScript(ctx, "//order/trueFn", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.FILTER, script);

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCube1() {
        CallingContext ctx = ContextFactory.getKernelUser();
        RaptureCubeResult res = Kernel.getUser().runFilterCubeView(ctx, "//order", "//order/trueFn", "", "asset.sector,asset.currency", "duration,amount");
        System.out.println(JacksonUtil.prettyfy(JacksonUtil.jsonFromObject(res)));
    }

}
