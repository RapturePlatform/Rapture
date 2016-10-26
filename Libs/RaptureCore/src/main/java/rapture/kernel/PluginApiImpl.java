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
package rapture.kernel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.MediaType;

import rapture.common.CallingContext;
import rapture.common.PluginConfig;
import rapture.common.PluginConfigStorage;
import rapture.common.PluginManifest;
import rapture.common.PluginManifestItem;
import rapture.common.PluginManifestStorage;
import rapture.common.PluginTransportItem;
import rapture.common.PluginVersion;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.PluginApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.storable.helpers.PluginConfigHelper;
import rapture.kernel.plugin.BlobEncoder;
import rapture.kernel.plugin.BlobInstaller;
import rapture.kernel.plugin.BlobRepoMaker;
import rapture.kernel.plugin.DocumentEncoder;
import rapture.kernel.plugin.DocumentInstaller;
import rapture.kernel.plugin.DocumentRepoMaker;
import rapture.kernel.plugin.EntitlementEncoder;
import rapture.kernel.plugin.EntitlementGroupEncoder;
import rapture.kernel.plugin.EntitlementGroupInstaller;
import rapture.kernel.plugin.EntitlementInstaller;
import rapture.kernel.plugin.EntityEncoder;
import rapture.kernel.plugin.EntityInstaller;
import rapture.kernel.plugin.EventEncoder;
import rapture.kernel.plugin.EventInstaller;
import rapture.kernel.plugin.FieldEncoder;
import rapture.kernel.plugin.FieldInstaller;
import rapture.kernel.plugin.FieldTransformEncoder;
import rapture.kernel.plugin.FieldTransformInstaller;
import rapture.kernel.plugin.IdGenEncoder;
import rapture.kernel.plugin.IdGenInstaller;
import rapture.kernel.plugin.IndexEncoder;
import rapture.kernel.plugin.IndexInstaller;
import rapture.kernel.plugin.JarEncoder;
import rapture.kernel.plugin.JarInstaller;
import rapture.kernel.plugin.LockEncoder;
import rapture.kernel.plugin.LockInstaller;
import rapture.kernel.plugin.ProgramEncoder;
import rapture.kernel.plugin.ProgramInstaller;
import rapture.kernel.plugin.RaptureEncoder;
import rapture.kernel.plugin.RaptureInstaller;
import rapture.kernel.plugin.RawBlobInstaller;
import rapture.kernel.plugin.RawScriptInstaller;
import rapture.kernel.plugin.ScheduleEncoder;
import rapture.kernel.plugin.ScheduleInstaller;
import rapture.kernel.plugin.ScriptEncoder;
import rapture.kernel.plugin.ScriptInstaller;
import rapture.kernel.plugin.SeriesEncoder;
import rapture.kernel.plugin.SeriesInstaller;
import rapture.kernel.plugin.SeriesRepoMaker;
import rapture.kernel.plugin.SnippetEncoder;
import rapture.kernel.plugin.SnippetInstaller;
import rapture.kernel.plugin.StructureEncoder;
import rapture.kernel.plugin.StructureInstaller;
import rapture.kernel.plugin.StructuredEncoder;
import rapture.kernel.plugin.StructuredRepoMaker;
import rapture.kernel.plugin.StructuredTableInstaller;
import rapture.kernel.plugin.UserEncoder;
import rapture.kernel.plugin.UserInstaller;
import rapture.kernel.plugin.WidgetEncoder;
import rapture.kernel.plugin.WidgetInstaller;
import rapture.kernel.plugin.WorkflowEncoder;
import rapture.kernel.plugin.WorkflowInstaller;
import rapture.plugin.install.PluginSandboxItem;
import rapture.util.IDGenerator;

public class PluginApiImpl extends KernelBase implements PluginApi {

    private static Logger log = Logger.getLogger(PluginApiImpl.class);

