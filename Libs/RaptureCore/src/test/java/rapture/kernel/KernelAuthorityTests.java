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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.exception.RaptureException;
import rapture.common.model.DocumentRepoConfig;

/**
 * Tests around working with authorities
 * 
 * @author alan
 * 
 */
public class KernelAuthorityTests {
    private CallingContext ctx = ContextFactory.getKernelUser();

    @Before
    public void init() {
        Kernel.initBootstrap();
    }

    @Test(expected = RaptureException.class)
    public void testBadAuthority() {
        Kernel.getDoc().createDocRepo(ctx, null, null);
    }

    @Test(expected = RaptureException.class)
    public void testBadAuthority2() {
        Kernel.getDoc().createDocRepo(ctx, "", "");
    }

    @Test
    public void testAuthorityCreation() {
        Kernel.getDoc().createDocRepo(ctx, "//test1", "NREP USING MEMORY {}");

        List<DocumentRepoConfig> ps = Kernel.getDoc().getDocRepoConfigs(ctx);
        boolean found = false;

        for (DocumentRepoConfig p : ps) {
            if (p.getAuthority().equals("test1")) {
                found = true;
            }
        }

        assertTrue(found);

        DocumentRepoConfig p2 = Kernel.getDoc().getDocRepoConfig(ctx, "//test1");
        assertNotNull(p2);
        assertEquals("test1", p2.getAuthority());
    }

    @Test(expected = RaptureException.class)
    public void testCreateTypeInInvalidAuthority() {
        Kernel.getDoc().createDocRepo(ctx, "//unknown/theTest", "NREP {} USING MEMORY {}");
    }

    @Test
    public void testDroppingAssociatedObjectsWithAAuthority() {

        // Create a new authority and type
        Kernel.getDoc().createDocRepo(ctx, "//testAssocObjAuthority", "NREP {} USING MEMORY {}");

        // Create a new authority and type
        Kernel.getDoc().createDocRepo(ctx, "//testAssocObjAuthorityLink", "NREP {} USING MEMORY {}");

        // Generate a script to run
        String script = "println('Hello from the Reflex script, value is ' + _params['value']);\n" + "_params['value'] = _params['value'] + '...';";
        Kernel.getScript().createScript(ctx, "//testAssocObjAuthority/testJobScript", RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, script);

        Kernel.getSchedule().createJob(ctx, "//testAssocObjAuthority/testJob", "A test job", "testJobScript", "* * * * * *", "America/New_York",
                new HashMap<String, String>(), false);

        Kernel.getSchedule().getTrusted().createJob(ctx, "//testAssocObjAuthorityLink/testJobFrom", "A test job from", "testJobScript", "* * * * * *", "America/New_York",
                new HashMap<String, String>(), false);

        Kernel.getSchedule().getTrusted().createJob(ctx, "//testAssocObjAuthorityLink/testJobTo", "A test job to", "testJobScript", "* * * * * *", "America/New_York",
                new HashMap<String, String>(), false);

        Kernel.getSchedule().getTrusted().setJobLink(ctx, "//testAssocObjAuthorityLink/testJobFrom", "//testAssocObjAuthority/testJob");
        Kernel.getSchedule().getTrusted().setJobLink(ctx, "//testAssocObjAuthority/testJob", "//testAssocObjAuthorityLink/testJobTo");

        // Drop the authority
        Kernel.getDoc().deleteDocRepo(ctx, "//testAssocObjAuthority");

        // Check that the type and job no longer exist
        assertTrue("Test job that was attached to deleted authority still exists.",
                Kernel.getSchedule().retrieveJob(ctx, "//testAssocObjAuthority/testJob") == null);
        assertTrue("Type that was attached to deleted authority still exists.", Kernel.getDoc().getDocRepoConfig(ctx, "//testAssocObjAuthority") == null);
        assertTrue("Link that was attached from a job on the deleted authority still exists.",
                Kernel.getSchedule().getTrusted().getLinksFrom(ctx, "//testAssocObjAuthority/testJob").size() == 0);
        assertTrue("Link that was attached to a job on the deleted authority still exists.",
                Kernel.getSchedule().getTrusted().getLinksTo(ctx, "//testAssocObjAuthority/testJobFrom").size() == 0);
        assertNotNull("From job on authority that was not deleted still exists.",
                Kernel.getSchedule().getTrusted().retrieveJob(ctx, "//testAssocObjAuthorityLink/testJobFrom"));
        assertNotNull("To job on authority that was not deleted still exists.", Kernel.getSchedule().getTrusted().retrieveJob(ctx, "//testAssocObjAuthorityLink/testJobTo"));
        assertNull("To job on wrong authority does not show up as existing.", Kernel.getSchedule().retrieveJob(ctx, "//testAssocObjAuthority/testJobTo"));

    }
}
