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
package reflex.function;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.value.ReflexProcessValue;
import reflex.value.ReflexValue;

/**
 * Spawn a secondary process (if possible)
 * 
 * @author amkimian
 * 
 */
public class SpawnNode extends BaseNode {

    private ReflexNode paramsNode;
    private ReflexNode envNode;
    private ReflexNode fileNode;

    public SpawnNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode paramsNode, ReflexNode envNode, ReflexNode fileNode) {
        super(lineNumber, handler, s);
        this.paramsNode = paramsNode;
        this.envNode = envNode;
        this.fileNode = fileNode;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        // Spawn a process
        debugger.stepStart(this, scope);
        Runtime runtime = Runtime.getRuntime();
        // a process is
        // (1) an array of command vars
        // (2) an array of env vars
        // (3) a directory (a file object)

        String[] commandVars = null;
        String[] envVars = null;
        File fDir = null;

        ReflexValue v = paramsNode.evaluate(debugger, scope);
        // If it's a list, convert it to a String[] array, otherwise, it's a
        // single string
        if (v.isList()) {
            commandVars = getList(v);
        } else {
            commandVars = new String[1];
            commandVars[0] = v.asString();
        }
        if (envNode != null) {
            ReflexValue eVars = envNode.evaluate(debugger, scope);
            if (eVars.isList()) {
                envVars = getList(eVars);
            } else if (eVars.isMap()) {
                envVars = getFromMap(eVars);
            }
        }

        if (fileNode != null) {
            ReflexValue fVar = fileNode.evaluate(debugger, scope);
            if (fVar.isFile()) {
                fDir = new File(fVar.asFile().getFileName());
            } else {
                fDir = new File(fVar.toString());
            }
        }

        try {
            Process p = runtime.exec(commandVars, envVars, fDir);
            ReflexValue retVal = new ReflexValue(new ReflexProcessValue(p));
            debugger.stepEnd(this, retVal, scope);
            return retVal;
        } catch (IOException e) {
            throw new ReflexException(lineNumber, "Failed to spawn process", e);
        }
    }

    private String[] getFromMap(ReflexValue eVars) {
        Map<String, Object> vals = eVars.asMap();
        String[] ret = new String[vals.size()];
        int pos = 0;
        for (Map.Entry<String, Object> e : vals.entrySet()) {
            ret[pos] = e.getKey() + "=" + e.getValue().toString();
            pos++;
        }
        return ret;
    }

    private String[] getList(ReflexValue eVars) {
        List<ReflexValue> vals = eVars.asList();
        String[] commandVars = new String[vals.size()];
        int pos = 0;
        for (ReflexValue va : vals) {
            commandVars[pos] = va.toString();
            pos++;
        }
        return commandVars;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("spawn(%s,%s,%s)", paramsNode, envNode == null ? "" : envNode, fileNode == null ? "" : fileNode);
    }
}
