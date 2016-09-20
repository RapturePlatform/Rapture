package rapture.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

// TODO ByteArrayInputStream means we hold the whole thing in memory.
// That's OK for a first pass, but we really need to make this a proper stream

public class RaptureURIInputStream extends ByteArrayInputStream {

    RaptureURI uri;

    private static byte[] bytesFromUri(RaptureURI uri) {
        switch (uri.getScheme()) {
        default:
            throw new IllegalArgumentException(uri.getScheme() + " not supported");
        case BLOB:
            BlobContainer bc = Kernel.getBlob().getBlob(ContextFactory.getAnonymousUser(), uri.toString());
            return bc.getContent();
        case DOCUMENT:
            return Kernel.getDoc().getDoc(ContextFactory.getAnonymousUser(), uri.toString()).getBytes();
        }
    }

    public RaptureURIInputStream(RaptureURI uri) {
        super(bytesFromUri(uri));
        this.uri = uri;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
