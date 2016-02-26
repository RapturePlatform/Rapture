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
package rapture.plugin.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

import rapture.common.PluginConfig;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.plugin.install.PluginContentReader;
import rapture.plugin.install.PluginSandbox;

public class PluginUtils {

    private static final Logger log = Logger.getLogger(PluginUtils.class);

    /**
     * Return a PluginConfig object that represents the root level plugin.txt
     * in a plugin zip file
     *
     * @param filename - The full path to a Plugin zip file
     * @return - PluginConfig object
     */
    public PluginConfig getPluginConfigFromZip(String filename) {
        File zip = new File(filename);
        if (!zip.exists() || !zip.canRead()) {
            return null;
        }
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zip);
            ZipEntry configEntry = zipFile.getEntry(PluginSandbox.PLUGIN_TXT);
            if (configEntry != null && PluginSandbox.PLUGIN_TXT.equals(configEntry.getName())) {
                try {
                    return JacksonUtil.objectFromJson(getContentFromEntry(zipFile, configEntry), PluginConfig.class);
                } catch (Exception ex) {
                    throw RaptureExceptionFactory.create("Plugin.txt manifest corrupt in zip file " + filename);
                }
            } else {
                log.info("No plugin.txt present in root level of zipfile: " + filename);
            }
        } catch (ZipException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (zipFile != null) try {
                zipFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Return a String object that represents the name from feature.txt
     * in a (deprecated) feature zip file
     *
     * @param filename - The full path to a (deprecated) feature zip file
     * @return - name of the feature
     */
    public String getDeprecatedFeatureName(String filename) {
        File zip = new File(filename);
        if (!zip.exists() || !zip.canRead()) {
            return null;
        }
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zip);
            ZipEntry configEntry = zipFile.getEntry(PluginSandbox.DEPRECATED_FEATURE_TXT);
            if (configEntry != null && PluginSandbox.DEPRECATED_FEATURE_TXT.equals(configEntry.getName())) {
                try {
                    Map<String, Object> mapFromJson = JacksonUtil.getMapFromJson(new String(getContentFromEntry(zipFile, configEntry)));
                    return (String)mapFromJson.get("feature");
                } catch (Exception ex) {
                    throw RaptureExceptionFactory.create("feature.txt manifest corrupt in zip file " + filename);
                }
            } else {
                log.info("No feature.txt present in root level of zipfile: " + filename);
            }
        } catch (ZipException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (zipFile != null) try {
                zipFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }

    private byte[] getContentFromEntry(ZipFile zip, ZipEntry entry) throws IOException {
        return PluginContentReader.readFromStream(zip.getInputStream(entry));
    }

}
