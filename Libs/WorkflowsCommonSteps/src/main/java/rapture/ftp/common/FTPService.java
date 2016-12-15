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

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.log4j.Logger;

import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;

public class FTPService {

    private static final Logger log = Logger.getLogger(FTPService.class);

    /**
     * Attempt to run this action. If it fails, retry several times. 
     * The number of times is specified in the FTPConnection retryCount
     * The time to wait between attempts is specified in the FTPConnection retryWait
     * 
     * @param errorMessage
     *            The error message to print out if we give up
     * @param connection
     *            The {@link FTPConnection} that we're working on. This will be
     *            used to reconnect, if the reconnectOnFailure flag is on
     * @param reconnectOnFailure
     *            Indicate whether we should reconnect if this fails due to
     *            something other than a timeout (e.g.
     *            {@link FTPConnectionClosedException} If the failure caused a
     *            disconnect, as opposed to a timeout, try disconnecting and
     *            reconnecting
     * @param action
     * @return
     */
    public static <T> T runWithRetry(String errorMessage, FTPConnection connection, boolean reconnectOnFailure, FTPAction<T> action) {
        T retVal = null;
        boolean success = false;
        Throwable lastException = null;
        for (int i = 0; success == false && i < connection.getRetryCount(); i++) {
            if (i > 0) {
                try {
                    Thread.sleep(connection.getRetryWait());
                } catch (InterruptedException e) {
                    log.warn("Sleep interrupted: " + ExceptionToString.format(e));
                }
            }
            try {
                retVal = action.run(i);
                success = true;
            } catch (SocketTimeoutException e) {
                log.debug("Timed out " + ExceptionToString.format(e));
                lastException = e;
            } catch (FTPConnectionClosedException e) {
                lastException = e;
                if (reconnectOnFailure) {
                    log.warn("Connection closed, will try to reconnect: " + e.getMessage());
                    reconnect(connection);
                } else {
                    log.warn("Connection closed: " + e.getMessage());
                }
            } catch (IOException e) {
                lastException = e;
                if (reconnectOnFailure) {
                    log.warn("IOException, will try to reconnect: " + ExceptionToString.summary(e));
                    log.debug(ExceptionToString.format(e));
                    reconnect(connection);
                } else {
                    log.warn("IOException: " + ExceptionToString.format(e));
                }
            }
        }

        if (!success) {
            throw RaptureExceptionFactory.create(errorMessage, lastException);
        }
        return retVal;
    }

    private static void reconnect(final FTPConnection connection) {
        FTPService.runWithRetry("Unable to disconnect!", connection, false, new FTPAction<Boolean>() {

            @Override
            public Boolean run(int attemptNum) throws IOException {
                log.info(String.format("Reconnect -- disconnecting: attempt %s of %s", 1 + attemptNum, connection.getRetryCount()));
                connection.logoffAndDisconnect();
                return true;
            }
        });

        FTPService.runWithRetry("Unable to disconnect!", connection, false, new FTPAction<Boolean>() {

            @Override
            public Boolean run(int attemptNum) throws IOException {
                log.info(String.format("Reconnect -- connecting: attempt %s of %s", 1 + attemptNum, connection.getRetryCount()));
                connection.connectAndLogin();
                return true;
            }
        });

    }
}
