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
package rapture.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.EventApi;
import rapture.common.event.EventConstants;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeReflexScriptRef;
import rapture.common.model.EventMessage;
import rapture.common.model.RaptureEvent;
import rapture.common.model.RaptureEventMessage;
import rapture.common.model.RaptureEventNotification;
import rapture.common.model.RaptureEventScript;
import rapture.common.model.RaptureEventStorage;
import rapture.common.model.RaptureEventWorkflow;
import rapture.common.model.RunEventHandle;
import rapture.common.pipeline.PipelineConstants;
import rapture.idgen.SystemIdGens;
import rapture.kernel.pipeline.TaskSubmitter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

public class EventApiImpl extends KernelBase implements EventApi {

    public EventApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    private static Logger log = Logger.getLogger(EventApiImpl.class);

    @Override
    public List<RaptureFolderInfo> listEventsByUriPrefix(CallingContext context, String eventUriPrefix) {
        return RaptureEventStorage.getChildren(new RaptureURI(eventUriPrefix, Scheme.EVENT).getShortPath());
    }

    // [testing for SDK-5]
    @Override
    public void addEventScript(CallingContext context, String eventUri, String scriptUri, Boolean performOnce) {
        // Get script from the repo, if it exists, add to it. If it
        // doesn't, create one

        RaptureEvent event = getEvent(context, eventUri);

        RaptureEventScript newScript = new RaptureEventScript();
        newScript.setFireCount(0L);
        newScript.setRunOnce(performOnce);
        newScript.setScriptURI(scriptUri);

        if (event == null) {
            event = new RaptureEvent();
            event.setUriFullPath(new RaptureURI(eventUri, Scheme.EVENT).getFullPath());
        }

        if (event.getScripts() == null) {
            Set<RaptureEventScript> events = Sets.newHashSet();
            event.setScripts(events);
        }

        boolean found = false;
        for (RaptureEventScript res : event.getScripts()) {
            if (res.getScriptURI().equals(scriptUri)) {
                res.setRunOnce(performOnce);
                found = true;
            }
        }
        if (!found) {
            event.getScripts().add(newScript);
        }
        putEvent(context, event);
    }

    @Override
    public Boolean runEvent(final CallingContext context, String eventUri, final String associatedUri, final String eventContext) {
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put(EventConstants.STRING_CONTEXT_KEY, eventContext);
        return runEventWithContext(context, eventUri, associatedUri, contextMap).getDidRun();
    }

