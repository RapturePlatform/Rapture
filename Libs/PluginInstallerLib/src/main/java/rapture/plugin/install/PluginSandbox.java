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
package rapture.plugin.install;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import rapture.common.PluginConfig;
import rapture.common.PluginManifest;
import rapture.common.PluginManifestItem;
import rapture.common.PluginVersion;
import rapture.common.RaptureURI;
import rapture.common.api.ScriptingApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.plugin.PluginUtil;

/**
 * The Plugin Sandbox is a client-side representation of a plugin. The plugin is a three-way binding between a zip archive representing a plugin, a directory
 * representing the plugin (the same as the zip file but expanded), and the resources in the server. Any of the three binding points can be left unbound when
 * only a one-way or two-way use is required. The sandbox tracks which changes have been propagated to what bindings. Refreshing for changes from other clients
 * is done only on request.
 *
 * @author mel
 */
public class PluginSandbox {

    private static final Logger log = Logger.getLogger(PluginSandbox.class);

    private boolean strict = false;
    private String pluginName;
    private String description;
    private PluginVersion version;
    private File rootDir;
    private static final boolean debug = true;

    public static final String CONTENT = PluginSandboxItem.CONTENTDIR;
    public static final String PLUGIN_TXT = "plugin.txt";
    public static final String DEPRECATED_FEATURE_TXT = "feature.txt";

