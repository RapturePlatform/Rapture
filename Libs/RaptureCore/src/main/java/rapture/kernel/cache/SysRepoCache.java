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
package rapture.kernel.cache;

import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.model.RaptureUser;
import rapture.common.model.RepoConfig;
import rapture.common.model.RepoConfigStorage;
import rapture.repo.RepoFactory;
import rapture.repo.Repository;

import org.apache.log4j.Logger;

import rapture.config.ConfigLoader;

/**
 * Created by yanwang on 6/23/14.
 */
public class SysRepoCache extends AbstractStorableRepoCache<RepoConfig> {

    public static final String SYS_PREFIX = "sys";

    private static final Logger log = Logger.getLogger(SysRepoCache.class);

    public SysRepoCache() {
        super(SYS_PREFIX);

        // Startup the bootstrap cache
        // Look for an environment variable called "RAPTURE_REPO"
        // If not exist, use "VREP {} using MEMORY {}"
        String bootstrapConfig = ConfigLoader.getConf().RaptureRepo;
        log.info("Bootstrap config is " + bootstrapConfig);
        Repository repository = RepoFactory.getRepo(bootstrapConfig);
        if (repository != null) {
            // Check that the bootstrap repo has the document "$bootstrap".
            // If it does not we need to initialize the repo and set that document
            RepoConfig config = new RepoConfig();
            config.setName(RaptureConstants.BOOTSTRAP_REPO);
            config.setConfig(bootstrapConfig);
            addRepo(RaptureConstants.BOOTSTRAP_REPO, config, repository);

            if (null == repository.getDocument("$bootstrap")) {
                log.info("No bootstrap document found, creating default config");
                createInitialConfig();
                repository.addDocument("$bootstrap", "{}", "admin", "Bootstrap", false);
            } else {
                log.debug("Found bootstrap document, continuing");
            }
        } else {
            log.error(String.format("Bootstrap repository is null for config [%s]", bootstrapConfig));
        }
    }

    /**
     * Using the sys.bootstrap, create some default entries for RaptureConfig
     * and RaptureEphemeral and then generate some default entries in the config
     * for initial users and authorities.
     */
    private void createInitialConfig() {
        // Add configs for RaptureConfig and RaptureEmphemeral
        Repository bootstrapRepo = getRepo(RaptureConstants.BOOTSTRAP_REPO);
        try {
            bootstrapRepo.createStage(RaptureConstants.OFFICIAL_STAGE);

            String sysConfig = ConfigLoader.getConf().InitSysConfig;
            log.info("Config Repository is " + sysConfig);
            addRepoToStage(bootstrapRepo, "RaptureConfig", ConfigLoader.getConf().InitSysConfig);
            addRepoToStage(bootstrapRepo, "RaptureEphemeral", ConfigLoader.getConf().InitSysEphemeral);
            addRepoToStage(bootstrapRepo, "RaptureSettings", ConfigLoader.getConf().InitSysSettings);

            bootstrapRepo.commitStage(RaptureConstants.OFFICIAL_STAGE, "internal", "Initial config");
            createDefaultUsers();
        } catch (Exception e) {
            log.error("Error creating initial config", e);
        }
    }

    private void addRepoToStage(Repository bootstrapRepo, String name, String configStr) {
        RepoConfig config = new RepoConfig();
        config.setName(name);
        config.setConfig(configStr);

        RaptureURI storageLocation = config.getStorageLocation();
        String jsonString = JacksonUtil.jsonFromObject(config);
        bootstrapRepo.addToStage(RaptureConstants.OFFICIAL_STAGE, storageLocation.getDocPath(), jsonString, false);

        Repository newRepo = RepoFactory.getRepo(configStr);
        addRepo(config.getStoragePath(), config, newRepo);
    }

    public void createDefaultUsers() {
        log.debug("Creating default users as non exist");
        RaptureUser defUser = new RaptureUser();
        defUser.setHashPassword(MD5Utils.hash16("rapture"));
        defUser.setUsername("rapture");
        addUser(defUser);


        RaptureUser defApiUser = new RaptureUser();
        defApiUser.setHashPassword(MD5Utils.hash16("raptivating"));
        defApiUser.setHasRoot(true);
        defApiUser.setDescription("Rapture Root User");
        defApiUser.setUsername("raptureApi");
        addUser(defApiUser);
    }

    private void addUser(RaptureUser user) {
        String jsonString = JacksonUtil.jsonFromObject(user);
        Repository repository = getRepo(RaptureConstants.SETTINGS_REPO);
        repository.addDocument(user.getStorageLocation().getDocPath(), jsonString, "internal", "Create default rapture user", false);
    }

    @Override
    public RepoConfig reloadConfig(String authority) {
        // remove "sys." from authority
        return RepoConfigStorage.readByFields(authority.substring(SYS_PREFIX.length() + 1));
    }

    @Override
    public Repository reloadRepository(RepoConfig repoConfig, boolean autoloadIndex) {
        return RepoFactory.getRepo(repoConfig.getConfig());
    }
}
