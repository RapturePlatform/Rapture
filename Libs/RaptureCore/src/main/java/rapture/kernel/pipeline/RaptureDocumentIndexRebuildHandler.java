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
package rapture.kernel.pipeline;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeDocumentIndexRebuild;
import rapture.common.model.IndexScriptPair;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class RaptureDocumentIndexRebuildHandler implements QueueHandler {
    private static final Logger log = Logger.getLogger(RaptureDocumentIndexRebuildHandler.class);
    private final PipelineTaskStatusManager statusManager;

    public RaptureDocumentIndexRebuildHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        String content = task.getContent();
        log.info("Attempting rebuild index for document");
        try {
            statusManager.startRunning(task);
            MimeDocumentIndexRebuild doc = JacksonUtil.objectFromJson(content, MimeDocumentIndexRebuild.class);
            // load the document and then fake a save of it
            log.info(String.format("Fake save of %s", doc.getDisplayName()));

            CallingContext context = ContextFactory.getKernelUser();
            IndexScriptPair iPair = doc.getIndexScriptPair();
            String displayName = doc.getDisplayName();
            String docContent = Kernel.getDoc().getDoc(context, "//" + doc.getAuthority() + "/" +  displayName);

            Kernel.getDoc().getTrusted().runIndex(context, iPair, doc.getAuthority(), displayName, docContent);
            statusManager.finishRunningWithSuccess(task);
        } catch (Exception e) {
            log.error("Error when running Index rebuild : ", e);
            statusManager.finishRunningWithFailure(task);
        }
        return true;
    }

}
