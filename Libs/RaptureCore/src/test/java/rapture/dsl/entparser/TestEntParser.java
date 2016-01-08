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
package rapture.dsl.entparser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rapture.common.RaptureEntitlementsContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.model.BasePayload;
import rapture.common.shared.doc.GetDocPayload;


public class TestEntParser {
    @Test
    public void testEntParsing() {
        GetDocPayload gc = new GetDocPayload();
        gc.setDocUri("//myauth/mypath");
        RaptureEntitlementsContext rec = new RaptureEntitlementsContext(gc);

        assertEquals("a/b/c", ParseEntitlementPath.getEntPath("/a/b/c", rec));
        assertEquals("a/myauth/mypath", ParseEntitlementPath.getEntPath("/a/$f(raptureURI)", rec));
        
        AuthTestPayload gmp = new AuthTestPayload();
        gmp.setUri("//this.test/is/a/path/to/a/doc/ok");
        rec = new RaptureEntitlementsContext(gmp);
        
        assertEquals("a/is/a/path/to/a/doc/ok", ParseEntitlementPath.getEntPath("/a/$d(mailboxURI)", rec));
        
        AuthTestPayload atp = new AuthTestPayload();
        atp.setUri("//this.test/is/a/path/to/a/doc/ok");
        atp.setUri2("//another/docpath");
        rec = new RaptureEntitlementsContext(atp);
        
        assertEquals("adfsdf/this.test/is/a/path/to/a/doc/ok", ParseEntitlementPath.getEntPath("/adfsdf/$a(uri)/$d(uri)", rec));


    }
    
    public class AuthTestPayload extends BasePayload {
        public String uri;
        public void setUri(String uri) {
            this.uri = uri;
        }
        
        @SuppressWarnings("unused")
        private String uri2;
        public void setUri2(String uri2) {
            this.uri2 = uri2;
        }
        
        @SuppressWarnings("unused")
        private String docPath;
        public String getDocPath() {
            return new RaptureURI(uri, Scheme.DOCUMENT).getDocPath();
        }
        
        @SuppressWarnings("unused")
        private String authority;
        public String getAuthority() {
            return new RaptureURI(uri, Scheme.DOCUMENT).getAuthority();
        }
    }
}
