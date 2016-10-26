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

import java.util.List;

public interface Connection {

    boolean connectAndLogin();

    boolean isConnected();

    /**
     * Log off and terminate the connection, e.g. close the connection socket.
     * 
     * If there is a failure in the implementation of this method, it may or may
     * not be appropriate to throw an Exception. Throwing an exception will
     * cause the entire Workflow to end in error. If this is not appropriate,
     * take a look at {@link WarningHandler}, which can be used to log a
     * warning.
     */
    void logoffAndDisconnect();

    public boolean doActions(List<FTPRequest> requests);

    public boolean doAction(FTPRequest request);

    public List<String> listFiles(String path);

}
