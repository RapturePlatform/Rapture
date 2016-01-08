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
package rapture.repo.cassandra;

import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.repo.cassandra.key.PathBuilder;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A folder handler stores rows by prefix
 * <p/>
 * When we add a prefix we store entries for each entry in that hierarchy - storing the next level down value at each point as a column name. The value of each
 * column is a count of the number of children - so often we may have to attempt to retrieve the value to update it by one - and if the value is decremented to
 * zero we should remove that cell intersection. Documents have a value of -1
 *
 * @author amkimian
 */
public class CassFolderHandler {
    private static final String FOLDERPOSTFIX = "_folder";
    private static Logger log = Logger.getLogger(CassFolderHandler.class);
    public static final String DOLLAR_ROOT = "$root";
    private AstyanaxRepoConnection cass;
    private String cf;

    public CassFolderHandler(AstyanaxRepoConnection cass, String standardCf) {
        this.cass = cass;
        this.cf = standardCf + FOLDERPOSTFIX;
        try {
            cass.ensureStandardCF(cf);
        } catch (Exception e) {
            // Hmm... the first call to a folder handler will fail and throw a
            // RaptureException there.
            e.printStackTrace();
        }
    }

    public void drop() {
        // TODO Auto-generated method stub

    }

    public void registerDocument(String k) {
        String[] pathParts = k.split(RaptureConstants.PATHSEP);
        StringBuilder currentPrefix = new StringBuilder();
        for (int i = 0; i < pathParts.length; i++) {
            String prefixKey;
            if (currentPrefix.length() == 0) {
                prefixKey = DOLLAR_ROOT;
                currentPrefix.append(pathParts[i]);
            } else {
                prefixKey = currentPrefix.toString();
                currentPrefix.append(RaptureConstants.PATHSEP);
                currentPrefix.append(pathParts[i]);
            }

            // Work with
            if (i == pathParts.length - 1) {
                // Document
                log.debug(String.format("Adding folder document %s:%s", prefixKey, pathParts[i]));
                cass.addFolderDocument(cf, prefixKey, pathParts[i]);
            } else {
                log.debug(String.format("Adding folder %s:%s", prefixKey, pathParts[i]));
                cass.addFolderFolder(cf, prefixKey, pathParts[i]);
            }
        }
    }

    public List<RaptureFolderInfo> getChildren(String prefix) {
        return cass.getFolderChildren(cf, prefix);
    }

    public List<String> getAllChildren(String prefix) {
        return cass.getAllFolderChildren(cf, prefix);
    }

    public void removeDocument(String k) {
        String[] pathParts = k.split(RaptureConstants.PATHSEP);
        StringBuilder currentPrefix = new StringBuilder();
        for (int i = 0; i < pathParts.length; i++) {
            if (currentPrefix.length() == 0) {
                // Just add
                currentPrefix.append(pathParts[i]);
            } else {
                // Work with
                if (i == pathParts.length - 1) {
                    // Document
                    cass.removeFolderDocument(cf, currentPrefix.toString(), pathParts[i]);
                } else {
                    cass.removeFolderFolder(cf, currentPrefix.toString(), pathParts[i]);
                }
                currentPrefix.append(RaptureConstants.PATHSEP);
                currentPrefix.append(pathParts[i]);
            }
        }
    }

    public List<RaptureFolderInfo> removeChildren(String prefix, Boolean force) {
        List<RaptureFolderInfo> ret = new LinkedList<>();
        List<RaptureFolderInfo> fi = getChildren(prefix);
        for (RaptureFolderInfo f : fi) {
            String next = new PathBuilder(prefix).subPath(f.getName()).build();
            RaptureFolderInfo rfi = new RaptureFolderInfo();
            rfi.setName(next);
            if (f.isFolder()) {
                ret.addAll(removeChildren(next, force));
                rfi.setFolder(true);
            } else {
                rfi.setFolder(false);
            }
            removeDocument(next);
            ret.add(rfi);            
        }
        return ret;
    }
}
