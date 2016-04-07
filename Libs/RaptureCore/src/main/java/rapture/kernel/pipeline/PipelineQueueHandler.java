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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.mime.MimeDecisionProcessAdvance;
import rapture.common.mime.MimeDocumentIndexRebuild;
import rapture.common.mime.MimeIndexRebuild;
import rapture.common.mime.MimeRaptureDocument;
import rapture.common.mime.MimeReflexScript;
import rapture.common.mime.MimeReflexScriptRef;
import rapture.common.mime.MimeReflexScriptResume;
import rapture.common.mime.MimeScheduleReflexScriptRef;
import rapture.common.mime.MimeSearchUpdateObject;
import rapture.exchange.QueueHandler;
import rapture.kernel.dp.RaptureDecisionProcessAdvanceHandler;

import com.google.common.collect.ImmutableMap;

/**
 * Instances of this class are used to invoke an appropriate response to manage
 * a message on a pipeline task queue.
 * <p>
 * The appropriate content handler is determined by the contentType field, this
 * class simply routes to the appropriate one.
 *
 * @author amkimian
 */
public class PipelineQueueHandler implements QueueHandler {

    @Override
    public String toString() {
        return "PipelineQueueHandler [handlers=" + handlers + "]";
    }

    private static final Logger log = Logger.getLogger(PipelineQueueHandler.class);
    private static final Map<String, QueueHandler> defaultHandlers;

    static {
        Map<String, QueueHandler> setupMap = new HashMap<String, QueueHandler>();
        setupMap.put("text/plain", new TextPlainHandler()); //$NON-NLS-1$
        setupMap.put(MimeReflexScript.getMimeType(), new RaptureReflexScriptHandler());
        setupMap.put(MimeReflexScriptRef.getMimeType(), new RaptureReflexScriptRefHandler());
        setupMap.put(MimeRaptureDocument.getMimeType(), new RaptureDocSaveHandler());
        setupMap.put("application/vnd.rapture.audit", new RaptureAuditHandler()); //$NON-NLS-1$
        setupMap.put(MimeIndexRebuild.getMimeType(), new RaptureIndexRebuildHandler());
        setupMap.put(MimeDocumentIndexRebuild.getMimeType(), new RaptureDocumentIndexRebuildHandler());
        setupMap.put(MimeReflexScriptResume.getMimeType(), new RaptureReflexScriptResumeHandler());
        setupMap.put(MimeScheduleReflexScriptRef.getMimeType(), new RaptureScheduleReflexScriptRefHandler());
        setupMap.put(MimeDecisionProcessAdvance.getMimeType(), new RaptureDecisionProcessAdvanceHandler());
        setupMap.put("application/vnd.rapture.event.alert", new RaptureAlertHandler());
        setupMap.put(MimeSearchUpdateObject.getMimeType(), new RaptureSearchUpdateHandler());
        defaultHandlers = Collections.unmodifiableMap(setupMap);
    }

    private Map<String, QueueHandler> handlers;

    public PipelineQueueHandler(Map<String, QueueHandler> customHandlers) {
        if (customHandlers != null && customHandlers.size() > 0) {
            this.handlers = ImmutableMap.copyOf(customHandlers);
        } else {
            this.handlers = defaultHandlers;
        }
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        log.info(String.format(Messages.getString("PipelineQueueHandler.routing"), contentType)); //$NON-NLS-1$
        if (handlers.containsKey(contentType)) {
            return handlers.get(contentType).handleMessage(tag, routing, contentType, task);
        } else {
            log.error(Messages.getString("PipelineQueueHandler.noRoute")); //$NON-NLS-1$
        }
        return false;
    }
}
