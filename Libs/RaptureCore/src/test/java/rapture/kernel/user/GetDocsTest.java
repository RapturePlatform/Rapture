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
package rapture.kernel.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.exception.RaptureException;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class GetDocsTest {
    // Test the batch get
    private static final CallingContext ctx = ContextFactory.getKernelUser();
    private static final String authority = "test";
    private static final String typeName = "batchGetTest";
    private static final int NUMBER_OF_AUTHORITYS = 2, DOCUMENTS_PER_AUTHORITY = 3;

    private static Map<String, String> testDocuments = new HashMap<String, String>();

    @Test
    public void runBatchGetMultipleAuthorityTest() {
        System.out.println("BATCH GET MULTIPLE AUTHORITY TEST STARTUP");
        List<String> uris = new ArrayList<String>();
        Map<String, String> docs = new HashMap<String, String>();
        String uri, doc;

        // Check that the documents we supplied to rapture are the exact same ones we get back
        for (int authorityNum = 0; authorityNum < NUMBER_OF_AUTHORITYS; authorityNum++) {

            for (int documentNum = 0; documentNum < DOCUMENTS_PER_AUTHORITY; documentNum++) {

                uris.add("document://" + authority + authorityNum + "." + typeName + authorityNum + "/" + documentNum);
            }
        }

        docs = Kernel.getDoc().getDocs(ctx, uris);
        for (int i = 0; i < uris.size(); i++) {
            uri = uris.get(i);
            doc = docs.get(uri);

            if (!testDocuments.get(uri).equals(doc)) {
                assertTrue("The document returned by getDocs at uri " + uri + " does not equal the supplied document value. Supplied: "
                        + testDocuments.get(uri) + ", Returned: " + doc, false);
                break;
            }
        }

        System.out.println("BATCH GET MULTIPLE AUTHORITY TEST END");
    }

    @Test
    public void runBatchGetEmptyListTest() {
        System.out.println("BATCH GET EMPTY LIST TEST STARTUP");

        // Check that batch get now handles an empty list correctly
        Map<String,String> res = Kernel.getDoc().getDocs(ctx, new ArrayList<String>());
        assertTrue("Passing getDocs an empty list does not result in an empty list.", res != null && res.isEmpty());

        System.out.println("BATCH GET EMPTY LIST TEST END");
    }

    @Test
    public void testGetDocs() {
        String repo = "document://getdocsrepo";
        if (!Kernel.getDoc().docRepoExists(ctx, repo)) {
            Kernel.getDoc().createDocRepo(ctx, repo, "NREP {} USING MEMORY {}");
        }

        // create a list of 100 new documents; put them into repo and create a reference list
        int size = 100;
        ArrayList<String> docList = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            Kernel.getDoc().putDoc(ctx, repo + "/" + i, "{\"key" + i + "\"" + ":\"value" + i + "\"" + "}");
            docList.add(repo + "/" + i);
        }
        Map<String, String> retList = Kernel.getDoc().getDocs(ctx, docList);
        assertEquals(size, retList.size());

        for (int i = 0; i < size; i++) {
            assertEquals("{\"key" + i + "\"" + ":\"value" + i + "\"" + "}", retList.get(docList.get(i)));
        }

        docList.set(50, "//unknown/x");
        retList = Kernel.getDoc().getDocs(ctx, docList);
        assertEquals(size, retList.size());
        for (int i = 0; i < size; i++) {
            if (i == 50) {
                assertNull(retList.get(docList.get(i)));
            } else {
                assertEquals("{\"key" + i + "\"" + ":\"value" + i + "\"" + "}", retList.get(docList.get(i)));
            }
        }
    }

    @Test
    public void testBatchGetShuffledInputs() {
        String[] repos = { "document://batchgetshuffle1", "document://batchgetshuffle2" };
        List<String> docList = new ArrayList<>();
        int size = 100;
        for (String repo : repos) {
            if (!Kernel.getDoc().docRepoExists(ctx, repo)) {
                Kernel.getDoc().createDocRepo(ctx, repo, "NREP {} USING MEMORY {}");
            }
            for (int i = 0; i < size; i++) {
                Kernel.getDoc().putDoc(ctx, repo + "/" + i, "{\"key" + i + "\"" + ":\"value" + i + "\"" + "}");
                docList.add(repo + "/" + i);
            }
        }
        Collections.shuffle(docList, new Random(System.nanoTime()));
        Map<String, String> result = Kernel.getDoc().getDocs(ctx, docList);
        assertEquals(result.size(), docList.size());
        for (int i = 0; i < docList.size(); i++) {
            String doc = docList.get(i);
            String res = result.get(doc);
            String index = doc.substring(doc.lastIndexOf('/') + 1);
            assertTrue(res.indexOf("key" + index) != -1);
            assertTrue(res.indexOf("value" + index) != -1);
        }
    }

    @Test
    public void testBatchGetBadInputs() {
        String repo = "//batchgetBadinput";
        if (!Kernel.getDoc().docRepoExists(ctx, repo)) {
            Kernel.getDoc().createDocRepo(ctx, repo, "NREP {} USING MEMORY {}");
        }
        List<String> docList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            docList.add(null);
        }
        try {
            Kernel.getDoc().getDocs(ctx, docList);
            fail("Exception should have been thrown for bad input.");
        } catch (RaptureException expected) {
        }
        docList.clear();
        for (int i = 0; i < 10; i++) {
            docList.add(repo+i);
        }
        Map<String, String> ret = Kernel.getDoc().getDocs(ctx, docList);
        assertEquals(ret.size(), docList.size());
        for (Map.Entry<String, String> r : ret.entrySet()) {
            assertNull(r.getValue());
        }
    }

    @Test
    public void testSingleRepoMultipleDocs() {
        String repo = "document://testmeplzzzzz";
        if (!Kernel.getDoc().docRepoExists(ctx, repo)) {
            Kernel.getDoc().createDocRepo(ctx, repo, "NREP {} USING MEMORY {}");
        }

        // create a list of 100 new documents; put them into repo and create a reference list
        int size = 100;
        List<String> docList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Kernel.getDoc().putDoc(ctx, repo + "/" + i, "{\"key" + i + "\"" + ":\"value" + i + "\"" + "}");
            docList.add(repo + "/" + i);
        }
        Map<String, String> retList = Kernel.getDoc().getDocs(ctx, docList);
        assertEquals(size, retList.size());

        for (int i = 0; i < size; i++) {
            assertEquals("{\"key" + i + "\"" + ":\"value" + i + "\"" + "}", retList.get(docList.get(i)));
        }

        docList.set(50, "//unknown/x");
        retList = Kernel.getDoc().getDocs(ctx, docList);
        assertEquals(size, retList.size());
        for (int i = 0; i < size; i++) {
            if (i == 50) {
                assertNull(retList.get(docList.get(i)));
            } else {
                assertEquals("{\"key" + i + "\"" + ":\"value" + i + "\"" + "}", retList.get(docList.get(i)));
            }
        }
    }

    @Test
    //@Ignore("This test runs fine in Eclipse but fails when run from the command line. Probably a locale issue at runtime.")
    public void testBadDocReturnsNull() {
        // RAP-757
        String repo = "document://歡天喜地";
        if (!Kernel.getDoc().docRepoExists(ctx, repo)) {
            Kernel.getDoc().createDocRepo(ctx, repo, "NREP {} USING MEMORY {}");
        }

        // create a list of documents, some of which are in the repo and some not.
        ArrayList<String> docList = new ArrayList<String>();
        int i = 0;
        long j = System.currentTimeMillis();
        // Add entry to the docList but don't call putDoc
        docList.add(repo + "/" + j++);

        for (i = 1; i < 4; i++) {
            Kernel.getDoc().putDoc(ctx, repo + "/" + j, "{\"key" + j + "\"" + ":\"милашка" + j + "\"" + "}");
            docList.add(repo + "/" + j++);
        }

        // Add another entry to the docList but don't call putDoc
        docList.add(repo + "/" + j++);
        Map<String, String> retList = Kernel.getDoc().getDocs(ctx, docList);

        assertEquals(docList.size(), retList.size());
        // retList order is not guaranteed to be same as docList order, so ensure that we got two NULLs back.
        int nullCount = 0;
        for (String s : retList.values()) {
            if (s == null) nullCount++;
        }
        assertEquals("Expected to get two NULLs back", 2, nullCount);

        // Try a bad repo too. Should be two more bad entries
        docList.add(repo + "DUMMY/" + j++);
        docList.add(repo + "DUMMY/" + j++);
        retList = Kernel.getDoc().getDocs(ctx, docList);
        assertEquals(docList.size(), retList.size());

        nullCount = 0;
        for (String s : retList.values()) {
            if (s == null) nullCount++;
        }
        assertEquals("Expected to get two NULLs back", 4, nullCount);
    }

    @AfterClass
    public static void cleanUp() {
        System.out.println("Cleaning up: deleting documents");
        for (int authorityNum = 0; authorityNum < NUMBER_OF_AUTHORITYS; authorityNum++) {
            String repo = "//" + authority + authorityNum + "." + typeName + authorityNum;
            Kernel.getDoc().deleteDocRepo(ctx, repo);
        }
    }

    @BeforeClass
    public static void setup() {
        System.out.println("BATCHGET STARTUP");
        Kernel.initBootstrap();
        String uri, doc;

        for (int authorityNum = 0; authorityNum < NUMBER_OF_AUTHORITYS; authorityNum++) {
            String repo = "//" + authority + authorityNum + "." + typeName + authorityNum;
            if (Kernel.getDoc().docRepoExists(ctx, repo))
                Kernel.getDoc().deleteDocRepo(ctx, repo);

            Kernel.getDoc().createDocRepo(ctx, repo, "NREP {} USING MEMORY {}");

            for (int documentNum = 0; documentNum < DOCUMENTS_PER_AUTHORITY; documentNum++) {
                uri = "document://" + authority + authorityNum + "." + typeName + authorityNum + "/" + documentNum;
                doc = "{ \"yay" + documentNum + "\" : " + documentNum + " }";
                Kernel.getDoc().putDoc(ctx, uri, doc);
                testDocuments.put(uri, doc);
            }
        }

        System.out.println("BATCHGET END");
    }
}
