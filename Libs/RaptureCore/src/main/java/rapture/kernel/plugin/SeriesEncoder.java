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

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureURI;
import rapture.common.SeriesRepoConfig;
import rapture.common.SeriesPoint;
import rapture.common.api.SeriesApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

import com.google.common.base.Charsets;

/**
 * This is a simple encoder for series where it is safe to materialize the
 * entire contents during loading. A streaming version will be required later
 * for large data sizes
 *
 * @author mel
 */
public class SeriesEncoder implements RaptureEncoder {
    @Override
    public PluginTransportItem encode(CallingContext ctx, String uri) {
        try {
            PluginTransportItem item = new PluginTransportItem();
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (isRepository(uri)) {
                SeriesRepoConfig config = Kernel.getSeries().getSeriesRepoConfig(ctx, uri);
                byte[] content = JacksonUtil.bytesJsonFromObject(config);
                item.setContent(content);
            } else {
                StringBuilder buf = new StringBuilder();
                SeriesApi api = Kernel.getSeries();
                for (SeriesPoint val : api.getPoints(ctx, uri)) {
                    // TODO MEL encapsulate newlines in data value
                    buf.append(val.getColumn());
                    buf.append(",");
                    buf.append(val.getValue());
                    buf.append("\n");
                }
                item.setContent(buf.toString().getBytes(Charsets.UTF_8));
            }
            md.update(item.getContent());
            item.setHash(Hex.encodeHexString(md.digest()));
            item.setUri(uri);
            return item;
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error encoding series", e);
        }
    }

    private static final boolean isRepository(String uri) {
        return StringUtils.isEmpty(new RaptureURI(uri).getDocPath());
    }
}
