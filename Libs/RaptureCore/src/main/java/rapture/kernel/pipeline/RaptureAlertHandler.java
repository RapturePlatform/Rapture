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
import rapture.common.event.DPEventConstants;
import rapture.common.event.EventConstants;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.EventMessage;
import rapture.dp.event.DecisionProcessEvent;
import rapture.dp.event.WorkOrderStatusUpdateEvent;
import rapture.event.RaptureAlertEvent;
import rapture.exchange.QueueHandler;
import rapture.kernel.alert.AlerterFactory;
import rapture.kernel.alert.EventAlerter;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class RaptureAlertHandler implements QueueHandler {

    private static final Logger log = Logger.getLogger(RaptureAlertHandler.class);
    private static final Map<String, Class<? extends DecisionProcessEvent>> EVENT_TYPE_MAP = initEventTypeMap();
    private final PipelineTaskStatusManager statusManager;

    public RaptureAlertHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    private static Map<String, Class<? extends DecisionProcessEvent>> initEventTypeMap() {
        Map<String, Class<? extends DecisionProcessEvent>> map = new HashMap<>();
        map.put(WorkOrderStatusUpdateEvent.TYPE, WorkOrderStatusUpdateEvent.class);
        return map;
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        log.info("Attempting to send alerts");
        try {
            statusManager.startRunning(task);

            // eventMessageContext = {associated_uri=, event_context={event_type=, event_content={status=}}}
            final EventMessage eventMessage = JacksonUtil.objectFromJson(task.getContent(), EventMessage.class);
            final Map<String, Object> eventMessageContext = eventMessage.getContext();
            String eventContextJson = (String) eventMessageContext.get(EventConstants.EVENT_CONTEXT);
            RaptureAlertEvent event = getEventFromJson(eventContextJson);
            for (EventAlerter alerter : AlerterFactory.getAlerters(event)) {
                alerter.alert(event);
            }

            statusManager.finishRunningWithSuccess(task);
        } catch (Exception e) {
            log.error("Failed to send alerts", e);
            statusManager.finishRunningWithFailure(task);
        }
        return true;
    }

    @Override
    public String toString() {
        return "RaptureAlertHandler [statusManager=" + statusManager + "]";
    }

    private RaptureAlertEvent getEventFromJson(String eventContextString) {
        // event_context={event_type=, event_content={status=}}
        Map<String, Object> eventContext = JacksonUtil.getMapFromJson(eventContextString);
        String eventType = (String) eventContext.get(DPEventConstants.EVENT_TYPE);
        if (EVENT_TYPE_MAP.containsKey(eventType)) {
            String eventContent = (String) eventContext.get(DPEventConstants.EVENT_CONTENT);
            return (RaptureAlertEvent) JacksonUtil.objectFromJson(eventContent, EVENT_TYPE_MAP.get(eventType));
        } else {
            log.error("Unrecognized event type " + eventType);
            return null;
        }
    }
}