    private Map<Scheme, RaptureEncoder> scheme2encoder = ImmutableMap.<Scheme, RaptureEncoder> builder()
            .put(Scheme.BLOB, new BlobEncoder())
            .put(Scheme.SERIES, new SeriesEncoder())
            .put(Scheme.IDGEN, new IdGenEncoder())
            .put(Scheme.SCRIPT, new ScriptEncoder())
            .put(Scheme.JOB, new ScheduleEncoder())
            .put(Scheme.DOCUMENT, new DocumentEncoder())
            .put(Scheme.TABLE, new IndexEncoder())
            .put(Scheme.INDEX, new IndexEncoder())
            .put(Scheme.EVENT, new EventEncoder())
            .put(Scheme.WORKFLOW, new WorkflowEncoder())
            .put(Scheme.LOCK, new LockEncoder())
            .put(Scheme.SNIPPET, new SnippetEncoder())
            .put(Scheme.STRUCTURED, new StructuredEncoder())
            .put(Scheme.JAR, new JarEncoder())
            .put(Scheme.ENTITLEMENT, new EntitlementEncoder())
            .put(Scheme.ENTITLEMENTGROUP, new EntitlementGroupEncoder())
            .put(Scheme.FIELD, new FieldEncoder())
            .put(Scheme.FIELDTRANSFORM, new FieldTransformEncoder())
            .put(Scheme.STRUCTURE, new StructureEncoder())
            .put(Scheme.ENTITY, new EntityEncoder())
            .put(Scheme.WIDGET, new WidgetEncoder())
            .put(Scheme.PROGRAM, new ProgramEncoder())
            .put(Scheme.USER, new UserEncoder())
            .build();

    public PluginApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public List<PluginConfig> getInstalledPlugins(CallingContext context) {
        return PluginConfigStorage.readAll();
    }

    @Override
    public void recordPlugin(CallingContext context, PluginConfig config) {
        PluginConfigStorage.add(config, context.getUser(), "Recorded plugin");
    }

    private Boolean unrecordFeature(CallingContext context, String pluginName) {
        PluginConfigStorage.deleteByFields(pluginName, context.getUser(), "unrecorded");
        return true;
    }

    @Override
    public PluginManifest getPluginManifest(CallingContext context, String manifestURI) {
        RaptureURI internalURI = new RaptureURI(manifestURI, Scheme.PLUGIN_MANIFEST);
        return PluginManifestStorage.readByAddress(internalURI);
    }

    @Override
    public void deletePluginManifest(CallingContext context, String manifestURI) {
        RaptureURI uri = new RaptureURI(manifestURI, Scheme.PLUGIN_MANIFEST);
        PluginManifestStorage.deleteByAddress(uri, context.getUser(), "removed");
    }

    @Override
    public void installPlugin(CallingContext context, PluginManifest manifest, Map<String, PluginTransportItem> payload) {
        PluginConfig config = PluginConfigHelper.make(manifest);
        PluginConfigStorage.add(config, context.getUser(), "Recorded plugin");
        PluginManifestStorage.add(manifest, context.getUser(), "Plugin Manifest");
        for (PluginTransportItem item : payload.values()) {
            installRepo(context, item);
        }
        for (PluginTransportItem item : payload.values()) {
            byte[] content = item.getContent();
            if ((content != null) && (content.length > 0)) installItem(context, item);
        }
    }

    private Map<Scheme, RaptureInstaller> scheme2installer = ImmutableMap.<Scheme, RaptureInstaller> builder()
            .put(Scheme.BLOB, new BlobInstaller()).put(Scheme.SERIES, new SeriesInstaller())
            .put(Scheme.SCRIPT, new ScriptInstaller())
            .put(Scheme.JOB, new ScheduleInstaller()).put(Scheme.DOCUMENT, new DocumentInstaller())
            .put(Scheme.EVENT, new EventInstaller())
            .put(Scheme.IDGEN, new IdGenInstaller()).put(Scheme.TABLE, new IndexInstaller()).put(Scheme.INDEX, new IndexInstaller())
            .put(Scheme.WORKFLOW, new WorkflowInstaller()).put(Scheme.ENTITLEMENT, new EntitlementInstaller())
            .put(Scheme.ENTITLEMENTGROUP, new EntitlementGroupInstaller()).put(Scheme.LOCK, new LockInstaller())
            .put(Scheme.SNIPPET, new SnippetInstaller())
            .put(Scheme.STRUCTURED, new StructuredTableInstaller()).put(Scheme.JAR, new JarInstaller())
            .put(Scheme.FIELD, new FieldInstaller())
            .put(Scheme.STRUCTURE, new StructureInstaller())
            .put(Scheme.FIELDTRANSFORM, new FieldTransformInstaller())
            .put(Scheme.ENTITY, new EntityInstaller())
            .put(Scheme.WIDGET, new WidgetInstaller())
            .put(Scheme.PROGRAM, new ProgramInstaller())
            .put(Scheme.USER, new UserInstaller())
            .build();

