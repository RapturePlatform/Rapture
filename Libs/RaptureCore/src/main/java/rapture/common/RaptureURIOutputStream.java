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
    CallingContext context = ContextFactory.getAnonymousUser();

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

    public RaptureURIOutputStream setContext(CallingContext ctxt) {
        context = ctxt;
        return this;
    }

    boolean flushed = false;

    @Override
    public void flush() throws IOException {
        switch (uri.getScheme()) {
        default:
            throw new IllegalArgumentException(uri.getScheme() + " not supported");
        case BLOB:
            Kernel.getBlob().putBlob(context, uri.toString(), super.toByteArray(), mediaType.toString());
            break;
        case DOCUMENT:
            Kernel.getDoc().putDoc(context, uri.toString(), super.toString());
            break;
        }
        flushed = true;
    }

    @Override
    public void close() throws IOException {
        if (!flushed && (count > 0)) {
            flush();
        }
        super.close();
    }
}
