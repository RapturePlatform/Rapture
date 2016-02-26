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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;

import rapture.api.hooks.ApiHooksService;
import rapture.audit.AuditLog;
import rapture.audit.AuditLogCache;
import rapture.common.CallingContext;
import rapture.common.CallingContextStorage;
import rapture.common.IEntitlementsContext;
import rapture.common.InstallableKernel;
import rapture.common.LicenseInfo;
import rapture.common.RaptureConstants;
import rapture.common.RaptureIPWhiteList;
import rapture.common.RaptureIPWhiteListStorage;
import rapture.common.RaptureRemote;
import rapture.common.RaptureRemoteStorage;
import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.api.NotificationApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.common.hooks.HooksConfig;
import rapture.common.hooks.HooksConfigRepo;
import rapture.common.model.RaptureEntitlement;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.common.model.RaptureEntitlementGroupStorage;
import rapture.common.model.RaptureEntitlementStorage;
import rapture.common.model.RaptureNetwork;
import rapture.common.model.RaptureServerInfo;
import rapture.common.model.RaptureServerStatus;
import rapture.common.model.RaptureServerStatusStorage;
import rapture.common.model.RaptureUser;
import rapture.common.model.RaptureUserStorage;
import rapture.config.ConfigLoader;
import rapture.dsl.entparser.ParseEntitlementPath;
import rapture.event.generator.TimeProcessorThread;
import rapture.exchange.QueueHandler;
import rapture.index.IndexHandler;
import rapture.kernel.cache.KernelCaches;
import rapture.kernel.cache.RepoCacheManager;
import rapture.kernel.internalnotification.ExchangeChangeManager;
import rapture.kernel.internalnotification.TypeChangeManager;
import rapture.kernel.pipeline.KernelTaskHandler;
import rapture.kernel.plugin.RapturePluginClassLoader;
import rapture.kernel.stat.StatHelper;
import rapture.log.management.LogManagerConnection;
import rapture.log.manager.LogManagerConnectionFactory;
import rapture.metrics.MetricsFactory;
import rapture.metrics.MetricsService;
import rapture.notification.MessageNotificationManager;
import rapture.notification.NotificationApiRetriever;
import rapture.notification.NotificationMessage;
import rapture.notification.RaptureMessageListener;
import rapture.object.storage.StorableIndexInfo;
import rapture.repo.Repository;
import rapture.repo.SeriesRepo;
import rapture.script.ScriptFactory;
import rapture.script.reflex.ReflexRaptureScript;
import rapture.util.DefaultConfigRetriever;
import rapture.util.IConfigRetriever;
import rapture.util.IDGenerator;
import rapture.util.ResourceLoader;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;

/**
 * The Rapture kernel is a singleton and hosts the apis for Rapture
 *
 * @author alan
 */
public enum Kernel {
    INSTANCE;

    private static final int CACHELIFETIME = 60000;
    private static final Logger log = Logger.getLogger(Kernel.class);

    public static ActivityApiImplWrapper getActivity() {
        return INSTANCE.activity;
    }

    public static AdminApiImplWrapper getAdmin() {
        return INSTANCE.admin;
    }

    public static AuditApiImplWrapper getAudit() {
        return INSTANCE.audit;
    }

    public static BlobApiImplWrapper getBlob() {
        return INSTANCE.blob;
    }

    public static JarApiImplWrapper getJar() {
        return INSTANCE.jar;
    }

    public static SheetApiImplWrapper getSheet() {
        return INSTANCE.sheet;
    }

    public static BootstrapApiImplWrapper getBootstrap() {
        return INSTANCE.bootstrap;
    }

    public static EntitlementApiImplWrapper getEntitlement() {
        return INSTANCE.entitlement;
    }

    public static EventApiImplWrapper getEvent() {
        return INSTANCE.event;
    }

    public static FieldsApiImplWrapper getFields() {
        return INSTANCE.fields;
    }

    public static IdGenApiImplWrapper getIdGen() {
        return INSTANCE.idgen;
    }

    public static TableApiImplWrapper getTable() {
        return INSTANCE.table;
    }

    public static IndexApiImplWrapper getIndex() {
        return INSTANCE.index;
    }

    public static Kernel getKernel() {
        return INSTANCE;
    }

