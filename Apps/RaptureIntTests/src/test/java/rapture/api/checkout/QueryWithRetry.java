package rapture.api.checkout;

import rapture.common.SearchResponse;

interface QueryWithRetry {

    // Either implement this method (Java 7) or use Lambda syntax (Java 8)
    // SearchResponse res = QueryWithRetry.query(3, 5, () -> { return searchApi.searchWithCursor(CallingContext, CursorID, Count, Query); });

    abstract SearchResponse doTheQuery();

    /**
     * Call doTheQuery a number of times. Wait 1s between each call. If a query returns the expected number of hits then return immediately.
     * 
     * @param expect
     * @param wait
     * @param q
     * @return
     */
    static SearchResponse query(int expect, int wait, QueryWithRetry q) {
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