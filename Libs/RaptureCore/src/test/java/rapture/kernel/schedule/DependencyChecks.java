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
package rapture.kernel.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.JobLink;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class DependencyChecks {

    Map<String, Boolean> foundFrom, foundTo;

    @Before
    public void setup() {
        foundFrom = new HashMap<String, Boolean>();
        foundTo = new HashMap<String, Boolean>();
        Kernel.initBootstrap(null, null, true);
    }

    @Test
    public void testDependencies() {
        CallingContext context = ContextFactory.getKernelUser();
        // For this we don't actually need to have a job, we just need to
        // reference them (as we
        // are not going to start anything)

        String sJob = "//authority/S";
        String aJob = "//authority/A";
        String bJob = "//authority/B";
        String cJob = "//authority/C";
        String dJob = "//authority/D";
        String eJob = "//authority/E";

        createLink(sJob, aJob);
        createLink(sJob, bJob);
        createLink(aJob, cJob);
        createLink(bJob, dJob);
        createLink(cJob, eJob);
        createLink(dJob, eJob);

        List<JobLink> linksFrom = Kernel.getSchedule().getTrusted().getLinksFrom(context, sJob);
        assertEquals(2, linksFrom.size());
        foundLinksFrom(linksFrom);

        linksFrom = Kernel.getSchedule().getTrusted().getLinksFrom(context, aJob);
        assertEquals(1, linksFrom.size());
        foundLinksFrom(linksFrom);

        linksFrom = Kernel.getSchedule().getTrusted().getLinksFrom(context, bJob);
        assertEquals(1, linksFrom.size());
        foundLinksFrom(linksFrom);

        linksFrom = Kernel.getSchedule().getTrusted().getLinksFrom(context, cJob);
        assertEquals(1, linksFrom.size());
        foundLinksFrom(linksFrom);

        linksFrom = Kernel.getSchedule().getTrusted().getLinksFrom(context, dJob);
        assertEquals(1, linksFrom.size());
        foundLinksFrom(linksFrom);

        for (String key : foundFrom.keySet()) {
            assertTrue("Found for " + key + "?", foundFrom.get(key));
        }

        List<JobLink> linksTo = Kernel.getSchedule().getTrusted().getLinksTo(context, aJob);
        assertEquals(1, linksTo.size());
        foundLinksTo(linksTo);

        linksTo = Kernel.getSchedule().getTrusted().getLinksTo(context, bJob);
        assertEquals(1, linksTo.size());
        foundLinksTo(linksTo);

        linksTo = Kernel.getSchedule().getTrusted().getLinksTo(context, cJob);
        assertEquals(1, linksTo.size());
        foundLinksTo(linksTo);

        linksTo = Kernel.getSchedule().getTrusted().getLinksTo(context, dJob);
        assertEquals(1, linksTo.size());
        foundLinksTo(linksTo);

        linksTo = Kernel.getSchedule().getTrusted().getLinksTo(context, eJob);
        assertEquals(2, linksTo.size());
        foundLinksTo(linksTo);

        for (String key : foundTo.keySet()) {
            assertTrue("Found for " + key + "?", foundTo.get(key));
        }

    }

    private void foundLinksFrom(List<JobLink> links) {
        for (JobLink link : links) {
            String key = link.getFrom() + "/" + link.getTo();
            if (foundFrom.containsKey(key)) {
                foundFrom.put(key, true);
            } else {
                fail("Found unexpected link from " + link.getFrom() + " to " + link.getTo() + "(key = " + key + ")");
            }
        }
    }

    private void foundLinksTo(List<JobLink> links) {
        for (JobLink link : links) {
            String key = link.getFrom() + "/" + link.getTo();
            if (foundTo.containsKey(key)) {
                foundTo.put(key, true);
            } else {
                fail("Found unexpected link from " + link.getFrom() + " to " + link.getTo() + "(key = " + key + ")");
            }
        }
    }

    private void createLink(String sJob, String aJob) {
        Kernel.getSchedule().getTrusted().setJobLink(ContextFactory.getKernelUser(), sJob, aJob);
        String key = new RaptureURI(sJob, Scheme.JOB) + "/" + new RaptureURI(aJob, Scheme.JOB);
        foundFrom.put(key, false);
        foundTo.put(key, false);
    }
}
