package rapture.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.net.MediaType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = rapture.common.series.SeriesUpdateObject.class, name = "seriesUpdateObject"),
        @JsonSubTypes.Type(value = rapture.common.DocUpdateObject.class, name = "documentUpdateObject"),
        @JsonSubTypes.Type(value = rapture.common.BlobUpdateObject.class, name = "blobUpdateObject") })

public abstract class AbstractUpdateObject<T> {

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    T payload;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof AbstractUpdateObject)) return false;
        AbstractUpdateObject other = (AbstractUpdateObject) obj;
        if (mimeType == null) {
            if (other.getMimeType() != null) return false;
        } else if (!mimeType.equals(other.getMimeType())) return false;

        if (uri == null) {
            if (other.uri != null) return false;
        } else if (!uri.equals(other.uri)) return false;

        if (payload == null) {
            if (other.payload != null) return false;
        } else if (!this.getPayload().equals(other.getPayload())) return false;

        return true;
    }

    private RaptureURI uri = null;
    private String mimeType = MediaType.ANY_TYPE.toString();

    public AbstractUpdateObject() {
    }

    public AbstractUpdateObject(RaptureURI uri) {
        this.uri = uri;
    }

    public AbstractUpdateObject(RaptureURI uri, String mimeType) {
        this.uri = uri;
        this.mimeType = mimeType;
    }

    public RaptureURI getUri() {
        return uri;
    }

    public void setUri(RaptureURI uri) {
        this.uri = uri;
    }

    abstract public T getPayload();

    abstract public void setPayload(T payload);

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
