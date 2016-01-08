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
package reflex.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import reflex.schema.SchemaDocument;
import reflex.schema.SchemaEntity;

/**
 * A data structure definition in Reflex
 * @author amkimian
 *
 */
public class Structure {
    private String name;
    private Map<String, StructureType> members;
    
    public Structure() {
        this.name = "anon";
        this.members = new HashMap<String, StructureType>();
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Structure(String name) {
        this.name = name;
        this.members = new HashMap<String, StructureType>();
    }
    
    public void addMember(String name, StructureType type) {
        members.put(name, type);
    }
    
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("Name = " + name);
        ret.append(", Members = " + members.toString());
        return ret.toString();
    }
    
    public Map<String, StructureType> getMembers() {
        return members;
    }

    private SchemaEntity getEntityForType(StructureType t) {
        SchemaEntity entity = new SchemaEntity();
        if (t instanceof NumberStructureType) {
            entity.setType("number");
        } else if (t instanceof IntegerStructureType) {
            entity.setType("integer");                
        } else if (t instanceof StringStructureType) {
            entity.setType("string");
        } else if (t instanceof ArrayStructureType) {
            entity.setType("array");
           ArrayStructureType aType = (ArrayStructureType) t;
           entity.setItems(getEntityForType(aType.getArrayType()));
        } else if (t instanceof InnerStructureType) {
            entity.setType("object");
            InnerStructureType iType = (InnerStructureType) t;
            entity.setProperties(iType.getStructure().getProperties());
        }
        return entity;
    }
    
    private Map<String, SchemaEntity> getProperties() {
        Map<String, SchemaEntity> props = new HashMap<String, SchemaEntity>();
        for(Map.Entry<String, StructureType> entry : members.entrySet()) {
            StructureType t = entry.getValue();
            props.put(entry.getKey(), getEntityForType(t));
        }
        return props;
    }
    
    public SchemaDocument getSchemaDocument() {
        SchemaDocument doc = new SchemaDocument();
        doc.setTitle(name);
        doc.setRequired(new ArrayList<String>());
        doc.setType("object");
        doc.setProperties(getProperties());
        return doc;
    }
}
