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
package rapture.kernel.plugin;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.JobType;
import rapture.common.RaptureJob;
import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

public class ScheduleInstaller implements RaptureInstaller {
    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        RaptureJob job = JacksonUtil.objectFromJson(item.getContent(), RaptureJob.class);
        if (job.getJobType() == JobType.WORKFLOW) {
            Kernel.getSchedule().createWorkflowJob(context, item.getUri(), job.getDescription(), job.getScriptURI(), job.getCronSpec(), job.getTimeZone(),
                    job.getParams(), job.getAutoActivate(), job.getMaxRuntimeMinutes(), job.getAppStatusNamePattern());
        } else {
            Kernel.getSchedule().createJob(context, item.getUri(), job.getDescription(), job.getScriptURI(), job.getCronSpec(), job.getTimeZone(),
                    job.getParams(), job.getAutoActivate());
        }
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getSchedule().deleteJob(context, uri.toString());
    }
}
