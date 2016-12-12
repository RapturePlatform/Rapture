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
package watchserver.server;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.log4j.Logger;

import watchserver.util.FTPConfig;
import watchserver.util.LocalConfig;
import watchserver.util.WatchEventListener;

public class WatchLocalRunner implements Runnable {
    private volatile Thread thread;
    private static Logger log = Logger.getLogger(WatchLocalRunner.class);
    private FileSystemManager fsManager;
    private FileSystem fs;
    private DefaultFileMonitor fm;
    private FileObject resolvedAbsPath;
    private FileSystemOptions opts = new FileSystemOptions();
    private WatchEventListener listener;
    private LocalConfig config;
    
    public WatchLocalRunner(LocalConfig config) {
        this.config = config;
        try { 
            fsManager = VFS.getManager();
            resolvedAbsPath = fsManager.resolveFile(config.getFolder(), opts);
            fs = resolvedAbsPath.getFileSystem();
            log.info("Connection successfully established to " + resolvedAbsPath.getName().getPath());
            
        } catch (FileSystemException e) {
            log.error("File system exception for " + config.getFolder(), e);
            //throw here?
        }
    }

    /**
     * Start a worker thread to listen for directory changes.
     */
    public void startThread() {
        thread = new Thread(this);
        listener = new WatchEventListener(config.getEvents(), config.getFolder());
        fm = new DefaultFileMonitor(listener);
        fm.setRecursive(false); 
        fm.addFile(resolvedAbsPath);
        fm.start();
        log.debug("Monitoring directory: " + resolvedAbsPath.toString());
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
        while(true){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
               log.error("Interrupted Exception for Thread",e);
            }
        }
    }
}
