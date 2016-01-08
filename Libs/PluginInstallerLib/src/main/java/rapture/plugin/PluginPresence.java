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
package rapture.plugin;

import rapture.common.PluginConfig;
import rapture.common.Scheme;
import rapture.common.RaptureURI;
import rapture.common.storable.helpers.PluginConfigHelper;
import rapture.common.PluginConfigStorage;
import rapture.common.client.ScriptClient;
import rapture.common.exception.RaptureException;

/**
 * Plugin Presence is used to determine whether a feature is installed in the
 * attached system or not.
 * 
 * It can also be used to bootstrap feature presence config on demand
 * from the application.
 * 
 * @author amkimian
 * 
 */
public class PluginPresence {
    private ScriptClient raptureClient;

    public PluginPresence(ScriptClient raptureClient) {
        this.raptureClient = raptureClient;
    }

    public boolean doesFeatureNeedInstalling(PluginConfig feature) {
        PluginConfig existingConfig = null;
        try {
            RaptureURI internalUri = new RaptureURI(feature.getPlugin(), Scheme.PLUGIN);
            existingConfig = PluginConfigStorage.readByAddress(internalUri);
            if (PluginConfigHelper.earlierThan(existingConfig, feature)) {
                return true;
            } else {
                return false;
            }
        } catch (RaptureException e) {
            return true;
        }
    }

    public void registerFeatureInstalled(PluginConfig plugin) {
        PluginConfigStorage.add(plugin, raptureClient.getUser().getContext().getContext(), "Recorded plugin");
    }
}