    public static LockApiImplWrapper getLock() {
        return INSTANCE.lock;
    }

    public static Login getLogin() {
        return INSTANCE.login;
    }

    public static MailboxApiImplWrapper getMailbox() {
        return INSTANCE.mailbox;
    }

    public static ScheduleApiImplWrapper getSchedule() {
        return INSTANCE.schedule;
    }

    public static ScriptApiImplWrapper getScript() {
        return INSTANCE.script;
    }

    public static UserApiImplWrapper getUser() {
        return INSTANCE.user;
    }

    public static PluginApiImplWrapper getPlugin() {
        return INSTANCE.plugin;
    }

    public static PipelineApiImplWrapper getPipeline() {
        return INSTANCE.pipeline;
    }

    public static AsyncApiImplWrapper getAsync() {
        return INSTANCE.async;
    }

    public static SysApiImplWrapper getSys() {
        return INSTANCE.sys;
    }

    public static RunnerApiImplWrapper getRunner() {
        return INSTANCE.runner;
    }

    public static NotificationApiImplWrapper getNotification() {
        return INSTANCE.notification;
    }

    public static SeriesApiImplWrapper getSeries() {
        return INSTANCE.series;
    }

    public static DecisionApiImplWrapper getDecision() {
        return INSTANCE.decision;
    }

    @Deprecated
    public static RepoApiImplWrapper getRepo() {
        return INSTANCE.repo;
    }

    public static DocApiImplWrapper getDoc() {
        return INSTANCE.doc;
    }

    public static RelationshipApiImplWrapper getRelationship() {
        return INSTANCE.relationship;
    }

    public static EnvironmentApiImplWrapper getEnvironment() {
        return INSTANCE.environment;
    }

    public static QuestionApiImplWrapper getQuestion() {
        return INSTANCE.question;
    }

    public static StructuredApiImplWrapper getStructured() {
        return INSTANCE.structured;
    }

    public static MetricsService getMetricsService() {
        return INSTANCE.metricsService;
    }

    public static LogManagerConnection getLogManagerConnection() {
        return INSTANCE.logManagerConnection;
    }

    public static Cache<RaptureURI, Optional<String>> getObjectStorageCache() {
        return INSTANCE.kernelCaches.getObjectStorageCache();
    }

    /**
     * The bootstrap uses an environment variable to boot up the initial repo that is used to define the configs - particularly RaptureConfig (used for
     * users/authorities etc.) and RaptureEphemeral (used for sessions/contexts etc.)
     * <p/>
     * These repositories are installed into the kernel and it is these repositories that are referred to or passed down to the RaptureKernel instances
     *
     * @
     */

    public static void initBootstrap() {
        // Ensure config is loaded
        initBootstrap(null, null, false);
    }

    public static void initBootstrap(Map<String, String> templates, Object context, boolean startScheduler) {
         // AT THIS POINT, attempt to load and run a startup script
        // TODO: These strings need to be in a constant
        if (templates != null) {
            for (Map.Entry<String, String> e : templates.entrySet()) {
                log.debug("Adding passed template for " + e.getKey() + " with value " + e.getValue());
                getAdmin().addTemplate(ContextFactory.getKernelUser(), e.getKey(), e.getValue(), true);
            }
        }
        // Ensure we have a machine ID
        setupMachineID();
        // Ensure network setup
        setupNetworkInfo();

        INSTANCE.loadStartupScript(INSTANCE, "/coreStartup/");
        if (context != null) {
            INSTANCE.loadStartupScript(context, "/startup/");
        }
        INSTANCE.initStat();
        INSTANCE.startMonitor();
        DefaultEntitlementCreator.ensureEntitlementSetup(getEntitlement());

        if (getEnvironment().getApplianceMode(ContextFactory.getKernelUser())) {
            startApplianceMode();
        }

        // populate an initial server status
        setupServerStatus();
    }

    /**
     * TODO For appliance mode if we don't have a TimeServer do we need to start the ScheduleServer?
     */
    private static TimeProcessorThread tThread;

    private static void startApplianceMode() {
        // INSTANCE.startScheduler();

        log.info("Starting time processor thread");
        tThread = new TimeProcessorThread();
        tThread.start();
    }

