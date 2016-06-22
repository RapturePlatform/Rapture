/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
