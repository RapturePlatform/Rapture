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
package rapture.repo.file;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.kernel.file.FileRepoUtils;
import rapture.repo.AbstractKeyStore;
import rapture.repo.KeyStore;
import rapture.repo.RepoLockHandler;
import rapture.repo.RepoVisitor;
import rapture.repo.StoreKeyVisitor;
import rapture.table.file.FileIndexHandler;

public class FileDataStore extends AbstractKeyStore implements KeyStore {
    private static final Logger log = Logger.getLogger(FileDataStore.class);
    private static final String EXTENSION = ".txt";
    private static final String SLASH = "/";
    private File parentDir = null;
    private String folderPrefix = null;
    private static final String CREATE_SYM_LINK = "createSymLink";
    private boolean createSymLink = false;

    public FileDataStore() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#setRepoLockHandler(rapture.repo.
     * RepoLockHandler)
     */
    @Override
    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {
        // TODO Auto-generated method stub
        super.setRepoLockHandler(repoLockHandler);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#delete(java.util.List)
     */
    @Override
    public boolean delete(List<String> keys) {
        // TODO Auto-generated method stub
        return super.delete(keys);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#dropKeyStore()
     */
    @Override
    public boolean dropKeyStore() {
        // TODO Auto-generated method stub
        return super.dropKeyStore();
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#getBatch(java.util.List)
     */
    @Override
    public List<String> getBatch(List<String> keys) {
        // TODO Auto-generated method stub
        return super.getBatch(keys);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * rapture.repo.AbstractKeyStore#runNativeQueryWithLimitAndBounds(java.lang.
     * String, java.util.List, int, int)
     */
    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        // TODO Auto-generated method stub
        return super.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#visit(java.lang.String,
     * rapture.repo.RepoVisitor)
     */
    @Override
    public void visit(String folderPrefix, RepoVisitor iRepoVisitor) {
        // TODO Auto-generated method stub
        super.visit(folderPrefix, iRepoVisitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see rapture.repo.AbstractKeyStore#matches(java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean matches(String key, String value) {
        // TODO Auto-generated method stub
        return super.matches(key, value);
    }

    @Override
    public boolean containsKey(String ref) {
        return FileRepoUtils.makeGenericFile(parentDir, convertKeyToPathWithExtension(ref)).canRead();
    }

    private String convertKeyToPath(String key) {
        return key;
        // return key.substring(0, 3) + "/" + key.substring(3);
    }

    private String convertKeyToPathWithExtension(String key) {
        return key + EXTENSION;
    }

    private String removeExtension(String name) {
        if (name.endsWith(EXTENSION)) {
            return name.substring(0, name.length() - EXTENSION.length());
        }
        return name;
    }

    @Override
    public long countKeys() throws RaptNotSupportedException {
        throw new RaptNotSupportedException("Not yet supported");
    }

    @Override
    public KeyStore createRelatedKeyStore(String relation) {
        Map<String, String> config = new HashMap<String, String>();
        config.put(FileRepoUtils.PREFIX, folderPrefix + "_" + relation);
        KeyStore related = new FileDataStore();
        related.setConfig(config);
        return related;
    }

    @Override
    public boolean delete(String key) {
        File f = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPathWithExtension(key));
        return FileUtils.deleteQuietly(f);
    }

    @Override
    public String get(String k) {
        File f = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPathWithExtension(k));
        try {
            return FileUtils.readFileToString(f);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getStoreId() {
        return folderPrefix;
    }

    @Override
    public void put(String k, String v) {
        // A key is a file name, the value is the contents
        if (createSymLink) {
            int index = k.indexOf("#");
            if (index > -1) {
                createSymLink(k.substring(0, index), k.substring(index + 1));
                return;
            }
        }

        File f = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPathWithExtension(k));
        try {
            FileUtils.writeStringToFile(f, v);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error writing to file", e);
        }
    }

    private void createSymLink(String k, String filePath) {
        Path originalFile = Paths.get(filePath);
        File fromFile = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPathWithExtension(k));
        Path fromFilePath = Paths.get(fromFile.getAbsolutePath());
        try {
            Files.deleteIfExists(fromFilePath);
            FileUtils.forceMkdir(fromFilePath.getParent().toFile());
            Files.createSymbolicLink(fromFilePath, originalFile);
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error creating sym link", e);
        }
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        if (repoType.toUpperCase().equals("FILE")) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not yet implemented");
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "RepoType mismatch. Repo is of type FILE, asked for " + repoType);
        }
    }

    @Override
    public void setConfig(Map<String, String> config) {
        if (parentDir != null) throw RaptureExceptionFactory.create("Calling setConfig twice is currently not supported");
        folderPrefix = config.get(FileRepoUtils.PREFIX);
        if (StringUtils.trimToNull(folderPrefix) == null) throw RaptureExceptionFactory.create("prefix must be specified");
        parentDir = FileRepoUtils.ensureDirectory(folderPrefix);
        createSymLink = Boolean.valueOf(config.get(CREATE_SYM_LINK));
    }

    @Override
    public void visitKeys(String prefix, StoreKeyVisitor iStoreKeyVisitor) {
        File dir = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath(prefix));
        if (!dir.exists()) {
            try {
                File file = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath(prefix) + EXTENSION);
                if (file.exists()) {
                    iStoreKeyVisitor.visit(prefix + removeExtension(file.getName()), FileUtils.readFileToString(file));
                }
            } catch (IOException e) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not read keys", e);
            }
            return;
        }
        Iterator<File> fIterator = FileUtils.iterateFiles(dir, null, true);
        while (fIterator.hasNext()) {
            File f = fIterator.next();
            if (f.isFile()) {
                try {
                    if (!iStoreKeyVisitor.visit(prefix + removeExtension(f.getName()), FileUtils.readFileToString(f))) break;
                } catch (IOException e) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not read keys", e);
                }
            }
        }
    }

    @Override
    public void visitKeysFromStart(String startPoint, StoreKeyVisitor iStoreKeyVisitor) {
        File dir = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath(""));
        if (!dir.exists()) {
            return;
        }
        boolean canStart = startPoint == null;
        Iterator<File> fIterator = FileUtils.iterateFiles(dir, null, true);
        while (fIterator.hasNext()) {
            File f = fIterator.next();
            if (f.isFile()) {
                try {
                    if (!canStart) {
                        if (f.getName().equals(startPoint)) {
                            canStart = true; // On the next loop
                        }
                    } else {
                        if (!iStoreKeyVisitor.visit(f.getName(), FileUtils.readFileToString(f))) break;
                    }
                } catch (IOException e) {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not read keys", e);
                }
            }
        }
    }

    @Override
    public void setInstanceName(String name) {
    }

    /**
     * Note that this only gets immediate children - it's not recursive. Should
     * it be?
     */
    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        File maybeFile = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath(prefix) + EXTENSION);
        File dir = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath(prefix));
        List<RaptureFolderInfo> ret = new ArrayList<RaptureFolderInfo>();

        if (maybeFile.isFile()) {
            ret.add(new RaptureFolderInfo(removeExtension(FileRepoUtils.decode(dir.getName())), false));
        }
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = FileRepoUtils.decode(f.getName());
                    if (!f.isDirectory()) name = removeExtension(name);
                    ret.add(new RaptureFolderInfo(name, f.isDirectory()));
                }
            }
        }
        return ret;
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String folder, Boolean force) {
        int parentlen = parentDir.getAbsolutePath().length();
        List<RaptureFolderInfo> ret = new ArrayList<RaptureFolderInfo>();
        File dir = FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath(folder));
        if (!dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                String path = f.getAbsolutePath();
                if (f.isFile()) path = removeExtension(path);
                ret.add(new RaptureFolderInfo("/" + (path.substring(parentlen)), f.isDirectory()));
            }
        }

        // and add yourself
        ret.add(new RaptureFolderInfo("/" + (dir.getAbsolutePath().substring(parentlen)), true));

        // Recursively delete the folder
        try {
            FileUtils.deleteDirectory(dir);
            dir.delete();
        } catch (IOException e) {
            log.warn("Cannot delete " + dir.getAbsolutePath(), e);
        }
        return ret;
    }

    @Override
    public List<String> getAllSubKeys(String displayNamePart) {
        return getSub("", FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath(displayNamePart)), true);
    }

    private List<String> getSub(String prefix, File path, boolean first) {
        File[] files = path.listFiles();
        List<String> ret = new ArrayList<String>(files.length);
        for (File f : files) {
            StringBuilder name = new StringBuilder();
            if (!prefix.isEmpty()) {
                name.append(prefix);
            }
            if (!(name.length() == 0)) {
                name.append(SLASH);
            }
            name.append(path.getName());
            if (f.isFile()) {
                name.append(SLASH);
                name.append(removeExtension(f.getName()));
                ret.add(name.toString());
            } else {
                ret.addAll(getSub(first ? "" : name.toString(), f, false));
            }
        }
        return ret;
    }

    @Override
    public void resetFolderHandling() {

    }

    @Override
    public IndexHandler createIndexHandler(IndexProducer indexProducer) {
        FileIndexHandler indexHandler = new FileIndexHandler(folderPrefix);
        indexHandler.setIndexProducer(indexProducer);
        return indexHandler;
    }

    @Override
    public Boolean validate() {
        File mainDir = FileRepoUtils.makeGenericFile(parentDir, null);
        return mainDir.isDirectory() || mainDir.mkdirs();
    }

    @Override
    public long getSize() {
        return FileRepoUtils.makeGenericFile(parentDir, convertKeyToPath("")).length();
    }
}
