package rapture.field;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import rapture.field.model.FieldDefinition;
import rapture.field.model.Structure;
import rapture.field.model.StructureField;
import rapture.field.model.FieldType;
import rapture.common.impl.jackson.JacksonUtil;

/**
 * This is the engine that performs transformations of data according to field definitions.
 * 
 * The idea is that if you have a set of data (perhaps a JSON document) that conforms to a schema (structure)
 * and you want to transform that into a different schema, you should be able to work out from the
 * schema and field and transformation definitions how to do this.
 */
 
public class FieldEngine {
    private Map<String, FieldDefinition> fieldCache = new HashMap<String, FieldDefinition>();  
    private Map<String, Structure> structureCache = new HashMap<String, Structure>();
    private StructureLoader structureLoader;
    private FieldLoader fieldLoader;
    private ScriptLoader scriptLoader;
    private ScriptContainer container;
    
    public FieldEngine(StructureLoader sLoader, FieldLoader fLoader, ScriptLoader scLoader) {
        this.structureLoader = sLoader;
        this.fieldLoader = fLoader;
        this.scriptLoader = scLoader;
        this.container = new ScriptContainer();
    }
    
    /**
     * Given a document (a json document) - check to see if it matches that structure
     * defined in the structureUri. If it doesn't, return a list of formatted error messages
     * that can be displayed to a user or written to a log. A zero size list return indicates
     * a document that validates against the structure.
     */
     
    public List<String> validateDocument(String jsonDocument, String structureUri) {
        // Convert document to a map of maps
        Map<String, Object> docMap = JacksonUtil.getMapFromJson(jsonDocument);
        List<String> ret = new ArrayList<String>();
        validateObject(docMap, structureUri, ret);
        return ret;
    }

    private void validateObject(Map<String, Object> docMap, String structureUri, List<String> ret) {   
        System.out.println("Validate using " + structureUri + " with " + docMap.toString());
        Structure s = getStructure(structureUri);
        // Now we need to look at the fields of the structure and check to see if the
        // keys and values in this document are present and valid.
        for(StructureField sf : s.getFields()) {
            if (docMap.containsKey(sf.getKey())) {
                FieldDefinition fd = getField(sf.getFieldUri());
  
                // Now check the type
                checkValue(sf, fd, docMap.get(sf.getKey()), ret);
                // Now for maps and arrays we need to repeat this one level down
                // with the structure associated with that
                if (fd.getFieldType() == FieldType.MAP) {
                    Map<String, Object> inner = (Map<String, Object>) docMap.get(sf.getKey());
                    validateObject(inner, fd.getFieldTypeExtra(), ret);
                } else if (fd.getFieldType() == FieldType.ARRAY) {
                    List<Object> inner = (List<Object>) docMap.get(sf.getKey());
                    for(Object p : inner) {
                        Map<String, Object> x = (Map<String, Object>) p;
                        validateObject(x, fd.getFieldTypeExtra(), ret);
                    }
                }
            } else if (sf.getMandatory()) {
                ret.add("Document does not contain mandatory field " + sf.getKey() + " of type " + sf.getFieldUri());
            }
        }
    }
    
    private void checkValue(StructureField sf, FieldDefinition fd, Object val, List<String> ret) {
         // First check that the type is correct
        boolean error = false;
        switch(fd.getFieldType()) {
            case STRING:
            case DATE:
                if (!(val instanceof String)) {
                    error = true;
                }
                break;
            case NUMBER:
            case INTEGER:
                if (!(val instanceof Number)) {
                    error = true;
                }
                break;
            case ARRAY:
                if (!(val instanceof List)) {
                    error = true;
                }
                break;
            case MAP:
                if (!(val instanceof Map)) {
                    error = true;
                }
                break;
        }
        if (error) {
            ret.add("Wrong type for " + sf.getKey() + " should be type " + fd.getFieldType());
        }
        // TODO: If there is a validation script, run that
        if (!fd.getValidationScript().isEmpty()) {
            container.runValidationScript(val, scriptLoader.getScript(fd.getValidationScript()), ret);
        }
    }
    
    /**
     * Hopefully structure is in the "cache". If it isn't, load it.
     */
     
    private Structure getStructure(String structureUri) {
        if (structureCache.containsKey(structureUri)) {
            return structureCache.get(structureUri);
        } else {
            Structure s = structureLoader.getStructure(structureUri);
            if (s != null) {
                structureCache.put(structureUri, s);
                return s;
            }
        } 
        return null;
    }
    
    private FieldDefinition getField(String fieldUri) {
        if (fieldCache.containsKey(fieldUri)) {
            return fieldCache.get(fieldUri);
        } else {
            FieldDefinition f = fieldLoader.getField(fieldUri);
            if (f != null) {
                fieldCache.put(fieldUri, f);
                return f;
            }
        }
        return null;
    }
}
