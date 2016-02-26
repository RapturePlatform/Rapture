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
package rapture.common.hooks;

import java.util.HashMap;

import org.apache.log4j.Logger;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.storable.helpers.HooksConfigHelper;
import rapture.config.ConfigLoader;

public enum HooksConfigRepo {
    INSTANCE;

    private Logger log = Logger.getLogger(getClass());

    /**
     * Loads the config from storage
     * 
     * @return
     */
    public HooksConfig loadHooksConfig() {
        /*
         * 1. First, read from CFG file. This is just a seed data that can be
         * overwritten by the repo (#2 below)
         */
        HooksConfig fileConfig = null;
        try {
            fileConfig = readFromFile();
        } catch (Exception e) {
            log.error("Error reading default hooks from cfg file. Will skip ", e);
        }

        /*
         * 2. Now, read from Rapture repo, if there is any data related to this.
         * The data in the repo takes precedendece over the one in the init file
         */

        HooksConfig repoConfig = null;
        try {
            HooksConfig hooksConfig = new HooksConfig();
            hooksConfig.setIdToHook(new HashMap<String, SingleHookConfig>());
            repoConfig = HooksConfigStorage.readByStorageLocation(hooksConfig.getStorageLocation());
        } catch (Exception e) {
            log.error("Error reading hooks from rapture repo. Will skip ", e);
        }

        /*
         * 3. Now do the mixing
         */
        HooksConfig config = new HooksConfig();
        config.setIdToHook(new HashMap<String, SingleHookConfig>());
        if (fileConfig != null) {
            for (SingleHookConfig hookConfig : HooksConfigHelper.getHooks(fileConfig)) {
                HooksConfigHelper.registerHook(config, hookConfig);
            }
        }
        if (repoConfig != null) {
            for (SingleHookConfig hookConfig : HooksConfigHelper.getHooks(repoConfig)) {
                HooksConfigHelper.registerHook(config, hookConfig);
            }
        }

        return config;
    }

    HooksConfig readFromFile() {
        String json = ConfigLoader.getConf().DefaultApiHooks;
        return JacksonUtil.objectFromJson(json, HooksConfig.class);

    }

    /**
     * Stores the {@link HooksConfig} into persistent storage
     * 
     * @param config
     * @param user
     * @param comment
     */
    public void storeHooksConfig(HooksConfig config, String user, String comment) {
        HooksConfigStorage.add(config, user, comment);
    }

}
