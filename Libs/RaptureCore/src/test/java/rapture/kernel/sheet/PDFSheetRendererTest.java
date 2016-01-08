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
package rapture.kernel.sheet;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rapture.common.BlobContainer;
import rapture.kernel.ContextFactory;
import rapture.kernel.script.KernelScript;
import rapture.kernel.script.ScriptBlob;
import rapture.kernel.script.ScriptSheet;
import rapture.render.PDFSheetRenderer;

import com.lowagie.text.DocumentException;

public class PDFSheetRendererTest {

    KernelScript api = new KernelScript();
    ScriptSheet sheet = api.getSheet();
    ScriptBlob blob = api.getBlob();

    @Before
    @After
    public void setUp() throws Exception {
        api.setCallingContext(ContextFactory.getKernelUser());
        if (sheet.sheetRepoExists("//foo")) sheet.deleteSheetRepo("//foo");
        if (blob.blobRepoExists("//bar")) blob.deleteBlobRepo("//bar");
    }

    @Test
    public void test() {
        String sheetURI = "//foo/bar";
        String blobURI = "//bar";
        

        sheet.createSheetRepo("//foo", "SHEET {} using MEMORY {prefix=\"foo\"}");
        blob.createBlobRepo("//bar",  "BLOB {} USING MEMORY {  }", "REP {} USING MEMORY { prefix = \"bar\" }");
        
        sheet.createSheet(sheetURI);
        sheet.setSheetCell(sheetURI, 0, 0, "Foo", 0);
        sheet.setSheetCell(sheetURI, 1, 0, "Bar", 0);
        sheet.setSheetCell(sheetURI, 0, 1, "Foo1", 1);
        sheet.setSheetCell(sheetURI, 1, 1, "Bar1", 1);
        blob.putBlob(blobURI, new byte[0], "text");

        PDFSheetRenderer renderer = new PDFSheetRenderer(sheetURI, blobURI);
        
        try {
            renderer.renderAndSave(api);
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        BlobContainer blobby = blob.getBlob(blobURI);
        int length = blobby.getContent().length;
        assertTrue(length > 0);
    }
}
