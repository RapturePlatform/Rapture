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
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import rapture.common.*;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.Kernel;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class SheetEncoder implements RaptureEncoder {

    @Override
    public PluginTransportItem encode(CallingContext ctx, String uriStr) {
        RaptureURI uri = new RaptureURI(uriStr, Scheme.SHEET);
        return uri.hasAttribute() ? encodeRaw(ctx, uri) : encodePlain(ctx, uri);
    }

    private PluginTransportItem encodePlain(CallingContext ctx, RaptureURI uri) {
        String uriString = uri.toString(); // normalized
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            PluginTransportItem item = new PluginTransportItem();
            if (isRepository(uri)) {
                SheetRepoConfig config = Kernel.getSheet().getSheetRepoConfig(ctx, uriString);
                byte[] content = JacksonUtil.bytesJsonFromObject(config);
                item.setContent(content);
            } else {
                item.setContent(Preconditions.checkNotNull(Kernel.getSheet().exportSheetAsScript(ctx, uriString)).getBytes(Charsets.UTF_8));
            }
            md.update(item.getContent());
            item.setHash(Hex.encodeHexString(md.digest()));
            item.setUri(uriString);
            return item;
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error encoding sheet at uri " + uri, e);
        }
    }

    private PluginTransportItem encodeRaw(CallingContext ctx, RaptureURI uri) {
        int maxRow = 0;
        int maxCol = 0;
        Table<Integer, Integer, String> values = HashBasedTable.create();
        List<RaptureSheetCell> cells = Kernel.getSheet().findCellsByEpoch(ctx, uri.toShortString(), 0, 0L);
        for (RaptureSheetCell cell : cells) {
            int row = cell.getRow();
            int col = cell.getColumn();
            if (row > maxRow) maxRow = row;
            if (col > maxCol) maxCol = col;
            values.put(row, col, cell.getData());
        }
        StringBuilder buf = new StringBuilder();

        for (int row = 1; row <= maxRow; row++) {
            for (int col = 1; col < maxCol; col++) {
                String value = values.get(row, col);
                buf.append(escape(value));
                buf.append(",");
            }
            buf.append(escape(values.get(row, maxCol)));
            buf.append("\n");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            PluginTransportItem result = new PluginTransportItem();
            result.setContent(buf.toString().getBytes(Charsets.UTF_8));
            md.update(result.getContent());
            result.setHash(Hex.encodeHexString(md.digest()));
            result.setUri(uri.toString());
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error encoding sheet at uri " + uri, e);
        }
    }

    private static final boolean isRepository(RaptureURI uri) {
        return StringUtils.isEmpty(uri.getDocPath());
    }

    private static String escape(String in) {
        return StringEscapeUtils.escapeCsv(in);
    }
}
