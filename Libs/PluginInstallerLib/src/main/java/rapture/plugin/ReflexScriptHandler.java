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

import rapture.common.RaptureScript;
import rapture.common.api.ScriptingApi;
import rapture.util.ScriptNameHelper;
import reflex.IReflexScriptHandler;

public class ReflexScriptHandler implements IReflexScriptHandler {

    private ScriptingApi client;

    public ReflexScriptHandler(ScriptingApi api) {
        this.client = api;
    }

    @Override
    public String getScript(String scriptName) {
        ScriptNameHelper scriptNameHelper = new ScriptNameHelper(scriptName);
        RaptureScript script = client.getScript().getScript("//" + scriptNameHelper.getAuthority() + "/" + scriptNameHelper.getName());
        if (script != null) {
            return script.getScript();
        }
        return "";
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

}
