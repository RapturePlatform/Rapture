package rapture.field.model;

public class StructureField {
    private String key;
    private String fieldUri;
    private boolean mandatory;
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public void setFieldUri(String fieldUri) {
        this.fieldUri = fieldUri;
    }
    
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getFieldUri() {
        return fieldUri;
    }
    
    public boolean getMandatory() {
        return mandatory;
    }
}