    @Override
    public RunEventHandle runEventWithContext(CallingContext context, String eventUri, String associatedUri, Map<String, String> eventContextMap) {
        RaptureURI eUri = new RaptureURI(eventUri, Scheme.EVENT);
        RunEventHandle handle = new RunEventHandle();
        handle.setEventUri(eUri.toString());
        handle.setDidRun(false);
        final RaptureEvent event = getEvent(context, eventUri);
        if (event != null) {
            String eventId = generateEventId(context);
            handle.setEventId(eventId);
            Kernel.getStackContainer().pushStack(context, eUri.toString());
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(EventConstants.ASSOCIATED_URI, associatedUri);
            String eventContext = JacksonUtil.jsonFromObject(eventContextMap);
            params.put(EventConstants.EVENT_CONTEXT, eventContext);
            RaptureURI eventURIFull = new RaptureURI(eventUri, Scheme.EVENT);

            final String activityId = Kernel.getActivity().createActivity(context, eventURIFull.toString(), "Firing Event", 1L, 100L);
            boolean didAnythingFire = false;
            if (event.getScripts() != null && !event.getScripts().isEmpty()) {
                log.info("Scripts attached to event - firing");
                didAnythingFire = true;
                for (RaptureEventScript res : event.getScripts()) {
                    MimeReflexScriptRef scriptRef = new MimeReflexScriptRef();
                    scriptRef.setScriptURI(res.getScriptURI());
                    scriptRef.setParameters(params);
                    Kernel.getActivity().updateActivity(context, activityId, "Signal script " + res.getScriptURI(), 1L, 100L);
                    TaskSubmitter.submitLoadBalancedToCategory(context, scriptRef, MimeReflexScriptRef.getMimeType(), PipelineConstants.CATEGORY_ALPHA);
                }
            }
            if (event.getMessages() != null && !event.getMessages().isEmpty()) {
                log.info("Messages attached to event - firing");
                didAnythingFire = true;
                for (RaptureEventMessage msg : event.getMessages()) {
                    RapturePipelineTask pTask = new RapturePipelineTask();
                    pTask.setPriority(1);
                    pTask.setContentType(msg.getName());
                    pTask.setCategoryList(ImmutableList.of(msg.getPipeline()));
                    pTask.initTask();
                    EventMessage eMsg = new EventMessage();
                    eMsg.setContext(params);
                    eMsg.setParams(msg.getParams());
                    pTask.addMimeObject(eMsg);
                    log.info("Publishing message to " + msg.getPipeline());
                    Kernel.getActivity().updateActivity(context, activityId, "Publish message " + msg.getName(), 1L, 100L);
                    Kernel.getPipeline().getTrusted().publishMessageToCategory(context, pTask);
                }
            }
            if (event.getNotifications() != null && !event.getNotifications().isEmpty()) {
                log.info("Notifications attached to event - firing");
                didAnythingFire = true;
                for (RaptureEventNotification not : event.getNotifications()) {
                    EventMessage eMsg = new EventMessage();
                    eMsg.setContext(params);
                    eMsg.setParams(not.getParams());
                    log.info("Publishing notification to " + not.getNotification());
                    Kernel.getActivity()
                            .updateActivity(context, activityId, "Publish notification " + not.getName(), 1L, 100L);
                    Kernel.getNotification()
                            .publishNotification(context, not.getNotification(), associatedUri, JacksonUtil.jsonFromObject(eMsg), "EventMessage");
                }

            }
            if (event.getWorkflows() != null && !event.getWorkflows().isEmpty()) {
                didAnythingFire = true;
                log.info("Workflows attached to event - firing");
                for (RaptureEventWorkflow workflow : event.getWorkflows()) {
                    Map<String, String> contextMap = new HashMap<String, String>();
                    contextMap.putAll(workflow.getParams());
                    contextMap.put("associatedURI", associatedUri);
                    contextMap.put("eventId", eventId);
                    contextMap.put("eventContext", eventContext);
                    log.info("Starting " + workflow.getWorkflow());
                    Kernel.getActivity().updateActivity(context, activityId, "Run workflow " + workflow.getName(), 1L, 100L);
                    Kernel.getDecision().createWorkOrder(context, workflow.getWorkflow(), contextMap);

                }
            }

            Kernel.getStackContainer().popStack(context);
            Kernel.getActivity().finishActivity(context, activityId, "Fired Event");
            handle.setDidRun(didAnythingFire);
        }

        if (handle.getDidRun()) {
            Kernel.getKernel().getStat().registerEventRun();
        }

        return handle;
    }

    private String generateEventId(CallingContext context) {
        return Kernel.getIdGen().nextIds(context, SystemIdGens.EVENT_IDGEN_URI, 1L);
    }

    @Override
    public void deleteEventScript(CallingContext context, String eventUri, String scriptUri) {
        RaptureEvent event = getEvent(context, eventUri);
        if (event != null) {
            RaptureEventScript toRemove = null;
            for (RaptureEventScript res : event.getScripts()) {
                if (res.getScriptURI().equals(scriptUri)) {
                    toRemove = res;
                    break;
                }
            }
            if (toRemove != null) {
                event.getScripts().remove(toRemove);
                putEvent(context, event);
            }
        }
    }

    @Override
    public RaptureEvent getEvent(CallingContext context, String eventURI) {
        RaptureURI addressURI = new RaptureURI(eventURI, Scheme.EVENT);
        RaptureEvent event = RaptureEventStorage.readByAddress(addressURI);
        if (event != null && event.getMessages() == null) event.setMessages(Sets.<RaptureEventMessage> newHashSet());
        return event;
    }

    @Override
    public void putEvent(CallingContext context, RaptureEvent event) {
        RaptureEventStorage.add(event, context.getUser(), "Adding event");
    }

    @Override
    public void deleteEvent(CallingContext context, String eventURI) {
        RaptureURI addressURI = new RaptureURI(eventURI, Scheme.EVENT);
        RaptureEventStorage.deleteByAddress(addressURI, context.getUser(), "Removing event");
    }

    // testing for tracking remote
    @Override
    public void addEventMessage(CallingContext context, String eventUri, String name, String pipeline, Map<String, String> params) {
        // Get script from the repo, if it exists, add to it. If it
        // doesn't, create one

        RaptureEvent event = getEvent(context, eventUri);

        RaptureEventMessage newMessage = new RaptureEventMessage();
        newMessage.setName(name);
        newMessage.setParams(params);
        newMessage.setPipeline(pipeline);

        if (event == null) {
            event = new RaptureEvent();
            event.setUriFullPath(new RaptureURI(eventUri, Scheme.EVENT).getFullPath());
        }

        if (event.getMessages() == null) {
            Set<RaptureEventMessage> messages = Sets.newHashSet();
            event.setMessages(messages);
        }

        boolean found = false;
        for (RaptureEventMessage msg : event.getMessages()) {
            if (msg.getName().equals(name)) {
                msg.setParams(params);
                msg.setPipeline(pipeline);
                found = true;
            }
        }
        if (!found) {
            event.getMessages().add(newMessage);
        }
        putEvent(context, event);
    }