    private static void setupMachineID() {
        RaptureServerInfo info = INSTANCE.environment.getThisServer(ContextFactory.getKernelUser());
        if (info == null) {
            log.info("This server does not have a name, setting one up");
            info = new RaptureServerInfo();
            info.setName("Rapture");
            info.setServerId(IDGenerator.getUUID());
            log.info("Saving server ID " + info.getServerId());
            INSTANCE.environment.setThisServer(ContextFactory.getKernelUser(), info);
        } else {
            log.info(String.format("This server is %s ( %s )", info.getServerId(), info.getName()));
        }
    }

    private static void setupNetworkInfo() {
        RaptureNetwork network = INSTANCE.environment.getNetworkInfo(ContextFactory.getKernelUser());
        if (network == null) {
            log.info("No Rapture Network found, creating one");
            network = new RaptureNetwork();
            network.setNetworkId(IDGenerator.getUUID());
            network.setNetworkName("Rapture");
            log.info("Saving network as " + network.getNetworkId());
            INSTANCE.environment.setNetworkInfo(ContextFactory.getKernelUser(), network);
        } else {
            log.info(String.format("This environment is part of Network %s ( %s )", network.getNetworkId(), network.getNetworkName()));
        }
    }

    private static void setupServerStatus() {
        RaptureServerStatus status = new RaptureServerStatus();
        status.setServerId(INSTANCE.environment.getThisServer(ContextFactory.getKernelUser()).getServerId());
        status.setStatusMessage(String.format("Server with serverId [%s] has started", status.getServerId()));
        RaptureServerStatusStorage.add(status, ContextFactory.getKernelUser().getUser(), "");
    }

    public static void shutdown() {
        INSTANCE.stopRapture();
    }

    private MessageNotificationManager notificationManager;
    private TypeChangeManager typeChangeManager;
    private ExchangeChangeManager exchangeChangeManager;
    private ContextStackContainer stackContainer = new ContextStackContainer();
    private RepoCacheManager repoCacheManager;

    public static ContextStackContainer getStackContainer() {
        return INSTANCE.stackContainer;
    }

    public static TypeChangeManager getTypeChangeManager() {
        return INSTANCE.typeChangeManager;
    }

    public static RepoCacheManager getRepoCacheManager() {
        return INSTANCE.repoCacheManager;
    }

    public static void typeChanged(RaptureURI uri) {
        INSTANCE.repoCacheManager.removeRepo(uri.getAuthority());
    }

    public static void exchangeChanged(String exchange) {
        if (INSTANCE.exchangeChangeManager != null) {
            INSTANCE.exchangeChangeManager.setExchangeChanged(exchange);
        }

        // fix instantly in this instance
        INSTANCE.pipeline.getTrusted().handleExchangeChanged(exchange);
    }

    private Map<String, RaptureMessageListener<NotificationMessage>> delayListen = new HashMap<String, RaptureMessageListener<NotificationMessage>>();

    /*
     * Because typeChangeManager might not be available yet
     */
    public synchronized void registerTypeListener(String typeName, RaptureMessageListener<NotificationMessage> listener) {
        if (typeChangeManager != null) {
            typeChangeManager.registerTypeListener(typeName, listener);
        } else {
            delayListen.put(typeName, listener);
        }
    }

    private synchronized void startMonitor() {
        // Start the monitor for changes to the environment
        // This is through an notification manager called "kernel"
        // which will have been setup in the startup script if it
        // isn't already present
        log.debug("Kernel notification manager starting");
        // there should only be one notification manager per kernel
        // Maybe calling this twice should be an error then?
        if (notificationManager != null) return;

        notificationManager = new MessageNotificationManager(new NotificationApiRetriever() {

            @Override
            public NotificationApi getNotification() {
                return notification;
            }

            @Override
            public CallingContext getCallingContext() {
                return ContextFactory.getKernelUser();
            }

        }, "//system/kernel");

        notificationManager.startNotificationManager();
        log.debug("Kernel notification manager started");
        typeChangeManager = new TypeChangeManager(notificationManager);
        if ((delayListen != null) && !delayListen.isEmpty()) {
            for (String s : delayListen.keySet())
                typeChangeManager.registerTypeListener(s, delayListen.get(s));
        }
        delayListen = null; // we don't need it any more
        exchangeChangeManager = new ExchangeChangeManager(notificationManager);
        exchangeChangeManager.registerExchangeListener(Kernel.INSTANCE.pipeline.getTrusted());
    }

