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
package rapture.plugin.install;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

import rapture.common.PluginConfig;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.plugin.util.PluginUtils;

/**
 * Handle all non-interactive options here where the user specifies options on
 * the command-line and the program doesn't expect any further user input
 * 
 * @author dukenguyen
 * 
 */
public class PluginBatchMode {

    private static final Logger log = Logger.getLogger(PluginBatchMode.class);

    private PluginShell shell;

    public PluginBatchMode(PluginShell shell) {
        this.shell = shell;
    }

    void go() {
        shell.getLogger().setLevel(Level.ERROR);
        try {
            List<String> includes = getCommaSeparatedStringsAsList(shell.getImportInclude());
            List<String> excludes = getCommaSeparatedStringsAsList(shell.getImportExclude());
            List<String> order = getCommaSeparatedStringsAsList(shell.getImportOrder());

            File[] zips = getZipFiles(shell.getImportDir(), includes, excludes);
            if (zips == null || zips.length == 0) {
                log.error("List of zip files empty");
                System.exit(1);
            }
            checkOrder(zips, order);
            sortZips(zips, order);
            applyOverlay(shell.getImportOverlay());
            for (File z : zips) {
                String pluginName = extractPluginName(z.getPath());
                if (pluginName == null) {
                    continue;
                }
                log.info(String.format("Installing plugin [%s] sourced from file [%s] ...", pluginName, z.getPath()));
                shell.eval("import " + z.getPath());
                shell.eval("stream " + pluginName);
                shell.eval("deflate " + pluginName);
            }
            log.info("All plugin installation successful.");
            System.exit(0);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        shell.getLogger().setLevel(Level.INFO);
    }

    void goDir() {
        shell.getLogger().setLevel(Level.ERROR);
        File[] dirs = getDirs(shell.getLoadDir());
        sortByList(dirs, shell.getLoadOrder());
        applyOverlay(shell.getImportOverlay());
        for (File d : dirs) {
            String name = d.getName();
            log.info(String.format("Loading plugin [%s] from directory [%s] ...", name, d.getPath()));
            shell.eval("load " + name);
            shell.eval("stream " + name);
            shell.eval("deflate " + name);
        }
        shell.getLogger().setLevel(Level.INFO);
        log.info("All feature installation successful.");
        System.exit(0);
    }

    void buildFeature(String featureName, String zipFile) {
        shell.getLogger().setLevel(Level.ERROR);
        shell.eval("load " + featureName);
        shell.eval("export " + featureName+" "+zipFile);        
    }
    
    private void applyOverlay(String importOverlay) {
        if (importOverlay != null && !importOverlay.trim().isEmpty()) {
            log.info("Applying import overlay: " + importOverlay);
            shell.eval("use " + importOverlay);
        }
    }

    private void checkOrder(File[] zips, List<String> order) {
        Set<String> plugins = new HashSet<>();
        for (File file : zips) {
            plugins.add(extractPluginName(file.getPath()));
        }
        for (String plugin : order) {
            if (!plugins.contains(plugin)) {
                throw RaptureExceptionFactory.create("plugin '" + plugin + "' from --importorder does not exist");
            }
        }
    }

    /**
     * Sort an array of File objects based on a List of plugin names.
     * 
     * @param zips
     * @param order
     *            - List of plugin names that implies the order in which they should be installed
     */
    void sortZips(File[] zips, final List<String> order) {
        if (order == null) {
            return;
        }
        Arrays.sort(zips, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return new Integer(order.indexOf(extractPluginName(f1.getPath()))).compareTo(order.indexOf(extractPluginName(f2.getPath())));
            }
        });
    }

    void sortByList(File[] dirs, String importOrder) {
        if (importOrder == null) return;
        final Map<String, Integer> map = Maps.newHashMap();
        int count = 1;
        for (String name : getCommaSeparatedStringsAsList(importOrder)) {
            map.put(name, count++);
        }
        Arrays.sort(dirs, new Comparator<File>() {
            private int getIndex(File f) {
                Integer i = map.get(f.getName());
                return (i == null) ? -1 : i;
            }

            @Override
            public int compare(File f1, File f2) {
                int f1i = getIndex(f1);
                int f2i = getIndex(f2);
                return f1i - f2i;
            }
        });
    }

    /**
     * Used to extract the pluginName from the zip file. Looks inside the zip file and reads the plugin.txt to get the pluginName
     * 
     * @param path
     *            - full path to a plugin zip file
     * @return String - representing the name of the plugin
     */
    String extractPluginName(String path) {
        PluginConfig config = new PluginUtils().getPluginConfigFromZip(path);
        if (config == null) {
            return null;
        }
        return config.getPlugin();
    }

    /**
     * Given a directory, get an array of File objects for the files that end in
     * the .zip extension
     * 
     * @param dir
     * @param includes
     *            - list of plugins to include
     * @param excludes
     *            - list of plugins to exclude
     * @return
     */
    File[] getZipFiles(File dir, final List<String> includes, final List<String> excludes) {
        File[] zips = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File f, String s) {
                if (includes != null) {
                    return endsWithZip(s) && includes.contains(extractPluginName(new File(f, s).getPath()));
                } else if (excludes != null) {
                    return endsWithZip(s) && !excludes.contains(extractPluginName(new File(f, s).getPath()));
                } else {
                    return endsWithZip(s);
                }
            }

            private boolean endsWithZip(String s) {
                return s.endsWith(".zip");
            }
        });
        return zips;
    }

    File[] getDirs(File root) {
        return root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() && !f.getName().startsWith(".");
            }
        });
    }

    List<String> getCommaSeparatedStringsAsList(String commaSep) {
        if (StringUtils.isEmpty(commaSep)) {
            return null;
        }
        return Arrays.asList(commaSep.trim().split("\\s*,\\s*"));
    }
}
