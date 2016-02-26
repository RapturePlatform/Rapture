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
package rapture.common.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The URLStateManager takes a list of URLS (which can be provided as a ;
 * separated string)
 * 
 * and assigns them into a series of "buckets" based on state (UP, DOWN,
 * MAYBEUP, REALLYDOWN). A URL starts in the UP state, and then can be moved to
 * DOWN by a caller if they detect an error on the connection. Periodically the
 * DOWN entries are moved to MAYBEUP, and MAYBEUP are equivalent to UP in terms
 * of returning entries for connections. If a MAYBEUP is moved down because of
 * an error it moves to REALLYDOWN, and that takes longer for it to come back to
 * DOWN.
 * 
 * So, an endpoint that is not available will do:
 * 
 * UP -> DOWN -> MAYBEUP -> REALLYDOWN -> DOWN -> MAYBEUP -> REALLYDOWN
 * 
 * a transient failure will:
 * 
 * UP -> DOWN -> MAYBEUP -> UP
 * 
 * a longer failure then resumption will do this:
 * 
 * UP -> DOWN -> MAYBEUP -> REALLYDOWN -> DOWN -> MAYBEUP -> UP
 * 
 * @author amkimian
 * 
 */
public class URLStateManager {
    private Map<URLState, List<URLStateEntry>> stateManager = new ConcurrentHashMap<URLState, List<URLStateEntry>>();
    private Thread resetStateThread;
    private Object accessObject;

    private void setup(String[] urls) {
        List<URLStateEntry> upStates = new ArrayList<URLStateEntry>(urls.length);
        for (String url : urls) {
            URLStateEntry entry = new URLStateEntry(url);
            upStates.add(entry);
        }
        stateManager.put(URLState.UP, upStates);
        stateManager.put(URLState.DOWN, new ArrayList<URLStateEntry>());
        stateManager.put(URLState.MAYBEUP, new ArrayList<URLStateEntry>());
        stateManager.put(URLState.REALLYDOWN, new ArrayList<URLStateEntry>());
        resetStateThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    refreshStates();
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        accessObject = new Object();
        resetStateThread.setDaemon(true);
        resetStateThread.setName("RaptureURLStateRefresher");
        resetStateThread.start();
    }

    public URLStateManager(String url) {
        if (url.contains(";")) {
            setup(url.split(";"));
        } else {
            setup(new String[] { url });
        }
    }

    public String getURL() {
        synchronized (accessObject) {
            List<String> urlToPick = new ArrayList<String>();
            for (URLStateEntry entry : stateManager.get(URLState.UP)) {
                urlToPick.add(entry.getUrl());
            }
            for (URLStateEntry entry : stateManager.get(URLState.MAYBEUP)) {
                urlToPick.add(entry.getUrl());
            }
            if (urlToPick.isEmpty()) {
                return null;
            } else {
                Collections.shuffle(urlToPick);
                return urlToPick.get(0);
            }
        }
    }

    public void markURLBad(String url) {
        // If the URL is in UP or MAYBEUP, handle the movement, setting the
        // "next look date" appropriately
        // We won't have that many URLs, so a linear search should be fine at
        // this point

        synchronized (accessObject) {
            if (!maybeMoveEntry(url, URLState.UP, URLState.DOWN, 30)) {
                maybeMoveEntry(url, URLState.MAYBEUP, URLState.REALLYDOWN, 600);
            }
        }
    }

    public void refreshStates() {
        synchronized (accessObject) {
            refreshState(URLState.MAYBEUP, URLState.UP, 0);
            refreshState(URLState.DOWN, URLState.MAYBEUP, 30);
            refreshState(URLState.REALLYDOWN, URLState.DOWN, 30);
        }
    }

    private void refreshState(URLState from, URLState to, int rejuveTime) {
        List<URLStateEntry> entriesToMove = new ArrayList<URLStateEntry>();

        for (URLStateEntry entry : stateManager.get(from)) {
            if (entry.rejuve()) {
                entriesToMove.add(entry);
            }
        }

        if (!entriesToMove.isEmpty()) {
            for (URLStateEntry entry : entriesToMove) {
                stateManager.get(from).remove(entry);
                entry.setRejuveTimeFromNow(rejuveTime);
                stateManager.get(to).add(entry);
            }
        }
    }

    private boolean maybeMoveEntry(String url, URLState from, URLState to, int rejuveTime) {
        URLStateEntry entryToMove = null;

        for (URLStateEntry entry : stateManager.get(from)) {
            if (entry.getUrl().equals(url)) {
                entryToMove = entry;
                break;
            }
        }

        if (entryToMove != null) {
            entryToMove.setRejuveTimeFromNow(rejuveTime);
            stateManager.get(from).remove(entryToMove);
            stateManager.get(to).add(entryToMove);
            return true;
        }
        return false;
    }
}