    private static LicenseInfo licenseInfo;

    public static LicenseInfo getLicenseInfo() {
        return licenseInfo;
    }


    /**
     * Check the config for a passed api user to run the kernel as. If so, check that it is in fact a valid API user, and if it is, set the KernelUser in the
     * ContextFactory to this user.
     */
    public void validateKernelUser() {
        if (!ConfigLoader.getConf().KernelUser.isEmpty()) {
            log.debug("Retrieving information for kernel api user passed - " + ConfigLoader.getConf().KernelUser);
            RaptureUser rootUser = getUserViaName(ConfigLoader.getConf().KernelUser);
            if (rootUser != null && rootUser.getHasRoot()) {
                log.debug("User is has root, using this user for kernel activity for this process");
                ContextFactory.setKernelUser(rootUser.getUsername(), IDGenerator.getUUID());
            } else {
                log.error("User is not found or does not have root permissions - will not use this user for kernel activity");
            }
        }
    }

    private IConfigRetriever config = new DefaultConfigRetriever();
    private KernelTaskHandler taskHandler;
    private IndexCache indexCache;
    private AuditLogCache auditLogCache;
    private boolean bypassWhiteList = false;
    private String appStyle = "webapp";
    private String appId = "1";

    /**
     * This is the stat api instance for this server
     */
    private StatHelper stat;

    private Login login;

    private ApiHooksService apiHooksService;

    private static RapturePluginClassLoader rapturePluginClassLoader;

    /**
     * The Api Wrappers
     */
    private UserApiImplWrapper user;
    private ActivityApiImplWrapper activity;
    private AdminApiImplWrapper admin;
    private TableApiImplWrapper table;
    private IndexApiImplWrapper index;
    private BootstrapApiImplWrapper bootstrap;
    private ScriptApiImplWrapper script;
    private EntitlementApiImplWrapper entitlement;
    private IdGenApiImplWrapper idgen;
    private ScheduleApiImplWrapper schedule;
    private LockApiImplWrapper lock;
    private EventApiImplWrapper event;
    private AuditApiImplWrapper audit;
    private MailboxApiImplWrapper mailbox;
    private FieldsApiImplWrapper fields;
    private JarApiImplWrapper jar;
    private BlobApiImplWrapper blob;

    private SheetApiImplWrapper sheet;

    private PluginApiImplWrapper plugin;
    private PipelineApiImplWrapper pipeline;
    private AsyncApiImplWrapper async;
    private SysApiImplWrapper sys;
    private RunnerApiImplWrapper runner;
    private NotificationApiImplWrapper notification;
    private SeriesApiImplWrapper series;
    private DecisionApiImplWrapper decision;
    @Deprecated
    private RepoApiImplWrapper repo;
    private DocApiImplWrapper doc;
    private RelationshipApiImplWrapper relationship;
    private EnvironmentApiImplWrapper environment;
    private QuestionApiImplWrapper question;
    private StructuredApiImplWrapper structured;
    private MetricsService metricsService = MetricsFactory.createDummyService(); // initialize to a dummy service initially, as this is not nullable
    private LogManagerConnection logManagerConnection;

    private KernelCaches kernelCaches = new KernelCaches();

    private static boolean up = true;

    public static boolean isUp() {
        return up;
    }

    /**
     * Run whatever additional scripts are available through this context
     *
     * @param path
     *            @
     */
    public static void runAdditional(Object context, String path) {
        INSTANCE.loadStartupScript(context, path);
    }

    public static void writeAuditEntry(String category, int level, String message) {
        try {
            getAudit().writeAuditEntry(ContextFactory.getKernelUser(), RaptureConstants.DEFAULT_AUDIT_URI, category, level, message);
        } catch (RaptureException e) {
            log.error("Could not write kernel audit entry - " + e.getMessage());
        }
    }

    public static void writeComment(String message) {
        try {
            AuditApiImpl audit = Kernel.getAudit().getTrusted();
            audit.writeAuditEntry(ContextFactory.getKernelUser(), RaptureConstants.DEFAULT_AUDIT_URI, "kernel", 0, message);
        } catch (RaptureException e) {
            log.error("Could not write kernel commentary entry - " + e.getMessage());
        }
    }

