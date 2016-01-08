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
package rapture.table.file;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.index.IndexHandler;
import rapture.kernel.file.FileRepoUtils;
import rapture.table.memory.MemoryIndexHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Paths;
import java.util.Map;

public class FileIndexHandler extends MemoryIndexHandler implements IndexHandler {
    private static final double MAX_RATIO_FILE_TO_MAP = 1.2;

    private File persistenceFile = null;
    private File tmpFile = null;
    private FileOutputStream updatesStream = null;
    private String charsetName = "UTF-8";

    public FileIndexHandler(String repoDirName) {
        super();
        log = Logger.getLogger(FileIndexHandler.class);

        initFileIO(repoDirName);
        loadIndexData();
    }

    private void initFileIO(String repoDirName) {
        log.info("Initializing index files for " + repoDirName);
        String fileSeparator = System.getProperty("file.separator");
        File fullRepoPath = FileRepoUtils.ensureDirectory(repoDirName);

        // Put this file in the same directory as the file repo itself
        String parentDir = Paths.get(fullRepoPath.toString()).getParent().toString();
        String indexPath = parentDir + fileSeparator + repoDirName + "_index";

        persistenceFile = new File(indexPath);
        tmpFile = new File(indexPath + "_tmp");

        initUpdatesStream();
    }

    private void initUpdatesStream() {
        try {
            if (updatesStream != null) {
                updatesStream.close();
            }

            Boolean append = true;
            updatesStream = new FileOutputStream(persistenceFile, append);
        }
        catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error (re)initializing index file stream", e);
        }
    }

    protected void finalize() {
        try {
            if (updatesStream != null) {
                updatesStream.close();
            }
        }
        catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error closing index file stream", e);
        }
    }

    private void loadIndexData() {
        log.info("Loading previously saved index data.");
        Integer numFileLines = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(persistenceFile)))) {
            String line;
            while ((line = br.readLine()) != null) {
                IndexEntry entry = JacksonUtil.objectFromJson(line, IndexEntry.class);
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    super.removeAll(entry.getKey());
                }
                else {
                    super.updateRow(entry.getKey(), entry.getValue());
                }

                numFileLines++;
            }

        }
        catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error reading index file", e);
        }

        if (isTimeToConsolidate(numFileLines)) {
            log.info("Consolidating index file.");
            persistFullIndex();
        }
    }

    private Boolean isTimeToConsolidate(Integer numFileLines) {
        Integer mapSize = memoryView.size();
        if (numFileLines == 0 || mapSize == 0) {
            // Nothing to consolidate
            return false;
        }

        return ((double)numFileLines / (double)mapSize > MAX_RATIO_FILE_TO_MAP);
    }

    /*
     * Write a full, fresh copy of the index to file
     */
    private void persistFullIndex() {
        // Write to a separate file and then rename so we don't risk losing data
        // if we are interrupted in the middle of this process.
        try (FileOutputStream tmpFileStream = new FileOutputStream(tmpFile)) {
            // Write each entry as a separate line to facilitate buffered reading.
            for (Map.Entry<String, Map<String, Object>> entry: memoryView.entrySet()) {
                persistIndexEntry(entry.getKey(), entry.getValue(), tmpFileStream);
            }

            FileUtils.copyFile(tmpFile, persistenceFile);
        }
        catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error writing full index to index file", e);
        }
        finally {
            FileUtils.deleteQuietly(tmpFile);

            // Seems to work just fine on a Mac without reinitializing after the file has
            // been changed underneath it, but to be safer let's close and reopen.
            initUpdatesStream();
        }
    }

    private void persistIndexEntry(String key, Map<String, Object> value, FileOutputStream stream) {
        IndexEntry entry = new IndexEntry();
        entry.setKey(key);
        entry.setValue(value);
        String output = JacksonUtil.jsonFromObject(entry) +  System.getProperty("line.separator");

        try {
            stream.write(output.getBytes(charsetName));
            stream.flush();
        }
        catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error writing incremental update to index file", e);
        }
    }

    @Override
    public void deleteTable() {
        super.deleteTable();
        persistFullIndex();
    }

    @Override
    public void removeAll(String rowId) {
        super.removeAll(rowId);
        persistIndexEntry(rowId, memoryView.get(rowId), updatesStream); // This is a put in MemoryIndexHandler, so don't just remove it from the map
    }

    @Override
    public void addedRecord(String key, String value, DocumentMetadata mdLatest) {
        super.addedRecord(key, value, mdLatest);
        persistIndexEntry(key, memoryView.get(key), updatesStream);
    }

    @Override
    public void updateRow(String key, Map<String, Object> recordValues) {
        super.updateRow(key, recordValues);
        persistIndexEntry(key, memoryView.get(key), updatesStream);
    }

    /*
     * Jackson will only work with inner classes if they are static,
     * because non-static inner classes have "hidden" constructors and
     * other goodies for giving them access to their parent object.
     */
    static class IndexEntry implements Serializable {
        private String key;
        private Map<String, Object> value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Map<String, Object> getValue() {
            return value;
        }

        public void setValue(Map<String, Object> value) {
            this.value = value;
        }
    }
}
