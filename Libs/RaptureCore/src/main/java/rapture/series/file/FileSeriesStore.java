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
package rapture.series.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

// TODO Not convinced that rapture.series.mem is the best package name for this
// but if it moves then the class's full package name is hard coded as a String in a couple of places.

import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.DecimalSeriesValue;
import rapture.dsl.serfun.LongSeriesValue;
import rapture.dsl.serfun.SeriesValueCodec;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.dsl.serfun.StructureSeriesValueImpl;
import rapture.kernel.file.FileRepoUtils;
import rapture.series.SeriesPaginator;
import rapture.series.SeriesStore;
import rapture.series.children.ChildrenRepo;

/**
 * A file based version of a series repo, for testing
 *
 * @author dtong
 */
public class FileSeriesStore implements SeriesStore {

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "FileSeriesStore [childrenRepo=" + childrenRepo + ", parentDir=" + parentDir + ", forwards=" + forwards + ", backwards="
                + backwards + ", instanceName=" + instanceName + "]";
    }

    public static final String PREFIX = "prefix";
    // Not sure what this is
    private final ChildrenRepo childrenRepo;
    private File parentDir = null;
    private static final Logger log = Logger.getLogger(FileSeriesStore.class);

    Comparator<SeriesValue> forwards = new Comparator<SeriesValue>() {
        @Override
        public int compare(SeriesValue o1, SeriesValue o2) {
            return o1.getColumn().compareTo(o2.getColumn());
        }
    };

    Comparator<SeriesValue> backwards = new Comparator<SeriesValue>() {
        @Override
        public int compare(SeriesValue o1, SeriesValue o2) {
            return o2.getColumn().compareTo(o1.getColumn());
        }
    };

    private String instanceName = "default";

    public FileSeriesStore() {
        this.childrenRepo = new ChildrenRepo() {

            @Override
            public List<SeriesValue> getPoints(String key) {
                return FileSeriesStore.this.getPoints(key);
            }

            @Override
            public boolean dropPoints(String key, List<String> points) {
                return FileSeriesStore.this.deletePointsFromSeriesByPointKey(key, points);
            }

            @Override
            public boolean addPoint(String key, SeriesValue value) {
                FileSeriesStore.this.addPointToSeries(key, value);
                return true;
            }

            @Override
            public void dropRow(String key) {
                FileSeriesStore.this.deletePointsFromSeries(key);
            }
        };
    }

    @Override
    public void setConfig(Map<String, String> config) {
        // What happens if this is called twice?
        if (parentDir != null) throw RaptureExceptionFactory.create("Calling setConfig twice is currently not supported");
        String prefix = config.get(FileRepoUtils.PREFIX);
        if (StringUtils.trimToNull(prefix) == null) throw RaptureExceptionFactory.create("prefix must be specified");
        parentDir = FileRepoUtils.ensureDirectory(prefix + "_series");
    }

    @Override
    public void drop() {
        if (parentDir != null) FileUtils.deleteQuietly(parentDir);
        parentDir = null;
    }

    /**
     * Returns a sorted series
     *
     * @param direction true for verbatim, false for reverse order.
     * @return
     */
    protected Collection<SeriesValue> readSeriesSorted(String key, boolean direction) {
        File seriesFile = FileRepoUtils.makeGenericFile(parentDir, key);
        if (!seriesFile.isFile()) return new ArrayList<>();
        TreeSet<SeriesValue> series = new TreeSet<>((direction) ? forwards : backwards);
        try {
            List<String> lines = Files.readAllLines(seriesFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                char c = line.charAt(0);
                int i = line.indexOf(c, 1);
                String column = line.substring(1, i);
                byte[] bytes = line.substring(i + 1).getBytes();
                series.add(SeriesValueCodec.decode(column, bytes));
            }
            return series;
        } catch (IOException e) {
            log.debug(ExceptionToString.format(e));
        }
        return null;
    }

    /**
     * Writes an ordered series back to the file system
     *
     * @param key
     * @param values
     * @return
     */
    protected boolean writeSeries(String key, Collection<SeriesValue> values) {
        File seriesFile = FileRepoUtils.makeGenericFile(parentDir, key);
        if (!seriesFile.exists()) try {
            if ((!seriesFile.getParentFile().isDirectory() && !seriesFile.getParentFile().mkdirs()) || !seriesFile.createNewFile())
                throw RaptureExceptionFactory
                        .create("Cannot create series " + seriesFile.getAbsolutePath());
        } catch (IOException ioe) {
            log.debug(ExceptionToString.format(ioe));
            throw RaptureExceptionFactory.create("Cannot create series " + key, ioe);
        }

        String path = seriesFile.getAbsolutePath();
        String backup = path + "_prev";
        File backupFile = new File(backup);
        try {
            seriesFile.renameTo(backupFile);
            try (FileOutputStream fos = new FileOutputStream(new File(path))) {
                for (SeriesValue value : values) {
                    int ch = 32;
                    // Find unique marker
                    String column = value.getColumn();
                    while (column.indexOf(ch) >= 0)
                        ch++;
                    fos.write(ch);
                    fos.write(column.getBytes());
                    fos.write(ch);
                    fos.write(SeriesValueCodec.encodeValue(value));
                    fos.write('\n');
                }
            }
        } catch (IOException e) {
            log.debug(ExceptionToString.format(e));
            // put it back
            seriesFile.renameTo(new File(path));
            return false;
        }
        backupFile.delete();
        return true;
    }

    @Override
    public void addDoubleToSeries(String key, String column, double value) {
        addPointToSeries(key, new DecimalSeriesValue(value, column));
    }

    @Override
    public void addDoublesToSeries(String key, List<String> columns, List<Double> values) {
        addPointsToSeries(key, DecimalSeriesValue.zip(columns, values));
    }

    @Override
    public void addLongToSeries(String key, String column, long value) {
        addPointToSeries(key, new LongSeriesValue(value, column));
    }

    @Override
    public void addLongsToSeries(String key, List<String> columns, List<Long> values) {
        addPointsToSeries(key, LongSeriesValue.zip(columns, values));
    }

    @Override
    public void addStringToSeries(String key, String column, String value) {
        addPointToSeries(key, new StringSeriesValue(value, column));
    }

    @Override
    public void addStringsToSeries(String key, List<String> columns, List<String> values) {
        addPointsToSeries(key, StringSeriesValue.zip(columns, values));
    }

    @Override
    public void addStructureToSeries(String key, String column, String jsonValue) {
        try {
            addPointToSeries(key, StructureSeriesValueImpl.unmarshal(jsonValue, column));
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error parsing json value " + jsonValue, e);
        }
    }

    @Override
    public void addStructuresToSeries(String key, List<String> columns, List<String> jsonValues) {
        try {
            addPointsToSeries(key, StructureSeriesValueImpl.zip(columns, jsonValues));
        } catch (IOException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Error parsing json values", e);
        }
    }

    @Override
    public void addPointsToSeries(String key, List<SeriesValue> values) {
        boolean nullKey = false;
        for (SeriesValue value : values) {
            if (value.getColumn() == null) nullKey = true;
            else addPointToSeries(key, value);
        }
        if (nullKey) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Column Key may not be null, other values added");
    }

    @Override
    public void addPointToSeries(String key, SeriesValue value) {
        Collection<SeriesValue> series = readSeriesSorted(key, true);
        series.add(value);
        writeSeries(key, series);
    }

    @Override
    public Boolean deletePointsFromSeriesByPointKey(String key, List<String> pointKeys) {
        File seriesFile = FileRepoUtils.makeGenericFile(parentDir, key);
        if ((pointKeys == null) || pointKeys.isEmpty() || !seriesFile.isFile()) return true;
        List<SeriesValue> series = getPoints(key);
        if (series == null) return true;
        List<SeriesValue> newSeries = new ArrayList<SeriesValue>();
        for (SeriesValue value : series) {
            if (pointKeys.contains(value.getColumn())) {
                pointKeys.remove(value.getColumn());
            } else {
                newSeries.add(value);
            }
        }
        return writeSeries(key, newSeries);
    }

    @Override
    public void deletePointsFromSeries(String key) {
        File seriesFile = FileRepoUtils.makeGenericFile(parentDir, key);
        if (!seriesFile.isFile()) return;
        seriesFile.delete();
    }

    @Override
    public List<SeriesValue> getPoints(String key) {
        return getPointsAfter(key, null, null, Integer.MAX_VALUE);
    }

    @Override
    public List<SeriesValue> getPointsAfter(String key, String startColumn, int maxNumber) {
        return getPointsAfter(key, startColumn, null, maxNumber);
    }

    @Override
    public List<SeriesValue> getPointsAfterReverse(String key, String startColumn, int maxNumber) {
        return getPointsAfterReverse(key, startColumn, null, maxNumber);
    }

    public List<SeriesValue> getPointsAfterReverse(String key, String startColumn, String endColumn, int maxNumber) {
        List<SeriesValue> series = new ArrayList<>(); // or ((maxNumber > values.size()) ? values.size() : maxNumber);
        boolean found = false;

        for (SeriesValue value : readSeriesSorted(key, false)) {
            if (!found) {
                if (value.getColumn().equals(startColumn)) {
                    found = true;
                } else continue;
            }
            series.add(value);
            if (series.size() == maxNumber) break;
            if ((endColumn != null) && value.getColumn().equals(endColumn)) break;
        }
        return series;
    }

    @Override
    public List<SeriesValue> getPointsAfter(String key, String startColumn, String endColumn, int maxNumber) {
        File seriesFile = FileRepoUtils.makeGenericFile(parentDir, key);
        if (!seriesFile.exists()) return new ArrayList<>();
        if (!seriesFile.isFile()) throw RaptureExceptionFactory.create("For FILE implementation you can't have a Series with the same name as a Folder");
        List<SeriesValue> series = new ArrayList<>();
        boolean found = (startColumn == null);
        try {
            List<String> lines = Files.readAllLines(seriesFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                char c = line.charAt(0);
                int i = line.indexOf(c, 1);
                String column = line.substring(1, i);
                byte[] bytes = line.substring(i + 1).getBytes();
                SeriesValue value = SeriesValueCodec.decode(column, bytes);
                if (!found) {
                    if (!value.getColumn().equals(startColumn)) continue;
                    found = true;
                }
                series.add(value);
                if (series.size() == maxNumber) break;
                if ((endColumn != null) && value.getColumn().equals(endColumn)) break;
            }
        } catch (IOException e) {
            log.debug(ExceptionToString.format(e));
            throw RaptureExceptionFactory.create("Cannot read Series " + key, e);
        }
        return series;
    }

    @Override
    public void setInstanceName(String instanceName) {
        // TODO what dis for?
        this.instanceName = instanceName;
    }

    @Override
    public List<String> getSeriesLike(String keyPrefix) {
        // TODO
        throw new RuntimeException("Yeah yeah, I'll get to it.");

        // List<String> ret = new ArrayList<>();
        // for (String key : seriesStore.keySet()) {
        // if (key.startsWith(keyPrefix)) {
        // ret.add(key);
        // }
        // }
        // return ret;

    }

    @Override
    public Iterable<SeriesValue> getRangeAsIteration(String key, String startCol, String endCol, int pageSize) {
        return new SeriesPaginator(key, startCol, endCol, pageSize, this);
    }

    @Override
    public List<SeriesValue> getRangeAsList(String key, String startCol, String endCol) {
        return getPointsAfter(key, startCol, endCol, Integer.MAX_VALUE);
    }

    @Override
    public List<RaptureFolderInfo> listSeriesByUriPrefix(String string) {
        File file = FileRepoUtils.makeGenericFile(parentDir, string);
        List<RaptureFolderInfo> info = Collections.emptyList();
        if (file.isFile()) {
            RaptureFolderInfo inf = new RaptureFolderInfo();
            inf.setName(file.getName());
            inf.setFolder(false);
            info = Arrays.asList(inf);
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            info = (children == null) ? Collections.emptyList() : new ArrayList<>(children.length);
            if (children != null) {
                info = new ArrayList<>(children.length);
                for (File kid : children) {
                    RaptureFolderInfo inf = new RaptureFolderInfo();
                    inf.setName(kid.getName());
                    inf.setFolder(kid.isDirectory());
                    info.add(inf);
                }
            }
        }
        return info;
    }

    @Override
    public void unregisterKey(String key) {
        childrenRepo.dropFileEntry(key);
    }

    @Override
    public void unregisterKey(String key, boolean isFolder) {
        if (isFolder) childrenRepo.dropFolderEntry(key);
        unregisterKey(key);
    }

    @Override
    public SeriesValue getLastPoint(String key) {
        File seriesFile = FileRepoUtils.makeGenericFile(parentDir, key);
        if (!seriesFile.isFile()) return null;
        try {
            List<String> lines = Files.readAllLines(seriesFile.toPath(), StandardCharsets.UTF_8);
            if (!lines.isEmpty()) {
                String line = lines.get(lines.size() - 1);
                char c = line.charAt(0);
                int i = line.indexOf(c, 1);
                String column = line.substring(1, i);
                byte[] bytes = line.substring(i + 1).getBytes();
                return SeriesValueCodec.decode(column, bytes);
            }
        } catch (IOException e) {
            log.debug(ExceptionToString.format(e));
        }
        return null;
    }
    
    @Override
    public void createSeries(String key) {
        writeSeries(key, new ArrayList<SeriesValue>());
    }

    @Override
    public void deleteSeries(String key) {
        unregisterKey(key);
        deletePointsFromSeries(key);
    }
}
