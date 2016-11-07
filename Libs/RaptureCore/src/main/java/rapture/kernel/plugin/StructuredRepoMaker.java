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
package rapture.kernel.plugin;

import org.apache.log4j.Logger;

import com.google.common.base.Charsets;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.StructuredRepoConfig;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

/**
 * Used to load plugins that represent structured store schemas
 * 
 * @author dukenguyen
 * 
 */
public class StructuredRepoMaker implements RaptureInstaller {

    private static final Logger log = Logger.getLogger(StructuredRepoMaker.class);

    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        if (Kernel.getStructured().structuredRepoExists(context, uri.toString())) {
            log.info(String.format("Structured repo [%s] already exists.  Skipping installation.", uri.toString()));
            return;
        }
        String content = new String(item.getContent(), Charsets.UTF_8);
        StructuredRepoConfig config = JacksonUtil.objectFromJson(content, StructuredRepoConfig.class);
        Kernel.getStructured().createStructuredRepo(context, uri.toString(), config.getConfig());
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        log.info(String.format("Removing structured repo [%s] via plugin installer...", uri.toString()));
        Kernel.getStructured().deleteStructuredRepo(context, uri.toString());
    }
}
