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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A tree object is a list of references (by name) of other trees and documents
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
 * 
 * @author amkimian
 * 
 */
public class TreeObject extends BaseObject {
    private Map<String, String> trees;

    private List<DocumentBagReference> documents;

    public TreeObject() {
        trees = new HashMap<String, String>();
        setDocuments(new LinkedList<DocumentBagReference>());
    }

    public List<DocumentBagReference> getDocuments() {
        return documents;
    }

    public Map<String, String> getTrees() {
        return trees;
    }

    public void setDocuments(List<DocumentBagReference> documents) {
        this.documents = documents;
    }

    public void setTrees(Map<String, String> trees) {
        this.trees = trees;
    }

}
