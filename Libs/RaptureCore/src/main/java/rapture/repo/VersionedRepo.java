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

import rapture.common.LockHandle;
import rapture.common.RaptureConstants;
import rapture.common.RaptureDNCursor;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.DocumentAttribute;
import rapture.common.model.DocumentVersionInfo;
import rapture.common.model.DocumentWithMeta;
import rapture.common.model.RaptureCommit;
import rapture.common.model.RemoteLink;
import rapture.common.repo.BaseObject;
import rapture.common.repo.CommentaryObject;
import rapture.common.repo.CommitObject;
import rapture.common.repo.DocumentBagObject;
import rapture.common.repo.DocumentBagReference;
import rapture.common.repo.DocumentObject;
import rapture.common.repo.PerspectiveObject;
import rapture.common.repo.TagObject;
import rapture.common.repo.TreeObject;
import rapture.dsl.dparse.BaseDirective;
import rapture.index.IndexHandler;
import rapture.lock.ILockingHandler;
import rapture.repo.db.KeyedDatabase;
import rapture.repo.db.ObjectDatabase;
import rapture.repo.stage.CommitCollector;
import rapture.repo.stage.Stage;
import rapture.repo.stage.StageTree;
import rapture.util.IDGenerator;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Optional;

/**
 * A repo contains an objectdatabase and references to perspectives It is used
 * to manage access to typed content in Rapture
 *
 * @author amkimian
 */
public class VersionedRepo extends BaseRepo implements Repository {
    private static Logger log = Logger.getLogger(VersionedRepo.class);
    private static final String ERROR_CANNOT_RUN = "Cannot run on versioned repo";

    private static final String OFFICIAL = RaptureConstants.OFFICIAL_STAGE;

    private static final String DEFAULT_STAGE = RaptureConstants.OFFICIAL_STAGE;

    private ObjectDatabase objDb;

    private KeyedDatabase keyDb;
    private KeyStore cacheKeyStore;

    private static String setupKey = "$SETUP";
    private Map<String, Stage> stages;
    private int capacity = 500;
    private ILockingHandler lockHandler;

    public VersionedRepo(Map<String, String> config, KeyStore store, KeyStore cacheKeyStore, ILockingHandler lockHandler) {
        super(store, config);
        this.cacheKeyStore = cacheKeyStore;
        this.lockHandler = lockHandler;
        if (config != null && config.containsKey("capacity")) {
            try {
                capacity = Integer.parseInt(config.get("capacity"));
            } catch (Exception e) {
                log.error("Invalid capacity " + config.get("capacity"));
                log.error("Will use default of 500");
            }
        }
        store.resetFolderHandling();
        objDb = new ObjectDatabase(store);
        keyDb = new KeyedDatabase(store);
        stages = new HashMap<String, Stage>();
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        if (po == null) {
            initBlankEnv();
        } else {
            if (!cacheKeyStore.containsKey(setupKey)) {
                log.info("Initializing vcache");
                initCacheStore();
                log.info("Initializing vcache - done");
            }
        }
    }

    @Override
    public void addCommentary(String key, String who, String description, String commentaryKey, String ref) {
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        final CommentaryObject comment = new CommentaryObject();
        comment.setCommentaryKey(commentaryKey);
        comment.setWho(who);
        comment.setWhen(new Date());
        comment.setMessage(description);

        handleCommentaryFromCommit(cObj, key, new CommentaryHandler() {

            @Override
            public boolean handleObject(BaseObject b) {
                try {
                    String ref = objDb.writeCommentary(comment);
                    b.getCommentaryReferences().add(ref);
                    return true;
                } catch (Exception e) {
                    log.error("Could not handle commentary from commit - " + e.getMessage());
                }
                return false;
            }

        });
    }

