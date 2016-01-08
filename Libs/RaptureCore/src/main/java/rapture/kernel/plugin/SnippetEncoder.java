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
package rapture.kernel.plugin;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

import com.google.common.base.Charsets;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureSnippet;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;

public class SnippetEncoder extends ReflectionEncoder {
    @Override
    public Object getReflectionObject(CallingContext ctx, String uri) {
        return Kernel.getScript().getSnippet(ctx, uri);
    }

    @Override
    public PluginTransportItem encode(CallingContext context, String uriString) {
        RaptureURI uri = new RaptureURI(uriString, Scheme.SNIPPET);
        return uri.hasAttribute() ? encodeRaw(context, uri) : super.encode(context, uriString);
    }

    private PluginTransportItem encodeRaw(CallingContext context, RaptureURI uri) {
        String uriString = uri.toString();
        PluginTransportItem result = new PluginTransportItem();
        RaptureSnippet snippet = Kernel.getScript().getSnippet(context, uriString);
        String text = snippet.getSnippet();
        result.setContent(text.getBytes(Charsets.UTF_8));
        result.setUri(uriString);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(text.getBytes("UTF-8"));
            result.setHash(Hex.encodeHexString(md.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create("JRE does not support MD5");
        } catch (UnsupportedEncodingException e) {
            throw RaptureExceptionFactory.create("JRE does not support UTF-8");
        }
        return result;
    }
}
