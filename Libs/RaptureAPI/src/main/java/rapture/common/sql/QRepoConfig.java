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
package rapture.common.sql;

/**
 * The config of a QREP. In particular how entities are mapped between a
 * relational form and a document centric form. The idea is that a json form of
 * this class is stored in a private part of the system repo and referenced from
 * the repo config.
 * 
 * The QRepo then loads that document, converts it to this and uses it.
 * 
 * @author amkimian
 * 
 */
public class QRepoConfig {
    public FieldMapping getFields() {
        return fields;
    }

    public void setFields(FieldMapping fields) {
        this.fields = fields;
    }

    public JoinMapping getJoins() {
        return joins;
    }

    public void setJoins(JoinMapping joins) {
        this.joins = joins;
    }

    public DisplayMapping getDisplay() {
        return display;
    }

    public void setDisplay(DisplayMapping display) {
        this.display = display;
    }

    /**
     * These two aspects are all about how to get to the data required to create
     * a document
     */
    private FieldMapping fields;
    private JoinMapping joins;

    /**
     * These are all about how to get to display names, and how to parse display
     * names into fields as part of a where clause.
     */

    private DisplayMapping display;
}
