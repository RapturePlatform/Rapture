/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
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
package rapture.kernel.plugin;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;

public class BlobEncoder extends ReflectionEncoder {
    protected Scheme scheme;

    public BlobEncoder() {
        super();
        scheme = Scheme.BLOB;
    }

    @Override
    public Object getReflectionObject(CallingContext ctx, String uri) {
        if (isRepository(uri)) return Kernel.getBlob().getBlobRepoConfig(ctx, uri);
        return Kernel.getBlob().getBlob(ctx, uri);
    }

    @Override
    public PluginTransportItem encode(CallingContext context, String uriString) {
        RaptureURI uri = new RaptureURI(uriString, scheme);
        return uri.hasDocPath() ? encodeRaw(context, uri) : super.encode(context, uriString);
    }

    protected BlobContainer getBlob(CallingContext context, RaptureURI uri) {
        return Kernel.getBlob().getBlob(context, uri.withoutAttribute().toString());
    }

    protected PluginTransportItem encodeRaw(CallingContext context, RaptureURI uri) {
        BlobContainer blob = getBlob(context, uri);
        if (blob == null) return null;
        String mimeType = getMimeType(blob);
        RaptureURI revisedUri = RaptureURI.builder(uri).attribute(mimeType).build();
        PluginTransportItem result = new PluginTransportItem();
        result.setUri(revisedUri.toString());
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(blob.getContent());
            result.setHash(Hex.encodeHexString(md.digest()));
            result.setContent(blob.getContent());
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create("JRE does not support MD5");
        }
        return result;
    }

    private String getMimeType(BlobContainer blob) {
        return blob.getHeaders().get(ContentEnvelope.CONTENT_TYPE_HEADER);
    }

    protected static final boolean isRepository(String uri) {
        return StringUtils.isEmpty(new RaptureURI(uri).getDocPath());
    }
}
