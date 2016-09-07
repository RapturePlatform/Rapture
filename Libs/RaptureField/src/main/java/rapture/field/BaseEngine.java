package rapture.field;

import java.util.Map;
import java.util.HashMap;

import rapture.common.RaptureField;
import rapture.common.RaptureStructure;
import rapture.field.model.FieldDefinition;
import rapture.field.model.Structure;

public class BaseEngine {
    private Map<String, RaptureField> fieldCache = new HashMap<String, RaptureField>();  
    private Map<String, RaptureStructure> structureCache = new HashMap<String, RaptureStructure>();
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
     
    protected RaptureStructure getStructure(String structureUri) {
        if (structureCache.containsKey(structureUri)) {
            return structureCache.get(structureUri);
        } else {
            RaptureStructure s = structureLoader.getStructure(structureUri);
            if (s != null) {
                structureCache.put(structureUri, s);
                return s;
            }
        } 
        return null;
    }
    
    protected RaptureField getField(String fieldUri) {
        if (fieldCache.containsKey(fieldUri)) {
            return fieldCache.get(fieldUri);
        } else {
            RaptureField f = fieldLoader.getField(fieldUri);
            if (f != null) {
                fieldCache.put(fieldUri, f);
                return f;
            }
        }
        return null;
    }
}