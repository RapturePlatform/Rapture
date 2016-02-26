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

import java.util.HashMap;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.Messages;
import rapture.common.RaptureConstants;
import rapture.common.api.BootstrapApi;
import rapture.common.model.RepoConfig;
import rapture.common.model.RepoConfigStorage;
import rapture.kernel.bootstrap.LowLevelBootstrapCopier;
import rapture.kernel.bootstrap.RebuildFinisher;

public class BootstrapApiImpl extends KernelBase implements BootstrapApi {
    private static final String RAPTURE_CONFIG = "RaptureConfig"; //$NON-NLS-1$
    private static final String RAPTURE_SETTINGS = "RaptureSettings"; //$NON-NLS-1$
    private static final String RAPTURE_EPHEMERAL = "RaptureEphemeral"; //$NON-NLS-1$
    private Map<String, String> scriptMapping = new HashMap<String, String>();

    public BootstrapApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public void addScriptClass(CallingContext context, String keyword, String className) {
        scriptMapping.put(keyword, className);
    }

    @Override
    public Map<String, String> getScriptClasses(CallingContext context) {
        return scriptMapping;
    }

    @Override
    public Boolean deleteScriptClass(CallingContext context, String keyword) {
        return scriptMapping.remove(keyword) != null;
    }

    @Override
    public void restartBootstrap(CallingContext context) {
        getKernel().restart();
    }

    @Override
    public void setConfigRepo(CallingContext context, String config) {
        RepoConfig configRepo = new RepoConfig();
        configRepo.setName(RAPTURE_CONFIG);
        configRepo.setConfig(config);
        RepoConfigStorage.add(configRepo, context.getUser(), "Set config repo");
        getBootstrapRepo().commitStage(RaptureConstants.OFFICIAL_STAGE, context.getUser(), Messages.getString("Bootstrap.UpdateConfig")); //$NON-NLS-1$
        // If we update the config repo, we should potentially reset the
        // repoCache and update
        // the users.
        Kernel.getKernel().clearRepoCache(false);
    }

    @Override
    public void setEmphemeralRepo(CallingContext context, String config) {
        RepoConfig configRepo = new RepoConfig();
        configRepo.setName(RAPTURE_EPHEMERAL);
        configRepo.setConfig(config);
        RepoConfigStorage.add(configRepo, context.getUser(), "Set ephemeral repo");
        getBootstrapRepo().commitStage(RaptureConstants.OFFICIAL_STAGE, context.getUser(), Messages.getString("Bootstrap.UpdateEphemeral")); //$NON-NLS-1$

        Kernel.getKernel().clearRepoCache(false);
    }

    @Override
    public void setSettingsRepo(CallingContext context, String config) {
        RepoConfig configRepo = new RepoConfig();
        configRepo.setName(RAPTURE_SETTINGS);
        configRepo.setConfig(config);
        RepoConfigStorage.add(configRepo, context.getUser(), "Set settings repo");
        getBootstrapRepo().commitStage(RaptureConstants.OFFICIAL_STAGE, context.getUser(), Messages.getString("Bootstrap.UpdateSettings")); //$NON-NLS-1$
        Kernel.getKernel().clearRepoCache(true);
    }

    private LowLevelBootstrapCopier rebuilder = new LowLevelBootstrapCopier();

    @Override
    public void migrateConfigRepo(final CallingContext context, String newConfig) {
        // Take the data in the current config repo, copy it into a new
        // repo with the passed config, and then update the
        // config
        // to the bootstrap config repo to this passed config. Then
        // ultimately remove the old
        // repo.
        rebuilder.runRebuildFor(getConfigRepo(), newConfig, new RebuildFinisher() {

            @Override
            public void rebuildFinished(String newConfig) {
                setConfigRepo(context, newConfig);
            }

        });
    }

    @Override
    public void migrateEphemeralRepo(final CallingContext context, String newConfig) {
        rebuilder.runRebuildFor(getEphemeralRepo(), newConfig, new RebuildFinisher() {

            @Override
            public void rebuildFinished(String newConfig) {
                setEmphemeralRepo(context, newConfig);
            }

        });
    }

    @Override
    public void migrateSettingsRepo(final CallingContext context, String newConfig) {
        rebuilder.runRebuildFor(getSettingsRepo(), newConfig, new RebuildFinisher() {

            @Override
            public void rebuildFinished(String newConfig) {
                setSettingsRepo(context, newConfig);
            }

        });
    }

    private String getConfigFor(CallingContext context, String name) {
        RepoConfig config = RepoConfigStorage.readByFields(name);
        return config.getConfig();
    }

    @Override
    public String getConfigRepo(CallingContext context) {
        return getConfigFor(context, RAPTURE_CONFIG);
    }

    @Override
    public String getSettingsRepo(CallingContext context) {
        return getConfigFor(context, RAPTURE_SETTINGS);
    }

    @Override
    public String getEphemeralRepo(CallingContext context) {
        return getConfigFor(context, RAPTURE_EPHEMERAL);
    }
}
