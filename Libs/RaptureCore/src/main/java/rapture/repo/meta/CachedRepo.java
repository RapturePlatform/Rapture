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

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

/**
 * A cached repo tries to use the main to retrieve everything. If it's not in
 * main, it looks to the shadow and goes to that, and if it finds something it
 * will put it in main (which is assumed to be "cache")
 *
 * @author amkimian
 */
public class CachedRepo implements Repository {
    private Repository main;
    private Repository shadow;

    public CachedRepo(Repository mainRepo, Repository shadowRepo) {
        this.main = mainRepo;
        this.shadow = shadowRepo;
    }

    @Override
    public void addCommentary(String key, String who, String description, String commentaryKey, String ref) {
        shadow.addCommentary(key, who, description, commentaryKey, ref);
    }

    @Override
    public DocumentWithMeta addDocument(String key, String value, String user, String comment, boolean mustBeNew) {
        // Add to both
        DocumentWithMeta ver = main.addDocument(key, value, user, comment, mustBeNew);
        shadow.addDocument(key, value, user, comment, mustBeNew);
        return ver;
    }

    @Override
    public void addDocuments(List<String> dispNames, String content, String user, String comment) {
        shadow.addDocuments(dispNames, content, user, comment);
    }

    @Override
    public void addToStage(String stage, String key, String value, boolean mustBeNew) {
        main.addToStage(stage, key, value, mustBeNew);
        shadow.addToStage(stage, key, value, mustBeNew);
    }

    @Override
    public void clearRemote() {
        shadow.clearRemote();
    }

    @Override
    public void commitStage(String stage, String user, String comment) {
        main.commitStage(stage, user, comment);
        shadow.commitStage(stage, user, comment);
    }

    @Override
    public long countDocuments() throws RaptNotSupportedException {
        return shadow.countDocuments();
    }

    @Override
    public Stage createStage(String stage) {
        main.createStage(stage);
        return shadow.createStage(stage);
    }

    @Override
    public void createTag(String user, String tagName) {
        shadow.createTag(user, tagName);
    }

    @Override
    public void drop() {
        // Drop them both
        main.drop();
        shadow.drop();
    }

    @Override
    public List<CommentaryObject> getCommentary(String key) {
        return shadow.getCommentary(key);
    }

    @Override
    public List<RaptureCommit> getCommitHistory() {
        return shadow.getCommitHistory();
    }

    @Override
    public CommitObject getCommitObject(String reference) {
        return shadow.getCommitObject(reference);
    }

    @Override
    public List<RaptureCommit> getCommitsSince(String commitReference) {
        return shadow.getCommitsSince(commitReference);
    }

    @Override
    public String getDocument(String key) {
        String content = main.getDocument(key);
        if (content == null) {
            content = shadow.getDocument(key);
            if (content != null) {
                main.addDocument(key, content, "cache", "Set from cache", true);
            }
        }
        return content;
    }

    @Override
    public String getDocument(String key, BaseDirective directive) {
        String content = main.getDocument(key);
        if (content == null) {
            content = shadow.getDocument(key);
            if (content != null) {
                main.addDocument(key, content, "cache", "Set from cache", true);
            }
        }
        return content;
    }

    @Override
    public DocumentObject getDocumentObject(String reference) {
        return shadow.getDocumentObject(reference);
    }

    @Override
    public List<String> getDocuments(List<String> keys) {
        return shadow.getDocuments(keys);
    }

    @Override
    public boolean[] getExistence(List<String> displays) {
        return shadow.getExistence(displays);
    }

    @Override
    public RaptureDNCursor getNextDNCursor(RaptureDNCursor cursor, int count) {
        return shadow.getNextDNCursor(cursor, count);
    }

    @Override
    public String getTagDocument(String tag, String key) {
        return shadow.getTagDocument(tag, key);
    }

    @Override
    public List<String> getTags() {
        return shadow.getTags();
    }

    @Override
    public TreeObject getTreeObject(String reference) {
        return shadow.getTreeObject(reference);
    }

    public boolean isVersioned() {
        return main.isVersioned();
    }

    @Override
    public boolean removeDocument(String key, String user, String comment) {
        boolean ret = main.removeDocument(key, user, comment);
        shadow.removeDocument(key, user, comment);
        return ret;
    }

    @Override
    public boolean removeFromStage(String stage, String key) {
        boolean ret = main.removeFromStage(stage, key);
        shadow.removeFromStage(stage, key);
        return ret;
    }

    @Override
    public void removeTag(String tagName) {
        shadow.removeTag(tagName);
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        return shadow.runNativeQuery(repoType, queryParams);
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        return shadow.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
    }

    @Override
    public void setRemote(String remote, String remoteAuthority) {
        shadow.setRemote(remote, remoteAuthority);
    }

    @Override
    public void visitAll(String prefix, BaseDirective directive, RepoVisitor visitor) {
        shadow.visitAll(prefix, directive, visitor);
    }

    @Override
    public void visitFolder(String folder, BaseDirective directive, RepoVisitor visitor) {
        shadow.visitFolder(folder, directive, visitor);

    }

