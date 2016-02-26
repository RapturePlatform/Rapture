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
package reflex.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reflex.ICapability;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.value.ReflexValue;

public abstract class CapTestBaseNode extends BaseNode {
    public CapTestBaseNode(int lineNumber, IReflexHandler handler, Scope s) {
        super(lineNumber, handler, s);
        setupCapInterfaces();
    }

    private void setupCapInterfaces() {
        capInterfaces.put("DEBUG", handler.getDebugHandler());
        capInterfaces.put("CACHE", handler.getCacheHandler());
        capInterfaces.put("DATA", handler.getDataHandler());
        capInterfaces.put("IO", handler.getIOHandler());
        capInterfaces.put("OUTPUT", handler.getOutputHandler());
        capInterfaces.put("PORT", handler.getPortHandler());
        capInterfaces.put("SCRIPT", handler.getScriptHandler());
        capInterfaces.put("SUSPEND", handler.getSuspendHandler());
    }

    private Map<String, ICapability> capInterfaces = new HashMap<String, ICapability>();

    public boolean testCapability(String name) {
        if (capInterfaces.containsKey(name)) {
            return capInterfaces.get(name).hasCapability();
        } else {
            throw new ReflexException(lineNumber, "Unknown capability to test - " + name);
        }
    }

    public ReflexValue getAllCapStatus() {
        List<String> oncaps = new ArrayList<String>();
        List<String> offcaps = new ArrayList<String>();
        for (String capName : capInterfaces.keySet()) {
            if (capInterfaces.get(capName).hasCapability()) {
                oncaps.add(capName);
            } else {
                offcaps.add(capName);
            }
        }
        Map<String, Object> retMap = new HashMap<String, Object>();
        retMap.put("ON", new ReflexValue(oncaps));
        retMap.put("OFF", new ReflexValue(offcaps));
        return new ReflexValue(retMap);
    }
}