    static {
        INSTANCE.restart();
        up = true;
    }

    /**
     * Here we need to look for the ipaddresswhitelist. If present, check this address against it If not present, everything goes. A separate parameter
     * "never check" in the kernel can change this behaviour - that will be set on startup.
     *
     * @param remoteAddr
     * @return
     */
    public boolean checkIPAddress(String remoteAddr) {
        if (isBypassWhiteList()) {
            return true;
        }
        try {
            RaptureIPWhiteList wlist = RaptureIPWhiteListStorage.readByFields();
            if (wlist != null && !wlist.getIpWhiteList().isEmpty()) {
                return wlist.getIpWhiteList().contains(remoteAddr);
            }
            return true;
        } catch (RaptureException e) {
            log.error("Error retrieveing white list");
        }

        return false;
    }

    // TODO Note that this does not delete the existing repositories.
    public void clearRepoCache(boolean recreateUsers) {
        try {
            restart();
            if (recreateUsers) {
                log.debug("Setting up default users");
                repoCacheManager.createDefaultUsers();
            }
        } catch (RaptureException e) {
            log.error("Could not restart the repo cache");
        }

    }

    public String getAppId() {
        return appId;
    }

    public String getAppStyle() {
        return appStyle;
    }

    public IConfigRetriever getConfig() {
        return config;
    }

    private RaptureEntitlement getEnt(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return RaptureEntitlementStorage.readByFields(path);

    }

    private RaptureEntitlementGroup getEntGroup(String path) {
        return RaptureEntitlementGroupStorage.readByFields(path);
    }

    public IndexHandler getIndex(String name) {
        return indexCache.getIndex(name);
    }

    public AuditLog getLog(CallingContext context, RaptureURI logURI) {
        return auditLogCache.getAuditLog(context, logURI);
    }

    public void resetLogCache(RaptureURI logURI) {
        auditLogCache.reset(logURI);
    }

    public RaptureRemote getRemote(String remoteId) {
        return RaptureRemoteStorage.readByFields(remoteId);
    }

    // get sys or document repo, for historical reasons
    public Repository getRepo(String name) {
        return repoCacheManager.getRepo(name);
    }

    public Optional<Repository> getStorableRepo(String name, StorableIndexInfo indexInfo) {
        return repoCacheManager.getStorableRepo(name, indexInfo);
    }

    public StatHelper getStat() {
        // Normally initialised by InitBootstrap, but that doesn't get called
        // for local instances.
        // Not really needed, but the caller doesn't expect a null return, so
        // create a dummy instance.
        if (stat == null) initStat();
        return stat;
    }

    private RaptureUser getUserViaCache(CallingContext context) {
        return getUserViaName(context.getUser());
    }

    private RaptureUser getUserViaName(String name) {
        return RaptureUserStorage.readByFields(name);
    }

    /**
     * Find the stat config and set up stat, then setup standard stat fields @
     */
    private void initStat() {
        stat = new StatHelper(repoCacheManager.getRepo(RaptureConstants.SETTINGS_REPO));
    }

    private boolean isAdminContext(CallingContext context) {
        return context.getUser().equals(ContextFactory.getKernelUser().getUser());
    }

    public boolean isBypassWhiteList() {
        return bypassWhiteList;
    }

    private boolean isRootUser(RaptureUser user) {
        return user != null && user.getHasRoot();
    }

    private boolean isValidEntitlementPath(String entitlementPath) {
        return entitlementPath != null && !entitlementPath.isEmpty();
    }

