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
package rapture.dp.invocable.testjar.steps;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.dp.AbstractInvocable;
import rapture.kernel.Kernel;

public class CreateData extends AbstractInvocable<Object> {
    private static Logger log = Logger.getLogger(CreateData.class.getName());

    public CreateData(String workerURI, String stepName) {
        super(workerURI, stepName);
    }

    @Override
    public String invoke(CallingContext ctx) {
        log.info("Starting test wf: " + getWorkerURI());
        Kernel.getDecision().writeWorkflowAuditEntry(ctx, getWorkerURI(), "***** Starting test wf for " + getWorkerURI(), false);
        String docUri = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "DOC_URI");
        String docContent = Kernel.getDecision().getContextValue(ctx, getWorkerURI(), "DOC_CONTENT");
        Kernel.getDoc().putDoc(ctx, docUri, docContent);
        log.info("Finishing test wf: " + getWorkerURI() + " by writing to " + docUri);
        Kernel.getDecision().writeWorkflowAuditEntry(ctx, getWorkerURI(), "Finishing test wf: " + getWorkerURI() + " by writing to " + docUri, false);
        return "next";
    }
}
