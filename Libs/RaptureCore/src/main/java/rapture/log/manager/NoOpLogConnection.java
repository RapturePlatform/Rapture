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
package rapture.log.manager;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rapture.common.LogQueryResponse;
import rapture.log.management.ConnectionException;
import rapture.log.management.LogManagerConnection;
import rapture.log.management.LogReadException;
import rapture.log.management.SessionExpiredException;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 3/5/15.
 */
public class NoOpLogConnection implements LogManagerConnection {
    @Override
    public String searchByWorkOrder(String workOrderURI, Optional<String> stepName, Optional<Long> stepStartTime, long startTime, long endTime,
            int scrollKeepAlive, int bufferSize) throws LogReadException {
        return null;
    }

    @Override
    public String searchByFields(Map<String, String> fieldToQuery, long startTime, long endTime, int scrollKeepAlive, int bufferSize) throws LogReadException {
        return null;
    }

    @Override
    public LogQueryResponse getScrollingResults(String scrollId, int scrollKeepAlive) throws LogReadException, SessionExpiredException {
        return null;
    }

    @Override
    public void connect() {

    }

    @Override
    public void waitForConnection(long timeout, TimeUnit timeUnit) throws ConnectionException, TimeoutException, InterruptedException {

    }

    @Override
    public void close() {
    }
}