    private void loadStartupScript(Object context, String scriptPath) {
        // Find a script in a location, then run it
        // start with the resource "autoexec.xxx"

        if (scriptPath == null) {
            scriptPath = "/startup/";
        }
        log.debug("Loading scripts in " + scriptPath);
        List<String> scripts = ResourceLoader.getScripts(context, scriptPath);
        if (scripts != null) {
            for (String scriptName : scripts) {
                log.debug("Loading script at " + scriptPath + scriptName);
                String scriptBody = ResourceLoader.getResourceAsString(context, scriptPath + scriptName);
                if (!scriptBody.isEmpty()) {
                    try {
                        RaptureScript script = new RaptureScript();
                        script.setName(scriptPath + scriptName);
                        script.setScript(scriptBody);
                        ReflexRaptureScript reflex = new ReflexRaptureScript();
                        reflex.runProgram(ContextFactory.getKernelUser(), null, script, new HashMap<String, Object>());
                        log.info("Script " + scriptPath + scriptName + " ran successfully");
                    } catch (RaptureException e) {
                        log.error("Failed to run Script " + scriptPath + scriptName + ": " + e.getMessage());
                    }
                }
            }
        }
        log.debug("Finished running scripts in " + scriptPath);
    }

    private List<KernelApi> kernelApis;

    private List<KernelBase> apiBases;

    public void restart() {
        try {
            log.debug("Restarting Rapture");
            ScriptFactory.init();

            /*
             * Initialize the caches
             */
            repoCacheManager = new RepoCacheManager();
            indexCache = new IndexCache();
            auditLogCache = new AuditLogCache();

            apiBases = new ArrayList<KernelBase>();
            login = new Login(this);
            apiBases.add(login);

            apiHooksService = new ApiHooksService();

            /*
             * Create the API wrappers
             */
            kernelApis = new LinkedList<KernelApi>();
            activity = new ActivityApiImplWrapper(this);
            kernelApis.add(activity);
            admin = new AdminApiImplWrapper(this);
            kernelApis.add(admin);
            table = new TableApiImplWrapper(this);
            kernelApis.add(table);
            index = new IndexApiImplWrapper(this);
            kernelApis.add(index);
            script = new ScriptApiImplWrapper(this);
            kernelApis.add(script);
            bootstrap = new BootstrapApiImplWrapper(this);
            kernelApis.add(bootstrap);
            entitlement = new EntitlementApiImplWrapper(this);
            kernelApis.add(entitlement);
            idgen = new IdGenApiImplWrapper(this);
            kernelApis.add(idgen);
            schedule = new ScheduleApiImplWrapper(this);
            kernelApis.add(schedule);
            lock = new LockApiImplWrapper(this);
            kernelApis.add(lock);
            event = new EventApiImplWrapper(this);
            kernelApis.add(event);
            audit = new AuditApiImplWrapper(this);
            kernelApis.add(audit);
            mailbox = new MailboxApiImplWrapper(this);
            kernelApis.add(mailbox);
            fields = new FieldsApiImplWrapper(this);
            kernelApis.add(fields);
            blob = new BlobApiImplWrapper(this);
            kernelApis.add(blob);
            jar = new JarApiImplWrapper(this);
            kernelApis.add(jar);
            sheet = new SheetApiImplWrapper(this);
            kernelApis.add(sheet);
            plugin = new PluginApiImplWrapper(this);
            kernelApis.add(plugin);
            pipeline = new PipelineApiImplWrapper(this);
            kernelApis.add(pipeline);
            async = new AsyncApiImplWrapper(this);
            kernelApis.add(async);
            runner = new RunnerApiImplWrapper(this);
            kernelApis.add(runner);
            notification = new NotificationApiImplWrapper(this);
            kernelApis.add(notification);
            series = new SeriesApiImplWrapper(this);
            kernelApis.add(series);
            decision = new DecisionApiImplWrapper(this);
            kernelApis.add(decision);
            user = new UserApiImplWrapper(this);
            kernelApis.add(user);
            repo = new RepoApiImplWrapper(this);
            kernelApis.add(repo);
            doc = new DocApiImplWrapper(this);
            kernelApis.add(doc);
            relationship = new RelationshipApiImplWrapper(this);
            kernelApis.add(relationship);
            environment = new EnvironmentApiImplWrapper(this);
            kernelApis.add(environment);
            question = new QuestionApiImplWrapper(this);
            kernelApis.add(question);
            structured = new StructuredApiImplWrapper(this);
            kernelApis.add(structured);

            // sys depends on series and doc
            sys = new SysApiImplWrapper(this);
            kernelApis.add(sys);

            validateKernelUser();
            coordinatedBegin();

            HooksConfig hooksConfig = HooksConfigRepo.INSTANCE.loadHooksConfig();
            apiHooksService.configure(hooksConfig);

            rapturePluginClassLoader = new RapturePluginClassLoader();
            taskHandler = new KernelTaskHandler(pipeline.getTrusted());

            if (metricsService != null) {
                metricsService.stop();
            }
            metricsService = MetricsFactory.createDefaultService();

            if (logManagerConnection != null) {
                logManagerConnection.close();
            }
            logManagerConnection = LogManagerConnectionFactory.createDefaultConnection();
            logManagerConnection.connect();

            kernelCaches = new KernelCaches();

            writeComment("Instance started");

        } catch (RaptureException e) {
            log.error("Cannot start Rapture " + e.getMessage());
        }
    }

