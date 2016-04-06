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
package rapture.blob.mongodb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.LockHandle;
import rapture.common.exception.ExceptionToString;
import rapture.kernel.Kernel;
import rapture.kernel.LockApiImpl;
import rapture.mongodb.MongoDBFactory;

import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class GridFSBlobHandler implements BlobHandler {
    private static final Logger log = Logger.getLogger(GridFSBlobHandler.class);

    private String instanceName;
    private String bucket;

    public GridFSBlobHandler(String instanceName, String bucket) {
        this.instanceName = instanceName;
        this.bucket = bucket;
    }

    // GridFS support for MongoDatabase did not make it into 3.0 so we still need to use the deprecated call
    // Should be available in 3.1
    public GridFS getGridFS() {
        DB db = MongoDBFactory.getDB(instanceName);
        return new GridFS(db, bucket);
    }

    @Override
    public Boolean storeBlob(CallingContext context, String docPath, InputStream newContent, Boolean append) {
        GridFS gridFS = getGridFS();
        GridFSInputFile file;
        if (!append) {
            gridFS.remove(docPath);
            file = createNewFile(docPath, newContent);
        } else {
            GridFSDBFile existing = gridFS.findOne(docPath);
            if (existing != null) {
                try {
                    file = updateExisting(context, docPath, newContent, gridFS, existing);
                } catch (IOException e) {
                    file = null;
                    log.error(String.format("Error while appending to docPath %s: %s", docPath, ExceptionToString.format(e)));
                }

            } else {
                file = createNewFile(docPath, newContent);
            }
        }
        return file != null;
    }

    protected GridFSInputFile updateExisting(CallingContext context, String docPath, InputStream newContent, GridFS gridFS, GridFSDBFile existing)
            throws IOException {
        GridFSInputFile file;
        String lockKey = createLockKey(gridFS, docPath);
        LockHandle lockHandle = grabLock(context, lockKey);
        try {
            File tempFile = File.createTempFile("rapture", "blob");
            existing.writeTo(tempFile);
            FileInputStream tempIn = FileUtils.openInputStream(tempFile);
            SequenceInputStream sequence = new SequenceInputStream(tempIn, newContent);
            try {
                gridFS.remove(docPath);
                file = createNewFile(docPath, sequence);
                if (!tempFile.delete()) {
                    log.warn(String.format("Unable to delete temp file created while appending docPath %s, at %s", docPath, tempFile.getAbsolutePath()));
                }
            } finally {
                try {
                    sequence.close();
                } catch (IOException e) {
                    log.error(String.format("Error closing sequence input stream: %s", ExceptionToString.format(e)));
                }
                try {
                    tempIn.close();
                } catch (IOException e) {
                    log.error(String.format("Error closing sequence input stream: %s", ExceptionToString.format(e)));
                }
            }
        } finally {
            releaseLock(context, lockKey, lockHandle);
        }
        return file;
    }

    private void releaseLock(CallingContext context, String lockKey, LockHandle lockHandle) {
        Kernel.getLock().getTrusted().releaseLock(context, LockApiImpl.KERNEL_MANAGER_URI.toString(), lockKey, lockHandle);
    }

    private LockHandle grabLock(CallingContext context, String lockKey) {
        long secondsToWait = 5;
        long secondsToKeep = 60;
        return Kernel.getLock().getTrusted()
                .acquireLock(context, LockApiImpl.KERNEL_MANAGER_URI.toString(), lockKey, secondsToWait, secondsToKeep);
    }

    private String createLockKey(GridFS gridFS, String docPath) {
        return gridFS.getDB().getName() + "." + gridFS.getBucketName() + "." + docPath;
    }

    protected GridFSInputFile createNewFile(String docPath, InputStream content) {
        GridFSInputFile file = getGridFS().createFile(content, docPath);
        if (file != null) {
            file.save();
        }
        return file;
    }

    @Override
    public Boolean deleteBlob(CallingContext context, String docPath) {
        GridFS gridFS = getGridFS();
        String lockKey = createLockKey(gridFS, docPath);
        LockHandle lockHandle = grabLock(context, lockKey);
        boolean retVal = false;
        try {
            if (gridFS.findOne(docPath) != null) {
                gridFS.remove(docPath);
                retVal = true;
            }
        } finally {
            releaseLock(context, lockKey, lockHandle);
        }
        return retVal;
    }

    @Override
    public InputStream getBlob(CallingContext context, String docPath) {
        GridFS gridFS = getGridFS();
        String lockKey = createLockKey(gridFS, docPath);
        LockHandle lockHandle = grabLock(context, lockKey);
        InputStream retVal = null;
        try {
            GridFSDBFile file = gridFS.findOne(docPath);
            if (file != null) {
                retVal = file.getInputStream();
            }
        } finally {
            releaseLock(context, lockKey, lockHandle);
        }
        return retVal;
    }

}
