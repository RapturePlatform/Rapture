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
package rapture.series.children.cleanup;

import rapture.repo.RepoLockHandler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Same as DefaultFolderCleanupService, but adds some listeners for testing
 *
 * @author bardhi
 * @since 5/11/15.
 */
public class CleanupServiceWithListeners extends DefaultFolderCleanupService {

    private List<CleanupListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(CleanupListener listener) {
        this.listeners.add(listener);
    }

    @Override
    protected void ignoreFolder(String repoDescription, String folderPath) {
        super.ignoreFolder(repoDescription, folderPath);
        for (CleanupListener listener : listeners) {
            listener.ignored(repoDescription, folderPath);
        }

    }

    @Override
    protected void deleteFolderIfEmpty(String repoDescription, Function<String, Boolean> cleanupFunction, Predicate<String> isEmptyPredicate,
            RepoLockHandler repoLockHandler, String folderPath) {
        super.deleteFolderIfEmpty(repoDescription, cleanupFunction, isEmptyPredicate, repoLockHandler, folderPath);
        for (CleanupListener listener : listeners) {
            listener.deleted(repoDescription, folderPath);
        }
    }

    public void removeListener(CleanupListener cleanupListener) {
        listeners.remove(cleanupListener);
    }
}
