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
package rapture.plugin.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * @author bardhi
 * @since 9/11/14.
 */
public class PluginContentReader {
    private static final Logger log = Logger.getLogger(PluginContentReader.class);

    public static byte[] readFromFile(File file, MessageDigest md) throws IOException {
        DigestInputStream is = new DigestInputStream(new FileInputStream(file), md);

        return readFromStream(is);
    }
    
    public static byte[] readFromStreamWithDigest(InputStream is, MessageDigest md) throws IOException {
        DigestInputStream dis = new DigestInputStream(is, md);

        return readFromStream(dis);
        
    }

    public static byte[] readFromZip(ZipFile zip, ZipEntry entry, MessageDigest md) throws IOException {
        DigestInputStream is = new DigestInputStream(zip.getInputStream(entry), md);

        return readFromStream(is);
    }

    public static byte[] readFromStream(InputStream is) throws IOException {
        try {
            byte[] bytes = IOUtils.toByteArray(is);
            is.close();
            log.debug("bytes are " + bytes.length);
            return bytes;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
