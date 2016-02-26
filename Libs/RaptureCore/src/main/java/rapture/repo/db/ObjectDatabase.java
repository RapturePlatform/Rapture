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
package rapture.repo.db;

import rapture.common.RaptureFolderInfo;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.MD5Utils;
import rapture.common.repo.*;
import rapture.repo.KeyStore;

import java.util.List;

/**
 * An object database reads and write BaseObjects
 * 
 * @author amkimian
 * 
 */
public class ObjectDatabase {
    private KeyStore store;

    public ObjectDatabase(KeyStore store) {
        this.store = store;
    }

    public CommentaryObject getCommentary(String ref) {
        if (store.containsKey(ref)) {
            return JacksonUtil.objectFromJson(store.get(ref), CommentaryObject.class);
        }
        return null;
    }

    public CommitObject getCommit(String ref) {
        if (store.containsKey(ref)) {
            return JacksonUtil.objectFromJson(store.get(ref), CommitObject.class);
        }
        return null;
    }

    public String getCommitReference(CommitObject cObj) {
        String json = JacksonUtil.jsonFromObject(cObj);
        return MD5Utils.hash16(json);
    }

    public DocumentObject getDocument(String ref) {
        if (store.containsKey(ref)) {
            return JacksonUtil.objectFromJson(store.get(ref), DocumentObject.class);
        }
        return null;
    }

    public DocumentBagObject getDocumentBag(String bagRef) {
        if (store.containsKey(bagRef)) {
            return JacksonUtil.objectFromJson(store.get(bagRef), DocumentBagObject.class);
        }
        return null;
    }

    public TreeObject getTree(String ref) {
        if (store.containsKey(ref)) {
            return JacksonUtil.objectFromJson(store.get(ref), TreeObject.class);
        }
        return null;
    }

    public String writeCommentary(CommentaryObject comment) {
        return writeJson(JacksonUtil.jsonFromObject(comment));
    }

    public String writeCommit(CommitObject d) {
        return writeJson(JacksonUtil.jsonFromObject(d));
    }

    public void writeCommit(CommitObject cObj, String cRef) {
        String json = JacksonUtil.jsonFromObject(cObj);
        store.put(cRef, json);
    }

    public String writeDocument(DocumentObject d) {
        return writeJson(JacksonUtil.jsonFromObject(d));
    }

    public void writeDocument(DocumentObject docObject, String reference) {
        store.put(reference, JacksonUtil.jsonFromObject(docObject));
    }

    public String writeDocumentBag(DocumentBagObject d) {
        return writeJson(JacksonUtil.jsonFromObject(d));
    }

    private String writeJson(String json) {
        String r = MD5Utils.hash16(json);
        store.put(r, json);
        return r;
    }

    public String writeTree(TreeObject d) {
        return writeJson(JacksonUtil.jsonFromObject(d));
    }

    public void writeTree(TreeObject treeObject, String reference) {
        store.put(reference, JacksonUtil.jsonFromObject(treeObject));
    }

    public boolean delete(List<String> keys) {
        return store.delete(keys);
    }

}
