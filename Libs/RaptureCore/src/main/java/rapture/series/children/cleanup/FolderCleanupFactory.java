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

import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.repo.RepoLockHandler;
import rapture.series.children.ChildrenRepo;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * @author bardhi
 * @since 5/11/15.
 */
public class FolderCleanupFactory {
    public static CleanupInfo createCleanupInfo(String repoDescription, RepoLockHandler repoLockHandler, ChildrenRepo childrenRepo) {
        CleanupInfo cleanupInfo = new CleanupInfo();
        cleanupInfo.repoDescription = repoDescription;
        cleanupInfo.uniqueId = childrenRepo.toString();
        cleanupInfo.cleanupFunction = createCleanupFunction(childrenRepo);
        cleanupInfo.isEmptyPredicate = createIsEmptyPredicate(childrenRepo);
        cleanupInfo.repoLockHandler = repoLockHandler;
        return cleanupInfo;
    }

    private static Predicate<String> createIsEmptyPredicate(final ChildrenRepo childrenRepo) {
        return new Predicate<String>() {
            @Override
            public boolean apply(String folderPath) {
                List<RaptureFolderInfo> children = childrenRepo.getChildren(folderPath);
                return children.size() == 0;
            }
        };
    }

    private static Function<String, Boolean> createCleanupFunction(final ChildrenRepo childrenRepo) {
        //returns a function with side effects!
        return new Function<String, Boolean>() {
            @Override
            public Boolean apply(String folderPath) {
                childrenRepo.dropFolderEntry(folderPath);
                return true;
            }
        };
    }
}
