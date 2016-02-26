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
package rapture.repo.stage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.repo.DocumentBagObject;
import rapture.common.repo.DocumentBagReference;
import rapture.common.repo.DocumentObject;
import rapture.common.repo.TreeObject;
import rapture.repo.VersionedRepo;

/*
 * A stage tree is used to represent the merging of a TreeObject and changes at that level to a tree object
 */
public class StageTree {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(StageTree.class);

    private TreeObject shadow; // The tree object
                               // the changes below
                               // are applied
                               // against

    private Map<String, StageTree> stagedTrees;
    private Map<String, DocumentObject> stagedDocuments;
    private Map<String, DocumentObject> newStagedDocuments;
    private Set<String> removedDocuments;
    private int capacity;

    public StageTree(TreeObject base, int capacity) {
        this.shadow = base;
        this.capacity = capacity;
        reset();
    }

    /**
     * Add this document to this tree
     * 
     * If parts is of length 1, add it to this tree, otherwise either resolve
     * (shadow) an existing tree in the shadow tree as a StagedTree and add the
     * document to that (removing one from the list)
     * 
     * @param parts
     * @param doc
     * @param mustBeNew
     * @throws Exception
     */
    public void addDocumentToStage(VersionedRepo rp, List<String> parts, DocumentObject doc, boolean mustBeNew) {
        if (parts.size() == 1) {
            if (mustBeNew) {
                newStagedDocuments.put(parts.get(0), doc);
            } else {
                stagedDocuments.put(parts.get(0), doc);
            }
        } else {
            String currentLevel = parts.remove(0);
            if (stagedTrees.containsKey(currentLevel)) {
                stagedTrees.get(currentLevel).addDocumentToStage(rp, parts, doc, mustBeNew);
            } else {
                if (shadow.getTrees().containsKey(currentLevel)) {
                    TreeObject to = rp.getObjectDatabase().getTree(shadow.getTrees().get(currentLevel));
                    StageTree newStage = new StageTree(to, capacity);
                    stagedTrees.put(currentLevel, newStage);
                } else {
                    StageTree newStage = new StageTree(new TreeObject(), capacity);
                    stagedTrees.put(currentLevel, newStage);
                }
                stagedTrees.get(currentLevel).addDocumentToStage(rp, parts, doc, mustBeNew);
            }
        }
    }

    public void apply(VersionedRepo rp, TreeObject apply, String cRef) {
        // Apply the changes from this tree object into this current staged tree
        for (Map.Entry<String, String> t : apply.getTrees().entrySet()) {
            if (!shadow.getTrees().containsKey(t.getKey())) {
                addStageTree(rp, t);
            } else {
                // If it does contain the same *key*, check the value
                if (!t.getValue().equals(shadow.getTrees().get(t.getKey()))) {
                    mergeStageTree(rp, cRef, t);
                }
            }
        }

        applyDocuments(rp, apply, cRef);
    }

    private void applyDocuments(VersionedRepo rp, TreeObject apply, String cRef) {
        // Documents is a bit easier, simply overwrite for now...
        for (DocumentBagReference bagRef : apply.getDocuments()) {
            DocumentBagObject dObj = rp.getObjectDatabase().getDocumentBag(bagRef.getBagRef());
            applyToBag(rp, cRef, dObj);
        }
    }

    private void applyToBag(VersionedRepo rp, String cRef, DocumentBagObject dObj) {
        for (Map.Entry<String, String> d : dObj.getDocRefs().entrySet()) {
            boolean found = false;
            for (DocumentBagReference bagRef2 : shadow.getDocuments()) {
                DocumentBagObject dObj2 = rp.getObjectDatabase().getDocumentBag(bagRef2.getBagRef());
                if (dObj2.getDocRefs().containsKey(d.getKey())) {
                    if (!dObj2.getDocRefs().get(d.getKey()).equals(d.getValue())) {
                        // Here we have to see what we prefer - the target
                        // could
                        // have moved this document forward and
                        // that is why they are different. OR we could have
                        // moved it forward. Who wins? How do we know
                        // whether
                        // this document was modified here? - perhaps we add
                        // the
                        // commit to the BaseObject
                        found = true;
                        DocumentObject dob = rp.getObjectDatabase().getDocument(d.getValue());
                        if (cRef.equals(dob.getCommitRef())) {
                            stagedDocuments.put(d.getKey(), rp.getObjectDatabase().getDocument(d.getValue()));
                        }
                    }
                }
            }
            if (!found) {
                stagedDocuments.put(d.getKey(), rp.getObjectDatabase().getDocument(d.getValue()));
            }
        }
    }

