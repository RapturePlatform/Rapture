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

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import rapture.repo.RepoLockHandler;

/**
 * @author bardhi
 * @since 5/11/15.
 */
class CleanupInfo {
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Set<String> getFoldersForReview() {
        return foldersForReview;
    }

    public void addFolderForReview(String folderForReview) {
        this.foldersForReview.add(folderForReview);
    }

    public String getRepoDescription() {
        return repoDescription;
    }

    public void setRepoDescription(String repoDescription) {
        this.repoDescription = repoDescription;
    }

    public RepoLockHandler getRepoLockHandler() {
        return repoLockHandler;
    }

    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {
        this.repoLockHandler = repoLockHandler;
    }

    public Function<String, Boolean> getCleanupFunction() {
        return cleanupFunction;
    }

    public void setCleanupFunction(Function<String, Boolean> cleanupFunction) {
        this.cleanupFunction = cleanupFunction;
    }

    public Predicate<String> getIsEmptyPredicate() {
        return isEmptyPredicate;
    }

    public void setIsEmptyPredicate(Predicate<String> isEmptyPredicate) {
        this.isEmptyPredicate = isEmptyPredicate;
    }

    private String uniqueId;
    private Set<String> foldersForReview = new HashSet<>();
    private String repoDescription;
    private RepoLockHandler repoLockHandler;
    private Function<String, Boolean> cleanupFunction;
    private Predicate<String> isEmptyPredicate;
}
