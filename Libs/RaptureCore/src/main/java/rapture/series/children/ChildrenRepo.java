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
package rapture.series.children;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

import rapture.common.RaptureFolderInfo;
import rapture.common.SeriesValue;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.repo.RepoLockHandler;
import rapture.series.children.cleanup.FolderCleanupFactory;
import rapture.series.children.cleanup.FolderCleanupService;

public abstract class ChildrenRepo {
    private static final Logger log = Logger.getLogger(ChildrenRepo.class);
    private String repoDescription;

    /**
     * This function takes a series name and makes sure that all parents as well as their children are registered in the underlying storage repo
     *
     * @param seriesPath
     */

    public void registerParentage(String seriesPath) {
        List<String> pathParts = SeriesPathParser.getPathParts(seriesPath);

        for (int index = 0; index < pathParts.size(); index++) {
            String parent = StringUtils.join(pathParts.subList(0, index), PathConstants.PATH_SEPARATOR);
            String child = pathParts.get(index);
            boolean isFolder = index < (pathParts.size() - 1);
            registerChild(parent, child, isFolder);
        }
    }

    public void registerChild(String parentName, String childName, boolean isFolder) {
        StringSeriesValue childSeriesValue;
        if (isFolder) {
            childSeriesValue = new StringSeriesValue(".", ChildKeyUtil.createColumnFolder(childName));
        } else {
            childSeriesValue = new StringSeriesValue(".", ChildKeyUtil.createColumnFile(childName));
        }
        if(!ChildKeyUtil.isRowKey(childName)) {
            addPoint(ChildKeyUtil.createRowKey(parentName), childSeriesValue);
        }
    }

    public List<RaptureFolderInfo> getChildren(String dirName) {
        // is it a file?
        List<RaptureFolderInfo> folderInfoList = new ArrayList<>();

        int lio = dirName.lastIndexOf('/');
        if (lio > 0) {
            String fileKey = ChildKeyUtil.createRowKey(dirName.substring(0, lio));
            List<SeriesValue> points = getPoints(fileKey);
            if (!points.isEmpty()) {
                String expect = "//FILE//"+dirName.substring(lio);
                for (SeriesValue point : points) {
                    if (point.getColumn().equals(expect)) {
                        RaptureFolderInfo folderInfo = new RaptureFolderInfo();
                        String name = ChildKeyUtil.fromColumnFile(point.getColumn());
                        if (name != null) {
                            folderInfo.setName(name);
                            folderInfo.setFolder(false);
                            folderInfoList.add(folderInfo);
                            break;
                        }
                    }
                }
            }
        }
        List<SeriesValue> points = getPoints(ChildKeyUtil.createRowKey(dirName));
        for (SeriesValue val : points) {
            RaptureFolderInfo folderInfo = new RaptureFolderInfo();
            String columnName = val.getColumn();
            boolean isGood = false;
            if (ChildKeyUtil.isColumnFolder(columnName)) {
                String name = ChildKeyUtil.fromColumnFolder(columnName);
                if (name != null) {
                    isGood = true;
                    folderInfo.setName(name);
                    folderInfo.setFolder(true);
                }
            } else {
                String name = ChildKeyUtil.fromColumnFile(columnName);
                if (name != null) {
                    isGood = true;
                    folderInfo.setName(name);
                    folderInfo.setFolder(false);
                }
            }
            if (isGood) {
                folderInfoList.add(folderInfo);
            }
        }
        return folderInfoList;
    }

    public boolean dropFileEntry(String filePath) {
        String parent = SeriesPathParser.getParent(filePath);
        String child = ChildKeyUtil.createColumnFile(SeriesPathParser.getChild(filePath));
        boolean ret = false;
        try {
            ret = dropPoints(ChildKeyUtil.createRowKey(parent), ImmutableList.of(child));
            if (ret) {
                List<RaptureFolderInfo> orphans = getChildren(parent);
                if ((orphans == null) || orphans.isEmpty()) {
                    dropFolderEntry(parent);
                }
                FolderCleanupService.getInstance().addForReview(getUniqueId(), SeriesPathParser.getParent(filePath));
            }
        } catch (Exception e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    "Error communicating with the database", e);
        }
        return ret;
    }

    private String getUniqueId() {
        return ChildrenRepo.this.toString();
    }

    /**
     * Returns the name of the repo whose children this stores
     *
     * @return
     */
    private String getRepoDescription() {
        return repoDescription;
    }

    public void setRepoDescription(String repoName) {
        this.repoDescription = repoName;
    }

    public boolean dropFolderEntry(String folderPath) {
        String rowName = ChildKeyUtil.createRowKey(folderPath);
        dropRow(rowName);
        // And remove this entry from the entry above
        List<String> pathParts = SeriesPathParser.getPathParts(folderPath);
        String parent = StringUtils.join(pathParts.subList(0, pathParts.size() - 1), PathConstants.PATH_SEPARATOR);
        String point = ChildKeyUtil.createColumnFolder(pathParts.get(pathParts.size() - 1));
        boolean ret = dropPoints(ChildKeyUtil.createRowKey(parent), Collections.singletonList(point));
        FolderCleanupService.getInstance().addForReview(getUniqueId(), SeriesPathParser.getParent(folderPath));
        return ret;
    }

    public void setRepoLockHandler(RepoLockHandler repoLockHandler) {
        if (repoDescription != null) {
            FolderCleanupService.getInstance().register(FolderCleanupFactory.createCleanupInfo(getRepoDescription(), repoLockHandler, this));
        } else {
            log.error(String.format("Impossible to reigster repo with no defined name (%s)", this));
        }
    }

    public abstract boolean addPoint(String key, SeriesValue value);

    public abstract List<SeriesValue> getPoints(String key);

    public abstract boolean dropPoints(String key, List<String> points);

    public abstract boolean dropRow(String key);

    public void drop() {
        FolderCleanupService.getInstance().unregister(getUniqueId());
    }
}
