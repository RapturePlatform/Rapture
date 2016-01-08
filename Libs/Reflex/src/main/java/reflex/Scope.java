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
package reflex;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import reflex.value.ReflexValue;

public class Scope {

    private boolean assignOnce = false;
    @SuppressWarnings("unused")
    private String scopeName = "general";
    private Scope parent;
    @JsonIgnore
    private Scope globalScope;
    @JsonIgnore
    private Scope constantScope;
    private String pendingComment = null;

    private Map<String, ReflexValue> variables;

    public Scope() {

    }

    // NB. Added this back in as the variables are retrieved in the NetBeans
    // REPL debugger
    public Map<String, ReflexValue> retrieveVariables() {
        return variables;
    }

    public static Scope defrostScope(Scope globalScope, Scope constantScope, Scope myScope) {
        Scope me = new Scope(null, true);
        me.constantScope = constantScope;
        me.globalScope = globalScope;
        me.variables = myScope.variables == null ? new HashMap<String, ReflexValue>() : myScope.variables;
        return me;
    }

    public static Scope getInitialScope() {
        Scope constScope = new Scope(null, true);
        constScope.scopeName = "Constant";
        Scope globalScope = new Scope(constScope, false);
        globalScope.scopeName = "Global";
        constScope.globalScope = globalScope;
        constScope.constantScope = constScope;
        globalScope.globalScope = globalScope;
        globalScope.constantScope = constScope;
        return globalScope;
    }

    /**
     * Creates a {@link Scope} that does not contain any of the variables inside Scope, but still knows about the global and constant scopes
     *
     * @param seedScope The {@link Scope} that has the global and constant scopes we want to use
     * @return
     */
    public static Scope createIsolatedScope(Scope seedScope) {
        Scope newScope = new Scope(seedScope.globalScope, false);
        newScope.constantScope = seedScope.constantScope;
        newScope.globalScope = seedScope.globalScope;
        return newScope;
    }

    public static Scope getNextScopeDown(Scope scope) {
        Scope ret = new Scope(scope);
        return ret;
    }

    private Scope(Scope parent, boolean assignOnce) {
        this.parent = parent;
        this.assignOnce = assignOnce;
        variables = new HashMap<String, ReflexValue>();
    }

    protected Scope(Scope p) {
        parent = p;
        variables = new HashMap<String, ReflexValue>();
        globalScope = p.globalScope;
        constantScope = p.constantScope;
    }

    public void assign(String var, ReflexValue value) {
        assign(var, value, "");
    }

    public void assign(String var, ReflexValue value, String namespacePrefix) {
        if (resolve(var, namespacePrefix) != null) {
            // There is already such a variable, re-assign it
            if (assignOnce) {
                throw new ReflexException(0, "Assignment into constant variable");
            }
            this.reAssign(var, value);
        } else {
            // Here, if we have a.b = value we really want to create "a" which is a map
            // and then create a key "b" in "a" that has the value given.
            // (but we should check for existence of a first)
            if (!tryDottedAssign(var, value, namespacePrefix)) {
                if (assignOnce) {
                    // A new constant
                    variables.put(namespacePrefix + var, value);
                } else {
                    // A newly declared variable
                    variables.put(var, value);
                }
            }
        }
    }

    private boolean tryDottedAssign(String var, ReflexValue value, String namespacePrefix) {
        if (var.indexOf('.') == -1) {
            return false;
        }
        String[] parts = var.split("\\.");
        ReflexValue existingValue = resolve(parts[0], namespacePrefix);
        if (existingValue == null) {
            existingValue = new ReflexValue(new HashMap<String, Object>());
            variables.put(parts[0], existingValue);
        }
        if (existingValue.isMap()) {
            return dottedAssignMap(existingValue, parts, value);
        } else if (existingValue.isStruct()) {
            return existingValue.asStruct().dottedAssign(parts, value);
        } else {
            throw new ReflexException(0, "Cannot assign in this way to a non-map");
        }

    }

    private boolean dottedAssignMap(ReflexValue existingValue, String[] parts, ReflexValue value) {
        Map<String, Object> mapper = existingValue.asMap();
        for (int i = 1; i < parts.length - 2; i++) {
            if (mapper.containsKey(parts[i])) {
                Object val = mapper.get(parts[i]);
                if (val instanceof ReflexValue) {
                    existingValue = (ReflexValue) val;
                    if (existingValue.isMap()) {
                        mapper = existingValue.asMap();
                    } else {
                        throw new ReflexException(0, "Cannot assign in this way to a part of a map that is not itself a map");
                    }
                } else if (val instanceof Map<?, ?>) {
                    mapper = (Map<String, Object>) val;
                }
            } else {
                mapper.put(parts[i], new HashMap<String, Object>());
                mapper = (Map<String, Object>) mapper.get(parts[i]);
            }
        }

        mapper.put(parts[parts.length - 1], value);
        return true;
    }

    public void clearScope() {
        variables = new HashMap<String, ReflexValue>();
    }

    public Scope copy() {
        // Create a shallow copy of this scope. Used in case functions are
        // are recursively called. If we wouldn't create a copy in such cases,
        // changing the variables would result in changes to the Maps from
        // other "recursive scopes".
        return Scope.createShallowCopy(this);
    }

    public static Scope createShallowCopy(Scope s) {
        Scope ret = new Scope(s);
        ret.parent = null;
        ret.variables = new HashMap<String, ReflexValue>(s.variables);
        return ret;
    }

    public Scope getParent() {
        return parent;
    }

    public Set<Entry<String, ReflexValue>> retrieveVariableSet() {
        return variables.entrySet();
    }

    public Scope parent() {
        return parent;
    }

    private void reAssign(String identifier, ReflexValue value) {
        if (variables.containsKey(identifier)) {
            // The variable is declared in this scope
            // IF the variable exists, look at the type, because we might want to coerce the assignment
            // variables.get(identifier).reassign(value);
            variables.put(identifier, value);
        } else if (parent != null) {
            // The variable was not declared in this scope, so let
            // the parent scope re-assign it
            parent.reAssign(identifier, value);
        }
    }

    public ReflexValue resolve(String var) {
        return resolve(var, "");
    }

    public ReflexValue resolve(String var, String namespacePrefix) {
        ReflexValue value;
        if (assignOnce) { // constants always contain a prefix, if there is any
            value = variables.get(namespacePrefix + var);
        } else {
            value = variables.get(var);
        }

        if (value != null) {
            // The variable resides in this scope
            return value;
        } else if (parent != null) {
            // Let the parent scope look for the variable
            return parent.resolve(var, namespacePrefix);
        } else {
            // Unknown variable
            return null;
        }
    }

    public String getAndUsePendingComment() {
        if (pendingComment != null) {
            String ret = pendingComment;
            pendingComment = null;
            return ret;
        }
        return null;
    }

    public String setPendingComment(String comment) {
        String ret = pendingComment;
        pendingComment = comment;
        return ret;
    }

    public Scope getConstantScope() {
        return constantScope;
    }

    public Scope getGlobalScope() {
        return globalScope;
    }
}