    private Map<String, PluginSandboxItem> uri2item = new TreeMap<>();
    private Map<String, PluginVersion> depends = new LinkedHashMap<>();
    private Map<String, Map<RaptureURI, PluginSandboxItem>> variant2map = new LinkedHashMap<>();

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
        for (PluginSandboxItem item : uri2item.values()) {
            item.updateFilePath(rootDir);
        }
    }

    public PluginSandboxItem getOrMakeItem(RaptureURI uri) {
        PluginSandboxItem item = uri2item.get(uri);
        if (item == null) {
            item = makeItem(uri);
            uri2item.put(uri.toString(), item);
        }
        return item;
    }

    public PluginSandboxItem getItem(RaptureURI uri) {
        return uri2item.get(uri.toString());
    }

    private PluginSandboxItem makeItem(RaptureURI uri) {
        if (rootDir == null) {
            return new PluginSandboxItem(uri, null);
        } else {
            return new PluginSandboxItem(uri, rootDir, null);
        }
    }

    public List<PluginSandboxItem> diffWithFolder(PluginManifest manifest, boolean cacheIfDifferent) throws NoSuchAlgorithmException, IOException {
        List<PluginSandboxItem> result = Lists.newArrayList();
        for (PluginManifestItem mItem : manifest.getContents()) {
            PluginSandboxItem sItem = getOrMakeItem(makeURI(mItem));
            if (sItem.diffWithFile(mItem.getHash(), cacheIfDifferent)) {
                result.add(sItem);
            }
        }
        return result;
    }

    /**
     * brute force, slow -- avoid
     */
    public boolean downloadAllContentFromRemote(ScriptingApi client, PluginManifest manifest) {
        boolean changed = false;
        for (PluginManifestItem item : manifest.getContents()) {
            changed |= downloadContentFromRemote(client, item);
        }
        return changed;
    }

    public boolean downloadContentFromRemote(ScriptingApi client, PluginManifestItem mItem) {
        PluginSandboxItem sItem = getOrMakeItem(makeURI(mItem));
        return sItem.download(client, true);
    }

    private static RaptureURI makeURI(PluginManifestItem item) {
        return new RaptureURI(item.getURI(), null);
    }

    public void readConfig() {
        String s = PluginUtil.getFileAsString(new File(rootDir, "plugin.txt"));
        PluginConfig config = null;
        try {
            config = JacksonUtil.objectFromJson(s, PluginConfig.class);
        } catch (Exception ex) {
            throw RaptureExceptionFactory.create("The plugin.txt file has been corrupted.", ex);
        }
        if (!config.getPlugin().equals(pluginName)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Config mismatch: " + config.getPlugin());
        }
        setConfig(config);
    }

    private static final Map<String, PluginVersion> emptyMap = ImmutableMap.of();

    public void setConfig(PluginConfig config) {
        pluginName = config.getPlugin();
        description = config.getDescription();
        version = config.getVersion();
        depends = config.getDepends();
    }

    private Collection<PluginSandboxItem> getVariantItems(String thisVariant) {
        if (thisVariant == null) return ImmutableSet.<PluginSandboxItem> of();
        List<String> matches = Lists.newArrayList();
        for (String name : variant2map.keySet()) {
            if (name.equalsIgnoreCase(thisVariant)) {
                matches.add(name);
            }
        }
        Set<PluginSandboxItem> result = Sets.newHashSet();
        for (String name : matches) {
            result.addAll(variant2map.get(name).values());
        }
        return result;
    }

    public Iterable<PluginSandboxItem> getItems(String variant) {
        Map<String, PluginSandboxItem> result = new TreeMap<String, PluginSandboxItem>();
        Set<String> uris = uri2item.keySet();
        for (String uri : uris) {
            result.put(uri, uri2item.get(uri));
        }
        Collection<PluginSandboxItem> variantItems = getVariantItems(variant);
        for (PluginSandboxItem variantItem : variantItems) {
            result.put(variantItem.getURI().toString(), variantItem);
        }
        return result.values();
    }

    // package private so PluginShell can conveniently get the config.
    PluginConfig makeConfig() {
        PluginConfig config = new PluginConfig();
        config.setDepends(emptyMap);
        config.setDescription(description == null ? "" : description);
        config.setVersion(version);
        config.setPlugin(pluginName);
        return config;
    }

    public void writeConfig() throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(rootDir, PLUGIN_TXT)));
        writePlugin(out);
        out.close();
    }

    void writePlugin(OutputStream out) throws IOException {
        String s = JacksonUtil.jsonFromObject(makeConfig());
        out.write(s.getBytes("UTF-8"));
    }

    public void save(ScriptingApi client) throws IOException {
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not create directory" + rootDir.getPath());
            }
        }
        writeConfig();
        for (PluginSandboxItem item : uri2item.values()) {
            if (item.isFileCurrent()) {
                continue;
            }
            if (!item.isRemoteCurrent()) {
                item.download(client, true);
            }
            item.storeFile();
        }
        for (Map<RaptureURI, PluginSandboxItem> map : variant2map.values()) {
            for (PluginSandboxItem item : map.values()) {
                if (!item.isRemoteCurrent()) {
                    item.download(client, true);
                }
                item.storeFile();
            }
        }
    }

    public PluginSandboxItem addURI(String variant, RaptureURI uri) {
        PluginSandboxItem item = new PluginSandboxItem(uri, rootDir, variant);
        updateIndex(uri, variant, item);
        return item;
    }

    public PluginSandboxItem addURI(String variant, RaptureURI uri, String remoteHash) {
        PluginSandboxItem item = new PluginSandboxItem(uri, rootDir, variant, remoteHash);
        updateIndex(uri, variant, item);
        return item;
    }

    private void updateIndex(RaptureURI uri, String variant, PluginSandboxItem item) {
        if (variant == null) uri2item.put(uri.toString(), item);
        else putVariantItem(uri, variant, item);
    }

    public boolean removeURI(RaptureURI uri) {
        PluginSandboxItem item = uri2item.remove(uri);
        if (item == null) {
            return false;
        } else {
            item.delete();
            return true;
        }

    }

    public Map<RaptureURI, String> extract(ScriptingApi client, boolean force) {
        Map<RaptureURI, String> errors = new LinkedHashMap<>();
        for (PluginSandboxItem item : uri2item.values()) {
            try {
                item.download(client, force);
            } catch (Exception ex) {
                errors.put(item.getURI(), ex.getMessage());
            }
        }
        return errors;
    }

    public void extract(ScriptingApi client, RaptureURI uri, boolean force) {
        PluginSandboxItem item = uri2item.get(uri);
        if (item == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "URI not in manifest");
        }
        item.download(client, force);
    }

    public void writeZip(String filename, ScriptingApi client, String thisVariant, boolean build) throws IOException {
        if (thisVariant == null) readAllVariants();
        else readContent(thisVariant);

        File zipFile = new File(filename);
        File p = zipFile.getParentFile();
        if (p != null) {
            p.mkdirs();
        }
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            ZipEntry entry = new ZipEntry(PLUGIN_TXT);
            out.putNextEntry(entry);
            writePlugin(out);
            // Sort the keys
            for (String uri : uri2item.keySet()) {
                uri2item.get(uri).writeZipEntry(out, client, build);
            }
            for (Entry<String, Map<RaptureURI, PluginSandboxItem>> nut : variant2map.entrySet()) {
                if (thisVariant != null && !thisVariant.equals(nut.getKey())) continue;
                for (PluginSandboxItem item : nut.getValue().values()) {
                    item.writeZipEntry(out, client, build);
                }
            }
        }
    }

    /**
     * As written, this assumes that the hashes have already been cached. FIXME
     */
    public PluginManifest makeManifest(String variant) {
        Map<RaptureURI, PluginManifestItem> hashes = new LinkedHashMap<>();
        PluginManifest manifest = new PluginManifest();
        manifest.setPlugin(pluginName);
        manifest.setDescription(description);
        manifest.setVersion(version);
        for (PluginSandboxItem item : uri2item.values()) {
            PluginManifestItem mItem = new PluginManifestItem();
            mItem.setURI(item.getURI().toShortString());
            mItem.setHash(item.getHash());

            hashes.put(item.getURI(), mItem);
        }
        if (variant != null) {
            Map<RaptureURI, PluginSandboxItem> map = variant2map.get(variant.toLowerCase());
            if (map != null) {
                for (PluginSandboxItem item : map.values()) {
                    PluginManifestItem mItem = new PluginManifestItem();
                    mItem.setURI(item.getURI().toString());
                    mItem.setHash(item.getHash());
                    hashes.put(item.getURI(), mItem);
                }
            }
        }
        List<PluginManifestItem> contents = new LinkedList<>();
        contents.addAll(hashes.values());
        manifest.setContents(contents);
        return manifest;
    }

    public PluginSandboxItem makeItemFromInternalEntry(RaptureURI uri, InputStream is, String variant) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] content = PluginContentReader.readFromStreamWithDigest(is, md);
        String hash = Hex.encodeHexString(md.digest());
        PluginSandboxItem result = new PluginSandboxItem(uri, null, hash, content);
        if (rootDir != null) {
            result.updateFilePath(rootDir);
        }
        updateIndex(uri, variant, result);
        return result;
    }

    public PluginSandboxItem makeItemFromInternalEntry(RaptureURI uri, InputStream is, String fullPath, String variant)
            throws NoSuchAlgorithmException, IOException {
        PluginSandboxItem result = makeItemFromInternalEntry(uri, is, null);
        if (!StringUtils.isBlank(fullPath)) {
            result.setFullFilePath(fullPath);
        }
        return result;
    }

    public void makeItemFromZipEntry(ZipFile zip, ZipEntry entry) throws IOException, NoSuchAlgorithmException {
        if ("plugin.txt".equals(entry.getName())) return;
        MessageDigest md = MessageDigest.getInstance("MD5");
        try {
            Pair<RaptureURI, String> pair = PluginSandboxItem.calculateURI(entry);
            RaptureURI uri = pair.getLeft();
            String variant = pair.getRight();
            if (uri == null) {
                return;
            }
            byte[] content = PluginContentReader.readFromZip(zip, entry, md);

            if (log.isDebugEnabled()) {
                log.debug(String.format("name=%s, size=%s", entry.getName(), entry.getSize()));
                log.debug(String.format("content size=%s", content.length));
                log.debug("********* SAME??? " + (content.length == entry.getSize()));
            }
            String hash = Hex.encodeHexString(md.digest());

            PluginSandboxItem result = new PluginSandboxItem(uri, variant, hash, content);
            if (rootDir != null) {
                result.updateFilePath(rootDir);
            }
            updateIndex(uri, variant, result);
        } catch (Exception ex) {
            // do nothing -- ignore extraneous files/entries
        }
    }

    private void putVariantItem(RaptureURI uri, String variant, PluginSandboxItem result) {
        if (variant != null) {
            variant = variant.toLowerCase();
        }
        if (!variant2map.containsKey(variant)) {
            variant2map.put(variant, Maps.<RaptureURI, PluginSandboxItem> newLinkedHashMap());
        }
        variant2map.get(variant).put(uri, result);
    }

    public void readAllVariants() {
        readContent("*");
    }

    // Define the filename
    private static final String IGNORE = "plugin.ignore";

    // If we want to follow a different syntax then we just need to implement a different form of Pattern matcher.

    private static Set<Pattern> parseIgnoreFile(File dir) {
        File ignoreFile = new File(dir, IGNORE);
        if (ignoreFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(ignoreFile))) {
                Set<Pattern> ignores = new TreeSet<>();
                while (br.ready()) {
                    String line = br.readLine();
                    // NULL means we are done reading
                    if (line == null) break;

                    // Ignore blank lines or lines starting with #
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    // Here we could import definitions from other files or Other Magic

                    // Assume that the string is a regular expression pattern
                    ignores.add(Pattern.compile(line));
                }
                return ignores;
            } catch (IOException e) {
                System.out.println("Unable to read " + ignoreFile.getAbsolutePath() + " : " + e.getMessage());
            }
        }
        return null;
    }

    public void readContent(String variant) {
        Set<Pattern> ignore = parseIgnoreFile(rootDir);
        File[] files = rootDir.listFiles();
        if (files != null) for (File f : files) {
            if ((f != null) && f.isDirectory()) {
                String name = f.getName();
                if (variant == null || "*".equals(variant) || "content".equals(name) || name.equalsIgnoreCase(variant)) {
                    loadDir(f, ignore);
                }
            }
        }
    }

    private void loadDir(File dir, Set<Pattern> ignore) {
        if (debug) System.out.println("Loading from " + dir.getAbsolutePath());

        Set<Pattern> localIgnores = parseIgnoreFile(dir);
        if (localIgnores != null) {
            if (ignore != null) {
                localIgnores.addAll(ignore);
            }
            ignore = localIgnores;
        }

        File file[] = dir.listFiles();
        for (File f : file) {
            if (f.isDirectory()) {
                loadDir(f, ignore);
            } else {
                loadSandboxItem(f, ignore);
            }
        }
    }

    private void loadSandboxItem(File f, Set<Pattern> ignore) {
        Pair<RaptureURI, String> pair = null;
        try {
            if (debug) System.out.println("Examining " + f.getAbsolutePath());
            if (ignore != null) {
                // Don't include the feature.ignore in the feature
                if (IGNORE.equals(f.getName())) return;
                for (Pattern pattern : ignore) {
                    if (pattern.matcher(f.getAbsolutePath()).matches()) {
                        warn("Ignoring " + f.getAbsolutePath() + " because it matches pattern " + pattern.pattern());
                        return;
                    }
                }
            }
            pair = PluginSandboxItem.calculateURI(f, rootDir);
        } catch (Exception ex) {
            if (strict) throw new Error(ex);
            else warn("Ignoring extraneous file: " + f.getPath());
            return;
        }
        RaptureURI uri = pair.getLeft();
        String variant = pair.getRight();
        PluginSandboxItem item = new PluginSandboxItem(uri, rootDir, variant);
        item.setFullFilePath(f.getAbsolutePath());
        if (variant == null) uri2item.put(uri.toString(), item);
        else putVariantItem(uri, variant, item);
    }

    public void setVersion(PluginVersion version) {
        this.version = version;
    }

    public void deflate() {
        for (PluginSandboxItem item : uri2item.values()) {
            item.deflate();
        }
        for (Map<RaptureURI, PluginSandboxItem> map : variant2map.values()) {
            for (PluginSandboxItem item : map.values()) {
                item.deflate();
            }
        }
    }

    public static final PluginVersion DEFAULT_VERSION = new PluginVersion(0, 0, 0, 0);

    public void include(PluginSandbox includee) {
        PluginVersion version = includee.version;
        if (version == null) {
            version = DEFAULT_VERSION;
        }
        depends.put(includee.pluginName, version);
    }

    private void warn(String msg) {
        System.err.println(msg);
    }

}
