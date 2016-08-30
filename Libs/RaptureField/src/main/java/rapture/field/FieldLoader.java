package rapture.field;

import rapture.common.RaptureField;

public interface FieldLoader {
    public RaptureField getField(String uri);
}