    private void mergeStageTree(VersionedRepo rp, String cRef, Map.Entry<String, String> t) {
        // They are different, we need to merge them
        StageTree st = new StageTree(rp.getObjectDatabase().getTree(shadow.getTrees().get(t.getKey())), capacity);
        st.apply(rp, rp.getObjectDatabase().getTree(t.getValue()), cRef);
        stagedTrees.put(t.getKey(), st);
    }

    private void addStageTree(VersionedRepo rp, Map.Entry<String, String> t) {
        StageTree st = new StageTree(rp.getObjectDatabase().getTree(t.getValue()), capacity);
        stagedTrees.put(t.getKey(), st);
    }

    /**
     * Commit the changes made on this stage to the repo, returning the
     * reference for the TreeObject we will save for this.
     * 
     * @param rp
     * @param commitRef
     * @return @
     */
    public String commitStage(VersionedRepo rp, String commitRef, CommitCollector collector, Map<DocumentObject, String> savedDocRefs) {
        // We create a new tree
        TreeObject newTreeObject = new TreeObject();
        // We need to work out a new document bag structure, by removing and
        // resaving any document bags that have had
        // items removed... We then add back any documents back into the bags
        // that have room

        Set<DocumentBagObject> needToSave = new HashSet<DocumentBagObject>();
        Map<String, DocumentBagObject> maybeReuse = new HashMap<String, DocumentBagObject>();
        Set<String> refsToSave = new HashSet<String>();

        workOutCommitChanges(rp, needToSave, maybeReuse, refsToSave);
        applyNewSaves(newTreeObject, refsToSave);

        // For the ones in needToSave, we can use them to hopefully fill a gap

        Map<String, DocumentObject> combinedSet = rebalanceTree();

        for (Map.Entry<String, DocumentObject> entries : combinedSet.entrySet()) {

            // If the document is already in the savedDocRefs, we don't need to
            // save it, we should simply get the reference
            // from that
            String reference = null;
            if (savedDocRefs.containsKey(entries.getValue())) {
                reference = savedDocRefs.get(entries.getValue());
            } else {
                entries.getValue().setCommitRef(commitRef);
                reference = rp.getObjectDatabase().writeDocument(entries.getValue());
                savedDocRefs.put(entries.getValue(), reference);
            }

            collector.addDocReference(reference);
            collector.addDocName(entries.getKey());

            // Find a spot to put this reference in
            boolean done = false;

            for (DocumentBagObject d : needToSave) {
                if (d.getDocRefs().size() < capacity) {
                    d.getDocRefs().put(entries.getKey(), reference);
                    done = true;
                    break;
                }
            }
            if (!done && maybeReuse.size() != 0) {
                Map.Entry<String, DocumentBagObject> d = maybeReuse.entrySet().iterator().next();
                d.getValue().getDocRefs().put(entries.getKey(), reference);
                needToSave.add(d.getValue());
                maybeReuse.remove(d.getKey());
                done = true;
            }
            if (!done) {
                DocumentBagObject dNew = new DocumentBagObject();
                dNew.getDocRefs().put(entries.getKey(), reference);
                needToSave.add(dNew);
            }
        }

        // Add back those we didn't use
        for (String k : maybeReuse.keySet()) {
            if (!removedDocuments.contains(k)) {
                DocumentBagReference dr = new DocumentBagReference();
                dr.setBagRef(k);
                dr.setSize(maybeReuse.get(k).getDocRefs().size());
                newTreeObject.getDocuments().add(dr);
            }
        }

        // Now we need to save the need to save

        for (DocumentBagObject d : needToSave) {
            String reference = rp.getObjectDatabase().writeDocumentBag(d);
            DocumentBagReference dr = new DocumentBagReference();
            dr.setBagRef(reference);
            dr.setSize(d.getDocRefs().size());
            newTreeObject.getDocuments().add(dr);
        }

        newTreeObject.getTrees().putAll(shadow.getTrees());
        // Add the new ones, do the trees first
        for (Map.Entry<String, StageTree> entries : stagedTrees.entrySet()) {
            collector.enterFolder(entries.getKey());
            String reference = entries.getValue().commitStage(rp, commitRef, collector, savedDocRefs);
            collector.addTreeReference(reference);
            collector.leaveFolder();
            collector.addFolderName(entries.getKey());
            newTreeObject.getTrees().put(entries.getKey(), reference);
        }
        shadow = newTreeObject;
        reset();
        return rp.getObjectDatabase().writeTree(newTreeObject);
    }