    @Override
    public DocumentWithMeta addDocument(String key, String value, String user, String comment, boolean mustBeNew) {
        String context = IDGenerator.getUUID();
        LockHandle lockHandle = lockHandler.acquireLock(context, DEFAULT_STAGE, 5, 5);
        if (lockHandle != null) {
            try {
                // Create a new stage to be consistent
                createStage(DEFAULT_STAGE);
                addToStage(DEFAULT_STAGE, key, value, mustBeNew);
                commitStage(DEFAULT_STAGE, user, comment);

                cacheKeyStore.put(key, value);
            } finally {
                lockHandler.releaseLock(context, DEFAULT_STAGE, lockHandle);
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not get lock for write");
        }
        return null; // Deprecated repository so don't do anything special here.
    }

    @Override
    public void addDocuments(List<String> keys, String value, String user, String comment) {
        // We want to add these displayNames, but point them to the same
        // document content
        String context = IDGenerator.getUUID();
        LockHandle lockHandle = lockHandler.acquireLock(context, DEFAULT_STAGE, 5, 5);
        if (lockHandle != null) {
            try {
                // Create a new stage to be consistent
                createStage(DEFAULT_STAGE);
                addToStage(DEFAULT_STAGE, keys, value, false);
                commitStage(DEFAULT_STAGE, user, comment);
                for (String x : keys) {
                    cacheKeyStore.put(x, value);
                }
            } finally {
                lockHandler.releaseLock(context, DEFAULT_STAGE, lockHandle);
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not get lock for write");
        }
    }

    private void addToStage(String stage, List<String> keys, String document, boolean mustBeNew) {
        Stage st = getStage(stage);
        DocumentObject doc = new DocumentObject();
        doc.setContent(new JsonContent(document));
        for (String path : keys) {
            String[] p = path.split("/");
            LinkedList<String> parts = new LinkedList<String>();
            for (String a : p) {
                parts.add(a);
            }
            st.getStageBase().addDocumentToStage(this, parts, doc, mustBeNew);
        }
    }

    /**
     * Add a document to the stage
     *
     * @throws Exception
     */

    public void addToStage(String stage, String path, String document, boolean mustBeNew) {
        Stage st = getStage(stage);
        DocumentObject doc = new DocumentObject();
        doc.setContent(new JsonContent(document));
        String[] p = path.split("/");
        LinkedList<String> parts = new LinkedList<String>();
        for (String a : p) {
            parts.add(a);
        }
        st.getStageBase().addDocumentToStage(this, parts, doc, mustBeNew);
    }

    @Override
    public void clearRemote() {
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        po.setRemoteLink(null);
        keyDb.writePerspective(OFFICIAL, po);

    }

    public void commitStage(String stage, String user, String comment) {
        Stage st = getStage(stage);
        CommitObject cObj = new CommitObject();
        cObj.setUser(user);
        cObj.setWhen(new Date());
        cObj.setComment(comment);
        cObj.setPreviousReference(st.getPerspective().getLatestCommit());
        String cRef = objDb.getCommitReference(cObj);
        cObj.setCommitRef(cRef);
        CommitCollector collector = new CommitCollector();
        Map<DocumentObject, String> docCollector = new HashMap<DocumentObject, String>();
        String reference = st.getStageBase().commitStage(this, cRef, collector, docCollector);
        collector.addTreeReference(reference);
        cObj.setTreeRef(reference);
        cObj.setChanges(collector.toString());
        cObj.setTreeReferences(collector.getTreeReferences());
        cObj.setDocReferences(collector.getDocReferences());
        objDb.writeCommit(cObj, cRef);
        st.getPerspective().setLatestCommit(cRef);
        // Need to write this back... (perhaps checking that the perspective on
        // db hasn't changed...!)
        keyDb.writePerspective(st.getPerspectiveName(), st.getPerspective());
    }

    @Override
    public long countDocuments() throws RaptNotSupportedException {
        throw new RaptNotSupportedException("Count not supported for versioned repo");
    }

    /**
     * Create a new perspective based on the one passed in
     *
     * @param basePerspective
     * @param name
     * @throws Exception
     */
    public void createPerspective(String basePerspective, String name) {
        PerspectiveObject original = keyDb.getPerspective(basePerspective);
        PerspectiveObject newP = new PerspectiveObject();
        newP.setBaseCommit(original.getLatestCommit());
        newP.setLatestCommit(original.getLatestCommit());
        newP.setOwner("caller");
        keyDb.writePerspective(name, newP);
    }

    /**
     * Create a stage on which to do some changes. A stage is kind of like a
     * realised tree with additional "overrides" on some names which reflect new
     * concepts created
     *
     * @throws Exception
     */

    @Override
    public Stage createStage(String stageName) {
        Stage stage = new Stage();
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        stage.setPerspectiveName(OFFICIAL);
        stage.setPerspective(po);
        CommitObject co = objDb.getCommit(po.getLatestCommit());
        TreeObject to = objDb.getTree(co.getTreeRef());
        StageTree st = new StageTree(to, capacity);
        stage.setStageBase(st);
        stages.put(stageName, stage);
        return stage;
    }

    @Override
    public void createTag(String user, String tagName) {
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        TagObject to = new TagObject();
        to.setCommitRef(po.getLatestCommit());
        to.setOwner(user);
        to.setWhen(new Date());
        keyDb.writeTag(tagName, to);
    }

    @Override
    public void drop() {
        if (cacheKeyStore != null) {
            cacheKeyStore.dropKeyStore();
        }
        if (!store.dropKeyStore()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not drop keystore");
        }
    }

    @Override
    public List<CommentaryObject> getCommentary(String key) {
        // Given a perspective and key, load the associated BasedObject and get
        // the list of references. Then load
        // each CommentaryObject from those references and return that
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        final List<CommentaryObject> ret = new ArrayList<CommentaryObject>();
        handleCommentaryFromCommit(cObj, key, new CommentaryHandler() {

            @Override
            public boolean handleObject(BaseObject b) {
                List<String> commentaryRefs = b.getCommentaryReferences();
                for (String c : commentaryRefs) {
                    CommentaryObject comment;
                    try {
                        comment = objDb.getCommentary(c);
                        if (comment != null) {
                            ret.add(comment);
                        }
                    } catch (RaptureException e) {
                        log.error("Could not load commentary object " + c);
                    }
                }
                return false; // Don't need to save BaseObject back
            }

        });
        return ret;
    }

    @Override
    public List<RaptureCommit> getCommitHistory() {
        return getCommitsSince(null);
    }

    @Override
    public CommitObject getCommitObject(String reference) {
        return objDb.getCommit(reference);
    }

    @Override
    public List<RaptureCommit> getCommitsSince(String commitReference) {
        List<RaptureCommit> commits = new ArrayList<RaptureCommit>();
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        boolean done = false;
        while (!done) {
            RaptureCommit co = new RaptureCommit();
            co.setComment(cObj.getComment());
            co.setWhen(cObj.getWhen());
            co.setWho(cObj.getUser());
            co.setChanges(cObj.getChanges());
            co.setReference(cObj.getCommitRef());
            co.setDocReferences(cObj.getDocReferences());
            co.setTreeReferences(cObj.getTreeReferences());
            commits.add(co);
            String ref = cObj.getPreviousReference();
            if (ref != null && ref.equals(commitReference)) {
                done = true;
            } else {
                if (ref != null) {
                    cObj = objDb.getCommit(ref);
                } else {
                    cObj = null;
                }
                if (cObj == null) {
                    done = true;
                }
            }
        }

        return commits;
    }

    private CommitObject getCommitViaDirective(CommitObject startPoint, String context, BaseDirective directive) {
        if (directive == null) {
            return startPoint;
        }
        directive.reset(context);
        CommitObject cObj = startPoint;
        while (directive.incorrect(cObj)) {
            cObj = objDb.getCommit(cObj.getPreviousReference());
            if (cObj == null) {
                break;
            }
        }
        return directive.retrieveCommit();
    }

    public String getDocument(String documentName) {
        // Retrieve a document from perspective, given its path
        String doc = cacheKeyStore.get(documentName);
        if (doc != null) {
            return doc;
        }

        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        return getDocumentFromCommit(documentName, cObj);
    }

    @Override
    public String getDocument(String documentName, BaseDirective directive) {
        if (directive == null) {
            return getDocument(documentName);
        }
        // In this request we start with the given commit object (the latest in
        // the perspective) and
        // use the directive to work back until we find the correct commit. Then
        // we get the document
        // from that commit
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        cObj = getCommitViaDirective(cObj, documentName, directive);

        if (cObj != null) {
            String ret = getDocumentFromCommit(documentName, cObj);
            return ret;
        } else {
            return null;
        }
    }

    public String getDocumentFromCommit(String documentName, CommitObject cObj) {
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        String[] parts = documentName.split("/");
        String ret = null;
        for (int i = 0; i < parts.length; i++) {
            if (tObj == null) {
                break;
            }
            if (i == parts.length - 1) {
                for (DocumentBagReference bagRef : tObj.getDocuments()) {
                    DocumentBagObject dObj = objDb.getDocumentBag(bagRef.getBagRef());

                    ret = dObj.getDocRefs().get(parts[i]);
                    if (ret != null) {
                        ret = objDb.getDocument(ret).getContent().toString();
                        break;
                    }
                }
            } else {
                String treeRef = tObj.getTrees().get(parts[i]);
                tObj = objDb.getTree(treeRef);
            }
        }
        return ret;
    }

    @Override
    public DocumentObject getDocumentObject(String reference) {
        return objDb.getDocument(reference);
    }

    @Override
    public List<String> getDocuments(List<String> keys) {
        // Ideally use the batch method of the cacheStore
        List<String> docs = null;
        List<String> docsFromCache = cacheKeyStore.getBatch(keys);
        docs = new ArrayList<String>();
        // Now check for cache misses
        for (int i = 0; i < docsFromCache.size(); i++) {
            String d = docsFromCache.get(i);
            if (d == null) {
                d = getDocument(keys.get(i));
                if (d != null) {
                    log.info("Cache miss for " + keys.get(0) + " during batch operation");
                    cacheKeyStore.put(keys.get(i), d);
                }
            }
            docs.add(d);
        }
        return docs;
    }

    @Override
    // For each of the displaynames (document names)
    // Does that document exist in the perspective point?
    public boolean[] getExistence(List<String> displays) {
        boolean ret[] = new boolean[displays.size()];
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        List<String> displaysCopy = new ArrayList<String>();
        displaysCopy.addAll(displays);

        // Try this level
        String level = "";
        List<DocumentBagReference> refs = tObj.getDocuments();
        for (DocumentBagReference r : refs) {
            DocumentBagObject dObj = objDb.getDocumentBag(r.getBagRef());
            for (String key : dObj.getDocRefs().keySet()) {
                String testKey = level + key;
                if (displaysCopy.contains(testKey)) {
                    int pos = displays.indexOf(testKey);
                    ret[pos] = true;
                    displaysCopy.remove(testKey);
                    if (displaysCopy.isEmpty()) {
                        break;
                    }
                }
            }
            if (displaysCopy.isEmpty()) {
                break;
            }
        }

        // And now dive into another level
        if (displaysCopy.isEmpty()) {
            return ret;
        } else {
            for (Map.Entry<String, String> tree : tObj.getTrees().entrySet()) {
                String newEntry = level.isEmpty() ? tree.getKey() : level + "/" + tree.getKey();
                TreeObject innerObj = objDb.getTree(tree.getValue());
                ret = walkInnerExistence(newEntry, innerObj, displaysCopy, displays, ret);
                if (displaysCopy.isEmpty()) {
                    break;
                }
            }
        }

        return ret;
    }

    @Override
    public RaptureDNCursor getNextDNCursor(RaptureDNCursor cursor, int count) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not yet implemented");
    }

    public ObjectDatabase getObjectDatabase() {
        return objDb;
    }

    private Stage getStage(String name) {
        Stage s = stages.get(name);
        if (s == null) {
            s = createStage(name);
        }
        return s;
    }

    @Override
    public String getTagDocument(String tag, String key) {
        TagObject to = keyDb.getTag(tag);
        CommitObject cObj = objDb.getCommit(to.getCommitRef());
        return getDocumentFromCommit(key, cObj);
    }

    @Override
    public List<String> getTags() {
        return keyDb.getTags();
    }

    @Override
    public TreeObject getTreeObject(String reference) {
        return objDb.getTree(reference);
    }

    private void handleCommentaryFromCommit(CommitObject cObj, String key, CommentaryHandler handler) {
        // Now visit the tree to find the document referenced by key
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        String[] parts = key.split("/");
        String ultimateRef = null;
        for (int i = 0; i < parts.length; i++) {
            if (tObj == null) {
                break;
            }
            if (i == parts.length - 1) {
                for (DocumentBagReference bagRef : tObj.getDocuments()) {
                    DocumentBagObject dObj = objDb.getDocumentBag(bagRef.getBagRef());
                    ultimateRef = dObj.getDocRefs().get(parts[i]);
                    if (ultimateRef == null) {
                        ultimateRef = tObj.getTrees().get(parts[i]);
                        TreeObject content = objDb.getTree(ultimateRef);
                        if (content != null) {
                            if (handler.handleObject(content)) {
                                objDb.writeTree(content, ultimateRef);
                            }
                        }
                    } else {
                        DocumentObject content = objDb.getDocument(ultimateRef);
                        if (handler.handleObject(content)) {
                            objDb.writeDocument(content, ultimateRef);
                        }
                    }
                }
            } else {
                String treeRef = tObj.getTrees().get(parts[i]);
                tObj = objDb.getTree(treeRef);
            }
        }
    }

    /**
     * A test function really to setup a blank environment
     *
     * @
     */
    private void initBlankEnv() {
        // Create an initial commit with a null tree
        TreeObject tObj = new TreeObject();
        String ref = objDb.writeTree(tObj);
        CommitObject cObj = new CommitObject();
        cObj.setTreeRef(ref);
        cObj.setUser("admin");
        cObj.setWhen(new Date());
        cObj.setComment("Repo creation");
        String cRef = objDb.getCommitReference(cObj);
        cObj.setCommitRef(cRef);
        List<String> refs = new ArrayList<String>();
        refs.add(ref);
        cObj.setTreeReferences(refs);
        cObj.setDocReferences(new ArrayList<String>());
        objDb.writeCommit(cObj, cRef);

        PerspectiveObject main = new PerspectiveObject();
        main.setBaseCommit(cRef);
        main.setLatestCommit(cRef);
        main.setDescription("Main branch of work");
        main.setOwner(OFFICIAL);
        main.setWhen(new Date());

        keyDb.writePerspective(OFFICIAL, main);
        createStage(OFFICIAL);
    }

    private void initCacheStore() {
        // Basically copy the latest keys/values to the cacheKeyStore
        visitAll("", null, new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    System.out.println(name);
                    if (!cacheKeyStore.containsKey(name)) {
                        log.info("VCache initialization, writing " + name);
                        try {
                            cacheKeyStore.put(name, content.getContent());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }

        });
        cacheKeyStore.put(setupKey, "done");
    }

    public boolean isVersioned() {
        return true;
    }

    /**
     * Archives old versions
     *
     * @param versionLimit       number of versions to retain
     * @param timeLimit          commits older than timeLimit will be archived
     * @param ensureVersionLimit ensure number of versions to retain even if commit is older than timeLimit
     * @param user
     * @return
     */
    public boolean archiveRepoVersions(int versionLimit, long timeLimit, boolean ensureVersionLimit, String user) {
        if (versionLimit <= 0) {
            log.error("versionLimit should > 0");
            return false;
        }

        PerspectiveObject perspective = keyDb.getPerspective(OFFICIAL);
        CommitObject commitObj = objDb.getCommit(perspective.getLatestCommit());
        if (commitObj.getWhen().getTime() < timeLimit && !ensureVersionLimit) {
            throw RaptureExceptionFactory.create("Invalid archive - no version left");
        }
        List<String> keysToArchive = new ArrayList<String>();
        int versionsRemaining = versionLimit;
        while (commitObj != null) {
            // do not archive the very first commit: repo creation
            if (StringUtils.isEmpty(commitObj.getPreviousReference())) {
                break;
            }
            // archive commit if it's older than specified timestamp, and no ensureVersions
            boolean isCommitTooOld = commitObj.getWhen().getTime() < timeLimit && !ensureVersionLimit;
            if (versionsRemaining <= 0 || isCommitTooOld) {
                // archive commit, along with its docref and treerefs
                getKeysToArchive(commitObj, keysToArchive);
            } else {
                versionsRemaining--;
            }
            // move on to previous commit
            commitObj = objDb.getCommit(commitObj.getPreviousReference());
        }

        return objDb.delete(keysToArchive);
    }

    private void getKeysToArchive(CommitObject commitObj, List<String> keysToArchive) {
        keysToArchive.add(commitObj.getCommitRef());
        // add doc refs to archive
        keysToArchive.addAll(commitObj.getDocReferences());
        // add tree refs to archive
        for (String treeRef : commitObj.getTreeReferences()) {
            keysToArchive.add(treeRef);
            // add document bag ref to archive
            TreeObject treeObject = objDb.getTree(treeRef);
            List<DocumentBagReference> docBagRefs = treeObject.getDocuments();
            if (docBagRefs != null) {
                for (DocumentBagReference bagRef : docBagRefs) {
                    keysToArchive.add(bagRef.getBagRef());
                }
            }
        }
    }

    /**
     * Merge one perspective into another
     *
     * @param targetPerspetive
     * @param sourcePerspective
     * @throws Exception
     */
    public void mergePerspective(String targetPerspetive, String sourcePerspective) {
        // We need to look at the target and sourcePerspectives with respect to
        // their commits
        // We find out a common ancestor commit, and start a Stage from the
        // targetPerspective
        // We then continuously apply the treeObject from the common ancestor
        // point to the commit in the sourcePerspective
        // That final stage is the one we commit to move the targetPerspective
        // forward.

        PerspectiveObject target = keyDb.getPerspective(targetPerspetive);
        PerspectiveObject source = keyDb.getPerspective(sourcePerspective);
        CommitObject co = objDb.getCommit(target.getLatestCommit());
        TreeObject to = objDb.getTree(co.getTreeRef());
        StageTree st = new StageTree(to, capacity);
        // Now apply each tree object associated with the commits between the
        // latest commit in source and the start point
        List<String> commitList = new ArrayList<String>();
        String commitRef = source.getLatestCommit();
        while (!commitRef.equals(source.getBaseCommit())) {
            CommitObject cotest = objDb.getCommit(commitRef);
            commitList.add(commitRef);
            commitRef = cotest.getPreviousReference();
        }
        // We now have a list of trees to apply (but in the wrong order)
        for (int i = commitList.size() - 1; i >= 0; i--) {
            CommitObject cob = objDb.getCommit(commitList.get(i));
            TreeObject apply = objDb.getTree(cob.getTreeRef());
            st.apply(this, apply, commitList.get(i));
        }
        // And now commit this stage
        CommitObject cObj = new CommitObject();
        cObj.setUser("merge");
        cObj.setWhen(new Date());
        cObj.setPreviousReference(target.getLatestCommit());
        String cRef = objDb.getCommitReference(cObj);
        cObj.setCommitRef(cRef);

        CommitCollector collector = new CommitCollector();
        String reference = st.commitStage(this, cRef, collector, new HashMap<DocumentObject, String>());
        cObj.setTreeRef(reference);
        cObj.setChanges(collector.toString());
        objDb.writeCommit(cObj, cRef);
        target.setLatestCommit(cRef);
        source.setLatestCommit(cRef);
        source.setBaseCommit(cRef);
        keyDb.writePerspective(targetPerspetive, target);
        keyDb.writePerspective(sourcePerspective, source);
    }

    private void queryForTree(TreeObject tObj, String indent) {
        for (DocumentBagReference bagRef : tObj.getDocuments()) {
            DocumentBagObject dObj = objDb.getDocumentBag(bagRef.getBagRef());
            for (Map.Entry<String, String> docEntries : dObj.getDocRefs().entrySet()) {
                log.info(indent + docEntries.getKey() + " - " + objDb.getDocument(docEntries.getValue()).getContent().toString());
            }
        }
        for (Map.Entry<String, String> treeEntries : tObj.getTrees().entrySet()) {
            log.info(indent + treeEntries.getKey() + " -V-");
            TreeObject inner = objDb.getTree(treeEntries.getValue());
            queryForTree(inner, indent + "   ");
        }
    }

    /**
     * For now this just dumps out the perspective by walking the associated
     * tree
     *
     * @param perspective
     * @throws Exception
     */
    public void queryPerspective(String perspective) {
        PerspectiveObject po = keyDb.getPerspective(perspective);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        queryForTree(tObj, "");
    }

    @Override
    public boolean removeDocument(String key, String user, String comment) {
        // Need to reset stage
        boolean removed = false;

        if (!stages.containsKey(OFFICIAL)) {
            createStage(OFFICIAL);
        }
        removed = removeFromStage(OFFICIAL, key);
        if (removed) {
            commitStage(OFFICIAL, user, comment);
        }
        cacheKeyStore.delete(key);
        return true;
    }

    @Override
    public boolean removeFromStage(String stage, String path) {
        Stage st = getStage(stage);
        String[] p = path.split("/");
        LinkedList<String> parts = new LinkedList<String>();
        for (String a : p) {
            parts.add(a);
        }
        return st.getStageBase().removeFromStage(this, parts);
    }

    @Override
    public void removeTag(String tagName) {
        keyDb.deleteTag(tagName);
    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType, List<String> queryParams) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, ERROR_CANNOT_RUN);
    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(String repoType, List<String> queryParams, int limit, int offset) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, ERROR_CANNOT_RUN);
    }

    @Override
    public void setRemote(String remote, String remoteAuthority) {
        // IF the perspective does not exist, throw, otherwise update the
        // perspective and
        // save it back
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        RemoteLink rl = new RemoteLink();
        rl.setAuthority(remoteAuthority);
        rl.setRemoteId(remote);
        rl.setPerspective(OFFICIAL);
        po.setRemoteLink(rl);
        keyDb.writePerspective(OFFICIAL, po);
    }

    @Override
    public void visitAll(String prefix, BaseDirective directive, RepoVisitor visitor) {
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        cObj = getCommitViaDirective(cObj, null, directive);
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        // TODO: optimise this by finding the correct part of the tree based on
        // the prefix
        visitTree(tObj, prefix, visitor, "");
    }

    @Override
    public List<RaptureFolderInfo> getChildren(String displayNamePart) {
        return null;
    }

    @Override
    public List<RaptureFolderInfo> removeChildren(String area, Boolean force) {
        return null;
    }

    @Override
    public void visitFolder(String folder, BaseDirective directive, RepoVisitor visitor) {
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        cObj = getCommitViaDirective(cObj, null, directive);
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        visitFolderFromTree(folder, visitor, tObj);
    }

    public void visitFolderFromTree(String folder, RepoVisitor visitor, TreeObject ptObj) {
        TreeObject tObj = ptObj;

        if (!folder.isEmpty()) {
            String[] parts = folder.split("/");
            for (String p : parts) {
                if (tObj.getTrees().containsKey(p)) {
                    tObj = objDb.getTree(tObj.getTrees().get(p));
                } else {
                    throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not found");
                }
            }
        }
        // Now tObj is where we should be
        for (DocumentBagReference bagRef : tObj.getDocuments()) {
            DocumentBagObject dObj = objDb.getDocumentBag(bagRef.getBagRef());
            for (Map.Entry<String, String> docEntries : dObj.getDocRefs().entrySet()) {
                if (!docEntries.getKey().isEmpty()) {
                    visitor.visit(docEntries.getKey(), objDb.getDocument(docEntries.getValue()).getContent(), false);
                }
            }
        }
        for (Map.Entry<String, String> folderEntries : tObj.getTrees().entrySet()) {
            visitor.visit(folderEntries.getKey(), null, true);
        }
    }

    @Override
    public void visitFolders(String folderPrefix, BaseDirective directive, final RepoFolderVisitor visitor) {
        // Visit just the folders, that match this prefix
        if (directive == null) {
            cacheKeyStore.visit(folderPrefix, new RepoVisitor() {
                @Override
                public boolean visit(String name, JsonContent content, boolean isFolder) {
                    visitor.folder(name);
                    return true;
                }
            });
            return;
        }
        PerspectiveObject po = keyDb.getPerspective(OFFICIAL);
        CommitObject cObj = objDb.getCommit(po.getLatestCommit());
        cObj = getCommitViaDirective(cObj, null, directive);
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        visitFolderFromTree(folderPrefix, new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                visitor.folder(name);
                return true;
            }

        }, tObj);
    }

    @Override
    public void visitTag(String tagName, String prefix, RepoVisitor visitor) {
        TagObject to = keyDb.getTag(tagName);
        CommitObject cObj = objDb.getCommit(to.getCommitRef());
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        visitTree(tObj, prefix, visitor, "");
    }

    @Override
    public void visitTagFolder(String tagName, String folder, RepoVisitor visitor) {
        TagObject to = keyDb.getTag(tagName);
        CommitObject cObj = objDb.getCommit(to.getCommitRef());
        TreeObject tObj = objDb.getTree(cObj.getTreeRef());
        visitFolderFromTree(folder, visitor, tObj);
    }

    private boolean visitTree(TreeObject tObj, String prefix, RepoVisitor visitor, String treePrefix) {
        // Visit docs first

        for (DocumentBagReference bagRef : tObj.getDocuments()) {
            DocumentBagObject dObj = objDb.getDocumentBag(bagRef.getBagRef());
            for (Map.Entry<String, String> docEntries : dObj.getDocRefs().entrySet()) {
                if (treePrefix.startsWith(prefix)) {
                    // If we cannot visit a document, (i.e. an exception is
                    // thrown, continue)
                    try {
                        if (!visitor.visit(treePrefix + docEntries.getKey(), objDb.getDocument(docEntries.getValue()).getContent(), false)) {
                            return false;
                        }
                    } catch (RaptureException e) {
                        log.error("Could not decode document " + e.getMessage());
                    }
                }
            }
        }
        for (Map.Entry<String, String> treeEntries : tObj.getTrees().entrySet()) {
            TreeObject inner = objDb.getTree(treeEntries.getValue());
            if (!visitTree(inner, prefix, visitor, treePrefix + treeEntries.getKey() + "/")) {
                return false;
            }
        }
        return true;
    }

    private boolean[] walkInnerExistence(String level, TreeObject tObj, List<String> displaysCopy, List<String> displays, boolean[] ret) {
        List<DocumentBagReference> refs = tObj.getDocuments();
        for (DocumentBagReference r : refs) {
            DocumentBagObject dObj = objDb.getDocumentBag(r.getBagRef());
            for (String key : dObj.getDocRefs().keySet()) {
                String testKey = level.isEmpty() ? key : level + "/" + key;
                if (displaysCopy.contains(testKey)) {
                    int pos = displays.indexOf(testKey);
                    ret[pos] = true;
                    displaysCopy.remove(testKey);
                    if (displaysCopy.isEmpty()) {
                        break;
                    }
                }
            }
            if (displaysCopy.isEmpty()) {
                break;
            }
        }

        // And now dive into another level
        if (displaysCopy.isEmpty()) {
            return ret;
        } else {
            for (Map.Entry<String, String> tree : tObj.getTrees().entrySet()) {
                String newEntry = level + "/" + tree.getKey();
                TreeObject innerObj = objDb.getTree(tree.getValue());
                ret = walkInnerExistence(newEntry, innerObj, displaysCopy, displays, ret);
                if (displaysCopy.isEmpty()) {
                    break;
                }
            }
        }
        return ret;
    }

    @Override
    public void writeCommitObject(String reference, CommitObject commit) {
        objDb.writeCommit(commit, reference);
    }

    @Override
    public void writeDocumentObject(String reference, DocumentObject docObject) {
        objDb.writeDocument(docObject, reference);
    }

    @Override
    public void writeTreeObject(String reference, TreeObject treeObject) {
        objDb.writeTree(treeObject, reference);
    }

    @Override
    public DocumentWithMeta addDocumentWithVersion(String disp, String content, String user, String comment, boolean mustBeNew, int expectedVersion) {
        return addDocument(disp, content, user, comment, mustBeNew);
    }

    @Override
    public boolean hasMetaContent() {
        return false;
    }

    @Override
    public List<DocumentVersionInfo> getVersionHistory(String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$		

    }

    @Override
    public List<DocumentWithMeta> getVersionMeta(String key, List<Integer> versions) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, Messages.getString("BaseSimpleRepo.notsupp")); //$NON-NLS-1$		

    }

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
    public Boolean validate() {
        return store.validate();
    }

    @Override
    public Optional<IndexHandler> getIndexHandler() {
        return Optional.absent();
    }

	@Override
	public DocumentWithMeta addTagToDocument(String user, String docPath,
			String tagUri, String value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentWithMeta addTagsToDocument(String user, String docPath,
			Map<String, String> tagMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentWithMeta removeTagFromDocument(String user, String docPath,
			String tagUri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentWithMeta removeTagsFromDocument(String user, String docPath,
			List<String> tags) {
		// TODO Auto-generated method stub
		return null;
	}
}