    @Override
    public void visitFolders(String folderPrefix, BaseDirective directive, RepoFolderVisitor visitor) {
        shadow.visitFolders(folderPrefix, directive, visitor);
    }

    @Override
    public void visitTag(String tagName, String prefix, RepoVisitor visitor) {
        shadow.visitTag(tagName, prefix, visitor);
    }

    @Override
    public void visitTagFolder(String tagName, String folder, RepoVisitor visitor) {
        shadow.visitTagFolder(tagName, folder, visitor);
    }

    @Override
    public void writeCommitObject(String reference, CommitObject commit) {
        shadow.writeCommitObject(reference, commit);
    }

    @Override
    public void writeDocumentObject(String reference, DocumentObject docObject) {
        shadow.writeDocumentObject(reference, docObject);
    }

    @Override
    public void writeTreeObject(String reference, TreeObject treeObject) {
        shadow.writeTreeObject(reference, treeObject);
    }

    @Override
    public DocumentWithMeta getDocAndMeta(String disp, BaseDirective directive) {
        return shadow.getDocAndMeta(disp, directive);
    }

    @Override
    public List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris) {
        return shadow.getDocAndMetas(uris);
    }

    @Override
    public DocumentMetadata getMeta(String key, BaseDirective directive) {
        return shadow.getMeta(key, directive);
    }

    @Override
    public DocumentWithMeta revertDoc(String disp, BaseDirective directive) {
        return shadow.revertDoc(disp, directive);
    }

    @Override
    public DocumentWithMeta addDocumentWithVersion(String disp, String content, String user, String comment, boolean mustBeNew, int expectedVersion) {
        DocumentWithMeta ret = main.addDocumentWithVersion(disp, content, user, comment, mustBeNew, expectedVersion);
        shadow.addDocumentWithVersion(disp, content, user, comment, mustBeNew, expectedVersion);
        return ret;
    }

    @Override
    public List<RaptureFolderInfo> getChildren(String displayNamePart) {
        return main.getChildren(displayNamePart);
    }

    @Override
    public List<RaptureFolderInfo> removeChildren(String area, Boolean force) {
        shadow.removeChildren(area, force);
        return main.removeChildren(area, force);
    }

    @Override
    public boolean hasMetaContent() {
        return main.hasMetaContent();
    }

    @Override
    public List<String> getAllChildren(String area) {
        return main.getAllChildren(area);
    }

    @Override
    public List<DocumentVersionInfo> getVersionHistory(String key) {
        return main.getVersionHistory(key);
    }

    @Override
    public List<DocumentWithMeta> getVersionMeta(String key, List<Integer> versions) {
        return main.getVersionMeta(key, versions);
    }

    @Override
    public List<Boolean> removeVersionMeta(String key, List<Integer> versions) {
        shadow.removeVersionMeta(key, versions);
        return main.removeVersionMeta(key, versions);
    }

    @Override
    public void setDocAttribute(RaptureURI uri, DocumentAttribute attribute) {
        main.setDocAttribute(uri, attribute);
        shadow.setDocAttribute(uri, attribute);
    }

    @Override
    public DocumentAttribute getDocAttribute(RaptureURI uri) {
        DocumentAttribute att = main.getDocAttribute(uri);
        if (att == null) {
            att = shadow.getDocAttribute(uri);
            if (att != null) {
                main.setDocAttribute(uri, att);
            }
        }
        return att;
    }

    @Override
    public List<DocumentAttribute> getDocAttributes(RaptureURI uri) {
        List<DocumentAttribute> atts = main.getDocAttributes(uri);
        if (atts == null || atts.isEmpty()) {
            atts = shadow.getDocAttributes(uri);
            if (atts != null && !atts.isEmpty()) {
                for (DocumentAttribute da : atts) {
                    main.setDocAttribute(new RaptureURI(da.getKey()), da);
                }
            }
        }
        return atts;
    }

    @Override
    public Boolean deleteDocAttribute(RaptureURI uri) {
        shadow.deleteDocAttribute(uri);
        return main.deleteDocAttribute(uri);
    }

    @Override
    public void setIndexProducer(IndexProducer producer) {
        main.setIndexProducer(producer);
        shadow.setIndexProducer(producer);
    }

    @Override
    public boolean hasIndexProducer() {
        return main.hasIndexProducer();
    }

    @Override
    public TableQueryResult findIndex(String query) {
        return main.findIndex(query);
    }

    @Override
    public Boolean validate() {
        return main.validate() && shadow.validate();
    }

    @Override
    public Map<String, String> getStatus() {
        return main.getStatus();
    }

    @Override
    public Optional<IndexHandler> getIndexHandler() {
        return main.getIndexHandler();
    }

	@Override
	public DocumentWithMeta addTagToDocument(String user, String docPath,
			String tagUri, String value) {
		DocumentWithMeta dwm = main.addTagToDocument(user, docPath, tagUri, value);
		shadow.addTagToDocument(user, docPath, tagUri, value);
		return dwm;
	}
}