    private Map<Scheme, RaptureInstaller> scheme2repoMaker = ImmutableMap.<Scheme, RaptureInstaller> builder()
            .put(Scheme.BLOB, new BlobRepoMaker())
            .put(Scheme.SERIES, new SeriesRepoMaker()).put(Scheme.DOCUMENT, new DocumentRepoMaker())
            .put(Scheme.STRUCTURED, new StructuredRepoMaker())
            .build();

    private Map<Scheme, RaptureInstaller> scheme2rawInstaller = ImmutableMap.of(
            Scheme.BLOB, new RawBlobInstaller(),
            Scheme.SCRIPT, new RawScriptInstaller());

    private static final ImmutableSet<Scheme> repoSet = ImmutableSet.of(Scheme.BLOB, Scheme.DOCUMENT, Scheme.SERIES, Scheme.STRUCTURED);

    public static boolean isRepository(RaptureURI uri) {
        String path = uri.getDocPath();
        return repoSet.contains(uri.getScheme()) && (path == null || "".equals(path));
    }

    @Override
    public void uninstallPluginItem(CallingContext context, PluginTransportItem item) {
        RaptureURI uri = new RaptureURI(item.getUri(), null);
        RaptureInstaller installer = null;
        if (isRepository(uri)) {
            installer = scheme2repoMaker.get(uri.getScheme());
        } else {
            installer = scheme2installer.get(uri.getScheme());
        }
        if (installer == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unknown install type: " + uri.getScheme());
        } else {
            installer.remove(context, uri, item);
        }
    }

    private void installItem(CallingContext ctx, PluginTransportItem item) {
        RaptureURI uri = new RaptureURI(item.getUri(), null);
        if (isRepository(uri)) return;
        log.info("Installing plugin item: " + uri.toShortString());

        RaptureInstaller installer = isRaw(uri) ? scheme2rawInstaller.get(uri.getScheme()) : scheme2installer.get(uri.getScheme());
        if (installer == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unknown install type: " + uri.getScheme());
        }
        installer.install(ctx, uri, item);
        log.info("Done installing plugin item: " + uri.toString());
    }

    private boolean isRaw(RaptureURI uri) {
        // TODO not actually checking that the attribute is @raw yet
        return uri.hasAttribute();
    }

    private void installRepo(CallingContext ctx, PluginTransportItem item) {
        RaptureURI uri = new RaptureURI(item.getUri(), null);
        if (!isRepository(uri)) return;
        log.info("Installing repository: " + uri.toString());

        RaptureInstaller installer = scheme2repoMaker.get(uri.getScheme());
        if (installer == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unknown install type: " + uri.getScheme());
        }
        installer.install(ctx, uri, item);
        log.info("Done installing plugin type: " + uri.toString());
    }

    @Override
    public void uninstallPlugin(CallingContext context, String name) {
        String manifestURIstring = Scheme.PLUGIN_MANIFEST.toString() + "://" + name;
        PluginManifest manifest = getPluginManifest(context, manifestURIstring);
        byte[] placeHolder = new byte[1];
        // uninstall plugin items
        for (PluginManifestItem item : manifest.getContents()) {
            log.info("Uninstalling " + item.getURI());
            PluginTransportItem transport = new PluginTransportItem();
            transport.setUri(item.getURI());
            transport.setHash(item.getHash());
            transport.setContent(placeHolder);
            uninstallPluginItem(context, transport);
        }
        // remove plugin manifest
        deletePluginManifest(context, manifestURIstring);
        unrecordFeature(context, name);
        log.info("Uninstalled plugin " + name);
    }

