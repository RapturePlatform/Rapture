/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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

package rapture.helper;

import rapture.common.SearchResponse;

public interface QueryWithRetry {

    // Either implement this method (Java 7) or use Lambda syntax (Java 8)
    // SearchResponse res = QueryWithRetry.query(3, 5, () -> { return searchApi.searchWithCursor(CallingContext, CursorID, Count, Query); });

    public abstract SearchResponse doTheQuery();

    /**
     * Call doTheQuery a number of times. Wait 1s between each call. If a query returns the expected number of hits then return immediately.
     * 
     * @param expect
     * @param wait
     * @param q
     * @return
     */
    public static SearchResponse query(int expect, int wait, QueryWithRetry q) {
        int waitCount = wait;
        SearchResponse resp = q.doTheQuery();
        while (--waitCount > 0) {
            resp = q.doTheQuery();
            if (resp.getSearchHits().size() == expect) break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            resp = q.doTheQuery();
        }
        return resp;
    }
}