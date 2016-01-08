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
package reflex.util.function;

import java.util.HashMap;
import java.util.Map;

import reflex.Function;
import reflex.ReflexException;
import reflex.structure.Structure;
import reflex.util.NamespaceStack;

/**
 * This class is a registry used by the parser and tree-walker to store and
 * retrieve stateful data, such as function definitions
 * 
 * @author bardhi
 * 
 */
public class LanguageRegistry {

    /*
     * namespacePrefix->functionName->function
     */
    private Map<String, Function> functionMap;
    private Map<String, Structure> structureMap;
    
    // private Map<String, String> aliasToNS;
    private NamespaceStack nsStack;

    public LanguageRegistry() {
        // this.aliasToNS = new HashMap<String, String>();

        // defaultNamespace = new HashMap<String, Function>();

        functionMap = new HashMap<String, Function>();
        structureMap = new HashMap<String, Structure>();
        
        // functionMap.put(FunctionKey.DEFAULT_NAMESPACE, defaultNamespace);

        nsStack = new NamespaceStack();
    }

    public Function getFunction(FunctionKey key) {
        return functionMap.get(key.toString());
    }

    public Structure getStructure(StructureKey key) {
        return structureMap.get(key.toString());
    }
    
    public void registerStructure(StructureKey key, Structure structure) {
        if (structureMap.containsKey(key.toString())) {
            StringBuilder error = new StringBuilder();
            error.append("Error! Trying to redefine structure '").append(key.toString()).append("'");
            throw new ReflexException(0, error.toString());
        } else {
            structureMap.put(key.toString(), structure);
        }        
    }
    public void registerFunction(FunctionKey key, Function function) {
        if (functionMap.containsKey(key.toString())) {
            StringBuilder error = new StringBuilder();
            error.append("Error! Trying to redefine function '").append(key.toString()).append("'");
            throw new ReflexException(0, error.toString());
        } else {
            functionMap.put(key.toString(), function);
        }
    }

    public NamespaceStack getNamespaceStack() {
        return nsStack;
    }

    public boolean hasFunction(String namespacePrefix, String name, int numParams) {
        FunctionKey key = new FunctionKey(namespacePrefix, name, numParams);
        return functionMap.containsKey(key.toString());
    }

    /**
     * Merge into this registry the functions of the savedRegistry
     * 
     * @param savedRegistry
     */
    public void merge(LanguageRegistry savedRegistry) {
        if (savedRegistry != null) {
            for (Map.Entry<String, Function> mergeEntry : savedRegistry.functionMap.entrySet()) {
                if (!functionMap.containsKey(mergeEntry.getKey())) {
                    functionMap.put(mergeEntry.getKey(), mergeEntry.getValue());
                }
            }
        }
    }

}
