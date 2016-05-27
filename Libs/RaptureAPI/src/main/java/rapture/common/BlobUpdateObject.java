package rapture.common;

import java.util.HashMap;
import java.util.Map;

public class BlobUpdateObject extends AbstractUpdateObject<BlobContainer> {

    BlobContainer payload = null;

    public BlobUpdateObject() {
    }

    public BlobUpdateObject(RaptureURI uri) {
        super(uri);
        assert (uri.getScheme() == Scheme.BLOB);
    }

    public BlobUpdateObject(RaptureURI uri, byte[] content, Map<String, String> headers, String mimeType) {
        this(uri);
        BlobContainer bc = new BlobContainer();
        bc.setContent(content);
        bc.setHeaders(headers);
        setPayload(bc);
        setMimeType(mimeType);
    }

    public BlobUpdateObject(RaptureURI uri, byte[] content, String mimeType) {
        this(uri, content, new HashMap<String, String>(), mimeType);
    }

    @Override
    public BlobContainer getPayload() {
        return payload;
    }

    @Override
    public void setPayload(BlobContainer payload) {
        this.payload = payload;
        setMimeType(payload.getHeaders().get(ContentEnvelope.CONTENT_TYPE_HEADER));
    }
}
