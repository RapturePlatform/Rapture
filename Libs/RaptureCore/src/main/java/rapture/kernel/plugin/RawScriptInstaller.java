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

import com.google.common.base.Charsets;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.kernel.Kernel;

public class RawScriptInstaller implements RaptureInstaller {

    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        String uriString = uri.withoutAttribute().toString();
        if (Kernel.getScript().doesScriptExist(context, uriString)) {
            Kernel.getScript().deleteScript(context, uriString);
        }
        RaptureScriptLanguage language = "jjs".equals(uri.getAttribute()) ?
                RaptureScriptLanguage.JAVASCRIPT : RaptureScriptLanguage.REFLEX;
        Kernel.getScript().createScript(context, uriString, language,
                RaptureScriptPurpose.PROGRAM, new String(item.getContent(), Charsets.UTF_8));
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        String uriString = uri.withoutAttribute().toString();
        if (Kernel.getScript().doesScriptExist(context, uriString)) {
            Kernel.getScript().deleteScript(context, uriString);
        }
    }
}
