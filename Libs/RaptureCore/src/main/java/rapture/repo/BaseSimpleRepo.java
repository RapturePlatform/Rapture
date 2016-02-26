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

import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
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
import rapture.index.IndexProducer;
import rapture.repo.stage.Stage;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseSimpleRepo implements Repository {
    protected RepoLockHandler repoLockHandler;

    @Override
    public void addCommentary(String key, String who, String description, String commentaryKey, String ref) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void clearRemote() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void commitStage(String stage, String user, String comment) {
        // Does nothing - addToStage adds it to the store
    }

    @Override
    public Stage createStage(String stage) {
        return null;
    }

    @Override
    public void createTag(String user, String tagName) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<CommentaryObject> getCommentary(String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<RaptureCommit> getCommitHistory() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.nocommits")); //$NON-NLS-1$
    }

    @Override
    public CommitObject getCommitObject(String reference) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<RaptureCommit> getCommitsSince(String commitReference) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public DocumentObject getDocumentObject(String reference) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public String getTagDocument(String tag, String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<String> getTags() {
        return new ArrayList<String>();
    }

    @Override
    public TreeObject getTreeObject(String reference) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void removeTag(String tagName) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void setRemote(String remote, String remoteAuthority) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void visitTag(String tagName, String prefix, RepoVisitor visitor) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void visitTagFolder(String tagName, String folder, RepoVisitor visitor) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void writeCommitObject(String reference, CommitObject commit) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void writeDocumentObject(String reference, DocumentObject docObject) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void writeTreeObject(String reference, TreeObject treeObject) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public DocumentWithMeta getDocAndMeta(String disp, BaseDirective directive) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public DocumentMetadata getMeta(String key, BaseDirective directive) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public DocumentWithMeta revertDoc(String disp, BaseDirective directive) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public boolean hasMetaContent() {
        return false;
    }

    public void setIndexProducer(IndexProducer producer) {
        //        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public boolean hasIndexProducer() {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseRepo.notsupp")); //$NON-NLS-1$
    }

    public TableQueryResult findIndex(String query) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<DocumentVersionInfo> getVersionHistory(String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    /**
     * Given a display name, return the documents for the given versions (or
     * null if that version doesn't exist)
     *
     * @param key
     * @param versions
     * @return
     */
    @Override
    public List<DocumentWithMeta> getVersionMeta(String key, List<Integer> versions) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$		
    }

    /**
     * Remove the versions of the documents given. Returns an array of
     * true/false depending on whether something was deleted.
     *
     * @param key
     * @param versions
     * @return
     */
    @Override
    public List<Boolean> removeVersionMeta(String key, List<Integer> versions) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public void setDocAttribute(RaptureURI uri, DocumentAttribute attribute) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public DocumentAttribute getDocAttribute(RaptureURI uri) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public List<DocumentAttribute> getDocAttributes(RaptureURI uri) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public Boolean deleteDocAttribute(RaptureURI uri) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$
    }

    @Override
    public Map<String, String> getStatus() {
        return new HashMap<String, String>();
    }
}
