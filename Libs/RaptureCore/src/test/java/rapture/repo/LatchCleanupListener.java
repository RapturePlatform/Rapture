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
package rapture.repo;

import rapture.series.children.cleanup.CleanupListener;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * @author bardhi
 * @since 5/11/15.
 */
class LatchCleanupListener implements CleanupListener {
    private final String repoName;
    private final boolean isUsesCleanupService;
    private CountDownLatch deletedLatch;
    private CountDownLatch ignoredLatch;
    private Set<String> ignored = Collections.synchronizedSet(new LinkedHashSet<String>());
    private Set<String> deleted = Collections.synchronizedSet(new LinkedHashSet<String>());

    public List<String> getIgnored() {
        return new LinkedList<>(ignored);
    }

    public List<String> getDeleted() {
        return new LinkedList<>(deleted);
    }

    public LatchCleanupListener(String repoName, boolean isUsesCleanupService) {
        this.repoName = repoName;
        this.isUsesCleanupService = isUsesCleanupService;
    }

    public CountDownLatch getIgnoredLatch() {
        return ignoredLatch;
    }

    public void resetLatches(int countDeleted, int countIgnored) {
        if (isUsesCleanupService) {
            deletedLatch = new CountDownLatch(countDeleted);
            ignoredLatch = new CountDownLatch(countIgnored);
        } else {
            deletedLatch = new CountDownLatch(0);
            ignoredLatch = new CountDownLatch(0);
        }
    }

    public CountDownLatch getDeletedLatch() {
        return deletedLatch;
    }

    @Override
    public void deleted(String repoDescription, String folderPath) {
        if (repoDescription.contains(repoName)) {
            deletedLatch.countDown();
            deleted.add(folderPath);
        }
    }

    @Override
    public void ignored(String repoDescription, String folderPath) {
        if (repoDescription.contains(repoName)) {
            ignoredLatch.countDown();
            ignored.add(folderPath);
        }

    }
}
