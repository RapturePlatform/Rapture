/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.common.repo;

import java.util.Date;
import java.util.List;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
**/
public class CommitObject extends BaseObject {
    private String treeRef;

    private String user;

    private Date when;

    private String comment;

    private String changes;

    private List<String> docReferences;

    private List<String> treeReferences;

    public String getChanges() {
        return changes;
    }

    public String getComment() {
        return comment;
    }

    public List<String> getDocReferences() {
        return docReferences;
    }

    public String getTreeRef() {
        return treeRef;
    }

    public List<String> getTreeReferences() {
        return treeReferences;
    }

    public String getUser() {
        return user;
    }

    public Date getWhen() {
        return when;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDocReferences(List<String> docReferences) {
        this.docReferences = docReferences;
    }

    public void setTreeRef(String treeRef) {
        this.treeRef = treeRef;
    }

    public void setTreeReferences(List<String> treeReferences) {
        this.treeReferences = treeReferences;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setWhen(Date when) {
        this.when = when;
    }
}
