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

import static com.google.common.base.Preconditions.checkArgument;
import static rapture.common.Scheme.BLOB;
import static rapture.common.Scheme.DOCUMENT;
import static rapture.common.Scheme.EVENT;
import static rapture.common.Scheme.FIELD;
import static rapture.common.Scheme.IDGEN;
import static rapture.common.Scheme.JAR;
import static rapture.common.Scheme.JOB;
import static rapture.common.Scheme.SCRIPT;
import static rapture.common.Scheme.SERIES;
import static rapture.common.Scheme.TABLE;
import static rapture.common.Scheme.WORKFLOW;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.ScriptingApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.util.MimeTypeResolver;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;

/**
 * This class maintains a two-way binding between a content file in the expanded directory for a PluginSandbox and the corresponding object on the rapture
 * server. URIs that can not encode to a content file will be rejected in the constructor.
 * 
 * @author mel
 */
public class PluginSandboxItem {
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PluginSandboxItem [uri=" + uri + ", variant=" + variant + ", file=" + file + ", fullFilePath=" + fullFilePath + ", fileHash=" + fileHash
                + ", remoteHash=" + remoteHash + ", content=" + Arrays.toString(content) + ", fileCurrent=" + fileCurrent + ", remoteCurrent=" + remoteCurrent
                + "]";
    }

    public static final String CONTENTDIR = "content/";
    public static final String REPODIR = "repo/";
    public static final String AUTHORITY = "/authority";
    private final RaptureURI uri;
    private final String variant;
    private File file; // the local file backing this feature
    private String fullFilePath;
    private String fileHash = null; // this is the hash of the content, not the
    // SandboxItem itself
    private String remoteHash = null;
    private byte[] content = null;
    private boolean fileCurrent = false;
    private boolean remoteCurrent = false;

    public PluginSandboxItem(RaptureURI uri, String variant) {
        checkArgument(isContent(uri));
        this.variant = variant;
        this.uri = uri;
        file = null;
    }

    public PluginSandboxItem(RaptureURI uri, File rootDir, String variant) {
        this.uri = uri;
        this.variant = variant;
        file = calculateFile(uri, rootDir, variant);
    }

    public PluginSandboxItem(RaptureURI uri, File rootDir, String variant, String remoteHash) {
        this(uri, rootDir, variant);
        this.remoteHash = remoteHash;
        remoteCurrent = true;
        try {
            fileCurrent = diffWithFile(remoteHash, false);
        } catch (Exception ex) {
            // ignore -- we only care bout the old file if it matches the hash
            // and can be read ok
        }
    }

    /**
     * For use when the source is a zip file.
     */
    public PluginSandboxItem(RaptureURI uri, String variant, String hash, byte[] content) {
        this.uri = uri;
        this.content = content;
        this.fileHash = hash;
        this.variant = variant;
    }

    public void deflate() {
        content = null;
    }

    public File getFile() {
        return file;
    }

    public boolean isFileCurrent() {
        return fileCurrent;
    }

    public boolean isRemoteCurrent() {
        return remoteCurrent;
    }

    public void updateFilePath(File rootDir) {
        File f = calculateFile(uri, rootDir, variant);
        if (!Objects.equal(f, file)) {
            file = f;
            fileCurrent = false;
        }
    }

    public static File calculateFile(RaptureURI uri, File rootDir, String variant) {
        if (isContent(uri)) {
            return new File(rootDir, calculatePath(uri, variant));
        }
        return null;
    }

    public static String calculatePath(RaptureURI uri, String variant) {
        return topDir(variant) + scrub(uri.getShortPath()) + getExtFromUri(uri);
    }

    private static String getExtFromUri(RaptureURI uri) {
        if(!uri.hasAttribute()) {
            return ext2scheme.inverse().get(uri.getScheme());
        }
        switch (uri.getScheme()) {
            case BLOB:
                return ".bits";
            case SCRIPT:
                return "jjs".equals(uri.getAttribute()) ? ".jjs" : ".rfx";
            default:
                return "";
        }
    }

    private static String topDir(String variant) {
        return (variant == null) ? CONTENTDIR : variant + "/";
    }