    @Override
    public void deleteEventMessage(CallingContext context, String eventUri, String name) {
        RaptureEvent event = getEvent(context, eventUri);
        if (event != null) {
            RaptureEventMessage toRemove = null;
            for (RaptureEventMessage res : event.getMessages()) {
                if (res.getName().equals(name)) {
                    toRemove = res;
                    break;
                }
            }
            if (toRemove != null) {
                event.getMessages().remove(toRemove);
                putEvent(context, event);
            }
        }
    }

    public void addEventNotification(CallingContext context, String eventUri, String name, String notification, Map<String, String> params) {
        // Get script from the repo, if it exists, add to it. If it
        // doesn't, create one

        RaptureEvent event = getEvent(context, eventUri);

        RaptureEventNotification newNotification = new RaptureEventNotification();
        newNotification.setName(name);
        newNotification.setParams(params);
        newNotification.setNotification(notification);

        if (event == null) {
            event = new RaptureEvent();
            event.setUriFullPath(new RaptureURI(eventUri, Scheme.EVENT).getFullPath());
        }

        if (event.getNotifications() == null) {
            Set<RaptureEventNotification> notifications = Sets.newHashSet();
            event.setNotifications(notifications);
        }

        boolean found = false;
        for (RaptureEventNotification not : event.getNotifications()) {
            if (not.getName().equals(name)) {
                not.setParams(params);
                not.setNotification(notification);
                found = true;
            }
        }
        if (!found) {
            event.getNotifications().add(newNotification);
        }
        putEvent(context, event);
    }

    @Override
    public void deleteEventNotification(CallingContext context, String eventUri, String name) {
        RaptureEvent event = getEvent(context, eventUri);
        if (event != null) {
            RaptureEventNotification toRemove = null;
            for (RaptureEventNotification res : event.getNotifications()) {
                if (res.getName().equals(name)) {
                    toRemove = res;
                    break;
                }
            }
            if (toRemove != null) {
                event.getNotifications().remove(toRemove);
                putEvent(context, event);
            }
        }
    }

    @Override
    public void addEventWorkflow(CallingContext context, String eventUri, String name, String workflowUri,
            Map<String, String> params) {
        // Get script from the repo, if it exists, add to it. If it
        // doesn't, create one

        RaptureEvent event = getEvent(context, eventUri);

        RaptureEventWorkflow newWorkflow = new RaptureEventWorkflow();
        newWorkflow.setName(name);
        newWorkflow.setParams(params);
        newWorkflow.setWorkflow(workflowUri);

        if (event == null) {
            event = new RaptureEvent();
            event.setUriFullPath(new RaptureURI(eventUri, Scheme.EVENT).getFullPath());
        }

        if (event.getWorkflows() == null) {
            Set<RaptureEventWorkflow> workflows = Sets.newHashSet();
            event.setWorkflows(workflows);
        }

        boolean found = false;
        for (RaptureEventWorkflow not : event.getWorkflows()) {
            if (not.getName().equals(name)) {
                not.setParams(params);
                not.setWorkflow(workflowUri);
                found = true;
            }
        }
        if (!found) {
            event.getWorkflows().add(newWorkflow);
        }
        putEvent(context, event);
    }

    @Override
    public void deleteEventWorkflow(CallingContext context, String eventUri, String name) {
        RaptureEvent event = getEvent(context, eventUri);
        if (event != null) {
            RaptureEventWorkflow toRemove = null;
            for (RaptureEventWorkflow res : event.getWorkflows()) {
                if (res.getName().equals(name)) {
                    toRemove = res;
                    break;
                }
            }
            if (toRemove != null) {
                event.getWorkflows().remove(toRemove);
                putEvent(context, event);
            }
        }
    }

    /**
     * Simple method to determine whether the specified event exists. Return false on any error.
     */
    @Override
    public Boolean eventExists(CallingContext context, String eventUri) {
        RaptureEvent event = null;
        try {
            event = getEvent(context, eventUri);
        } catch (Exception e) {
            log.trace("Event " + eventUri + " does not exist", e);
        }
        return (event != null);
    }

    @Override
    public List<String> deleteEventsByUriPrefix(CallingContext context, String uriPrefix){
        List<RaptureFolderInfo> rfis = RaptureEventStorage.removeFolder(uriPrefix);
        List<String> deletedEvents = new ArrayList<String>();
        for(RaptureFolderInfo rfi : rfis) {
            deletedEvents.add(rfi.getName());
        }
        return deletedEvents;
    }
}
