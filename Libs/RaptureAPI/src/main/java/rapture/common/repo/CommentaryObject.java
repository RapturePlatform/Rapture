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
package rapture.common.repo;

import java.util.Date;

/**
 * A list of these objects (well the references to them) can be stored with any
 * BaseObject, but are usually associated with a DocumentObject
 * 
 * It is used to store commentary about an object in a repo (for example,
 * who did what)
 * 
 * @author alan
 * 
 */
public class CommentaryObject extends BaseObject {
    private String who;

    private Date when;

    private String message;

    private String ref; // If set, this can point to another reference.

    private String commentaryKey; // If set, used to provide an update ability
                                  // to commentary - reuse the same key

    public String getCommentaryKey() {
        return commentaryKey;
    }

    public String getMessage() {
        return message;
    }

    public String getRef() {
        return ref;
    }

    public Date getWhen() {
        return when;
    }

    public String getWho() {
        return who;
    }

    public void setCommentaryKey(String commentaryKey) {
        this.commentaryKey = commentaryKey;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    public void setWho(String who) {
        this.who = who;
    }
}
