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
package rapture.repo;

import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.blob.BlobStore;
import rapture.blob.file.FileBlobStore;
import rapture.common.CallingContext;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.BlobApiImpl;

/**
 * Created by seanchen on 6/30/15.
 */
public class BlobRepo {

    private BlobStore store;
    private Repository metaDataRepo;
    private static Logger logger = Logger.getLogger(BlobApiImpl.class);

    public BlobRepo(BlobStore store, Repository metaDataRepo) {
        this.store = store;
        this.metaDataRepo = metaDataRepo;
    }

    public Boolean storeBlob(CallingContext context, RaptureURI blobUri, Boolean append, InputStream content) {
        // TODO RFS-3141 Add Graphite code here
        return store.storeBlob(context, blobUri, append, content);
    }

    public Boolean deleteBlob(CallingContext context, RaptureURI blobUri) {
        return store.deleteBlob(context, blobUri);
    }

    public Boolean deleteBlobRepo(CallingContext context, RaptureURI blobUri) {
        if (blobUri.hasDocPath()) {
            logger.warn("URI should not have a doc path "+blobUri);
            return false;
        }

        return store.deleteRepo();
    }

    public InputStream getBlob(CallingContext context, RaptureURI blobUri) {
        return store.getBlob(context, blobUri);
    }

    public InputStream getBlobPart(CallingContext context, RaptureURI blobUri, Long start, Long size) {
        return store.getBlobPart(context, blobUri, start, size);
    }

    public Long getBlobSize(CallingContext context, RaptureURI blobUri) {
        return store.getBlobSize(context, blobUri);
    }

    public DocumentWithMeta putMeta(String metaUri, String value, String user, String comment, boolean mustBeNew) {
        return metaDataRepo.addDocument(metaUri, value, user, comment, mustBeNew);
    }

    public boolean deleteMeta(String metaUri, String user, String comment){
        return metaDataRepo.removeDocument(metaUri, user, comment);
    }

    public String getMeta(String metaUri) {
        return metaDataRepo.getDocument(metaUri);
    }

    public List<RaptureFolderInfo> listMetaByUriPrefix(String blobUriPrefix) {
        return metaDataRepo.getChildren(blobUriPrefix);
    }

    public String getStoreType() {
        return store.getClass().getSimpleName();
    }

}
