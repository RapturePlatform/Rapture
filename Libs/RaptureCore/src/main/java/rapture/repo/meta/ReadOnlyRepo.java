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
package rapture.repo.meta;

import rapture.common.RaptureDNCursor;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentAttribute;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentVersionInfo;
import rapture.common.model.DocumentWithMeta;
import rapture.common.model.RaptureCommit;
import rapture.common.repo.CommentaryObject;
import rapture.common.repo.CommitObject;
import rapture.common.repo.DocumentObject;
import rapture.common.repo.TreeObject;
import rapture.dsl.dparse.BaseDirective;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.repo.RepoFolderVisitor;
import rapture.repo.RepoVisitor;
import rapture.repo.Repository;
import rapture.repo.stage.Stage;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

/**
 * A readonly repo doesn't allow any modifications
 *
 * @author amkimian
 */
public class ReadOnlyRepo implements Repository {
    private static final String READ_ONLY_REPOSITORY = "Read only repo";
    private Repository realRepo;

    public ReadOnlyRepo(Repository createRepo) {
        this.realRepo = createRepo;
    }

    @Override
    public void addCommentary(String key, String who, String description, String commentaryKey, String ref) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public long addDocument(String key, String value, String user, String comment, boolean mustBeNew) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void addDocuments(List<String> dispNames, String content, String user, String comment) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void addToStage(String stage, String key, String value, boolean mustBeNew) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void clearRemote() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void commitStage(String stage, String user, String comment) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public long countDocuments() throws RaptNotSupportedException {
        return realRepo.countDocuments();
    }

    @Override
    public Stage createStage(String stage) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void createTag(String user, String tagName) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void drop() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public List<CommentaryObject> getCommentary(String key) {
        return realRepo.getCommentary(key);
    }

    @Override
    public List<RaptureCommit> getCommitHistory() {
        return realRepo.getCommitHistory();
    }

    @Override
    public CommitObject getCommitObject(String reference) {
        return realRepo.getCommitObject(reference);
    }

    @Override
    public List<RaptureCommit> getCommitsSince(String commitReference) {
        return realRepo.getCommitsSince(commitReference);
    }

    @Override
    public String getDocument(String key) {
        return realRepo.getDocument(key);
    }

    @Override
    public String getDocument(String key, BaseDirective directive) {
        return realRepo.getDocument(key, directive);
    }

    @Override
    public DocumentObject getDocumentObject(String reference) {
        return realRepo.getDocumentObject(reference);
    }

    @Override
    public List<String> getDocuments(List<String> keys) {
        return realRepo.getDocuments(keys);
    }

    @Override
    public boolean[] getExistence(List<String> displays) {
        return realRepo.getExistence(displays);
    }

    @Override
    public RaptureDNCursor getNextDNCursor(RaptureDNCursor cursor, int count) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public String getTagDocument(String tag, String key) {
        return realRepo.getTagDocument(tag, key);
    }

    @Override
    public List<String> getTags() {
        return realRepo.getTags();
    }

    @Override
    public TreeObject getTreeObject(String reference) {
        return realRepo.getTreeObject(reference);
    }

    public boolean isVersioned() {
        return realRepo.isVersioned();
    }

    @Override
    public Map<String, String> getStatus() {
        return realRepo.getStatus();
    }

    @Override
    public Optional<IndexHandler> getIndexHandler() {
        return realRepo.getIndexHandler();
    }

    @Override
    public boolean removeDocument(String key, String user, String comment) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public boolean removeFromStage(String stage, String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void removeTag(String tagName) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        return realRepo.runNativeQuery(repoType, queryParams);
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        return realRepo.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
    }

    @Override
    public void setRemote(String remote, String remoteAuthority) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void visitAll(String prefix, BaseDirective directive, RepoVisitor visitor) {
        realRepo.visitAll(prefix, directive, visitor);
    }

    @Override
    public void visitFolder(String folder, BaseDirective directive, RepoVisitor visitor) {
        realRepo.visitFolder(folder, directive, visitor);
    }

    @Override
    public void visitFolders(String folderPrefix, BaseDirective directive, RepoFolderVisitor visitor) {
        realRepo.visitFolders(folderPrefix, directive, visitor);
    }

    @Override
    public void visitTag(String tagName, String prefix, RepoVisitor visitor) {
        realRepo.visitTag(tagName, prefix, visitor);
    }

    @Override
    public void visitTagFolder(String tagName, String folder, RepoVisitor visitor) {
        realRepo.visitTagFolder(tagName, folder, visitor);
    }

    @Override
    public void writeCommitObject(String reference, CommitObject commit) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void writeDocumentObject(String reference, DocumentObject docObject) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void writeTreeObject(String reference, TreeObject treeObject) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public DocumentWithMeta getDocAndMeta(String disp, BaseDirective directive) {
        return realRepo.getDocAndMeta(disp, directive);
    }

    @Override
    public List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris) {
        return realRepo.getDocAndMetas(uris);
    }

    @Override
    public DocumentMetadata getMeta(String key, BaseDirective directive) {
        return realRepo.getMeta(key, directive);
    }

    @Override
    public DocumentWithMeta revertDoc(String disp, BaseDirective directive) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public boolean addDocumentWithVersion(String disp, String content, String user, String comment, boolean mustBeNew, int expectedVersion) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public List<RaptureFolderInfo> getChildren(String displayNamePart) {
        return realRepo.getChildren(displayNamePart);
    }

    @Override
    public List<RaptureFolderInfo> removeChildren(String area, Boolean force) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public boolean hasMetaContent() {
        return realRepo.hasMetaContent();
    }

    @Override
    public List<String> getAllChildren(String area) {
        return realRepo.getAllChildren(area);
    }

    @Override
    public List<DocumentVersionInfo> getVersionHistory(String key) {
        return realRepo.getVersionHistory(key);
    }

    @Override
    public List<DocumentWithMeta> getVersionMeta(String key, List<Integer> versions) {
        return realRepo.getVersionMeta(key, versions);
    }

    @Override
    public List<Boolean> removeVersionMeta(String key, List<Integer> versions) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void setDocAttribute(RaptureURI uri, DocumentAttribute attribute) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public DocumentAttribute getDocAttribute(RaptureURI uri) {
        return realRepo.getDocAttribute(uri);
    }

    @Override
    public List<DocumentAttribute> getDocAttributes(RaptureURI uri) {
        return realRepo.getDocAttributes(uri);
    }

    @Override
    public Boolean deleteDocAttribute(RaptureURI uri) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public void setIndexProducer(IndexProducer producer) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, READ_ONLY_REPOSITORY);
    }

    @Override
    public boolean hasIndexProducer() {
        return realRepo.hasIndexProducer();
    }

    @Override
    public TableQueryResult findIndex(String query) {
        return realRepo.findIndex(query);
    }

    @Override
    public Boolean validate() {
        return realRepo.validate();
    }
}
