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
package reflex.function;

import java.util.Map;

import rapture.common.impl.jackson.JacksonUtil;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.node.BaseNode;
import reflex.node.ReflexNode;
import reflex.schema.SchemaDocument;
import reflex.schema.SchemaEntity;
import reflex.structure.ArrayStructureType;
import reflex.structure.InnerStructureType;
import reflex.structure.IntegerStructureType;
import reflex.structure.NumberStructureType;
import reflex.structure.StringStructureType;
import reflex.structure.Structure;
import reflex.structure.StructureType;
import reflex.util.function.LanguageRegistry;
import reflex.util.function.StructureFactory;
import reflex.util.function.StructureKey;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * Cast one value type to another
 * 
 * @author amkimian
 * 
 */
public class GenStructNode extends BaseNode {

    private String name;
    private ReflexNode definition;
    private LanguageRegistry registry;
    private String namespacePrefix;

    public GenStructNode(int lineNumber, IReflexHandler handler, Scope scope, String name, ReflexNode definition, LanguageRegistry registry, String namespacePrefix) {
        super(lineNumber, handler, scope);
        this.name = name;
        this.definition = definition;
        this.registry = registry;
        this.namespacePrefix = namespacePrefix;
    }

    private StructureType getStructureType(String typeName) {
            if (typeName.equals("number")) {
                return new NumberStructureType();
            } else if (typeName.equals("integer")) {
                return new IntegerStructureType();
            } else if (typeName.equals("string")) {
                return new StringStructureType();
            } else if (typeName.equals("array")) {
                return new ArrayStructureType();
            } else if (typeName.equals("object")) {
                return new InnerStructureType();
            }
            throw new ReflexException(-1, "Unknown structure member type - " + typeName);
    }
    
    private Structure getStructure(Map<String, SchemaEntity> entries) {
        Structure s = new Structure();
        for( Map.Entry<String, SchemaEntity> entry : entries.entrySet()) {
            String entityName = entry.getKey();
            String entityType = entry.getValue().getType();
            StructureType type = getStructureType(entityType);
            if (type instanceof ArrayStructureType) {
                ((ArrayStructureType)type).setArrayType(getStructureType(entry.getValue().getItems().getType()));
            } else if (type instanceof InnerStructureType) {
                Structure innerS = getStructure(entry.getValue().getProperties());
                ((InnerStructureType)type).setStructure(innerS);
            }
            s.addMember(entityName, type);
        }
        return s;
    }
    
    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue definitionValue = definition.evaluate(debugger, scope);
        ReflexValue retVal = new ReflexVoidValue();
        
        SchemaDocument doc = JacksonUtil.objectFromJson(definitionValue.asString(), SchemaDocument.class);
        Structure s = getStructure(doc.getProperties());
        s.setName(name);
        
        StructureKey key = namespacePrefix == null ? StructureFactory.createStructureKey(name) : StructureFactory.createStructureKey(namespacePrefix, name);
        registry.registerStructure(key, s);
        
        retVal = new ReflexValue(true);
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return super.toString() + " - " + String.format("new(%s)", name);
    }
}
