package rapture.field;

import rapture.field.model.FieldTransform;
import java.util.List;

public interface FieldTransformLoader {
    public FieldTransform getFieldTransform(String uri);
    public List<String> getFieldTransforms(String prefix);
}