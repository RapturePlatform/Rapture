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
package rapture.module;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import rapture.home.RaptureHomeRetriever;
import rapture.config.MultiValueConfigLoader;

public class AddinLoader {
    private static Logger log = Logger.getLogger(AddinLoader.class);
    private static ModuleLoader loader = new ModuleLoader();

    /**
     * The addin loader looks for a predefined RaptureLOCAL config option for a
     * lib folder directory. All of the jar files in this directory are added to
     * the classpath (including embedded jars) If the config option isn't
     * present it defaults to ../addin
     * 
     * @author amkimian
     * 
     */
    private static File getAddinFolder() {
        String raptureHome = RaptureHomeRetriever.getRaptureHome();
        File addinFolder;
        File homeFolder;
        if (raptureHome != null) {
            homeFolder = new File(raptureHome);
            log.info(String.format(Messages.getString("AddinLoader.RaptureHomeSource"), homeFolder.getAbsolutePath())); //$NON-NLS-1$
            addinFolder = new File(homeFolder, "addins");
        } else {
            String filePath = AddinLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            // Folder is currently the path of the jar, need to go up 2 entries
            // to get to the point at which we want to branch down to addins
            log.info(String.format(Messages.getString("AddinLoader.ClassPathSource"), filePath)); //$NON-NLS-1$
            homeFolder = new File(filePath).getParentFile().getParentFile();
            addinFolder = new File(homeFolder, "addins"); //$NON-NLS-1$
        }
        return addinFolder;
    }

    public static void loadAddins() {
        log.info(Messages.getString("AddinLoader.loading")); //$NON-NLS-1$
        String addinFolder = MultiValueConfigLoader.getConfig("LOCAL-addin"); //$NON-NLS-1$
        File addinFolderFile = null;
        if (addinFolder == null) {
            addinFolderFile = getAddinFolder();
        } else {
            addinFolderFile = new File(addinFolder);
        }
        log.info(String.format(Messages.getString("AddinLoader.loadingLocation"), addinFolderFile.getAbsolutePath())); //$NON-NLS-1$
        File[] files = addinFolderFile.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    log.info(String.format(Messages.getString("AddinLoader.tryingToLoad"), f.getName())); //$NON-NLS-1$
                    loader.addJar(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
