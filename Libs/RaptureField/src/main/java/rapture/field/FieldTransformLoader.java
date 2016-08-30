package rapture.field;

import java.util.List;

import rapture.common.RaptureFieldTransform;

public interface FieldTransformLoader {
    public RaptureFieldTransform getFieldTransform(String uri);
    public List<String> getFieldTransforms(String prefix);
}