    private void coordinatedBegin() {
        for (KernelBase base : apiBases) {
            base.start();
        }
        /*
         * Start up all Kernel APIs
         */
        for (KernelApi api : kernelApis) {
            api.start();
        }
    }

    public void setAdmin(AdminApiImplWrapper admin) {
        this.admin = admin;
    }

    public void setAppId(String appId) {
        if (appId != null) {
            log.debug("Application id is " + appId);
            this.appId = appId;
        }
    }

    public void setAppStyle(String appStyle) {
        log.info("AppStyle is " + appStyle);
        this.appStyle = appStyle;
    }

    public void setBypassWhiteList(boolean bypassWhiteList) {
        this.bypassWhiteList = bypassWhiteList;
    }

    public void setConfig(IConfigRetriever config) {
        this.config = config;
    }

    public void stopRapture() {
        notificationManager.stopNotificationManager();
        repoCacheManager.resetAllCache();
    }

    public CallingContext loadContext(String contextId) {
        return CallingContextStorage.readByFields(contextId);
    }

    public void validateContext(CallingContext context, String entitlementPath, IEntitlementsContext entCtx) throws RaptureException, RaptNotLoggedInException {
        // Check to see if the user is an apiKey (it will have a prefix of zz )

        // Also, if the user is valid we can check the entitlementPath and see
        // if the user is a member of a group
        // that is associated with the entitlement path given

        if (isAdminContext(context)) return;
        RaptureUser user = getUserViaCache(context);
        if (isRootUser(user)) return;

        // If the entitlementPath is not null and length > 0
        // Validate the entitlements, using that path, the RaptureCallingContext
        // and the RaptureEntitlementsContext
        if (isValidEntitlementPath(entitlementPath)) {
            validateEntitlements(context, entitlementPath, entCtx);
        }
    }

    private static Map<String, DynamicEntitlementGroup> classCache = new HashMap<String, DynamicEntitlementGroup>();

