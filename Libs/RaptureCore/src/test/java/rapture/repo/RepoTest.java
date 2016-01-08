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
package rapture.repo;

import static org.junit.Assert.assertTrue;

import rapture.common.exception.RaptureException;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("ConstantConditions")
public class RepoTest {
    @Test
    public void test10Updates() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"}");
        String stage = "alan";
        r.createStage(stage);
        for (int i = 0; i < 20; i++) {
            r.addToStage(stage, "1/2/" + i, "{\"alan\": " + i + "}", false);
        }
        r.commitStage(stage, "alan", "Some work that I have done");
    }

    @Test
    public void test10UpdatesManyCommits() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"}");
        String stage = "alan";
        for (int i = 0; i < 20; i++) {
            r.createStage(stage);
            r.addToStage(stage, "1/2/" + i, "{\"alan\": " + i + "}", false);
            r.commitStage(stage, "alan", "Some work that I have done");
        }
    }

    @Test
    public void testAdditionThenRemoval() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"}");
        String stage = "alan";
        r.createStage(stage);
        r.addDocument("test/1", "{ \"123\" : 123}", "Test", "Test Add", false);
        r.addDocument("test/2", "{ \"123\" : 123}", "Test", "Test Add", false);
        r.removeDocument("test/1", "Test", "Test Remove");
        r.removeDocument("test/2", "Test", "Test Remove");
        r.removeDocument("test/2", "Test", "Test Remove");
    }

    @Test
    public void testCachedRepo() throws Exception {
        Repository r = RepoFactory.getRepo("CACHE REP {} using MEMORY {} VSHADOW MEMORY {}");
        String stage = "alan";
        r.createStage(stage);
        r.addToStage(stage, "1/official", "{\"alan\":1}", false);
        r.addToStage(stage, "1/official2", "{\"alan\":2}", false);
        r.commitStage(stage, "alan", "Changes on main");
        assertTrue(null != r.getDocument("1/official"));
        assertTrue(null == r.getDocument("2/official"));
    }

    @Test
    public void testFolderRetrieval() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"}");
        String stage = "alan";
        r.createStage(stage);
        String doc = "{ \"test\" : 1}";
        r.addDocument("cusip1/date1", doc, "alan", "test", true);
        r.addDocument("cusip1/date2", doc, "alan", "test", true);
        r.addDocument("cusip1/date3", doc, "alan", "test", true);
        r.addDocument("cusip1/date4", doc, "alan", "test", true);
        r.addDocument("cusip2/date2", doc, "alan", "test", true);
        r.addDocument("cusip2/date1", doc, "alan", "test", true);

        r.visitFolders("", null, new RepoFolderVisitor() {

            @Override
            public void folder(String folder) {
                System.out.println("Found folder " + folder);
            }

        });
    }

    @Test
    public void testLinkedUpdates() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"}");
        String stage = "alan";
        r.createStage(stage);

        List<String> displayNames = new ArrayList<String>();
        displayNames.add("1/2");
        displayNames.add("2/3");
        displayNames.add("2/4");
        r.addDocuments(displayNames, "{\"alan\" : 100}", "alan", "A test");
    }

    @Test(expected = RaptureException.class)
    public void testReadOnly() throws Exception {
        Repository r = RepoFactory.getRepo("READONLY REP { test = \"\"} using MEMORY { size=\"\"}");
        String stage = "alan";
        r.createStage(stage);
        r.addToStage(stage, "1/2/3", "{\"alan\": 123}", false);
    }

    @Test
    public void testRepoCreation() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"}");
        String stage = "alan";
        r.createStage(stage);
        r.addToStage(stage, "1/2/3", "{\"alan\": 123}", false);
        r.addToStage(stage, "1/alan", "{\"alan\": 1}", false);
        r.commitStage(stage, "alan", "Some work that I have done");
    }

    @Test
    public void testShadowCreation() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"} SHADOW MEMORY { size=\"\"} ");
        String stage = "alan";
        r.createStage(stage);
        r.addToStage(stage, "1/2/3", "{\"alan\": 123}", false);
        r.addToStage(stage, "1/alan", "{\"alan\": 1}", false);
        r.commitStage(stage, "alan", "Some work that I have done");
    }

    @Test
    public void testVShadowCreation() throws Exception {
        Repository r = RepoFactory.getRepo("VREP { test = \"\"} using MEMORY { size=\"\"} VSHADOW MEMORY { size=\"\"} ");
        String stage = "alan";
        r.createStage(stage);
        r.addToStage(stage, "1/2/3", "{\"alan\": 123}", false);
        r.addToStage(stage, "1/alan", "{\"alan\": 1}", false);
        r.commitStage(stage, "alan", "Some work that I have done");
    }
}
