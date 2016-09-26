package rapture.field.model;

import java.util.List;

/**
 * A structure is a definition of some or part of a document (or an array of fields)
 * It lists a set of fields with key names, positions, and field definitions.
 */
 
public class Structure {
    private String uri;
    private String description;
    private List<StructureField> fields; // Order can be important here if the fields are passed from, say a result set...   
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setFields(List<StructureField> fields) {
        this.fields = fields;
    }
    
    public String getUri() {
        return uri;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<StructureField> getFields() {
        return fields;
    }
}