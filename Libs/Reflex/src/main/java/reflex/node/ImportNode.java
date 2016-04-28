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
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.collections.CollectionUtils;

import rapture.common.jar.ChildFirstClassLoader;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.importer.ImportHandler;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;
import reflex.value.internal.ReflexVoidValue;

public class ImportNode extends BaseNode {
    private ImportHandler importHandler;
    private String importName;
    private String aliasName;
    private List<ReflexNode> configParams;
    private List<String> jarUris;

    public ImportNode(int lineNumber, IReflexHandler handler, Scope s, ImportHandler importHandler, String importId, String alias,
            List<ReflexNode> configParams, List<String> jarUris) {
        super(lineNumber, handler, s);
        this.importHandler = importHandler;
        this.importName = importId;
        this.aliasName = alias;
        this.configParams = configParams;
        this.jarUris = jarUris;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        List<ReflexValue> config = new ArrayList<ReflexValue>(configParams == null ? 0 : configParams.size());
        if (configParams != null) {
            for (ReflexNode n : configParams) {
                config.add(n.evaluate(debugger, scope));
            }
        }
        // load a classloader using any jarUris specified in the import 'from' directive
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (CollectionUtils.isNotEmpty(jarUris) && handler.getApi() != null) {
            try {
                classLoader = new ChildFirstClassLoader(this.getClass().getClassLoader(), handler.getApi(), jarUris);
            } catch (ExecutionException e) {
                throw new ReflexException(-1, "Failed to import jars", e);
            }
        }
        if (aliasName != null) {
            importHandler.addImportModuleWithAlias(importName, aliasName, config, debugger, classLoader);
        } else {
            importHandler.addImportModule(importName, config, debugger, classLoader);
        }
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexNullValue();
    }

    @Override
    public String toString() {
        return String.format("import %s %s", importName, aliasName);
    }
}