    private Map<String, DocumentObject> rebalanceTree() {
        Map<String, DocumentObject> combinedSet = null;
        if (!stagedDocuments.isEmpty() && !newStagedDocuments.isEmpty()) {
            combinedSet = new HashMap<String, DocumentObject>();
            combinedSet.putAll(stagedDocuments);
            combinedSet.putAll(newStagedDocuments);
        } else if (!stagedDocuments.isEmpty()) {
            combinedSet = stagedDocuments;
        } else {
            combinedSet = newStagedDocuments;
        }
        return combinedSet;
    }

    private void applyNewSaves(TreeObject newTreeObject, Set<String> refsToSave) {
        for (DocumentBagReference docRef : shadow.getDocuments()) {
            if (refsToSave.contains(docRef.getBagRef())) {
                newTreeObject.getDocuments().add(docRef);
            }
        }
    }

    private void workOutCommitChanges(VersionedRepo rp, Set<DocumentBagObject> needToSave, Map<String, DocumentBagObject> maybeReuse,
            Set<String> refsToSave) {
        for (DocumentBagReference docRef : shadow.getDocuments()) {
            refsToSave.add(docRef.getBagRef());
            DocumentBagObject dbo = rp.getObjectDatabase().getDocumentBag(docRef.getBagRef());
            boolean changed = false;
            for (String key : stagedDocuments.keySet()) {
                if (dbo.getDocRefs().containsKey(key)) {
                    // We need to be saving this back
                    dbo.getDocRefs().remove(key);
                    needToSave.add(dbo);
                    refsToSave.remove(docRef.getBagRef());
                    changed = true;
                    break;
                }
            }
            for (String rem : removedDocuments) {
                if (dbo.getDocRefs().containsKey(rem)) {
                    dbo.getDocRefs().remove(rem);
                    refsToSave.remove(docRef.getBagRef());
                    needToSave.add(dbo);
                    changed = true;
                }
            }
            if (!changed) {
                if (dbo.getDocRefs().size() < capacity) {
                    maybeReuse.put(docRef.getBagRef(), dbo);
                    refsToSave.remove(docRef.getBagRef());
                }
            }
        }
    }

    public boolean removeFromStage(VersionedRepo rp, LinkedList<String> parts) {
        // Find the staged tree associated with this part and remove the
        // document reference from it

        String currentLevel = parts.remove();
        boolean removed = false;

        if (!parts.isEmpty()) {
            if (stagedTrees.containsKey(currentLevel)) {
                stagedTrees.get(currentLevel).removeFromStage(rp, parts);
            } else if (shadow.getTrees().containsKey(currentLevel)) {
                TreeObject to = rp.getObjectDatabase().getTree(shadow.getTrees().get(currentLevel));
                StageTree newStage = new StageTree(to, capacity);
                stagedTrees.put(currentLevel, newStage);
                removed = newStage.removeFromStage(rp, parts);
            }
        } else {
            removedDocuments.add(currentLevel);
            removed = true;
        }
        return removed;
    }

    private void reset() {
        this.stagedTrees = new HashMap<String, StageTree>();
        this.stagedDocuments = new HashMap<String, DocumentObject>();
        this.newStagedDocuments = new HashMap<String, DocumentObject>();
        this.removedDocuments = new HashSet<String>();
    }

}
