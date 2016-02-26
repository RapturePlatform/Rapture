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

import java.util.List;

import org.junit.After;

import rapture.common.CallingContext;
import rapture.common.RaptureJob;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.ScheduleApiImplWrapper;



public class ScheduleSchemeImplTest extends RepoSchemeContractTest {

    @Override
    public Object getDocument() {
        RaptureJob rj = new RaptureJob();
        rj.setActivated(true);
        rj.setJobURI(getDocumentURI());
        return rj;
    }

    @Override
    public String getDocumentURI() {
        return "job://junit/junittest/testjob";
    }
    
    @Override
    public String getBookmarkedDocumentURI() {
        return null;
    }

    @Override
    public Object getBookmarkedDocument() {
        return null;
    }

    @After
    public void after() {
        ScheduleApiImplWrapper schedule = Kernel.getSchedule();
        CallingContext context = ContextFactory.getKernelUser();
        List<String> jobs = schedule.getJobs(context);
        if (jobs != null) {
            for (String jobURI : jobs) {
                System.out.println("deleting job " + jobURI);
                schedule.deleteJob(context, jobURI);
            }
        }

    }
}
