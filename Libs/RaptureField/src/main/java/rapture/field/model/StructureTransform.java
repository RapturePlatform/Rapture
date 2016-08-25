package rapture.field.model;

import java.util.List;

/**
 * The cached configuration on how we can transform one structure into another, using a set
 * of field transforms.
 */
 
public class StructureTransform {
    private String fromStructureUri;
    private String toStructureUri;
    private List<String> transforms;
    
    public void setFromStructureUri(String fromStructureUri) {
        this.fromStructureUri = fromStructureUri;
    }
    
    public void setToStructureUri(String toStructureUri) {
        this.toStructureUri = toStructureUri;
    }
    
    public void setTransforms(List<String> transforms) {
        this.transforms = transforms;
    }
    
    public String getFromStructureUri() {
        return fromStructureUri;
    }
    
    public String getToStructureUri() {
        return toStructureUri;
    }
    
    public List<String> getTransforms() {
        return transforms;
    }
}