package rapture.common;

import com.google.common.net.MediaType;

import rapture.common.model.DocumentWithMeta;

public class DocUpdateObject extends AbstractUpdateObject<DocumentWithMeta> {

    public DocUpdateObject() {
    }

    DocumentWithMeta payload;

    public DocUpdateObject(RaptureURI uri) {
        super(uri);
        assert (uri.getScheme() == Scheme.DOCUMENT);
        setMimeType(MediaType.JSON_UTF_8.toString());
    }

    public DocUpdateObject(RaptureURI uri, DocumentWithMeta meta) {
        this(uri);
        setPayload(meta);
        setMimeType(MediaType.JSON_UTF_8.toString());
    }

    public DocUpdateObject(DocumentWithMeta meta) {
        // Slightly messy but Java is picky about calling code before
        // constructors
        this(new RaptureURI((meta.getMetaData().getSemanticUri().length() > 0) ? meta.getMetaData().getSemanticUri() : meta.getDisplayName(), Scheme.DOCUMENT));
        setPayload(meta);
        setMimeType(MediaType.JSON_UTF_8.toString());
    }

    @Override
    public void setPayload(DocumentWithMeta meta) {
        payload = meta;
    }

    @Override
    public DocumentWithMeta getPayload() {
        return payload;
    }
}
