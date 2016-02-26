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

import java.util.ArrayList;
import java.util.List;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.repo.PerspectiveObject;
import rapture.common.repo.TagObject;
import rapture.repo.KeyStore;
import rapture.repo.StoreKeyVisitor;

/**
 * A keyed database stores information about perspectives and tags
 * 
 * @author amkimian
 * 
 */
public class KeyedDatabase {
    // Trivial implementation at present - this will be some implementation
    // later
    private KeyStore db;

    public KeyedDatabase(KeyStore db) {
        this.db = db;
    }

    public void deleteTag(String tagName) {
        String key = "T-" + tagName;
        db.delete(key);
    }

    public PerspectiveObject getPerspective(String name) {
        String key = "P-" + name;
        if (db.containsKey(key)) {
            return JacksonUtil.objectFromJson(db.get(key), PerspectiveObject.class);
        }
        return null;
    }

    public List<String> getPerspectives() {
        final List<String> ret = new ArrayList<String>();
        db.visitKeys("P-", new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                ret.add(key.substring(2));
                return true;
            }

        });
        return ret;
    }

    public TagObject getTag(String name) {
        String key = "T-" + name;
        if (db.containsKey(key)) {
            return JacksonUtil.objectFromJson(db.get(key), TagObject.class);
        }
        return null;
    }

    public List<String> getTags() {
        final List<String> ret = new ArrayList<String>();
        db.visitKeys("T-", new StoreKeyVisitor() {

            @Override
            public boolean visit(String key, String value) {
                ret.add(key.substring(2));
                return true;
            }

        });
        return ret;
    }

    public void writePerspective(String name, PerspectiveObject p) {
        String key = "P-" + name;
        String json = JacksonUtil.jsonFromObject(p);
        db.put(key, json);
    }

    public void writeTag(String name, TagObject p) {
        String key = "T-" + name;
        String json = JacksonUtil.jsonFromObject(p);
        db.put(key, json);
    }

}
