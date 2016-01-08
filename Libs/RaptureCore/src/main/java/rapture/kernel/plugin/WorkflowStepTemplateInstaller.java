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
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.dp.WorkflowStepTemplate;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

import com.google.common.base.Preconditions;

public class WorkflowStepTemplateInstaller implements RaptureInstaller {
    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        WorkflowStepTemplate wst = JacksonUtil.objectFromJson(item.getContent(), WorkflowStepTemplate.class);
        Preconditions.checkNotNull(uri);
        Preconditions.checkNotNull(wst);
        if (uri.equals(new RaptureURI(wst.getWorkflowStepTemplateURI(), Scheme.WORKFLOWSTEPTEMPLATE))) {
            Kernel.getDecision().putWorkflowStepTemplate(context, wst);
        } else {
            throw RaptureExceptionFactory.create("URI in workflow step template does not match target URI");
        }
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getDecision().deleteWorkflowStepTemplate(context, uri.toString());
    }
}
