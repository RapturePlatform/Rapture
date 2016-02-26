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

import rapture.common.RaptureDNCursor;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.sql.DisplayPoint;
import rapture.common.sql.QRepoConfig;
import rapture.common.sql.SQLField;
import rapture.dsl.dparse.BaseDirective;
import rapture.index.IndexHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.repo.qrep.DistinctSpecification;
import rapture.repo.qrep.FieldValue;
import rapture.repo.qrep.WhereRestriction;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;

public class QRepo extends BaseSimpleRepo implements Repository {
    private static Logger log = Logger.getLogger(QRepo.class);
    private SQLStore underlyingSQLStore;
    private QRepoConfig repoConfig;

    public QRepo(SQLStore baseSQLStore, Map<String, String> config) {
        underlyingSQLStore = baseSQLStore;
        log.info("Initializing QREP repo");
        String configDocument = config.get("config");
        log.info("Config document is " + configDocument);
        String configContent = Kernel.getSys().retrieveSystemConfig(ContextFactory.getKernelUser(), "private", configDocument);
        repoConfig = JacksonUtil.objectFromJson(configContent, QRepoConfig.class);
        log.info("Loaded and resolved config");
    }

    @Override
    public long addDocument(String key, String value, String user, String comment, boolean mustBeNew) {
        // The key defines the the field values
        // The document (the value), when converted to a map, defines some of
        // the field values also
        String[] pathParts = getPathParts(key);
        int depth = getDepth(key, pathParts);
        List<FieldValue> fieldValues = new ArrayList<FieldValue>();
        // This is either an update or an insert. Do a get existence to
        // determine that, and then
        // branch into an update using field values with a where restriction
        // or an insert (just using field values)

        Set<String> seenFields = new HashSet<String>();
        for (int i = 0; i < depth - 1; i++) {
            FieldValue val = new FieldValue();
            val.setField(repoConfig.getDisplay().getPoints().get(i).getRef());
            seenFields.add(val.getField().testString());
            val.setValue(pathParts[i]);
            fieldValues.add(val);
        }

        // Now extract out of the document the fields as defined in the qConfig
        addFieldsFromDocument(value, fieldValues, seenFields);

        if (null == getDocument(key)) {
            doInsert(fieldValues);
        } else {
            List<WhereRestriction> whereClauses = getWhereClausesFromDisplayname(pathParts, depth);
            doUpdate(fieldValues, whereClauses);
        }
        return 1;
    }

    private void doUpdate(List<FieldValue> fieldValues, List<WhereRestriction> whereClauses) {
        log.info("Performing update on document");
        underlyingSQLStore.performUpdate(fieldValues, whereClauses, repoConfig.getJoins());

    }

    private void doInsert(List<FieldValue> fieldValues) {
        log.info("Would do insert with " + fieldValues.toString());
    }

    private void addFieldsFromDocument(String value, List<FieldValue> fieldValues, Set<String> seenFields) {
        Map<String, Object> docAsMap = (Map<String, Object>) JacksonUtil.getMapFromJson(value);
        for (SQLField f : repoConfig.getFields().getFields()) {
            if (seenFields.contains(f.getRef().testString())) {
                continue;
            }
            FieldValue fv = new FieldValue();
            fv.setField(f.getRef());
            fv.setValue(getValueFromDoc(docAsMap, f.getJsonKey()));
            fieldValues.add(fv);
            seenFields.add(f.getRef().testString());
        }
    }

    @SuppressWarnings("unchecked")
    private Object getValueFromDoc(Map<String, Object> docAsMap, String jsonKey) {
        String[] parts = jsonKey.split("\\.");
        Map<String, Object> current = docAsMap;
        for (int i = 0; i < parts.length - 1; i++) {
            if (current.containsKey(parts[i])) {
                current = (Map<String, Object>) current.get(parts[i]);
            } else {
                return null;
            }
        }
        return current.get(parts[parts.length - 1]);
    }

    @Override
    public void addDocuments(List<String> dispNames, String content, String user, String comment) {
        for (int i = 0; i < dispNames.size(); i++) {
            addDocument(dispNames.get(i), content, user, comment, false);
        }
    }

    @Override
    public void addToStage(String stage, String key, String value, boolean mustBeNew) {
    }

    @Override
    public long countDocuments() throws RaptNotSupportedException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void drop() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getDocument(String key) {
        log.info("Retrieving document " + key);
        // Here we simply want to retrieve the document
        String[] pathParts = getPathParts(key);
        int depth = getDepth(key, pathParts);
        if (depth < repoConfig.getDisplay().getPoints().size()) {
            return null;
        }
        List<WhereRestriction> whereClauses = getWhereClausesFromDisplayname(pathParts, depth);
        String content = underlyingSQLStore.executeDocument(whereClauses, repoConfig.getFields());
        return content;
    }

