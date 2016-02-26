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
package rapture.kernel.schemes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.kernel.Kernel;
import rapture.kernel.RepoApiImpl;

@SuppressWarnings("deprecation")
abstract public class RepoSchemeContractTest {

    public static final String AUTHORITY = "junit";
    private RepoApiImpl repo;
    private CallingContext callingContext;
    private String docURI = "document://junit";
    @Before
    public void before() {
        System.setProperty("LOGSTASH-ISENABLED", "false");
        System.out.println("superclass");
        repo = new RepoApiImpl(Kernel.INSTANCE);
        this.callingContext = new CallingContext();
        this.callingContext.setUser("dummy");
        Kernel.INSTANCE.clearRepoCache(false);
        
 
        Kernel.initBootstrap();
        if (!Kernel.getDoc().docRepoExists(callingContext, docURI)) {
        Kernel.getDoc().createDocRepo(callingContext, docURI, "REP {} USING MEMORY {}");
        }
    }

    @After
    public void after() {
        Kernel.getDoc().deleteDocRepo(callingContext, docURI);
    }
    
    public RepoApiImpl getRepo() {
        return repo;
    }

    public CallingContext getCallingContext() {
        return callingContext;
    }

    @Test
    public void testPutAndGetContent() {

        getRepo().putContent(callingContext, getDocumentURI(), getDocument(), null);
        ContentEnvelope envelope = getRepo().getContent(callingContext, getDocumentURI());
        if (getDocument() instanceof byte[]) {
            Assert.assertArrayEquals((byte[]) getDocument(), (byte[]) envelope.getContent());
        } else {
            assertEquals(getDocument(), envelope.getContent());
        }
    }

    @Test
    public void testDeleteContent() {

        testPutAndGetContent();
        getRepo().deleteContent(callingContext, getDocumentURI(), null);
        ContentEnvelope result = getRepo().getContent(callingContext, getDocumentURI());
        assertNull(result.getContent());
    }

    @Test
    public void testBookmarks() {
        if (getBookmarkedDocumentURI() != null) {
            testPutAndGetContent();
            ContentEnvelope content = getRepo().getContent(callingContext, getBookmarkedDocumentURI());
            assertEquals(getBookmarkedDocument(), content.getContent());
        }
    }

    public abstract Object getDocument();

    public abstract String getDocumentURI();

    public abstract String getBookmarkedDocumentURI();

    public abstract Object getBookmarkedDocument();

}