    private void validateEntitlements(CallingContext context, String entitlementPath, IEntitlementsContext entCtx) {
        if (context == null) throw RaptureExceptionFactory.create("Null calling context in security validation");
        // Convert the entitlement path to a context specific path
        String realEntPath = ParseEntitlementPath.getEntPath(entitlementPath, entCtx);
        // Now find an appropriate entitlement document for this path
        RaptureEntitlement rEnt = null;
        while (rEnt == null) {
            rEnt = getEnt(realEntPath);
            if (rEnt == null) {
                int lastIndexPoint = realEntPath.lastIndexOf('/');
                if (lastIndexPoint != -1) {
                    realEntPath = realEntPath.substring(0, lastIndexPoint);
                } else {
                    break;
                }
            }
        }
        if (rEnt != null) {
            // The rEnt will be associated with groups, and the current user
            // may be in one of those groups
            boolean valid = false;
            if (rEnt.getGroups().isEmpty()) {
                // No groups defined == all groups
                valid = true;
            } else {
                for (String group : rEnt.getGroups()) {
                    RaptureEntitlementGroup rGrp = getEntGroup(group);

                    if (rGrp == null) continue;

                    // If your name is on the list, you're in.
                    if (rGrp.getUsers() != null && rGrp.getUsers().contains(context.getUser())) {
                        valid = true;
                        break;
                    }

                    // Is this a Dynamic Entitlement Group?
                    String className = rGrp.getDynamicEntitlementClassName();
                    if ((className != null) && !className.isEmpty()) {
                        try {
                            log.debug("Found a dynamic entitlement group : " + className);
                            DynamicEntitlementGroup dynamicGroupInstance = classCache.get(className);
                            if (dynamicGroupInstance == null) {
                                Class<?> dynamicGroup = Class.forName(className);
                                dynamicGroupInstance = (DynamicEntitlementGroup) dynamicGroup.newInstance();
                                classCache.put(className, dynamicGroupInstance);
                            }
                            valid = dynamicGroupInstance.isEntitled(context, context.getUser());
                            log.debug(context.getUser() + " isEntitled " + valid);
                            break;
                        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                            log.error("Cannot create instance of " + className + ": " + e.getMessage());
                            log.debug(ExceptionToString.format(e));
                        }
                    }
                }
            }
            if (!valid) {
                // HTTP code 401 UNAUTHORIZED means you aren't logged in.
                // 403 FORBIDDEN means you're logged in but can't do that
                RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_FORBIDDEN,
                        String.format("User %s not authorized for that operation", context.getUser()));
                log.info(RaptureExceptionFormatter.getExceptionMessage(raptException,
                        String.format("User %s not in any group associated with entitlement %s", context.getUser(), realEntPath)));
                throw raptException;
            }
        }
    }

    /**
     * Register this server to handle messages on exchanges associated with the given category
     *
     * @param category
     */
    public static void setCategoryMembership(String category) {
        INSTANCE.taskHandler.setCategoryMembership(category);
    }

    /**
     * Register this server to handle messages on exchanges associated with the given category. This differs from {@link #setCategoryMembership(String)} in what
     * Queue Handlers it allows. This method allows defining custom handlers, which will override all default handlers. If you wish to handle some custom mime
     * types or use custom handlers *in addition* to the default handlers, you currently need to pass in all the default handlers. It may be worth writing a
     * method that makes this easier.
     *
     * @param category
     * @param customHandlers
     */
    public static void setCategoryMembership(String category, Map<String, QueueHandler> customHandlers) {
        INSTANCE.taskHandler.setCategoryMembership(category, customHandlers);
    }

    public SeriesRepo getSeriesRepo(RaptureURI seriesURI) {
        return repoCacheManager.getSeriesRepo(seriesURI.getAuthority());
    }

    /**
     * Return the {@link ApiHooksService} object that should be used within Rapture
     *
     * @return
     */
    public static ApiHooksService getApiHooksService() {
        return INSTANCE.apiHooksService;
    }

    /**
     * Return the RapturePluginClassLoader that should be used for finding invocables.
     */
    public static RapturePluginClassLoader getRapturePluginClassLoader() {
        return rapturePluginClassLoader;
    }

    private Map<String, InstallableKernel> iKernels = new HashMap<String, InstallableKernel>();

    public static void addInstallableKernel(InstallableKernel iKernel) {
        INSTANCE.iKernels.put(iKernel.getName(), iKernel);
        iKernel.restart();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstalledKernel(String name) {
        InstallableKernel iKernel = INSTANCE.iKernels.get(name);
        if (iKernel != null) {
            try {
                return (T) iKernel;
            } catch (Exception e) {
                log.error(String.format("Error casting installed kernel with id '%s' to %s: %s", name, iKernel.getClass(), ExceptionToString.format(e)));
                return null;
            }
        } else {
            return null;
        }
    }

    public static Collection<InstallableKernel> getInstalledKernels() {
        return Collections.unmodifiableCollection(INSTANCE.iKernels.values());
    }

    public static String versions() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<URL> manifests = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (manifests.hasMoreElements()) {
                URL url = manifests.nextElement();
                String s = url.toString().replace(".jar!/META-INF/MANIFEST.MF", "");
                s = s.substring(s.lastIndexOf('/'));
                if (s.contains("Rapture")) {
                    InputStream is = url.openStream();
                    if (is != null) {
                        Manifest manifest = new Manifest(is);
                        Attributes mainAttribs = manifest.getMainAttributes();
                        String title = (mainAttribs.getValue("Implementation-Title") + "                                ").substring(0, 32);
                        String version = mainAttribs.getValue("Implementation-Version");
                        String date = mainAttribs.getValue("Built-Date");
                        sb.append(title).append(version).append(" ").append(date).append("\n");
                    }
                }
            }
        } catch (IOException ioe) {
            sb.append("Error getting version info " + ioe.getLocalizedMessage());
        }
        return (sb.toString());
    }

}
