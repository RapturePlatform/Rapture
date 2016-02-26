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
package rapture.log.management;

import rapture.common.LogQueryResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 2/23/15.
 */
public interface LogManagerConnection extends AutoCloseable {

    /**
     * Get log messages filtered by a workOrderURI.
     * <p/>
     * This method just initiates the search and returns a scrollId, which is a handle that is then passed to
     * {@link #getScrollingResults(String, int)}
     *
     * @param workOrderURI    The work order URI to match against
     * @param stepName        The name of a step to filter by, if there is one, or Optional.absent() otherwise.
     * @param stepStartTime   Filter by the stepStartTime mdc variable
     * @param startTime       The start time for the query
     * @param endTime         The end time for the query
     * @param scrollKeepAlive How long should the query be cached for scrolling/pagination purposes, in minutes. Cannot be larger than 10. See {@link
     *                        #searchByFields and #getScrollingResults for details}
     * @param bufferSize      How many results to return. Cannot be > 1000
     * @return A scrollId that can be passed to {@link #getScrollingResults(String, int)}
     */
    public String searchByWorkOrder(String workOrderURI, Optional<String> stepName, Optional<Long> stepStartTime, long startTime, long endTime,
            int scrollKeepAlive, int bufferSize)
            throws LogReadException;

    /**
     * Get log messages filtered by a specified field in the log
     * <p/>
     * This method just initiates the search and returns a scrollId, which is a handle that is then passed to
     * {@link #getScrollingResults(String, int)}
     *
     * @param fieldToQuery    A map of field name to the query string that must match on the field
     * @param startTime       The start time for the query
     * @param endTime         The end time for the query
     * @param scrollKeepAlive How long should the query be cached for scrolling/pagination purposes, in minutes. Cannot be larger than 10. If there are no
     *                        more results left to query outside of the current set, the return object will have isFinished set to true, and no more
     *                        scrolling is needed.
     * @param bufferSize      How many results to return. Cannot be > 1000
     * @return A scrollId that can be passed to {@link #getScrollingResults(String, int)}
     */
    public String searchByFields(Map<String, String> fieldToQuery, long startTime, long endTime, int scrollKeepAlive, int bufferSize)
            throws LogReadException;

    /**
     * Get the next set of results for a scrolling query
     *
     * @param scrollId        The scrollId returned from the previous result set
     * @param scrollKeepAlive How long should the query be cached for scrolling/pagination purposes, in minutes. Cannot be larger than 10
     * @return
     */
    public LogQueryResponse getScrollingResults(String scrollId, int scrollKeepAlive) throws LogReadException, SessionExpiredException;

    /**
     * Start the connection. Without the connection being started, you cannot call any of the other methods. Connect should not block, it should return
     * immediately. Call {@link #waitForConnection(long, java.util.concurrent.TimeUnit)} if you want to wait
     */
    public void connect();

    /**
     * Waits until the connection is ready
     *
     * @param timeout
     * @param timeUnit
     * @throws ConnectionException
     */
    public void waitForConnection(long timeout, TimeUnit timeUnit) throws ConnectionException, TimeoutException, InterruptedException;

    /**
     * Close the client, no exception thrown
     */
    @Override
    public void close();
}