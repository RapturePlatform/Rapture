package rapture.common;

public class BlobUpdateObject extends AbstractUpdateObject {

    public BlobUpdateObject() {
    }

    public BlobUpdateObject(RaptureURI uri) {
        super(uri);
        assert (uri.getScheme() == Scheme.BLOB);
    }

    public BlobUpdateObject(RaptureURI uri, byte[] content, String mimeType) {
        this(uri);
        setContent(content);
        setMimeType(mimeType);
    }

    public byte[] getContent() {
        return (byte[]) super.getPayload();
    }

    public void setContent(byte[] content) {
        super.setPayload(content);
    }
}
