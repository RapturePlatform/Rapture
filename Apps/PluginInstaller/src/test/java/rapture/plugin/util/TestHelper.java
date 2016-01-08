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
package rapture.plugin.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import rapture.common.PluginConfig;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.plugin.install.PluginSandbox;

/**
 * Utility class for unit tests. Put all your shared unit test stuff in here
 * 
 * @author dukenguyen
 * 
 */
public class TestHelper {

    public static File createTempZipWithPluginTxt(File tempDir, String filename, String pluginName) throws IOException {
        File ret = new File(tempDir, filename);
        ret.createNewFile();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(ret));
        ZipEntry e = new ZipEntry(PluginSandbox.PLUGIN_TXT);
        out.putNextEntry(e);
        PluginConfig config = new PluginConfig();
        config.setPlugin(pluginName);
        byte[] data = JacksonUtil.jsonFromObject(config).toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
        return ret;
    }

}
