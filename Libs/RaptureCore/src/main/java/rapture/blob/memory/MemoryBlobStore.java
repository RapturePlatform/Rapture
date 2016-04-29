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
package rapture.blob.memory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;

import rapture.blob.BaseBlobStore;
import rapture.blob.BlobStore;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;

/**
 * A trivial implementation of the BlobStore api that should not be used in
 * production.
 * 
 * @author amkimian
 * 
 */
public class MemoryBlobStore extends BaseBlobStore implements BlobStore {

    private Map<String, byte[]> contentMap = new ConcurrentHashMap<String, byte[]>();

    @Override
    public Boolean storeBlob(CallingContext context, RaptureURI blobUri, Boolean append, InputStream content) {

        byte[] contentAsBytes;
        try {
            contentAsBytes = IOUtils.toByteArray(content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (append && contentMap.containsKey(blobUri.getDocPath())) {
            byte[] originalContent = contentMap.get(blobUri.getDocPath());
            byte[] newContent = Arrays.copyOf(originalContent, originalContent.length + contentAsBytes.length);
            // TODO: ArrayUtils instead?
            System.arraycopy(contentAsBytes, 0, newContent, originalContent.length, contentAsBytes.length);
            contentMap.put(blobUri.getDocPath(), newContent);
        } else {
            contentMap.put(blobUri.getDocPath(), contentAsBytes);
        }
        return true;
    }

    @Override
    public Boolean deleteBlob(CallingContext context, RaptureURI blobUri) {
        if (contentMap.containsKey(blobUri.getDocPath())) {
            contentMap.remove(blobUri.getDocPath());
            return true;
        }
        return false;
    }

    @Override
    public Boolean deleteFolder(CallingContext context, RaptureURI blobUri) {
    	// Memory folders automatically cease to exist when the blob is deleted.
    	// Return false because nothing was deleted, not because there was a problem
        return false;
    }

    @Override
    public InputStream getBlob(CallingContext context, RaptureURI blobUri) {
        if (contentMap.containsKey(blobUri.getDocPath())) {
            return new ByteArrayInputStream(contentMap.get(blobUri.getDocPath()));
        }
        return null;
    }

    @Override
    public void setConfig(Map<String, String> config) {
    }

    @Override
    public void init() {
        // nothing to do
    }

    @Override
    public Boolean deleteRepo() {
        return true;
    }

}
