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
package rapture.kernel.pipeline;

import rapture.common.RapturePipelineTask;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.mime.MimeDocumentIndexRebuild;
import rapture.common.mime.MimeIndexRebuild;
import rapture.common.pipeline.PipelineConstants;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.repo.RepoVisitor;
import rapture.repo.Repository;

import java.net.HttpURLConnection;

import org.apache.log4j.Logger;

public class RaptureIndexRebuildHandler implements QueueHandler {
    private static final Logger log = Logger.getLogger(RaptureIndexRebuildHandler.class);
    private final PipelineTaskStatusManager statusManager;

    public RaptureIndexRebuildHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, final RapturePipelineTask task) {
        String content = task.getContent();
        log.info("Attempting to run index rebuild");
        try {
            statusManager.startRunning(task);
            final MimeIndexRebuild doc = JacksonUtil.objectFromJson(content, MimeIndexRebuild.class);
            // Get all of the documents in the type specified in the doc
            // and submit tasks to rebuild index

            final Repository repository = Kernel.getKernel().getRepo(doc.getAuthority());
            if (repository == null) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Type not loadable");
            }

            repository.visitAll("", null, new RepoVisitor() {

                @Override
                public boolean visit(String name, JsonContent content, boolean isFolder) {
                    MimeDocumentIndexRebuild iRebuild = new MimeDocumentIndexRebuild();
                    iRebuild.setIndexScriptPair(doc.getIndexScriptPair());
                    iRebuild.setDisplayName(doc.getAuthority() + "/" + name);
                    iRebuild.setCtx(doc.getCtx());
                    iRebuild.setAuthority(doc.getAuthority());
                    TaskSubmitter.submitLoadBalancedToCategory(ContextFactory.getKernelUser(), iRebuild, MimeDocumentIndexRebuild.getMimeType(),
                            PipelineConstants.CATEGORY_ALPHA);
                    return true;
                }

            });
            statusManager.finishRunningWithSuccess(task);
        } catch (Exception e) {
            log.error("Error when running Index rebuild, but marking as done: ", e);
            statusManager.finishRunningWithFailure(task);
        }
        return true;
    }

}
