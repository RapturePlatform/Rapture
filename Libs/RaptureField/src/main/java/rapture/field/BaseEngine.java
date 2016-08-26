package rapture.field;

import java.util.Map;
import java.util.HashMap;

import rapture.field.model.FieldDefinition;
import rapture.field.model.Structure;

public class BaseEngine {
    private Map<String, FieldDefinition> fieldCache = new HashMap<String, FieldDefinition>();  
    private Map<String, Structure> structureCache = new HashMap<String, Structure>();
    private StructureLoader structureLoader;
    private FieldLoader fieldLoader;
    protected FieldTransformLoader ftLoader;
    protected ScriptLoader scriptLoader;
    protected ScriptContainer container;
    
    public BaseEngine(StructureLoader sLoader, FieldLoader fLoader, ScriptLoader scLoader, FieldTransformLoader ftLoader) {
        this.structureLoader = sLoader;
        this.fieldLoader = fLoader;
        this.scriptLoader = scLoader;
        this.ftLoader = ftLoader;
        this.container = new ScriptContainer();
    }
    
    /**
     * Hopefully structure is in the "cache". If it isn't, load it.
     */
     
    protected Structure getStructure(String structureUri) {
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
    
    protected FieldDefinition getField(String fieldUri) {
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