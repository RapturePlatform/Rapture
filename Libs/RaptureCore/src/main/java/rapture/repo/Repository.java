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
import rapture.repo.stage.Stage;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

/**
 * The general interface to a repository Note that this is an exception to the rule that Repository is abbreviated to Repo.
 *
 * @author alan
 */
public interface Repository {
    void addCommentary(String key, String who, String description, String commentaryKey, String ref);

    /**
     * This is usually a helper for addToStage/commit for the default stage
     *
     * @param key
     * @param value
     * @param mustBeNew
     * @return - Returns the version created by this addition
     */
    long addDocument(String key, String value, String user, String comment, boolean mustBeNew);

    void addDocuments(List<String> dispNames, String content, String user, String comment);

    void addToStage(String stage, String key, String value, boolean mustBeNew);

    void clearRemote();

    void commitStage(String stage, String user, String comment);

    long countDocuments() throws RaptNotSupportedException;

    Stage createStage(String stage);

    void createTag(String user, String tagName);

    // Drop the repo data
    void drop();

    List<CommentaryObject> getCommentary(String key);

    /**
     * Return the commit history
     *
     * @return
     */
    List<RaptureCommit> getCommitHistory();

    CommitObject getCommitObject(String reference);

    List<RaptureCommit> getCommitsSince(String commitReference);

    String getDocument(String key);

    String getDocument(String key, BaseDirective directive);

    DocumentObject getDocumentObject(String reference);

    List<String> getDocuments(List<String> keys);

    // Returns the existence vector for these displaynames
    boolean[] getExistence(List<String> displays);

    RaptureDNCursor getNextDNCursor(RaptureDNCursor cursor, int count);

    String getTagDocument(String tag, String key);

    List<String> getTags();

    TreeObject getTreeObject(String reference);

    boolean isVersioned();

    /**
     * Given a display name, what is the version history (number + date)
     *
     * @param key
     * @return
     */
    List<DocumentVersionInfo> getVersionHistory(String key);

    /**
     * Given a display name, return the documents for the given versions (or null if that version doesn't exist)
     *
     * @param key
     * @param versions
     * @return
     */
    List<DocumentWithMeta> getVersionMeta(String key, List<Integer> versions);

    /**
     * Remove the versions of the documents given. Returns an array of true/false depending on whether something was deleted.
     *
     * @param key
     * @param versions
     * @return
     */
    List<Boolean> removeVersionMeta(String key, List<Integer> versions);

    /**
     * @param key
     * @param user
     * @param comment
     * @return
     */
    boolean removeDocument(String key, String user, String comment);

    boolean removeFromStage(String stage, String key);

    void removeTag(String tagName);

    // Runs a native query. Only relevant for unversioned repositories, and the
    // repoType must match
    // the native type of the repo
    RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams);

    /**
     * Runs a native query with specific limit and bounds (if supported by the underlying repo)
     *
     * @param repoType
     * @param queryParams
     * @param limit
     * @param offset
     * @return
     */
    RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset);

    void setRemote(String remote, String remoteAuthority);

    void visitAll(String prefix, BaseDirective directive, RepoVisitor visitor);

    void visitFolder(String folder, BaseDirective directive, RepoVisitor visitor);

    void visitFolders(String folderPrefix, BaseDirective directive, RepoFolderVisitor visitor);

    void visitTag(String tagName, String prefix, RepoVisitor visitor);

    void visitTagFolder(String tagName, String folder, RepoVisitor visitor);

    /**
     * Low level commands used when synchronizing a repo
     */

    void writeCommitObject(String reference, CommitObject commit);

    void writeDocumentObject(String reference, DocumentObject docObject);

    void writeTreeObject(String reference, TreeObject treeObject);

    DocumentWithMeta getDocAndMeta(String disp, BaseDirective directive);

    List<DocumentWithMeta> getDocAndMetas(List<RaptureURI> uris);

    DocumentMetadata getMeta(String key, BaseDirective directive);

    DocumentWithMeta revertDoc(String disp, BaseDirective directive);

    // Like addDocument, but if the repo supports versioning the version to be
    // overwritten
    // must exist at the version given
    boolean addDocumentWithVersion(String disp, String content, String user, String comment, boolean mustBeNew, int expectedVersion);

    List<RaptureFolderInfo> removeChildren(String area, Boolean force);

    List<RaptureFolderInfo> getChildren(String displayNamePart);

    // DOes the repo support metaContent?
    boolean hasMetaContent();

    List<String> getAllChildren(String area);

    void setDocAttribute(RaptureURI attributeUri, DocumentAttribute attribute);

    DocumentAttribute getDocAttribute(RaptureURI uri);

    List<DocumentAttribute> getDocAttributes(RaptureURI uri);

    Boolean deleteDocAttribute(RaptureURI uri);

    // If this is set, use this implementation to generate index records when data is changed, and store those index
    // records in the low level storage repo if indexes are supported.

    void setIndexProducer(IndexProducer producer);

    boolean hasIndexProducer();

    // If the repo has an index, return the result of executing this query against that index
    TableQueryResult findIndex(String query);

    /**
     * connect to the underlying resource and perform a trivial confirmation of operation
     */
    Boolean validate();

    Map<String, String> getStatus();

    Optional<IndexHandler> getIndexHandler();
}
