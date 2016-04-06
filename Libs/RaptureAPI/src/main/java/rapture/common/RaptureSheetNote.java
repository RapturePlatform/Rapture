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
package rapture.common;

import java.util.Date;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
**/

@Deprecated
public class RaptureSheetNote {
 
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "RaptureSheetNote [id=" + id + ", who=" + who + ", note=" + note + ", when=" + when + "]";
    }
    
    @Deprecated
    public String getId() {
        return id;
    }
    @Deprecated
    public void setId(String id) {
        this.id = id;
    }
    @Deprecated
    public String getWho() {
        return who;
    }
    @Deprecated
    public void setWho(String who) {
        this.who = who;
    }
    @Deprecated
    public String getNote() {
        return note;
    }
    @Deprecated
    public void setNote(String note) {
        this.note = note;
    }
    @Deprecated
    public Date getWhen() {
        return when;
    }
    @Deprecated
    public void setWhen(Date when) {
        this.when = when;
    }
    private String id;
    private String who;
    private String note;
    private Date when;
 
}
