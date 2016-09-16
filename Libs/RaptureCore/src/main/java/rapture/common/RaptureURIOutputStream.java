package rapture.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.common.net.MediaType;

import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

// TODO ByteArrayOutputStream means we hold the whole thing in memory.
// That's OK for a first pass, but we really need to make this a proper stream

public class RaptureURIOutputStream extends ByteArrayOutputStream {

    RaptureURI uri;
    MediaType mediaType = MediaType.ANY_TYPE;

    public RaptureURIOutputStream(RaptureURI uri) {
        super();
        this.uri = uri;

        switch (uri.getScheme()) {
        default:
            throw new IllegalArgumentException(uri.getScheme() + " not supported");
        case BLOB:
        case DOCUMENT:
            // Nothing to do yet
        }
    }

    public RaptureURIOutputStream setMediaType(MediaType type) {
        mediaType = type;
        return this;
    }

    @Override
    public void close() throws IOException {
        switch (uri.getScheme()) {
        default:
            throw new IllegalArgumentException(uri.getScheme() + " not supported");
        case BLOB:
            Kernel.getBlob().putBlob(ContextFactory.getAnonymousUser(), uri.toString(), super.toByteArray(), mediaType.toString());
            break;
        case DOCUMENT:
            Kernel.getDoc().putDoc(ContextFactory.getAnonymousUser(), uri.toString(), super.toString());
            break;
        }
        super.close();
    }
}
