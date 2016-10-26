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
package rapture.ftp.common;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

public class FTPRequest {
    @Override
    public String toString() {
        return "FTPRequest [localName=" + localName + ", source=" + source + ", remoteName=" + remoteName + ", destination=" + destination + ", isEmpty="
                + isEmpty + ", isLocal=" + isLocal + ", action=" + action + ", status=" + status + "]";
    }

    private static final Logger log = Logger.getLogger(FTPRequest.class);

    public enum Action {
        READ, WRITE, EXISTS
    }

    public enum Status {
        READY, SUCCESS, ERROR
    }

    /**
     * TODO Allow user to specify RaptureURI as localName or remoteName and automatically read the data from or write it to Rapture
     */
    private String localName;
    private InputStream source = null;
    private String remoteName;
    private OutputStream destination = null;
    private boolean isEmpty = true;
    private boolean isLocal = false;
    final private Action action;
    private Object result = null;

    private Status status = Status.READY;

    public FTPRequest(Action action) {
        this.action = action;
    }

    public String getLocalName() {
        return localName;
    }

    public FTPRequest setLocalName(String localName) {
        this.localName = localName;
        return this;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public InputStream getSource() {
        return source;
    }

    public FTPRequest setSource(InputStream source) {
        if (action == Action.READ) throw new IllegalArgumentException("Cannot set Source on READ - use RemoteName");
        this.source = source;
        return this;
    }

    public OutputStream getDestination() {
        return destination;
    }

    public FTPRequest setDestination(OutputStream destination) {
        if (action == Action.WRITE) throw new IllegalArgumentException("Cannot set Destination on WRITE - use LocalName");
        this.destination = destination;
        return this;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public FTPRequest setRemoteName(String remoteName) {
        this.remoteName = remoteName;
        return this;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public FTPRequest setEmpty(boolean isEmpty) {
        this.isEmpty = isEmpty;
        return this;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public FTPRequest setLocal(boolean isLocal) {
        this.isLocal = isLocal;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public Status getStatus() {
        return status;
    }

    public FTPRequest setStatus(Status status) {
        this.status = status;
        return this;
    }
}
