package rapture.field;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import rapture.field.model.FieldDefinition;
import rapture.field.model.Structure;

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
    
    public FieldEngine(StructureLoader sLoader) {
        this.structureLoader = sLoader;
    }
    
    /**
     * Given a document (a json document) - check to see if it matches that structure
     * defined in the structureUri. If it doesn't, return a list of formatted error messages
     * that can be displayed to a user or written to a log. A zero size list return indicates
     * a document that validates against the structure.
     */
     
    public List<String> validateDocument(String jsonDocument, String structureUri) {
        return null;
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
}
