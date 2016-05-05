package rapture.common;

import com.google.common.net.MediaType;

public class AbstractUpdateObject {

    private RaptureURI uri = null;
    private Object payload = null;
    private String mimeType = MediaType.ANY_TYPE.toString();

    public AbstractUpdateObject() {
    }

    public AbstractUpdateObject(RaptureURI uri) {
        this.uri = uri;
    }

    public AbstractUpdateObject(RaptureURI uri, byte[] payload, String mimeType) {
        this.uri = uri;
        this.payload = payload;
        this.mimeType = mimeType;
    }

    public RaptureURI getUri() {
        return uri;
    }

    public void setUri(RaptureURI uri) {
        this.uri = uri;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
