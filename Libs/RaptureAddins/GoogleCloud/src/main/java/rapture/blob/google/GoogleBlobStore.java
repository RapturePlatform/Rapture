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
package rapture.blob.google;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.cloud.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

import rapture.blob.BaseBlobStore;
import rapture.blob.BlobStore;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.config.MultiValueConfigLoader;

/**
 * GoogleBlobStore uses the Google Cloud Storage to implement a BlobStore Google has the concept of Buckets which store blobs. At first pass it seems logical to
 * map Repositories to Buckets; will see if that holds up. The downside of this is that buckets are globally named, so we'll need a prefix that guarantees all
 * our repositories are our own. Bucket names containing dots require verification which we will need to do at some point.
 * 
 * @author Dave Tong
 * 
 */
public class GoogleBlobStore extends BaseBlobStore implements BlobStore {

    private static Logger logger = Logger.getLogger(GoogleBlobStore.class);

    static final String INCAPTURE = "incapture_"; // MUST BE LOWER CASE
    private Bucket bucket = null;
    private Storage storage = null;

    public GoogleBlobStore() {
    }

    @Override
    public Boolean storeBlob(CallingContext context, RaptureURI blobUri, Boolean append, InputStream content) {
        // Google cloud storage objects are immutable.
        // Not sure what happens to the old blob, if it's deleted.
        // But we don't want to delete the old until after the new is written
        String key = blobUri.getDocPath();
        Blob blob = bucket.get(key);
        if (blob != null) {
            if (append) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    baos.write(blob.getContent());
                    IOUtils.copy(content, baos);
                    bucket.create(key, baos.toByteArray());
                    return true;
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }
            System.out.println("Overwriting bucket " + bucketName + " Key " + key);
        }
        Blob x = bucket.create(key, content);
        System.out.println("Wrote to bucket " + bucketName + " with key " + key);
        return true;
    }

    public Collection<Blob> listBlobs(String prefix) {
        Page<Blob> blobs = bucket.list();
        Iterator<Blob> iter = blobs.iterateAll();
        List<Blob> list = new ArrayList<>();

        while (iter.hasNext()) {
            Blob blob = iter.next();
            if (blob.getName().startsWith(prefix)) {
                list.add(blob);
            }
        }
        return list;
    }

    public Boolean storeBlob(String key, Boolean append, InputStream content) {
        // Google cloud storage objects are immutable.
        // Not sure what happens to the old blob, if it's deleted.
        // But we don't want to delete the old until after the new is written
        Blob blob = bucket.get(key);
        if (blob != null) {
            if (append) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    baos.write(blob.getContent());
                    IOUtils.copy(content, baos);
                    Blob x = bucket.create(key, baos.toByteArray());
                    return true;
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }
            System.out.println("Overwriting blob " + key + " in bucket " + bucketName + " value " + new String(blob.getContent()));
        }

        Blob x = bucket.create(key, content);
        System.out.println("Wrote to bucket " + bucketName + " Value " + new String(x.getContent()) + " with key " + key);

        return true;
    }

    @Override
    public Boolean deleteBlob(/* unused */CallingContext context, RaptureURI blobUri) {
        if (bucket == null) return false;
        Blob blob = bucket.get(blobUri.getDocPath());
        if (blob == null) return false;
        return blob.delete();
    }

    public Boolean deleteBlob(String key) {
        if (bucket == null) return false;
        Blob blob = bucket.get(key);
        if (blob == null) return false;
        return blob.delete();
    }

    /**
     * Delete folder only if empty
     */
    @Override
    public Boolean deleteFolder(CallingContext context, RaptureURI blobUri) {
        return false; // MongoDBBlobStore doesn't implement it either
    }

    @Override
    public Boolean deleteRepo() {
        String name = bucket.getName();
        boolean success = storage.delete(name);
        if (success) bucket = null;
        return success;
    }

    @Override
    public InputStream getBlob(CallingContext context, RaptureURI blobUri) {
        if (bucket == null) return null;

        // Google cloud storage objects are immutable.
        // Not sure what happens to the old blob, if it's deleted.
        // But we don't want to delete the old until after the new is written
        Blob blob = bucket.get(blobUri.getDocPath());
        if (blob == null) return null;
        // System.out.println("DEBUG blob " + blobUri.toString() + " is " + new String(blob.getContent()));
        return new ByteArrayInputStream(blob.getContent());
    }

    public InputStream getBlob(String key) {
        if (bucket == null) return null;

        // Google cloud storage objects are immutable.
        // Not sure what happens to the old blob, if it's deleted.
        // But we don't want to delete the old until after the new is written
        Blob blob = bucket.get(key);
        if (blob == null) return null;
        // System.out.println("DEBUG blob " + key + " is " + new String(blob.getContent()));

        return new ByteArrayInputStream(blob.getContent());
    }

    public boolean blobExists(CallingContext context, RaptureURI blobUri) {
        if (bucket == null) return false;
        return (bucket.get(blobUri.getDocPath()) != null);
    }

    String bucketName = null;

    @Override
    public void setConfig(Map<String, String> config) {
        String prefix = StringUtils.trimToNull(config.get("prefix"));
        if (prefix == null) {
            throw new RuntimeException("Prefix not set in " + config);
        }

        String projectId = StringUtils.trimToNull(config.get("projectid"));
        if (projectId == null) {
            projectId = MultiValueConfigLoader.getConfig("GOOGLE-projectId");
            if (projectId == null) {
                throw new RuntimeException("Project ID not set in RaptureGOOGLE.cfg or in config " + config);
            }
        }

        storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        // NOTE cannot currently use a full stop in a bucket name.
        bucketName = INCAPTURE + prefix.replaceAll("\\.", "").toLowerCase();
        try {
            bucket = storage.get(bucketName);
        } catch (StorageException e) {
            System.out.println("Cannot find bucket " + bucketName);
            e.printStackTrace();
        }
        if (bucket == null) bucket = storage.create(BucketInfo.of(bucketName));
    }

    @Override
    public void init() {
    }

    // For cleanup after testing
    void destroyBucket(String name) {
        String bName = INCAPTURE + name.replaceAll("\\.", "").toLowerCase();
        Bucket bukkit = storage.get(bName);
        if (bukkit != null) {
            for (Blob blob : this.listBlobs(name)) {
                storage.delete(blob.getBlobId());
            }
            try {
                storage.delete(bName);
            } catch (Exception e) {
                System.err.println("Cannot delete bucket " + name + " : " + e.getMessage());
            }
        }
    }
}