    public static Pair<RaptureURI, String> calculateURI(File leafFile, File rootDir) {
        String rootPath = rootDir.getPath();
        String leafPath = unscrub(leafFile.getPath());
        checkArgument(leafPath.startsWith(rootPath), "File is not a child of the rootDir: " + leafPath);
        checkArgument(leafPath.charAt(rootPath.length()) == '/', "File is not a child of the rootDir (missing slash?): " + leafPath);
        String leafTail = leafPath.substring(rootPath.length() + 1);
        return calculateURI(leafTail);
    }

    public static Pair<RaptureURI, String> calculateURI(String leafTail) {
        String variant = (leafTail.startsWith(CONTENTDIR)) ? null : extractVariant(leafTail);
        Triple<String, String, Scheme> trip = extractScheme(leafTail.substring(variantLength(variant)));
        checkArgument(trip != null, "extraneuous file");
        RaptureURI uri = RaptureURI.createFromFullPathWithAttribute(trip.getLeft(), trip.getMiddle(), trip.getRight());
        return new ImmutablePair<RaptureURI, String>(uri, variant);
    }

    public static final Pair<RaptureURI, String> calculateURI(ZipEntry entry) {
        String leafTail = entry.getName();
        return calculateURI(leafTail);
    }

    private static String extractVariant(String path) {
        int cut = path.indexOf('/');
        if (cut < 0) return null;
        return path.substring(0, cut);
    }

    private static int variantLength(String variant) {
        return (variant == null) ? CONTENTDIR.length() : (variant.length() + 1);
    }

    /**
     * Split the path of a filename into the extensionless path (for URI construction) and the URI scheme (based on the file extension)
     * 
     * @param path
     *            The relative path within the contents directory
     * @return a triple of the path (left), the attribute(middle), and the URI scheme (right)
     */
    public static Triple<String, String, Scheme> extractScheme(String path) {
        int chop = path.lastIndexOf('.');
        if (chop < 0) return null;
        String schemeName = path.substring(chop);

        String attr = null;
        Scheme scheme = ext2scheme.get(schemeName);
        if (scheme == null) {
            scheme = raw2scheme.get(schemeName);
            if (scheme == null) {
                scheme = BLOB; // Oooerr - unmatched extensions will be blobs (THIS IS GOOD, BELIEVE ME)
            } else {
                path = path.substring(0, chop);
            }
            switch (scheme) {
                case BLOB:
                    attr = getResolver().getMimeTypeFromPath(path);
                    break;
                case SCRIPT:
                    attr = schemeName.equals(".jjs") ? "jjs" : "raw";
                    break;
                default:
                    throw new Error("Severe: Unhandled scheme in raw2scheme map"); // can only happen if a bad checkin is made
            }
        } else {
            path = path.substring(0, chop);
        }
        return ImmutableTriple.of(path, attr, scheme);
    }

