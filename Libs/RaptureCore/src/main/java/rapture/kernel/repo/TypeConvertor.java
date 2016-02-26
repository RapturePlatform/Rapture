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
package rapture.kernel.repo;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

public class TypeConvertor implements Runnable {
    private static Logger log = Logger.getLogger(TypeConvertor.class);
    private String authority;
    private String typeName;
    private String newConfig;
    private static final String TMPAUTHORITY = "workingTemp"; //$NON-NLS-1$
    private boolean supportsMetaContent = true;

    private CallingContext ctx = ContextFactory.getKernelUser();

    public TypeConvertor(String authority, String typeName, String newConfig, int versionsToKeep) {
        this.authority = authority;
        this.typeName = typeName;
        this.newConfig = newConfig;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("TC-" + typeName); //$NON-NLS-1$
        log.info(String.format(Messages.getString("TypeConvertor.StartConversion"), authority, typeName, newConfig)); //$NON-NLS-1$
        log.info(Messages.getString("TypeConvertor.EnsureTemp")); //$NON-NLS-1$
        if (Kernel.getDoc().docRepoExists(ctx, "//" + TMPAUTHORITY + "/" + typeName)) {
            Kernel.getDoc().deleteDocRepo(ctx, "//" + TMPAUTHORITY + "/" + typeName);
        }
        Kernel.getDoc().createDocRepo(ctx, "//" + TMPAUTHORITY + "/" + typeName, newConfig);

        log.info(Messages.getString("TypeConvertor.CopyDocuments")); //$NON-NLS-1$
        long totalCopied = workWith(typeName, 0L);
        log.info(String.format(Messages.getString("TypeConvertor.TotalCopied"), totalCopied)); //$NON-NLS-1$
        if (totalCopied == 0) {
            log.info(Messages.getString("TypeConvertor.NoSwitch")); //$NON-NLS-1$
        } else {
            log.info(Messages.getString("TypeConvertor.SwitchConfig")); //$NON-NLS-1$

            log.info(Messages.getString("TypeConvertor.DropType")); //$NON-NLS-1$
            Kernel.getDoc().deleteDocRepo(ctx, "//" + authority + "/" + typeName);
            log.info(Messages.getString("TypeConvertor.CreateType")); //$NON-NLS-1$
            Kernel.getDoc().createDocRepo(ctx, "//" + authority + "/" + typeName, newConfig);
        }
        log.info(Messages.getString("TypeConvertor.DropTmpType")); //$NON-NLS-1$
        Kernel.getDoc().deleteDocRepo(ctx, "//" + TMPAUTHORITY + "/" + typeName);
    }

    private long workWith(String path, long current) {
        long ret = current;
        if (path.isEmpty()) {
            log.info(Messages.getString("TypeConvertor.WorkAtRoot")); //$NON-NLS-1$
        } else {
            log.info(String.format(Messages.getString("TypeConvertor.WorkAt"), path)); //$NON-NLS-1$
        }
        Map<String, RaptureFolderInfo> childrenMap = Kernel.getDoc().listDocsByUriPrefix(ctx, "//" + authority + "/" + path, 1);
        for (RaptureFolderInfo child : childrenMap.values()) {
            if (child.isFolder()) {
                ret = workWith(path + "/" + child.getName(), ret); //$NON-NLS-1$
            } else {
                String displayName = path + "/" + child.getName(); //$NON-NLS-1$
                log.info(String.format(Messages.getString("TypeConvertor.Copying"), displayName)); //$NON-NLS-1$
                if (supportsMetaContent) {

                    try {
                        DocumentWithMeta content = Kernel.getDoc().getDocAndMeta(ctx, "//" + authority + "/" + displayName);
                        // Here potentially go back versionsToKeep versions and
                        // write
                        // these back, using the same comment
                        Kernel.getDoc().putDoc(ctx, "//" + TMPAUTHORITY + "/" + displayName, content.getContent());
                    } catch (Exception e) {
                        supportsMetaContent = false;
                    }
                }

                if (!supportsMetaContent) {
                    String content = Kernel.getDoc().getDoc(ctx, "//" + authority + "/" + displayName);
                    Kernel.getDoc().putDoc(ctx, "//" + TMPAUTHORITY + "/" + displayName, content); //$NON-NLS-1$
                }
                ret = ret + 1;
            }
        }
        return ret;
    }
}
