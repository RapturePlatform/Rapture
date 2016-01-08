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
package rapture.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.common.RaptureFolderInfo;
import rapture.common.exception.RaptureException;
import rapture.kernel.folder.BaseFolderVisitor;
import rapture.kernel.folder.DepthFolderVisitor;
import rapture.repo.Repository;

public class LowLevelRepoHelper {
    private Repository repository;
    private static Logger log = Logger.getLogger(LowLevelRepoHelper.class);

    public LowLevelRepoHelper(Repository repository) {
        this.repository = repository;
    }

    public List<RaptureFolderInfo> getChildren(String prefix) {
        log.debug("Working on " + prefix);
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        List<RaptureFolderInfo> res = repository.getChildren(prefix);
        if (res == null) {
            int depth = 1;
            if (!prefix.isEmpty()) {
                depth = prefix.split("/").length + 1;
            }
            log.debug("Calling folder query on '" + prefix + "' instead with depth " + depth);
            List<String> folders = folderQuery(prefix, depth);
            res = new ArrayList<RaptureFolderInfo>();

            List<Boolean> existence = batchExist(folders);

            for (int i = 0; i < folders.size(); i++) {
                RaptureFolderInfo rfi = new RaptureFolderInfo();
                rfi.setFolder(!existence.get(i));
                String lastName = folders.get(i);
                log.debug("Got " + lastName);
                lastName = lastName.substring(lastName.lastIndexOf("/") + 1);
                log.debug("Adding " + lastName + " as " + (existence.get(i) ? "file" : "folder"));
                rfi.setName(lastName);
                res.add(rfi);
            }
        }
        return res;
    }

    public List<String> folderQuery(String prefix, final int depth) {
        List<String> ret = new ArrayList<String>();
        try {
            BaseFolderVisitor visitor = new DepthFolderVisitor(depth, "");
            repository.visitFolders(prefix == null ? "" : prefix, null, visitor);
            ret.addAll(visitor.getSet());
        } catch (RaptureException e) {
            log.error("Tried to query for an area that does not exist");
        }
        Collections.sort(ret);
        return ret;
    }

    public List<Boolean> batchExist(List<String> displayNames) {
        List<Boolean> ret = new ArrayList<Boolean>(displayNames.size());
        boolean existState[] = repository.getExistence(displayNames);
        for (boolean anExistState : existState) {
            ret.add(anExistState);
        }
        return ret;
    }

}
