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

import java.util.Map;

import org.antlr.runtime.RecognitionException;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureIdGenConfig;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dsl.idgen.IdGenFactory;
import rapture.generated.IdGenParser;
import rapture.kernel.Kernel;

public class IdGenInstaller implements RaptureInstaller {
    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        RaptureIdGenConfig idgen = JacksonUtil.objectFromJson(item.getContent(), RaptureIdGenConfig.class);
        if (Kernel.getIdGen().idGenExists(context, item.getUri())) {
            RaptureIdGenConfig rfc = Kernel.getIdGen().getIdGenConfig(context, item.getUri());
            if (!same(idgen, rfc, item.getUri())) {
                throw RaptureExceptionFactory.create("IdGen " + item.getUri() + " already exists with a different configuration");
            }
        } else {
            Kernel.getIdGen().createIdGen(context, item.getUri(), idgen.getConfig());
        }
    }

    private static boolean same(RaptureIdGenConfig input, RaptureIdGenConfig old, String uri) {
        IdGenParser oldP = null, inputP = null;
        try {
            oldP = IdGenFactory.parseConfig(old.getConfig());
        } catch (RecognitionException e) {
            throw RaptureExceptionFactory.create("Existing idgen configuration is corrupt -- uninstall first (" + uri + ")", e);
        }
        try {
            inputP = IdGenFactory.parseConfig(input.getConfig());
        } catch (RecognitionException e) {
            throw RaptureExceptionFactory.create("Plugin IdGen config is corrupt -- cannot install " + uri, e);
        }
        if (oldP.getStore().getType() != inputP.getStore().getType()) return false;
        Map<String, String> oldM = oldP.getConfig().getConfig();
        Map<String, String> inputM = inputP.getConfig().getConfig();
        return oldM.equals(inputM);
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getIdGen().deleteIdGen(context, uri.toString());
    }
}
