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
package rapture.kernel.plugin;

import com.google.common.base.Charsets;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

public class ScriptInstaller implements RaptureInstaller {
    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        // A script document is either (a) a raw script (just text) or (b) a
        // RaptureScript serialized object
        // we assume that (b) has a leading { as the first char.

        if (Kernel.getScript().doesScriptExist(context, item.getUri())) {
            Kernel.getScript().deleteScript(context, item.getUri());
        }
        String scriptString = new String(item.getContent(), Charsets.UTF_8);
        if (scriptString.startsWith("{")) {
            RaptureScript script = JacksonUtil.objectFromJson(scriptString, RaptureScript.class);
            Kernel.getScript().createScript(context, item.getUri(), script.getLanguage(), script.getPurpose(),
                    script.getScript());
        } else if (scriptString.startsWith("@")) {
            Kernel.getScript().createScriptLink(context, item.getUri(), scriptString.substring(1).trim());
        } else {
            Kernel.getScript().createScript(context, item.getUri(), RaptureScriptLanguage.REFLEX,
                    RaptureScriptPurpose.PROGRAM, scriptString);
        }
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getScript().deleteScript(context, uri.toString());

    }
}
