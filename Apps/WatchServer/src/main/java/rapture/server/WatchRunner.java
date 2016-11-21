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
package rapture.server;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class WatchRunner implements Runnable {

    private volatile Thread thread;
    private WatchService watchService = null;
    private String dirPath;
    private static Logger log = Logger.getLogger(WatchRunner.class);
    private List<Map> config = new ArrayList<Map>();

    public WatchRunner(String path, List<Map> config) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            this.dirPath = path;
            this.config = config;
        } catch (IOException e) {
            log.error(e.getMessage() + ":" + e.getCause());
        }
    }

    /**
     * Start a worker thread to listen for directory changes.
     */
    public void startThread() {
        thread = new Thread(this);
        // TODO: support each watch to have a poll frequency
        try {
            Paths.get(dirPath).register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            log.info("Monitoring directory: " + dirPath);
        } catch (IOException e) {
            log.error(e.getMessage() + ":" + e.getCause());
        }
        thread.start();
    }

    /**
     * Flag worker thread to stop gracefully.
     */
    public void stopThread() {
        if (thread != null) {
            Thread runningThread = thread;
            thread = null;
            runningThread.interrupt();
        }
    }

    @Override
    public void run() {
        while (true) {
            WatchKey watchKey = null;
            try {
                watchKey = watchService.take();
                if (watchKey != null) {
                    for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
                        if (watchEvent.kind() == OVERFLOW) {
                            log.error("Overflow event count: " + watchEvent.count());
                            continue;
                        }
                        // TODO: Add support to run scripts with an event.
                        // TODO: Check filepath is correct
                        String actionUri = getActionForEvent(this.dirPath, watchEvent.kind().name());
                        if (!actionUri.isEmpty()) {
                            String scheme = new RaptureURI(actionUri).getScheme().name();
                            log.debug("event created for path: " + this.dirPath);
                            ImmutableMap<String, String> params = ImmutableMap.of("filetoupload", this.dirPath + watchEvent.context());

                            if (scheme.equals(Scheme.SCRIPT.name())) {
                                String runScript = Kernel.getScript().runScript(ContextFactory.getKernelUser(), actionUri, params);
                                log.info("Started script: " + runScript + " with params: " + params.toString());
                            }
                            if (scheme.equals(Scheme.WORKFLOW.name())) {
                                String createWorkOrder = Kernel.getDecision().createWorkOrder(ContextFactory.getKernelUser(), actionUri, params);
                                log.info("Started workorder: " + createWorkOrder + " with params: " + params.toString());
                            }
                        } else {
                            log.debug("Event: " + watchEvent.kind().name() + " not set for directory: " + this.dirPath);
                        }
                    }
                    watchKey.reset();
                }
            } catch (InterruptedException e) {
                log.error("Thread ID: " + Thread.currentThread().getId() + " interrupted!", e);
            }
        }
    }

    /**
     * Get the config setting for the Directory path and event type
     */
    private String getActionForEvent(String dir, String event) {
        String actionUri = "";
        for (int i = 0; i < config.size(); i++) {
            if (config.get(i).get("event").equals(event)) {
                actionUri = (String) config.get(i).get("action");
                break;
            }
        }
        return actionUri;
    }
}