    private List<WhereRestriction> getWhereClausesFromDisplayname(String[] pathParts, int depth) {
        if (depth < repoConfig.getDisplay().getPoints().size()) {
            return null;
        }
        List<WhereRestriction> whereClauses = new ArrayList<WhereRestriction>();
        for (int i = 0; i < depth - 1; i++) {
            WhereRestriction restriction = new WhereRestriction();
            restriction.setField(repoConfig.getDisplay().getPoints().get(i).getRef());
            restriction.setValue(pathParts[i]);
            whereClauses.add(restriction);
        }
        return whereClauses;
    }

    private int getDepth(String key, String[] pathParts) {
        int depth = 0;
        if (!key.isEmpty()) {
            depth = pathParts.length + 1;
        }
        log.info("Depth is " + depth);
        return depth;
    }

    private String[] getPathParts(String key) {
        String[] pathParts = key.split("/");
        return pathParts;
    }

    @Override
    public String getDocument(String key, BaseDirective directive) {
        return getDocument(key);
    }

    @Override
    public List<String> getDocuments(List<String> keys) {
        List<String> ret = new ArrayList<String>();
        for (String k : keys) {
            ret.add(getDocument(k));
        }
        return ret;
    }

    @Override
    public boolean[] getExistence(List<String> displays) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RaptureDNCursor getNextDNCursor(RaptureDNCursor cursor, int count) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");
    }

    @Override
    public boolean isVersioned() {
        return false;
    }

    @Override
    public boolean removeDocument(String key, String user, String comment) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeFromStage(String stage, String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");
    }

    @Override
    public void visitAll(String prefix, BaseDirective directive, RepoVisitor visitor) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitFolder(String folder, BaseDirective directive, RepoVisitor visitor) {
        // Implement this for REST interface (so that's kind of useful)
        // We ignore the perspective, and the folder gives us what we need to do
        // For each part of the path will fix on the first set of restrictions
        // in the query, and the
        // displayname config will give the free part of the value returned.

        // E.g. If path is empty, we do a SELECT DISTINCT (firstField)
        // If path is /[something]/ we do a SELECT DISTINCT (secondField) WHERE
        // firstField=something
        // etc
        log.info("VisitFolder with path " + folder);

        // If the path is greater than the displayPoints, we have a document to
        // return instead

        String[] pathParts = getPathParts(folder);
        int depth = 0;
        if (!folder.isEmpty()) {
            depth = pathParts.length + 1;
        }
        if (depth >= repoConfig.getDisplay().getPoints().size()) {
            // Execute a document retrieval
            log.info("Document retrieval");
            List<WhereRestriction> whereClauses = new ArrayList<WhereRestriction>();
            for (int i = 0; i < depth - 1; i++) {
                WhereRestriction restriction = new WhereRestriction();
                restriction.setField(repoConfig.getDisplay().getPoints().get(i).getRef());
                restriction.setValue(pathParts[i]);
                whereClauses.add(restriction);
            }
            underlyingSQLStore.executeDocument(whereClauses, repoConfig.getFields(), visitor);
        } else {
            log.info("Folder retrieval");
            List<WhereRestriction> whereClauses = new ArrayList<WhereRestriction>();
            DistinctSpecification distinct = null;
            distinct = getDistinctFrom(repoConfig.getDisplay().getPoints().get(depth));
            for (int i = 0; i < depth; i++) {
                WhereRestriction restriction = new WhereRestriction();
                restriction.setField(repoConfig.getDisplay().getPoints().get(i).getRef());
                restriction.setValue(pathParts[i]);
            }
            // TODO: Fix folder implementation here
            log.info("Depth is " + depth);
            log.info("Display size is " + repoConfig.getDisplay().getPoints().size());
            underlyingSQLStore.executeDistinct(distinct, whereClauses, visitor, false);
        }
    }

    private DistinctSpecification getDistinctFrom(DisplayPoint displayPoint) {
        DistinctSpecification distinct = new DistinctSpecification();
        distinct.setField(displayPoint.getRef());
        return distinct;
    }

    @Override
    public void visitFolders(String folderPrefix, BaseDirective directive, RepoFolderVisitor visitor) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean addDocumentWithVersion(String disp, String content, String user, String comment, boolean mustBeNew, int expectedVersion) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<RaptureFolderInfo> getChildren(String displayNamePart) {
        return null;
    }

    @Override
    public List<RaptureFolderInfo> removeChildren(String displayNamePart, Boolean force) {
        return null;
    }

    @Override
    public List<String> getAllChildren(String area) {
        return null;
    }

    @Override
    public Boolean validate() {
        return underlyingSQLStore.validate();
    }

    @Override
    public Optional<IndexHandler> getIndexHandler() {
        return Optional.absent();
    }
}
