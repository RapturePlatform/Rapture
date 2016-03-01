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
import static rapture.log.LogManager.configureLogging;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import rapture.common.PluginConfig;
import rapture.common.PluginManifest;
import rapture.common.PluginManifestItem;
import rapture.common.PluginTransportItem;
import rapture.common.PluginVersion;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.ScriptingApi;
import rapture.common.client.HttpLoginApi;
import rapture.common.client.ScriptClient;
import rapture.common.client.SimpleCredentialsProvider;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.script.KernelScript;
import rapture.plugin.PluginUtil;
import rapture.plugin.util.PluginUtils;
import rapture.plugin.validators.BlobValidator;
import rapture.plugin.validators.DocumentValidator;
import rapture.plugin.validators.EventValidator;
import rapture.plugin.validators.FieldValidator;
import rapture.plugin.validators.IdGenValidator;
import rapture.plugin.validators.JobValidator;
import rapture.plugin.validators.LockValidator;
import rapture.plugin.validators.Note;
import rapture.plugin.validators.ScriptValidator;
import rapture.plugin.validators.SeriesValidator;
import rapture.plugin.validators.SnippetValidator;
import rapture.plugin.validators.WorkflowValidator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This class implements a simple DSL for defining, uploading, downloading, installing, uninstalling, and upgrading collections of user content called "plugins"
 * to the Rapture Server
 * 
 * @author mel
 */
public class PluginShell {
    public final static String EXIT = "exit";

    private String host = "http://localhost:8665/rapture";
    private String username = "rapture";
    private String password = "rapture";
    private String rootDirname = "/tmp/plugins";
    private String importDirname;
    private String loadDirname;
    private String loadOrder;
    private final static boolean debug = true;

    private static Logger logger = Logger.getLogger(PluginShell.class.getName());

    protected ScriptingApi client;
    protected File rootDir;
    protected File importDir;
    protected File loadDir;
    protected String importOrder;
    protected String importInclude;
    protected String importExclude;
    protected String importOverlay;

    protected boolean hasError = false;
    private String variant = null;
    private boolean strict = false;
    private boolean build = false;
    private String featureName;
    private String zipFile;

    protected Map<String, PluginSandbox> name2sandbox = Maps.newHashMap();

    private boolean isLocal;

    public static void main(String[] args) {
        new PluginShell(args).go();
        System.exit(0);
    }

    public PluginShell(String args[]) {
        configureLogging();
        configureStdoutLayout();
        processArgs(args);
        RaptureConfig.setLoadYaml(!build);
        createClient();
        initStorage();
        checkImportDir();
        checkIncludeExclude();
        checkLoadDir();
    }

    /**
     * Shell output doesn't need to be fancy, just the message and a line break
     */
    private void configureStdoutLayout() {
        Appender stdoutAppender = Logger.getRootLogger().getAppender("stdout");
        PatternLayout p = new PatternLayout();
        p.setConversionPattern("%m%n");
        stdoutAppender.setLayout(p);
    }

    private void checkImportDir() {
        if (importDirname != null && !importDirname.trim().isEmpty()) {
            importDir = new File(importDirname);
            checkArgument(importDir.isDirectory() && importDir.exists(), "Invalid importdir specified " + importDirname);
        }
    }

    private void checkIncludeExclude() {
        if (!StringUtils.isEmpty(importInclude) && !StringUtils.isEmpty(importExclude)) {
            throw new IllegalArgumentException("Cannot specify both -include and -exclude.  Must be one or the other or neither");
        }
    }

    private void checkLoadDir() {
        if ((loadDirname != null) && !loadDirname.trim().isEmpty()) {
            loadDir = new File(loadDirname);
            rootDir = loadDir;
            checkArgument(loadDir.isDirectory() && loadDir.exists(), "LoaddDir invalid:" + loadDirname);
        } else if (build) {
            rootDir = new File(".");
        }
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public void deflate(String plugin) {
        PluginSandbox box = name2sandbox.get(plugin);
        if (box == null) {
            error("plugin " + plugin + " is not in memory");
        } else {
            box.deflate();
        }
    }

    public String getVariant() {
        return variant;
    }

    private void initStorage() {
        rootDir = new File(rootDirname);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        } else {
            checkArgument(rootDir.isDirectory(), "Cannot save to non-directory " + rootDirname);
        }
    }

    private void go() {
        // non-interactive batch mode
        if (build) {
            new PluginBatchMode(this).buildFeature(featureName, zipFile);
            return;
        }

        if (importDir != null || loadDir != null) {
            if (importDir != null) {
                new PluginBatchMode(this).go();
            }

            if (loadDir != null) {
                new PluginBatchMode(this).goDir();
            }
            return;
        }

        System.out.println("\nWelcome to the pluginInstaller.\nPlease enter your command, or type 'help' for help");

        try (Scanner in = new Scanner(System.in)) {
            while (true) {
                String line = in.nextLine().trim();
                if (StringUtils.isEmpty(line)) {
                    continue;
                }
                eval(line);
            }
        }
    }

