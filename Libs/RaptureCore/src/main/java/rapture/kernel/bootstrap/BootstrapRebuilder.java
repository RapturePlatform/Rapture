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
package rapture.kernel.bootstrap;

import rapture.common.RaptureFolderInfo;
import rapture.kernel.ContextFactory;
import rapture.kernel.LowLevelRepoHelper;
import rapture.repo.RepoFactory;
import rapture.repo.Repository;

import java.util.List;

import org.apache.log4j.Logger;

public class BootstrapRebuilder implements Runnable {
    private static Logger log = Logger.getLogger(BootstrapRebuilder.class);
    private Repository srcRepo;
    private Repository targetRepo;
    private String newConfig;
    private RebuildFinisher finisher;
    private LowLevelRepoHelper repoHelper;

    public BootstrapRebuilder(Repository srcRepo, String newConfig, RebuildFinisher finisher) {
        this.srcRepo = srcRepo;
        this.newConfig = newConfig;
        this.finisher = finisher;
        this.repoHelper = new LowLevelRepoHelper(srcRepo);
    }

    @Override
    public void run() {
        log.info("Rebuilding bootstrap top level repo");
        targetRepo = RepoFactory.getRepo(newConfig);
        targetRepo.drop();
        // Now basically loop through the documents, copying them all
        long documentsCopied = workWith("", 0L);
        if (documentsCopied == 0L) {
            log.info("No documents copied, not finishing...");
        } else {
            log.info("Documents copied = " + documentsCopied + ", calling the finisher");
            finisher.rebuildFinished(newConfig);
        }
    }

    private long workWith(String prefix, long currentCount) {
        long thisCount = currentCount;
        List<RaptureFolderInfo> contents = repoHelper.getChildren(prefix);
        for (RaptureFolderInfo entry : contents) {
            String fullName = prefix + "/" + entry.getName();
            if (fullName.startsWith("/")) {
                fullName = fullName.substring(1);
            }
            if (entry.isFolder()) {
                thisCount = workWith(fullName, thisCount);
            } else {
                log.info("Copying " + fullName);
                String content = srcRepo.getDocument(fullName);
                targetRepo.addDocument(fullName, content, ContextFactory.getKernelUser().getUser(), "Bootstrap rebuild", true);
                thisCount++;
            }
        }
        return thisCount;
    }

}
