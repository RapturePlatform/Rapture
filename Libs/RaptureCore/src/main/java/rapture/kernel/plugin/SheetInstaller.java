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

import java.util.UUID;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.common.RaptureURI;
import rapture.kernel.Kernel;

public class SheetInstaller implements RaptureInstaller {
    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        String script = new String(item.getContent(), Charsets.UTF_8);
        String tempURI = temporaryURI(uri);
        Kernel.getScript().createScript(context, tempURI, RaptureScriptLanguage.REFLEX, RaptureScriptPurpose.PROGRAM, script);
        Kernel.getScript().runScript(context, tempURI, ImmutableMap.<String, String>of());
        Kernel.getScript().deleteScript(context, tempURI);
    }

    //TODO MEL get rid of this and find a way to run the script without saving it.
    private String temporaryURI(RaptureURI uri) {
        return "script://" + uri.getAuthority() + "/__reserved_installers/" + UUID.randomUUID().toString();
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        Kernel.getSheet().deleteSheet(context, uri.toString());
    }
}
