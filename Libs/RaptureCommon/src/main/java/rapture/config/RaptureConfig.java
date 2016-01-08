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
package rapture.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.util.ConfigRetriever;

/**
 * This is the class that represents the config of Rapture, as represented by a yaml file "rapture.cfg"
 * 
 * @author amkimian
 * 
 */
public class RaptureConfig {
    private static boolean loadYaml = true;
    private static Logger log = Logger.getLogger(RaptureConfig.class);

    public static boolean getLoadYaml() {
        return loadYaml;
    }

    public static void setLoadYaml(boolean load) {
        loadYaml = load;
    }

    public String KernelUser = "raptureApi";
    public String CacheExpiry = "";
    public String RaptureRepo = "REP {} USING MEMORY {}";
    public String ServerType = "webserver";
    public String StandardTemplate = "REP {} USING MEMORY {}";
    public String DefaultAudit = "LOG {} USING MEMORY {}";
    public String DefaultExchange = "EXCHANGE {} USING MEMORY {}";
    public String DefaultCommentary = "COMMENTARY {} USING MEMORY {}";
    public String DefaultNotification = "NOTIFICATION USING MEMORY {}";
    public String DefaultStatus = "STATUS {} USING MEMORY {}";
    public String DefaultPipelineTaskStatus = "TABLE {} USING MEMORY {}";
    public String Categories = "alpha";
    public Boolean WorkflowOnPipeline = false;
    public Boolean InitConfig = false;
    public String InitSysConfig = "NREP {} USING MEMORY { prefix=\"sys.config\"}";
    public String InitSysEphemeral = "REP {} USING MEMORY { prefix=\"sys.emphemeral\"}";
    public String InitSysSettings = "NREP {} USING MEMORY { prefix=\"sys.settings\"}";

    public String DefaultDecisionRepoConfig = "NREP {} USING MEMORY { prefix=\"sys.decision\"}";

    public String DefaultDecisionIdGenConfig = "IDGEN { base=\"36\", length=\"8\", prefix=\"wo.\" } USING MEMORY {}";
    public String EventIdGenConfig = "IDGEN { base=\"36\", length=\"8\", prefix=\"mem-event.\" } USING MEMORY {}";
    public String ActivityIdGenConfig = "IDGEN { base=\"36\", length=\"8\", prefix=\"\" } USING MEMORY {}";

    public String DefaultSemaphoreConfig = "LOCKING USING MEMORY {}";
    public String DefaultKernelLockConfig = "LOCKING USING MEMORY {}";
    public String DefaultWorkflowLockConfig = "LOCKING USING MEMORY {}";
    public String DefaultSystemBlobConfig = "BLOB {} USING MEMORY {}";
    public String DefaultSystemBlobFoldersConfig = "REP {} USING MEMORY {}";
    public String DefaultWorkflowAuditLog = "LOG {} USING NOTHING { blobRepo = \"sys.blob\"}";
    public String DefaultPerfAuditLog = "LOG {} USING NOTHING { blobRepo = \"sys.blob\"}";
    public Boolean SystemBlobConfigOn = true;
    public String ServerGroup = "localRun";
    public String AppInstance = "localRun";
    public String AppName = "unknown";
    public String DefaultApiHooks = "{\"idToHook\":{\"AuditHook\":{\"id\":\"AuditHook\",\"className\":\"rapture.api.hooks.impl.AuditApiHook\",\"hookTypes\":[\"PRE\"]}}}";
    public String IsOverlaid = "false";
    public Boolean RelationshipSystemActive = false;
    public Boolean AllowAutoRepoCreation = true;
    public Boolean PerformanceSystemActive = false;
    public String FileRepoDirectory = "";
    public String JarStorage = "FILE";

    public String DefaultPythonLocation = "/usr/bin/python";
    public String DefaultAnacondaPythonLocation = "/opt/anaconda/bin/python";
    public String DefaultAnacondaRoot = "/opt/anaconda/envs/";

    public FolderCleanupConfig folderCleanup = new FolderCleanupConfig();

    public WebConfig web = new WebConfig();
    public WebConfig execWeb = new WebConfig("8667");
    public Boolean Configured = false;

    public void applyOverrides() {
        // Apply overrides to the public fields by looking for properties or env
        // variables that
        // are formed by RAPTURE_[fieldName in uppercase]
        for (Field f : RaptureConfig.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers())) {
                String attrName = "RAPTURE_" + f.getName().toUpperCase();
                String val = ConfigRetriever.getSetting(null, attrName, null);
                if (val != null) {
                    log.info("Overriding " + f.getName() + " to " + val);
                    if (f.getType().equals(String.class)) {
                        try {
                            f.set(this, val);
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading config", e);
                        }
                    } else if (f.getType().equals(Boolean.class)) {
                        try {
                            f.set(this, Boolean.valueOf(val));
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading config", e);
                        }
                    }
                }
            }
        }

        ensureValidFileRepoDirectory();
    }

    private void ensureValidFileRepoDirectory() {
        String baseDir = FileRepoDirectory;
        String userHome = System.getProperty("user.home").replace('\\', '/'); // We don't allow backslashes, and Java allows slashes with Windows.

        if (baseDir.contains("\\")) {
            throw RaptureExceptionFactory.create("FileRepoDirectory cannot contain backslashes (\\). Please replace with slashes (/).");
        }

        if ( baseDir == null || baseDir.isEmpty()) {
            baseDir = userHome + "/RaptureFileRepositories/";
        }
        else {
            if (baseDir.startsWith("~")) {
                if (baseDir.startsWith("~/")) {
                    baseDir = baseDir.replaceFirst("^~", userHome);
                }
                else {
                    throw RaptureExceptionFactory.create("Cannot expand ~ for other users in FileRepoDirectory configuration.");
                }
            }

            if (!Paths.get(baseDir).isAbsolute()) {
                throw RaptureExceptionFactory.create("Please specify an absolute path for FileRepoDirectory configuration.");
            }

            if (!baseDir.endsWith("/")) {
                baseDir += "/";
            }
        }

        if (baseDir != FileRepoDirectory) {
            log.info("Overriding FileRepoDirectory from " + FileRepoDirectory + " to " + baseDir);
            FileRepoDirectory = baseDir;
        }
    }
}