    public static final String ARG_HOST = "-host";
    public static final String ARG_USER = "-user";
    public static final String ARG_PASSWORD = "-password";
    public static final String ARG_SAVEDIR = "-savedir";
    public static final String ARG_IMPORTDIR = "-importdir";
    public static final String ARG_IMPORTORDER = "-importorder";
    public static final String ARG_IMPORTOVERLAY = "-importoverlay";
    public static final String ARG_LOADDIR = "-loaddir";
    public static final String ARG_LOADORDER = "-loadorder";
    public static final String ARG_LOCAL = "-local";
    public static final String ARG_BUILD = "-build";
    public static final String ARG_STRICT = "-strict";
    public static final String ARG_INCLUDE = "-include";
    public static final String ARG_EXCLUDE = "-exclude";

    private void processArgs(String args[]) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (ARG_HOST.equals(args[i])) {
                    host = args[++i];
                } else if (ARG_USER.equals(args[i])) {
                    username = args[++i];
                } else if (ARG_PASSWORD.equals(args[i])) {
                    password = args[++i];
                } else if (ARG_SAVEDIR.equals(args[i])) {
                    rootDirname = args[++i];
                } else if (ARG_IMPORTDIR.equals(args[i])) {
                    importDirname = args[++i];
                } else if (ARG_IMPORTORDER.equals(args[i])) {
                    importOrder = args[++i];
                } else if (ARG_INCLUDE.equals(args[i])) {
                    importInclude = args[++i];
                } else if (ARG_EXCLUDE.equals(args[i])) {
                    importExclude = args[++i];
                } else if (ARG_IMPORTOVERLAY.equals(args[i])) {
                    importOverlay = args[++i];
                } else if (ARG_LOADDIR.equals(args[i])) {
                    loadDirname = args[++i];
                } else if (ARG_LOADORDER.equals(args[i])) {
                    loadOrder = args[++i];
                } else if (ARG_LOCAL.equals(args[i])) {
                    isLocal = true;
                } else if (ARG_STRICT.equals(args[i])) {
                    strict = true;
                } else if (ARG_BUILD.equals(args[i])) {
                    build = true;
                    isLocal = true;
                    featureName = args[++i];
                    i++;
                    zipFile = (i >= args.length) ? featureName+".zip" : args[i];
                } else {
                    showProgramUsage();
                    System.exit(1);
                }
            }
        } catch (Exception ex) {
            showProgramUsage();
            System.exit(1);
        }
    }

    private void showProgramUsage() {
        println("Usage: pluginShell [-host hostname] [-user username] [-password password] [-savedir savedir] [-local] [-loaddir loaddir] [-loadorder loadorder] [-importdir importdir] [-importorder plugin1,plugin2,...] [-build name [name.zip]] [[-include plugin1,plugin2,...] | [-exclude plugin1,plugin2,...]] [-importoverlay prod | qa | ...]");
    }

    private void createClient() {
        if (isLocal) {
            KernelScript ks = new KernelScript();
            ks.setCallingContext(ContextFactory.getKernelUser());
            client = ks;
        } else {
            SimpleCredentialsProvider creds = new SimpleCredentialsProvider(username, password);
            HttpLoginApi login = new HttpLoginApi(host, creds);
            login.login();
            client = new ScriptClient(login);
        }
    }

    private Command validateCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String name = nextToken(args);
            if (name == null) {
                showUsage(this);
                return;
            }
            File pluginDir = new File(rootDir, name);
            if (!pluginDir.exists()) {
                System.out.println("No plugin has been saved to the specified directory: " + pluginDir.getAbsolutePath());
                return;
            }
            try {
                File f = new File(pluginDir, "plugin.txt");
                if (!f.exists()) {
                    System.out.println("There is no plugin.txt file in specified directory: " + pluginDir.getAbsolutePath());
                    return;
                }
                String raw = PluginUtil.getFileAsString(f);
                JacksonUtil.objectFromJson(raw, PluginConfig.class);
            } catch (Exception e) {
                System.out.println("Invalid plugin.txt syntax in specified directory: " + pluginDir.getAbsolutePath());
                return;
            }

            List<Note> errors = Lists.newArrayList();
            System.out.println("validating plugin in " + pluginDir.getAbsolutePath());
            for (File f : pluginDir.listFiles()) {
                if (f.isDirectory()) {
                    validateDir(f, rootDir, errors);
                }
            }
            for (Note note : errors) {
                System.out.println(note.toString());
            }
            System.out.println("done");
        }

        @Override
        public String help() {
            return "validate [plugin]\t\t\tCheck for syntax errors in the local working directory for a plugin.";
        }

    };

    private static void validateDir(File dir, File rootDir, List<Note> errors) {
        File file[] = dir.listFiles();
        for (File f : file) {
            if (f.isDirectory()) {
                validateDir(f, rootDir, errors);
            } else {
                validateSandboxItem(f, rootDir, errors);
            }
        }
    }

    private static void validateSandboxItem(File f, File rootDir, List<Note> errors) {

        Pair<RaptureURI, String> uriPair = null;
        try {
            uriPair = PluginSandboxItem.calculateURI(f, rootDir);
        } catch (Exception ex) {
            errors.add(new Note(Note.Level.WARNING, "Skipping over unrecognized file " + f.getPath()));
            return;
        }
        RaptureURI uri = uriPair.getLeft();
        String content;
        try {
            content = FileUtils.readFileToString(f);
        } catch (IOException e) {
            errors.add(new Note(Note.Level.ERROR, "Unable to validate file due to I/O problem: " + f.getPath()));
            errors.add(new Note(Note.Level.INFO, e.getMessage()));
            return;
        }
        switch (uri.getScheme()) {
            case BLOB:
                BlobValidator.getValidator().validate(content, uri, errors);
                break;
            case DOCUMENT:
                DocumentValidator.getValidator().validate(content, uri, errors);
                break;
            case EVENT:
                EventValidator.getValidator().validate(content, uri, errors);
                break;
            case FIELD:
                FieldValidator.getValidator().validate(content, uri, errors);
                break;
            case IDGEN:
                IdGenValidator.getValidator().validate(content, uri, errors);
                break;
            case JOB:
                JobValidator.getValidator().validate(content, uri, errors);
                break;
            case LOCK:
                LockValidator.getValidator().validate(content, uri, errors);
                break;
            case SCRIPT:
                ScriptValidator.getValidator().validate(content, uri, errors);
                break;
            case SERIES:
                SeriesValidator.getValidator().validate(content, uri, errors);
                break;
            case SNIPPET:
                SnippetValidator.getValidator().validate(content, uri, errors);
                break;
            case TABLE:
                errors.add(new Note(Note.Level.ERROR, "No Installer available for TABLE scheme"));
                break;
            case WORKFLOW:
                WorkflowValidator.getValidator().validate(content, uri, errors);
                break;
            default:
                errors.add(new Note(Note.Level.WARNING, "No validator registered for type " + uri.getScheme()));
                break;
        }
    }

    private static Command exitCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            System.exit(0);
        }

        @Override
        public String help() {
            return "exit\t\t\t\tQuit the shell";
        }
    };

    private Command addCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String plugin = nextToken(args);
            String uriStr = nextToken(args);
            if (plugin == null || uriStr == null) {
                showUsage(this);
                return;
            }
            String addVariant = nextToken(args);
            PluginSandbox sandbox = name2sandbox.get(plugin);
            if (sandbox == null) {
                error("plugin not present. Create, load, download, or import first");
                return;
            }
            RaptureURI uri;
            try {
                uri = new RaptureURI(uriStr, null);
            } catch (RaptureException ex) {
                error(ex.getMessage());
                return;
            }
            sandbox.addURI(addVariant, uri);
            println("Added");
        }

        @Override
        public String help() {
            return "add [plugin] [uri]\t\tadd a URI to the manifest as part of core content (defer download)\n"
                    + "add [plugin] [uri] [variant]\tadd a URI to the manifest as part of an overlay";
        }
    };

    private Command addAllCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String plugin = nextToken(args);
            if (plugin == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(plugin);
            if (sandbox == null) throw RaptureExceptionFactory.create("plugin not present. Create, download, load, or import first");
            String root = nextToken(args);
            String addVariant = nextToken(args);
            RaptureURI uri = new RaptureURI(root, null);
            switch (uri.getScheme()) {
                case BLOB:
                    addBlobTree(addVariant, sandbox, uri);
                    break;
                case DOCUMENT:
                    addDocTree(addVariant, sandbox, uri);
                    break;
                case SERIES:
                    addSeriesTree(addVariant, sandbox, uri);
                    break;
                default:
                    error("Unsupported type for recursive addition");
                    return;
            }
            System.out.println("Done");
        }

        @Override
        public String help() {
            return "addall [plugin] [uri]\t\tadd all uris in the tree rooted at the given uri\n"
                    + "addall [plugin] [uri] [vrnt]\tadd all uris in the tree rooted at the given uri to a variant";
        }
    };

    private Command streamInstallCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String pluginName = nextToken(args);
            if (pluginName == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(pluginName);
            if (sandbox == null) {
                error("plugin not present.  Try download, import, or load first.");
                return;
            }
            String thisVariant = nextToken(args);
            if (thisVariant == null) thisVariant = variant;
            PluginManifest manifest = sandbox.makeManifest(thisVariant);
            for (PluginSandboxItem item : sandbox.getItems(thisVariant)) {
                try {
                    if (debug) System.out.println("Installing " + item.getURI().toString());
                    client.getPlugin().installPluginItem(pluginName, item.makeTransportItem());
                } catch (NoSuchAlgorithmException e) {
                    error("Bad JDK: No MD5 calculator");
                } catch (IOException e) {
                    error("Problem installing " + item.getURI().toString());
                    e.printStackTrace();
                }
            }
            System.out.println("Done");
        }

        @Override
        public String help() {
            return "stream [plugin]\t\tInstall a plugin using streaming interface\n"
                    + "stream [plugin] [variant]\tInstall a plugin with a variant using streaming interface";
        }
    };

    private Command useCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String variant = nextToken(args);
            if (variant == null) {
                showUsage(this);
                return;
            }
            setVariant(variant);
            println("Done.");
        }

        @Override
        public String help() {
            return "use [variant]\t\t\tselect an environment overlay (e.g. dev, prod, research, qa)";
        }
    };

    private static byte[] PLACEHOLDER = new byte[1];
    static {
        PLACEHOLDER[0] = 64;
    } // doesn't matter what this byte is -- just the transport hates empty arrays

    private Command uninstallCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String plugin = nextToken(args);
            if (plugin == null) {
                showUsage(this);
                return;
            }

            List<PluginConfig> remote = client.getPlugin().getInstalledPlugins();
            boolean present = false;
            for (PluginConfig pc : remote) {
                if (plugin.equals(pc.getPlugin())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                error("Plugin " + plugin + " is not installed");
                return;
            }

            PluginSandbox sandbox = name2sandbox.get(plugin);
            if (sandbox == null) {
                error("plugin not present.  Load, download, or import first");
                return;
            }
            String thisVariant = nextToken(args);
            if (thisVariant == null) {
                thisVariant = variant;
            }

            try (Scanner in = new Scanner(System.in)) {

                boolean uninstallAll = false;
                String manifestURIstring = Scheme.PLUGIN_MANIFEST.toString() + "://" + plugin;
                PluginManifest manifest = client.getPlugin().getPluginManifest(manifestURIstring);
                for (PluginManifestItem item : manifest.getContents()) {
                    boolean uninstallThis = uninstallAll;
                    if (!uninstallThis) {
                        println("Uninstall " + item.getURI() + " ? [nyaq]");
                        String response = in.nextLine().trim();
                        if (response.equalsIgnoreCase("y")) {
                            uninstallThis = true;
                        } else if (response.equalsIgnoreCase("n") || response.isEmpty()) {
                            uninstallThis = false;
                        } else if (response.equalsIgnoreCase("a")) {
                            uninstallThis = true;
                            uninstallAll = true;
                        } else if (response.equalsIgnoreCase("q")) {
                            break;
                        }
                    }
                    if (uninstallThis) {
                        println("Uninstalling " + item.getURI());
                        PluginTransportItem transport = new PluginTransportItem();
                        transport.setUri(item.getURI());
                        transport.setHash(item.getHash());
                        transport.setContent(PLACEHOLDER);
                        client.getPlugin().uninstallPluginItem(transport);
                    } else {
                        println("Skipping " + item.getURI());
                    }
                }
                println("Remove manifest from server? [ny]");
                String response = in.nextLine().trim();
                if (response.equalsIgnoreCase("y")) {
                    client.getPlugin().deletePluginManifest(manifestURIstring);
                }
            }
            println("Done.");
        }

        @Override
        public String help() {
            return "uninstall [plugin] \t\tattempt to uninstall the items in this plugin";
        }
    };

    private Command removeCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String plugin = nextToken(args);
            String uriStr = nextToken(args);
            if (plugin == null || uriStr == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(plugin);
            if (sandbox == null) {
                error("plugin not present.  Load, download, or import first");
                return;
            }
            RaptureURI uri;
            try {
                uri = new RaptureURI(uriStr, null);
            } catch (RaptureException ex) {
                error(ex.getMessage());
                return;
            }
            if (!sandbox.removeURI(uri)) {
                error("URI not present");
            } else {
                println("removed");
            }
        }

        @Override
        public String help() {
            return "remove [plugin] [uri]\t\tremove a uri from the manifest";
        }
    };

    private Command listCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            List<PluginConfig> remote = client.getPlugin().getInstalledPlugins();
            for (PluginConfig c : remote) {
                prettyPrint(c);
            }
            if (remote.isEmpty()) {
                println("No Plugins installed.");
            }
        }

        @Override
        public String help() {
            return "installed\t\t\tShow the list of Plugins that have been installed on the server";
        }
    };

    private Command listLocalCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            for (PluginSandbox s : name2sandbox.values()) {
                prettyPrint(s.makeConfig());
            }
            if (name2sandbox.isEmpty()) {
                println("No Plugins opened.");
            }
        }

        @Override
        public String help() {
            return "opened\t\t\t\tShow the list of Plugins that have been opened for editing";
        }
    };

    private Command createSandboxCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String name = nextToken(args);
            if (name == null) {
                showUsage(this);
                return;
            }
            File dir = new File(rootDir, name);
            if (dir.exists()) {
                error("Plugin already exists. Load instead or destroy first.");
                return;
            }
            if (!dir.mkdirs()) {
                error("Could not create temporary directory " + dir.getPath());
                return;
            }
            PluginSandbox sandbox = new PluginSandbox();
            sandbox.setPluginName(name);
            sandbox.setStrict(strict);
            sandbox.setRootDir(dir);
            try {
                sandbox.writeConfig();
            } catch (IOException ex) {
                error("SEVERE: Could not create Plugin.txt: fix file system premissions before continuing");
            }

            name2sandbox.put(name, sandbox);
            println("Created.");
        }

        @Override
        public String help() {
            return "create [name]\t\t\tCreate a new Plugin locally";
        }
    };

    private Command loadSandboxCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String name = nextToken(args);
            if (name == null) {
                showUsage(this);
                return;
            }
            String thisVariant = nextToken(args);
            if (thisVariant == null) {
                thisVariant = variant;
            }
            File dir = new File(rootDir, name);
            if (!dir.exists()) {
                error("Plugin not found locally: " + name);
                return;
            }
            PluginSandbox sandbox = new PluginSandbox();
            sandbox.setRootDir(dir);
            sandbox.setPluginName(name);
            sandbox.readConfig();
            sandbox.readContent(thisVariant);
            name2sandbox.put(name, sandbox);
            println("loaded");
        }

        @Override
        public String help() {
            return "load [plugin]\t\t\tLoad plugin manifest from the local disk (overwrite, no hashes).\n"
                    + "load [plugin] [variant]\tLoad plugin manifest from the local disk for a given overlay";
        }
    };

    private Command saveSandboxCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String name = nextToken(args);
            if (name == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(name);
            if (sandbox == null) {
                error("Not ready to save.  Create, load, or download first");
                return;
            }
            try {
                sandbox.save(client);
            } catch (Exception ex) {
                error(ex);
                return;
            }
            println("saved");
        }

        @Override
        public String help() {
            return "save [plugin]\t\t\tExtract content that has not been cached from the server and save all to local files.";
        }
    };

    private Command downloadCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String pluginName = nextToken(args);
            if (pluginName == null) {
                showUsage(this);
                return;
            }
            PluginManifest fm = client.getPlugin().getPluginManifest("//" + pluginName);
            PluginSandbox sandbox = new PluginSandbox();
            sandbox.setPluginName(pluginName);
            sandbox.setRootDir(new File(rootDir, pluginName));
            for (PluginManifestItem item : fm.getContents()) {
                sandbox.addURI(null, new RaptureURI(item.getURI(), null), item.getHash());
            }
            name2sandbox.put(pluginName, sandbox);
            println("Done");
        }

        @Override
        public String help() {
            return "download [plugin]\t\tDownload (and overwrite) the plugin manifest into memory";
        }
    };

    private Command extractCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String plugin = nextToken(args);
            String uriStr = nextToken(args);
            if (plugin == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(plugin);
            if (sandbox == null) {
                error("plugin not present.  Create, load, download, or import first");
                return;
            }
            if (uriStr == null) {
                sandbox.extract(client, false);
            } else {
                RaptureURI uri;
                try {
                    uri = new RaptureURI(uriStr, null);
                } catch (RaptureException ex) {
                    error(ex.getMessage());
                    return;
                }
                sandbox.extract(client, uri, false);
            }
            println("Done");
        }

        @Override
        public String help() {
            return "extract [plugin]\t\tExtract missing content for URIs in the local manifest into memory\n"
                    + "extract [plugin] [uri]\t\tExtract specified content into memory";
        }
    };

    // TODO MEL move this to a utility class
    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    private Command versionCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String name = nextToken(args);
            String version = nextToken(args);
            if (name == null || version == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(name);
            if (sandbox == null) {
                error("plugin is not loaded");
                return;
            }
            PluginVersion ver = parseVersion(version);
            if (ver == null) {
                error("Cannot parse as number.numer.number: " + version);
                return;
            }
            sandbox.setVersion(ver);
            try {
                sandbox.writeConfig();
            } catch (IOException e) {
                error("Could not save new version: " + e.getMessage());
                return;
            }
            println("Done");
        }

        @Override
        public String help() {
            return "setver [plugin] [x.y.z]\tSet the plugin version";
        }
    };

    private PluginVersion parseVersion(String in) {
        PluginVersion fv = new PluginVersion();
        String part[] = in.split("\\.");
        if (part.length != 3) {
            System.err.println("Bad split");
            return null;
        }
        try {
            fv.setMajor(Integer.parseInt(part[0]));
            fv.setMinor(Integer.parseInt(part[1]));
            fv.setRelease(Integer.parseInt(part[2]));
        } catch (Exception ex) {
            return null;
        }
        return fv;
    }

    private Command describeCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String name = nextToken(args);
            String where = nextToken(args);
            if (name == null || where == null) {
                showUsage(this);
                return;
            }

            PluginManifest manifest = null;
            if ("local".equals(where)) {
                PluginSandbox sandbox = name2sandbox.get(name);
                if (sandbox == null) {
                    error("plugin not loaded.  download, load, or import first.");
                    return;
                }
                manifest = sandbox.makeManifest(variant);
            } else if ("remote".equals(where)) {
                manifest = client.getPlugin().getPluginManifest("//" + name);
                if (manifest == null) {
                    error("Manifest not found.  Is plugin installed?");
                }
            } else {
                error("Location more be 'remote' or 'local'");
                showUsage(this);
                return;
            }
            prettyPrint(manifest);
        }

        @Override
        public String help() {
            return "describe [plugin] remote\tshow the manifest as it appears on the server\n"
                    + "describe [plugin] local\tshow the manifest as it appears in the editor";
        }
    };

    private Command destroyCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String name = nextToken(args);
            if (name == null) {
                showUsage(this);
                return;
            }
            name2sandbox.remove(name);
            File dir = new File(rootDir, name);
            if (!dir.exists()) {
                error("Content not present");
                return;
            }
            if (dir.isDirectory()) {
                deleteFolder(dir);
            } else {
                dir.delete();
            }
            println("Done");
        }

        @Override
        public String help() {
            return "destroy [plugin]\t\tIrrevocably delete the local record of this plugin (does not uninstall)";
        }
    };
    private Command exportCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String pluginName = nextToken(args);
            String zipFileName = nextToken(args);
            if (pluginName == null || zipFileName == null) {
                showUsage(this);
                return;
            }
            String thisVar = nextToken(args);
            PluginSandbox sandbox = name2sandbox.get(pluginName);
            if (sandbox == null) {
                error("plugin not loaded.  download, load, or import first.");
                return;
            }
            try {
                sandbox.writeZip(zipFileName, client, thisVar, build);
                println("Done");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public String help() {
            return "export [plugin] [zipname]\tCreate a zip file from the local manifest (will also extract)";
        }
    };

    private Command helpCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String command = nextToken(args);
            if (command == null || "topics".equals(command)) {
                println("Topics:\n\nhelp commands\t\tList the commands\n" + "help lifecycle\t\tA brief discussion of the plugin lifecycle\n"
                        + "help options\t\tShow the command line usage\n" + "help raw\t\tHow to add blobs and scripts directly to a plugin.\n"
                        + "help topics\t\tshow this list\n" + "help overlay\t\texplain how to include different variants for different environments\n"
                        + "help [command]\t\tShow the usage for a given command\n");
            } else if ("lifecycle".equals(command)) {
                println("A plugin is the name for a versioned collection of Rapture resources.\n"
                        + "The plugin can be stored in three forms: as live data on the server,\n"
                        + "as a zip file, or as a directory structure with plain text files suitable\n" + "for checkin to source control.\n\n"
                        + "This shell creates or opens any number of plugins for editing and can store\n"
                        + "them in any of the three forms.  The commands for opening a plugin are:\n" + "\tcommand\t\tform\n\t-------\t\t----\n"
                        + "\tcreate\t\t(from nothing)\n\tload\t\tfrom saved folder\n\timport\t\tfrom zip file\n" + "\tdownload\tfrom live server content\n\n"
                        + "When you open a plugin only the manifest is kept in memory until you perform\n"
                        + "an operation that requires the contents.  MD5 hashes are kept for each object\n"
                        + "to allow easy verification of whether the objects have been changed.\n\n"
                        + "To change the contents of a plugin, just use the URI of the resource\n"
                        + "you want using the add or remove commands respectively.\n"
                        + "Once you have the manifest the way you want it, you can output the contents.\n" + "\tcommand\t\tform\n\t-------\t\t----\n"
                        + "\tsave\t\toutput to source folder\n" + "\texport\t\toutput to zipfile\n" + "\tinstall\t\toutput to the live server\n");
            } else if ("raw".equals(command)) {
                println("Inside the content directory for a plugin, files with extension .rfx or .bits\n"
                        + "can be inserted to represent a Script or Blob object respectively.  The contents\n"
                        + "of the file will be used as the content of the script or blob verbatim,\n"
                        + "which allows the user to edit these files directly and have the changes\n"
                        + "picked up the next time the plugin is loaded from the directory.\n\n"
                        + "The directory path relative to the content directory will match the URI\n"
                        + "of the corresponding object.  For example, if the file path is\n"
                        + "/pluginStorage/myplugin/content/myScripts/doSomething.rfx, the URI will\n" + "be script://myScripts/doSomething\n\n"
                        + "The script object corresponding to a .rfx file will have the language\n"
                        + "automatically set to REFLEX and the purpose automatically set to PROGRAM.\n\n"
                        + "The blob corresponding to a .bits file will have the mime type set based\n"
                        + "on the file extension.  For example, content/myProject/file.csv.bits would\n"
                        + "result in blob://myProject/file.csv with MIME type text/csv.\n\n"
                        + "To download a script or blob in raw mode to get a .rfx/.bits file instead of\n"
                        + "a .script or .blob file, use the attribute $raw in the URI when making the add\n"
                        + "command and then save or export the plugin normally.\n");
            } else if ("overlay".equals(command)) {
                println("Sometimes you need to install a plugin with minor differences from one environment to another.\n"
                        + "The installed items that are the same for all environments are known as the content of the\n"
                        + "plugin.  The items which are specific to an environment are stored separately.  This separate\n"
                        + "area is known as an 'overlay' and is usually named after the environment to be deployed to.\n\n" + "Example:\n\n"
                        + "The Foo plugin has severals scripts and one config document that the scripts read.  The config\n"
                        + "document for Test, and Production differ in content, but all use the same URI.  The version of\n"
                        + "the document for testing is stored in the 'test' overlay and the version for production is stored\n"
                        + "in the 'prod' overlay. To add to an overlay, use the variant name as the optional last parameter\n" + "to an add command\n\n");
            } else if ("build".equals(command)) {
                println("The build option is used to package a plugin as a single file without further user input. It is equivalent to the commands load followed by export.\n"
                        + "The option requires a name, which indicates a subdirectory in the directory referenced by the savedir option.\n"
                        + "The option can also take a second parameter indicating the target file name. If not present the default value is ./name.zip\n\n");
            } else if ("commands".equals(command)) {
                SortedSet<String> help = Sets.newTreeSet();
                for (Command c : name2command.values()) {
                    help.add(c.help());
                }
                println("Commands:\n");
                for (String s : help) {
                    println(s);
                }
            } else if ("options".equals(command)) {
                showProgramUsage();
            } else {
                Command c = name2command.get(command);
                if (c == null) {
                    error("No such help topic: " + command);
                } else {
                    println(c.help());
                }
            }
        }

        @Override
        public String help() {
            return "help\t\t\t\tShow main help screen\nhelp [command]\t\t\tShow help for the specified command or topic";
        }
    };

    private Command importCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String zipFilename = nextToken(args);
            String pluginName;
            PluginSandbox sandbox;
            ZipFile in = null;
            if (zipFilename == null) {
                showUsage(this);
                return;
            }
            // TODO MEL move this logic into an API call?
            try {
                in = new ZipFile(zipFilename);
                PluginConfig plugin = new PluginUtils().getPluginConfigFromZip(zipFilename);
                if (plugin == null) {
                    return;
                }

                sandbox = new PluginSandbox();
                sandbox.setConfig(plugin);
                sandbox.setStrict(strict);
                pluginName = plugin.getPlugin();
                sandbox.setRootDir(new File(rootDir, pluginName));
                Enumeration<? extends ZipEntry> entries = in.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }

                    sandbox.makeItemFromZipEntry(in, entry);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ex) {
                        // close quietly -- ignore
                    }
                }
            }
            name2sandbox.put(pluginName, sandbox);
            println("Imported " + pluginName);
            println("Done");
        }

        @Override
        public String help() {
            return "import [zipFile]\t\tload a zip file";
        }
    };

    /**
     * SCALING FAULT: This version builds a map with the entire contents and sends it in one API call. The real version will need to replace this with a
     * streaming implementation.
     */
    private Command installCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String pluginName = nextToken(args);
            if (pluginName == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(pluginName);
            if (sandbox == null) {
                error("plugin not present.  Try download, import, or load first.");
                return;
            }
            String thisVariant = nextToken(args);
            if (thisVariant == null) thisVariant = variant;
            Map<String, PluginTransportItem> payload = Maps.newHashMap();
            for (PluginSandboxItem item : sandbox.getItems(thisVariant)) {
                try {
                    if (debug) System.out.println("Packaging " + item.getURI().toString());
                    PluginTransportItem payloadItem = item.makeTransportItem();
                    payload.put(item.getURI().toString(), payloadItem);
                } catch (Exception ex) {
                    error("Not installing " + item.getURI());
                    ex.printStackTrace();
                }
            }
            client.getPlugin().installPlugin(sandbox.makeManifest(thisVariant), payload);
            println("Done.");
        }

        @Override
        public String help() {
            return "install [plugin]\t\tUpload (and overwrite) plugin contents to the server\n"
                    + "install [plugin] [variant]\tUpload (and overwrite) plugin contents to server for variant";
        }
    };

    private Command compareCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            boolean same = true;
            String pluginName = nextToken(args);
            if (pluginName == null) {
                showUsage(this);
                return;
            }
            PluginSandbox sandbox = name2sandbox.get(pluginName);
            if (sandbox == null) {
                error("plugin not present locally.  Try download, import, or load first.");
                return;
            }
            PluginManifest fm = client.getPlugin().getPluginManifest("//" + pluginName);
            if (fm == null) {
                error("plugin not installed on connected server");
            }
            for (PluginManifestItem mItem : fm.getContents()) {
                PluginSandboxItem sItem = sandbox.getItem(new RaptureURI(mItem.getURI(), null));
                if (sItem == null) {
                    println("Absent: " + mItem.getURI());
                } else try {
                    if (sItem.diffWithFile(mItem.getHash(), false)) {
                        println("Changed: " + mItem.getURI());
                        same = false;
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new Error("JRE does not support MD5");
                } catch (IOException e) {
                    println("Unreadable: " + mItem.getURI());
                }
            }
            if (same) println("Local hashes match with manifest.  Use 'verify' to check if server content was altered.");
        }

        @Override
        public String help() {
            return "compare [plugin]\t\tcompare the server manifest to the local contents";
        }
    };

    private Command deflateCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String pluginName = nextToken(args);
            if (pluginName == null) {
                showUsage(this);
                return;
            }
            deflate(pluginName);
        }

        @Override
        public String help() {
            return "deflate [plugin]\t\treduce memory usage by uncaching content";
        }
    };

    private Command verifyCommand = new Command() {
        @Override
        public void execute(Matcher args) {
            String pluginName = nextToken(args);
            if (pluginName == null) {
                showUsage(this);
                return;
            }
            Map<String, String> diff = client.getPlugin().verifyPlugin("//" + pluginName);
            if (diff.isEmpty()) {
                println("No changes from manifest");
            } else for (Map.Entry<String, String> entry : diff.entrySet()) {
                println(entry.getKey() + " " + entry.getValue());
            }
        }

        @Override
        public String help() {
            return "verify [plugin]\t\tcheck that the contents on the server match its manifest";
        }
    };

    private void prettyPrint(PluginManifest c) {
        String versionString = c.getVersion() == null ? "0.0.0" : c.getVersion().toString();
        String descString = c.getDescription() == null ? "<no description>" : c.getDescription();
        println("Name: " + c.getPlugin() + "\tVersion: " + versionString);
        println("\t" + descString);
        for (PluginManifestItem item : c.getContents()) {
            prettyPrint(item);
        }
    }

    private void prettyPrint(PluginManifestItem c) {
        String hash = c.getHash() == null ? "<content not loaded>" : c.getHash();
        println(c.getURI() + "(" + hash + ")");
    }

    private void prettyPrint(PluginConfig c) {
        String versionString = c.getVersion() == null ? "0.0.0" : c.getVersion().toString();
        String descString = c.getDescription() == null ? "<no description>" : c.getDescription();
        println("Name: " + c.getPlugin() + "\tVersion: " + versionString);
        println("\t" + descString);
    }

    // please do not reformat this map -- one per line is much easier to edit
    // and search
    protected Map<String, Command> name2command = ImmutableMap.<String, Command> builder()
            .put("add", addCommand)
            .put("addall", addAllCommand)
            .put("compare", compareCommand)
            .put("create", createSandboxCommand)
            .put("deflate", deflateCommand)
            .put("describe", describeCommand)
            .put("destroy", destroyCommand)
            .put("download", downloadCommand)
            .put("exit", exitCommand)
            .put("export", exportCommand)
            .put("extract", extractCommand)
            .put("help", helpCommand)
            .put("import", importCommand)
            .put("install", installCommand)
            .put("stream", streamInstallCommand)
            .put("installed", listCommand)
            .put("load", loadSandboxCommand)
            .put("opened", listLocalCommand)
            .put("quit", exitCommand)
            .put("remove", removeCommand)
            .put("save", saveSandboxCommand)
            .put("setver", versionCommand)
            .put("uninstall", uninstallCommand)
            .put("use", useCommand)
            .put("validate", validateCommand)
            .put("verify", verifyCommand)
            .build();

    private Pattern tokenizer = Pattern.compile("([^\"]\\S*|\".*?\")\\s*");

    void eval(String line) {
        // TODO MEL replace this with an interactive Reflex shell
        Matcher tokens = tokenizer.matcher(line);
        String verb = nextToken(tokens);
        if (verb == null) {
            return;
        }
        Command c = name2command.get(verb);
        if (c == null) {
            error("Unknown Command: " + verb);
        } else {
            try {
                c.execute(tokens);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String nextToken(Matcher tokens) {
        return tokens.find() ? tokens.group(1) : null;
    }

    protected void error(String msg) {
        hasError = true;
        logger.error(msg);
    }

    protected void error(Exception ex) {
        hasError = true;
        logger.error(ex);
    }

    protected void showUsage(Command c) {
        error("Usage: " + c.help());
    }

    public interface Command {
        void execute(Matcher args);

        String help();
    }

    private void println(String s) {
        logger.info(s);
    }

    public File getImportDir() {
        return importDir;
    }

    public void setImportDir(File importDir) {
        this.importDir = importDir;
    }

    public String getImportOrder() {
        return importOrder;
    }

    public String getLoadOrder() {
        return loadOrder;
    }

    public void setImportOrder(String importOrder) {
        this.importOrder = importOrder;
    }

    public String getImportOverlay() {
        return importOverlay;
    }

    public void setImportOverlay(String importOverlay) {
        this.importOverlay = importOverlay;
    }

    Logger getLogger() {
        return logger;
    }

    public void addDocTree(String variant, PluginSandbox plugin, RaptureURI root) {
        Map<String, RaptureFolderInfo> docs = client.getDoc().listDocsByUriPrefix(root.toString(), 0);
        for (Entry<String, RaptureFolderInfo> entry : docs.entrySet()) {
            if (entry.getValue().isFolder()) continue;
            RaptureURI uri = new RaptureURI(entry.getKey(), Scheme.DOCUMENT);
            plugin.addURI(variant, uri);
        }
    }

    public void addBlobTree(String variant, PluginSandbox plugin, RaptureURI root) {
        Map<String, RaptureFolderInfo> docs = client.getBlob().listBlobsByUriPrefix(root.toString(), 0);
        for (Entry<String, RaptureFolderInfo> entry : docs.entrySet()) {
            if (entry.getValue().isFolder()) continue;
            RaptureURI uri = new RaptureURI(entry.getKey(), Scheme.BLOB);
            plugin.addURI(variant, uri);
        }
    }

    public void addSeriesTree(String variant, PluginSandbox plugin, RaptureURI root) {
        Map<String, RaptureFolderInfo> docs = client.getSeries().listSeriesByUriPrefix(root.toString(), 0);
        for (Entry<String, RaptureFolderInfo> entry : docs.entrySet()) {
            if (entry.getValue().isFolder()) continue;
            RaptureURI uri = new RaptureURI(entry.getKey(), Scheme.SERIES);
            plugin.addURI(variant, uri);
        }
    }

    public File getLoadDir() {
        return loadDir;
    }

    public String getImportInclude() {
        return importInclude;
    }

    public void setImportInclude(String importInclude) {
        this.importInclude = importInclude;
    }

    public String getImportExclude() {
        return importExclude;
    }

    public void setImportExclude(String importExclude) {
        this.importExclude = importExclude;
    }
}
