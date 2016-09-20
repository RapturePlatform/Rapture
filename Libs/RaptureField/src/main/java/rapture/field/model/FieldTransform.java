package rapture.field.model;

import java.util.List;

/**
 * The definition of how to transform one set of fields into another set of fields.
 */
 
public class FieldTransform {
    private String uri;
    private List<String> sourceFields;
    private List<String> targetFields;
    private String transformScript;
    private String description;
    
    public String getUri() {
        return uri;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getSourceFields() {
        return sourceFields;
    }
    
    public List<String> getTargetFields() {
        return targetFields;
    }
    
    public String getTransformScript() {
        return transformScript;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public void setSourceFields(List<String> sourceFields) {
        this.sourceFields = sourceFields;
    }
    
    public void setTargetFields(List<String> targetFields) {
        this.targetFields = targetFields;
    }
    
    public void setTransformScript(String transformScript) {
        this.transformScript = transformScript;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}