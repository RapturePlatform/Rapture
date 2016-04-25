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

import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeJarCacheUpdate;
import rapture.exchange.QueueHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.jar.JarCache;

/**
 * Pipeline handler responsible for updating the jar cache of the local JVM (Rapture Kernel) when it receives a message off of the pipeline
 * 
 * @author dukenguyen
 *
 */
public class JarCacheUpdateHandler implements QueueHandler {

    private static final Logger log = Logger.getLogger(JarCacheUpdateHandler.class);

    private final PipelineTaskStatusManager statusManager;

    public JarCacheUpdateHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        statusManager.startRunning(task);
        MimeJarCacheUpdate payload = JacksonUtil.objectFromJson(task.getContent(), MimeJarCacheUpdate.class);
        String jarUri = payload.getJarUri().toString();
        log.info(String.format("Updating jar cache for jar uri [%s]", jarUri));
        JarCache.getInstance().invalidate(jarUri);
        if (!payload.isDeletion()) {
            try {
                JarCache.getInstance().get(ContextFactory.getKernelUser(), jarUri);
            } catch (ExecutionException e) {
                log.error(String.format("Failed to update jar cache for jar uri [%s]", jarUri), e);
                statusManager.finishRunningWithFailure(task);
                return true;
            }
        }
        statusManager.finishRunningWithSuccess(task);
        return true;
    }
}