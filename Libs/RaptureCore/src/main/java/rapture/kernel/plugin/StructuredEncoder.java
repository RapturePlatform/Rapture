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

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

import com.google.common.base.Charsets;

/**
 * Encoder for structured store schemas and tables
 * 
 * @author dukenguyen
 * 
 */
public class StructuredEncoder implements RaptureEncoder {

    @Override
    public PluginTransportItem encode(CallingContext ctx, String uriStr) {
        PluginTransportItem item = new PluginTransportItem();
        item.setUri(uriStr);
        StringBuilder contents = new StringBuilder();
        if (isRepository(uriStr)) {
            contents.append(JacksonUtil.jsonFromObject(Kernel.getStructured().getStructuredRepoConfig(ctx, uriStr)));
            contents.append("\n");
        }
        contents.append(Kernel.getStructured().getDdl(ctx, uriStr, true));
        item.setContent(contents.toString().getBytes(Charsets.UTF_8));
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(item.getContent());
            item.setHash(Hex.encodeHexString(md.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "MD5 message digest not installed in JVM.  Can't encode plugin", e);
        }
        return item;
    }

    private boolean isRepository(String uri) {
        return StringUtils.isBlank(new RaptureURI(uri).getDocPath());
    }
}