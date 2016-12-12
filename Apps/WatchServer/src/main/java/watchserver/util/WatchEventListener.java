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
package watchserver.util;

import java.io.File;
import java.util.List;

import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import watchserver.util.EventType;

public class WatchEventListener implements FileListener {
    private static Logger log = Logger.getLogger(WatchEventListener.class);    
    private List<Event> events;
    private String folder;
    
    public WatchEventListener(List<Event> events,String folder){
        this.events = events;
        
        if (folder.charAt(folder.length() - 1) != File.separatorChar) {
            folder += File.separator;
        }
        
        this.folder = folder;
        log.debug("Loaded the following events: " + events.toString());
    }
    
    public void fileCreated( FileChangeEvent fileChangeEvent ) throws Exception {
        log.info( "file created : " + folder + FilenameUtils.getName(fileChangeEvent.getFile().getName().getBaseName()));
        callRaptureAction(EventType.CREATE, fileChangeEvent);
    }
    
    public void fileDeleted( FileChangeEvent fileChangeEvent ) throws Exception {
        log.info( "file deleted : " + folder + FilenameUtils.getName(fileChangeEvent.getFile().getName().getBaseName()));
        callRaptureAction(EventType.DELETE, fileChangeEvent);
    }
    
    public void fileChanged( FileChangeEvent fileChangeEvent ) throws Exception {
        log.info( "file changed : " + folder + FilenameUtils.getName(fileChangeEvent.getFile().getName().getBaseName()));
        callRaptureAction(EventType.MODIFY, fileChangeEvent);
    }
    
    private void callRaptureAction(EventType et, FileChangeEvent fe){
        String actionForEvent = getActionForEvent(et);
        
        if (!actionForEvent.isEmpty()) {
            //get the action type
            Scheme scheme = new RaptureURI(actionForEvent).getScheme();
            ImmutableMap<String, String> params = ImmutableMap.of("filetoprocess", fe.getFile().getName().toString());
            
            switch (scheme){
                case SCRIPT:
                    String runScript = Kernel.getScript().runScript(ContextFactory.getKernelUser(), actionForEvent, params);
                    log.info("Started script: " + runScript + " with params: " + params.toString());
                    break;
                case WORKFLOW:
                    String createWorkOrder = Kernel.getDecision().createWorkOrder(ContextFactory.getKernelUser(), actionForEvent, params);
                    log.info("Started workorder: " + createWorkOrder + " with params: " + params.toString());
                    break;
                default:
                    log.error(scheme + " is not supported for " + folder + FilenameUtils.getName(fe.getFile().getName().getBaseName()));
                    break;
            }
        } else {
            log.error(et + " has no associated action for monitor on " + folder);
        }
        
    }
 
    private String getActionForEvent(EventType eventToFind){
        String actionUri = "";
        for(Event event:events){
            EventType eventItem = event.getEvent();
            if(eventToFind.equals(eventItem)) {
                log.debug("Found a match for " + eventItem + " with action " + event.getAction());
                actionUri = event.getAction();
                break;
            }
        }
        return actionUri;
    }
}