    @Override
    public PluginTransportItem getPluginItem(CallingContext context, String uriString) {
        RaptureURI uri = new RaptureURI(uriString, null);
        Scheme scheme = uri.getScheme();
        RaptureEncoder encoder = scheme2encoder.get(scheme);
        if (encoder == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "No encoder for " + scheme);
        }
        return encoder.encode(context, uriString);
    }

    @Override
    public Map<String, String> verifyPlugin(CallingContext context, String plugin) {
        
        Map<String, String> result = Maps.newHashMap();
        PluginManifest manifest = getPluginManifest(context, plugin);
        if (manifest == null) {
            result.put("<manifest not found>", "Plugin is not installed");
        } else for (PluginManifestItem item : manifest.getContents()) {
            try {
                PluginTransportItem xport = getPluginItem(context, item.getURI());
                if (xport == null) {
                    result.put(item.getURI(), "Missing");
                } else if (item.getHash() == null) {
                    // Defensive code -- this should not happen
                    result.put(item.getURI(), "No MD5 in manifest");
                } else if (!item.getHash().equals(xport.getHash())) {
                    result.put(item.getURI(), "MD5 Hash in manifest " + item.getHash() + " does not match hash for content " + xport.getHash());
                } else result.put(item.getURI(), "Verified");
            } catch (Exception ex) {
                result.put(item.getURI(), "Exception:" + ex.getMessage());
            }
        }
        return result;
    }

    /**
     * create an empty manifest on the server with version 0.0.0
     */
    @Override
    public void createManifest(CallingContext context, String pluginName) {
        PluginManifest manifest = PluginManifestStorage.readByFields(pluginName);
        if (manifest != null) {
            throw RaptureExceptionFactory.create("Plugin " + pluginName + " already exists. Cannot create.");
        }
        manifest = new PluginManifest();
        manifest.setVersion(new PluginVersion(0, 0, 0, 0L));
        manifest.setContents(Lists.<PluginManifestItem> newArrayList());
        manifest.setDepends(Maps.<String, PluginVersion> newHashMap());
        manifest.setPlugin(pluginName);
        PluginManifestStorage.add(manifest, context.getUser(), "Create Empty Manifest");
    }

    /**
     * add an object on the server to a plugin manifest on the server
     */
    @Override
    public void addManifestItem(CallingContext context, String pluginName, String uriS) {
        PluginManifest manifest = PluginManifestStorage.readByFields(pluginName);
        if (manifest == null) {
            throw RaptureExceptionFactory.create("Plugin " + pluginName + " does not exist. Cannot add items.");
        }
        RaptureURI uri = new RaptureURI(uriS);
        PluginManifestItem item = new PluginManifestItem();
        item.setURI(uri.toString());
        PluginTransportItem xport = getPluginItem(context, item.getURI());
        if (xport == null) {
            log.error("unable to get item URI: " + item.getURI());
            throw RaptureExceptionFactory.create("item " + item.getURI() + " does not exist. Cannot add.");
        }
        item.setHash(xport.getHash());
        List<PluginManifestItem> items = manifest.getContents();
        if (items == null) items = Lists.newArrayList();
        if (!hasUri(items, item.getURI())) items.add(item);
        manifest.setContents(items);
        PluginManifestStorage.add(manifest, context.getUser(), "Add item to plugin");
    }

    private boolean hasUri(List<PluginManifestItem> items, String uri) {
        for (PluginManifestItem item : items) {
            if (uri.equals(item.getURI())) return true;
        }
        return false;
    }

    /**
     * add uris within the specified docpath root. If no type is specifed in the uri, use all four of doc, blob, series, and sheet. Example1:
     * //myProject/myfolder adds all four types. Example2: blob://myproject/myfolder adds only blobs
     */
    @Override
    public void addManifestDataFolder(CallingContext context, String pluginName, String uriS) {
        boolean flag = false;
        PluginManifest manifest = PluginManifestStorage.readByFields(pluginName);
        if (manifest == null) {
            throw RaptureExceptionFactory.create("Plugin " + pluginName + " does not exist. Cannot add item.");
        }
        if (uriS.contains(":")) {
            RaptureURI uri = new RaptureURI(uriS);
            flag = addFolder(context, manifest, uri);
        } else {
            flag = addFolder(context, manifest, new RaptureURI(uriS, Scheme.SERIES));
            flag |= addFolder(context, manifest, new RaptureURI(uriS, Scheme.DOCUMENT));
            flag |= addFolder(context, manifest, new RaptureURI(uriS, Scheme.BLOB));
        }
        if (flag) PluginManifestStorage.add(manifest, context.getUser(), "Add folder to plugin");
    }

    private boolean addFolder(CallingContext context, PluginManifest manifest, RaptureURI uri) {
        boolean flag = false;
        Map<String, RaptureFolderInfo> map = Kernel.getSys().listByUriPrefix(context, uri.toString(), null, 0, Long.MAX_VALUE, 0L).getChildren();
        List<PluginManifestItem> items = manifest.getContents();
        if (items == null) items = Lists.newArrayList();
        Set<String> orig = Sets.newHashSet();
        for (PluginManifestItem item : items) {
            orig.add(item.getURI());
        }
        for (Map.Entry<String, RaptureFolderInfo> entry : map.entrySet()) {
            if (entry.getValue().isFolder()) continue;
            if (orig.contains(entry.getKey())) continue;
            PluginManifestItem item = new PluginManifestItem();
            item.setURI(entry.getKey());
            PluginTransportItem xport = getPluginItem(context, item.getURI());
            item.setHash(xport.getHash());
            items.add(item);
            flag = true;
        }
        manifest.setContents(items);
        return flag;
    }

    /**
     * refresh the MD5 checksums in the manifest and set the version for a manifest on the server
     */
    @Override
    public void setManifestVersion(CallingContext context, String pluginName, String version) {
        PluginManifest manifest = PluginManifestStorage.readByFields(pluginName);
        if (manifest == null) {
            throw RaptureExceptionFactory.create("Plugin " + pluginName + " does not exist. Cannot set version.");
        }
        PluginVersion fv = parseVersion(version);
        manifest.setVersion(fv);
        for (PluginManifestItem item : manifest.getContents()) {
            PluginTransportItem xport = getPluginItem(context, item.getURI());
            item.setHash(xport.getHash());
        }
        PluginManifestStorage.add(manifest, context.getUser(), "Set version");
    }

    private PluginVersion parseVersion(String version) {
        PluginVersion result = new PluginVersion();
        String parts[] = version.split("\\.");
        if (parts.length > 4) {
            throw RaptureExceptionFactory.create("Malformatted version string. Version is a maximum of four dot separated integers.");
        }
        result.setMajor(parseIntOrDie(parts[0]));
        if (parts.length > 1) result.setMinor(parseIntOrDie(parts[1]));
        if (parts.length > 2) result.setRelease(parseIntOrDie(parts[2]));
        if (parts.length > 3) result.setTimestamp(parseLongOrDie(parts[3]));
        return result;
    }

    private int parseIntOrDie(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw RaptureExceptionFactory.create("Error: could not set version. " + string + " is not a number.");
        }
    }

    private Long parseLongOrDie(String string) {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            throw RaptureExceptionFactory.create("Error: could not set version. " + string + " is not a number.");
        }
    }

    @Override
    public void installPluginItem(CallingContext context, String pluginName, PluginTransportItem item) {
        installRepo(context, item);
        installItem(context, item);
    }

    @Override
    public void removeItemFromManifest(CallingContext context, String pluginName, String uriS) {
        PluginManifest manifest = PluginManifestStorage.readByFields(pluginName);
        if (manifest == null) {
            throw RaptureExceptionFactory.create("Plugin " + pluginName + " does not exist. Cannot remove item.");
        }
        RaptureURI uri = new RaptureURI(uriS);
        String uriN = uri.toString();
        Iterator<PluginManifestItem> iter = manifest.getContents().iterator();
        while (iter.hasNext()) {
            PluginManifestItem item = iter.next();
            if (uriN.equals(item.getURI())) {
                iter.remove();
            }
        }
        PluginManifestStorage.add(manifest, context.getUser(), "Remove plugin item");
    }

    @Override
    public void removeManifestDataFolder(CallingContext context, String pluginName, String uriS) {
        PluginManifest manifest = PluginManifestStorage.readByFields(pluginName);
        if (manifest == null) {
            throw RaptureExceptionFactory.create("Plugin " + pluginName + " does not exist. Cannot remove item.");
        }
        if (uriS.contains(":")) {
            RaptureURI uri = new RaptureURI(uriS);
            String uriN = uri.toString() + "/";
            Iterator<PluginManifestItem> iter = manifest.getContents().iterator();
            while (iter.hasNext()) {
                PluginManifestItem item = iter.next();
                if (item.getURI().startsWith(uriN)) {
                    iter.remove();
                }
            }
        } else {
            RaptureURI uri = new RaptureURI(uriS, Scheme.SERIES);
            String uriN = uri.toString() + "/";
            uriN = uriN.split(":")[1];
            Iterator<PluginManifestItem> iter = manifest.getContents().iterator();
            while (iter.hasNext()) {
                PluginManifestItem item = iter.next();
                String parts[] = item.getURI().split(":");
                String uriO = (parts.length == 2) ? parts[1] : item.getURI();
                if (uriO.startsWith(uriN)) {
                    iter.remove();
                }
            }
        }
    }

    public static final String PLUGIN_TXT = "plugin.txt";

    class Metadata {
        String creator;
        String lastModified;
        long objectCount;
        String[] objectsNotAdded;
        long fileSize;
        String filePath;

        SimpleDateFormat sdf = new SimpleDateFormat();

        Metadata(File file, String name, String user, int count, String[] objectsNotAdded) {
            this.creator = user;
            this.lastModified = sdf.format(new Date(file.lastModified()));
            this.objectCount = count;
            this.objectsNotAdded = objectsNotAdded;
            this.fileSize = file.length();
            this.filePath = name;
        }

        public String getCreator() {
            return creator;
        }

        public String getLastModified() {
            return lastModified;
        }

        public long getFileSize() {
            return fileSize;
        }

        public long getObjectCount() {
            return objectCount;
        }

        public String[] getObjectsNotAdded() {
            return objectsNotAdded;
        }

        public String getFilePath() {
            return filePath;
        }

    }

    @Override
    public String exportPlugin(CallingContext context, String pluginName, String path) {
        String zipDirName = IDGenerator.getUUID();
        String zipFileName = pluginName + ".zip";

        RaptureURI blobUri = null;
        try {
            blobUri = new RaptureURI(path);
            if (blobUri.getScheme().equals(Scheme.BLOB)) {
                path = "/tmp/" + blobUri.getDocPath();
            } else {
                blobUri = null;
            }
        } catch (RaptureException re) {
            log.debug(path + " is not a Blob URI");
        }
        File zipDir = new File(path, zipDirName);

        if (!zipDir.mkdirs()) {
            log.error("Cannot create " + zipDirName);
            throw RaptureExceptionFactory.create(HttpStatus.SC_UNAUTHORIZED, "Cannot create " + zipFileName + ".zip in " + path);
        }

        PluginManifest fm = getPluginManifest(context, "//" + pluginName);

        int objectCount = 0;
        List<String> objectsNotAdded = new ArrayList<>();
        File zipFile = new File(zipDir, zipFileName);
        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            PluginConfig config = new PluginConfig();
            config.setDepends(fm.getDepends());
            config.setDescription(fm.getDescription());
            config.setVersion(fm.getVersion());
            config.setPlugin(pluginName);
            ZipEntry entry = new ZipEntry(PLUGIN_TXT);
            out.putNextEntry(entry);
            out.write(JacksonUtil.jsonFromObject(config).getBytes("UTF-8"));

            for (PluginManifestItem item : fm.getContents()) {
                String uri = item.getURI();
                try {
                    RaptureURI ruri = new RaptureURI(uri);
                    PluginTransportItem pti = getPluginItem(context, uri);
                    String filePath = PluginSandboxItem.calculatePath(ruri, null);
                    ZipEntry zentry = new ZipEntry(filePath);
                    out.putNextEntry(zentry);
                    out.write(pti.getContent());
                    objectCount++;
                } catch (Exception e) {
                    objectsNotAdded.add(uri);
                }
            }
        } catch (IOException e) {
            log.error("Cannot export " + pluginName);
            throw RaptureExceptionFactory.create(HttpStatus.SC_UNAUTHORIZED, "Cannot export " + pluginName, e);
        }

        String filePath = zipDirName + "/" + zipFileName;
        if (blobUri != null) {
            byte[] rawFileData = new byte[(int) zipFile.length()];
            filePath = blobUri.toString();
            try (FileInputStream fileInputStream = new FileInputStream(zipFile)) {
                fileInputStream.read(rawFileData);
                fileInputStream.close();
                Kernel.getBlob().putBlob(context, filePath, rawFileData, MediaType.ZIP.toString());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        String user = Kernel.getUser().getWhoAmI(context).getUsername();
        String[] fna = objectsNotAdded.toArray(new String[objectsNotAdded.size()]);
        
        Metadata metadata = new Metadata(zipFile, filePath, user, objectCount, fna);

        return JacksonUtil.jsonFromObject(metadata);
    }
}
