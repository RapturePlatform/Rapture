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
package rapture.blob.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.blob.BaseBlobStore;
import rapture.blob.BlobStore;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.file.FileRepoUtils;

/**
 * A file blob store treats files as blobs, with the path of the file reflecting
 * the display name of the object. In a cloud installation the file system needs
 * to be shared and have lock semantics.
 * 
 * @author amkimian
 * 
 */
public class FileBlobStore extends BaseBlobStore implements BlobStore {

    private static Logger logger = Logger.getLogger(FileBlobStore.class);
    public static final String CREATE_SYM_LINK = "createSymLink";
    private File parentDir = null;
    private boolean createSymLink;

    public FileBlobStore() {
    }

    @Override
    public Boolean storeBlob(CallingContext context, RaptureURI blobUri, Boolean append, InputStream content) {
        if(createSymLink) {
            // the display name is either a RaptureURI's docPath or docPathWithElement
            // if the display name contains an element (starts with #), then that element is the local file path
            // create a sym link to that local path
            //
            // an example blobUri will be like
            // app/admin/blob.html#/Curtis/CurtisAdmin/FEATURE/content/curtisweb/app/admin/blob.html
            int index = blobUri.getDocPathWithElement().indexOf("#");
            if(index > -1) {
                String filePath = blobUri.getDocPathWithElement().substring(index+1);
                String docPath = blobUri.getDocPathWithElement().substring(0, index);
                return createSymLink(docPath, filePath);
            }
        }

        try {
            File f = FileRepoUtils.makeGenericFile(parentDir, blobUri.getDocPathWithElement());
            if (f.isDirectory())
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, f.getCanonicalPath()+" exists and is a directory");
            FileUtils.forceMkdir(f.getParentFile());
            try (FileOutputStream fos = new FileOutputStream(f, append)) {
                IOUtils.copy(content, fos);
                fos.close(); // this shouldn't be necessary; the try is supposed to close it.
            }
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error storing blob into file", e);
        }
        return true;
    }

    private Boolean createSymLink(String fromDisplayName, String toFilePath) {
        try {
            File fromFile = FileRepoUtils.makeGenericFile(parentDir, fromDisplayName);
            Path fromFilePath = Paths.get(fromFile.getAbsolutePath());
            Files.deleteIfExists(fromFilePath);
            FileUtils.forceMkdir(fromFilePath.getParent().toFile());
            Files.createSymbolicLink(fromFilePath, Paths.get(toFilePath));
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Fail to read blob content", e);
        }
        return true;
    }

    @Override
    public Boolean deleteBlob(CallingContext context, RaptureURI blobUri) {
        File f = FileRepoUtils.makeGenericFile(parentDir, blobUri.getDocPath());
        if (f.exists()) {
            try {
                java.nio.file.Files.delete(f.toPath());
                return true;
            } catch (IOException e) {
                logger.error("Unable to delete "+blobUri.getDocPath()+" because "+e.getMessage());
            }
        } else {
            logger.error("Blob doesn't exist "+blobUri.getDocPath());
        }
        return false;
    }

    @Override
    public Boolean deleteRepo() {
        if (parentDir.list().length > 0){
            logger.error("Unable to delete "+parentDir.toPath()+" because it is not empty");
            return false;
        }
            
        try {
            java.nio.file.Files.delete(parentDir.toPath());
        } catch (IOException e) {
            logger.error("Unable to delete "+parentDir.toPath()+" because "+e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public InputStream getBlob(CallingContext context, RaptureURI blobUri) {
        File f = FileRepoUtils.makeGenericFile(parentDir, blobUri.getDocPath());
        try {
            if (f.exists()) {
                return new FileInputStream(f);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading blob from file", e);
        }
    }

    @Override
    public void setConfig(Map<String, String> config) {
        // What happens if this is called twice?
        if (parentDir != null) throw RaptureExceptionFactory.create("Calling setConfig twice is currently not supported");
        String prefix = config.get(FileRepoUtils.PREFIX);
        if (StringUtils.trimToNull(prefix) == null) 
            throw RaptureExceptionFactory.create("prefix must be specified");
        parentDir = FileRepoUtils.ensureDirectory(prefix + "_blob");
        createSymLink = Boolean.valueOf(config.get(CREATE_SYM_LINK));
    }

    @Override
    public void init() {
    }
}
