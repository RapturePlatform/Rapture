package rapture.field;

import rapture.field.model.FieldDefinition;

public interface FieldLoader {
    public FieldDefinition getField(String uri);
}