    public void cacheFileContent() throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        content = PluginContentReader.readFromFile(file, md);
        fileHash = Hex.encodeHexString(md.digest());
        fileCurrent = true;
    }

    private static MimeTypeResolver resolver = null;

    private static MimeTypeResolver getResolver() {
        if (resolver == null) resolver = new MimeTypeResolver();
        return resolver;
    }

    public byte[] getFileContent(boolean forceRefresh) throws NoSuchAlgorithmException, IOException {
        if (!fileCurrent || forceRefresh) {
            cacheFileContent();
        }
        return content;
    }

    public void clearCache() {
        content = null;
    }

    /**
     * @return true if this content is a change from the existing content
     */
    public boolean setContentFromRemote(byte[] content, String hash) {
        this.content = content;
        remoteCurrent = true;
        remoteHash = hash;
        if (fileHash != null && !fileHash.equals(remoteHash)) {
            fileHash = null;
            fileCurrent = false;
            return true;
        }
        return fileHash != null;
    }

    /**
     * Hash-based comparisson with optional caching side-effects
     * 
     * @param hash
     *            the external hexified MD5 hash to compare against
     * @param cacheIfDifferent
     *            if set, the file contents will be cached
     * @return true if different, false if same
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public boolean diffWithFile(String hash, boolean cacheIfDifferent) throws IOException, NoSuchAlgorithmException {
        boolean cached = false;
        byte[] oldContent = content;
        if (fileHash == null) {
            try {
                cacheFileContent();
            } catch (FileNotFoundException ex) {
                content = cacheIfDifferent ? null : oldContent;
                return true;
            }
            cached = true;
        }

        boolean different = !Objects.equal(hash, fileHash);

        if (!cached && cacheIfDifferent && different) {
            cacheFileContent();
        } else if (!cacheIfDifferent) {
            content = oldContent;
        }
        return different;
    }

    public void storeFile() throws IOException {
        if (content == null) return;
        PrintWriter writer = null;
        boolean early = true;
        try {
            file.getParentFile().mkdirs();
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            writer.print(content);
            early = false;
            fileCurrent = true;
        } finally {
            Closeables.close(writer, early);
        }
    }

    public static final BiMap<String, Scheme> ext2scheme = ImmutableBiMap.<String, Scheme> builder()
            .put(".script", SCRIPT)
            .put(".series", SERIES)
            .put(".blob", BLOB)
            .put(".jar", JAR)
            .put(".rdoc", DOCUMENT)
            .put(".idgen", IDGEN)
            .put(".revent", EVENT)
            .put(".field", FIELD)
            .put(".table", TABLE)
            .put(".job", JOB)
            .put(".workflow", WORKFLOW)
            .put(".lock", Scheme.LOCK)
            .put(".snippet", Scheme.SNIPPET)
            .put(".structured", Scheme.STRUCTURED)
            .build();

    // do not edit this without making matching edits to extractScheme and getExtFromURI
    // .jjs is extension for server side javascript
    public static final Map<String, Scheme> raw2scheme = ImmutableMap.of(".rfx", SCRIPT, ".jjs", SCRIPT, ".bits", BLOB);

    private static boolean isContent(RaptureURI uri) {
        return ext2scheme.containsValue(uri.getScheme());
    }

    /**
     * Download the encoded contents from the specified client
     * 
     * @return true if the contents were changed as a result of this call
     */
    public boolean download(ScriptingApi client, boolean force) {
        if (force || needExtract()) {
            PluginTransportItem item = null;
            try {
                item = client.getPlugin().getPluginItem(uri.toString());
            } catch (RaptureException ex) {
                return false;
            }
            if (item == null) return false;
            return setContentFromRemote(item.getContent(), item.getHash());
        }
        return false;
    }

    protected boolean needExtract() {
        return !remoteCurrent || content == null || getHash() == null || (fileHash != null && !fileHash.equals(remoteHash));
    }

    public void delete() {
        if (file.exists()) {
            file.delete();
        }
    }

    public RaptureURI getURI() {
        return uri;
    }

    public void writeZipEntry(ZipOutputStream out, ScriptingApi client, boolean build) throws IOException {
        String filePath = calculatePath(uri, variant);
        ZipEntry entry = new ZipEntry(filePath);
        out.putNextEntry(entry);
        if (build && (file != null)) {
            content = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        } else {
            download(client, false);
        }
        if (content != null)
            out.write(content);
    }

    public String getHash() {
        if (fileHash != null) {
            return fileHash;
        }
        if (remoteHash != null) {
            return remoteHash;
        }
        return null;
    }

    /**
     * make transport item from local cache if present or local file if not
     * 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public PluginTransportItem makeTransportItem() throws NoSuchAlgorithmException, IOException {
        if (content == null) {
            cacheFileContent();
            if (content == null) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Content not present");
            }
        }
        PluginTransportItem result = new PluginTransportItem();
        result.setContent(content);
        result.setUri(uri.toString());
        result.setHash(getHash());

        return result;
    }

    public static String scrub(String in) {
        return in.replace("\\", "_BACKSLASH_");
    }

    public static String unscrub(String in) {
        return in.replace("_BACKSLASH_", "\\");
    }

    public String getFullFilePath() {
        return fullFilePath;
    }

    public void setFullFilePath(String fullFilePath) {
        this.fullFilePath = fullFilePath;
    }
}
