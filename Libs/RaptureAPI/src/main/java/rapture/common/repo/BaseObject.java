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

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A BaseObject is the common ancestor of all entries stored in the object
 * database part of a repo
 *
 * @author amkimian
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = rapture.common.repo.DocumentObject.class, name = "RaptDocument"),
        @JsonSubTypes.Type(value = rapture.common.repo.TreeObject.class, name = "RaptTree"),
        @JsonSubTypes.Type(value = rapture.common.repo.CommitObject.class, name = "RaptCommit"),
        @JsonSubTypes.Type(value = rapture.common.repo.PerspectiveObject.class, name = "RaptPersp"),
        @JsonSubTypes.Type(value = rapture.common.repo.TagObject.class, name = "RaptTag"),
        @JsonSubTypes.Type(value = rapture.common.repo.CommentaryObject.class, name = "RaptComment") })
public class BaseObject {
    // If this object is an update to an existing object, what is that object?
    private String previousReference;

    private String commitRef;

    private List<String> commentaryReferences = new LinkedList<String>();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BaseObject other = (BaseObject) obj;
        if (commitRef == null) {
            if (other.commitRef != null) {
                return false;
            }
        } else if (!commitRef.equals(other.commitRef)) {
            return false;
        }
        if (previousReference == null) {
            if (other.previousReference != null) {
                return false;
            }
        } else if (!previousReference.equals(other.previousReference)) {
            return false;
        }
        return true;
    }

    public List<String> getCommentaryReferences() {
        return commentaryReferences;
    }

    // What is the commit associated with this writing out?

    public String getCommitRef() {
        return commitRef;
    }

    public String getPreviousReference() {
        return previousReference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((commitRef == null) ? 0 : commitRef.hashCode());
        result = prime * result + ((previousReference == null) ? 0 : previousReference.hashCode());
        return result;
    }

    public void setCommentaryReferences(List<String> commentaryReferences) {
        this.commentaryReferences = commentaryReferences;
    }

    public void setCommitRef(String commitRef) {
        this.commitRef = commitRef;
    }

    public void setPreviousReference(String previousReference) {
        this.previousReference = previousReference;
    }

}
