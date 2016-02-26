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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CommitCollector {
    private List<String> pathParts;
    private List<String> folders;
    private List<String> documents;
    private List<String> docReferences;
    private List<String> treeReferences;

    public CommitCollector() {
        pathParts = new LinkedList<String>();
        folders = new ArrayList<String>();
        documents = new ArrayList<String>();
        docReferences = new ArrayList<String>();
        treeReferences = new ArrayList<String>();
    }

    public void addDocName(String name) {
        documents.add(currentPath(name));
    }

    public void addDocReference(String reference) {
        docReferences.add(reference);
    }

    public void addFolderName(String name) {
        folders.add(currentPath(name));
    }

    public void addTreeReference(String reference) {
        treeReferences.add(reference);
    }

    private String currentPath(String name) {
        StringBuilder sb = new StringBuilder();
        for (String p : pathParts) {
            sb.append(p);
            sb.append("/");
        }
        sb.append(name);
        return sb.toString();
    }

    public void enterFolder(String folder) {
        pathParts.add(folder);
    }

    public List<String> getDocReferences() {
        return docReferences;
    }

    public List<String> getTreeReferences() {
        return treeReferences;
    }

    public void leaveFolder() {
        pathParts.remove(pathParts.size() - 1);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("Changes:");
        if (!folders.isEmpty()) {
            ret.append("F=");
            for (String f : folders) {
                ret.append(f);
                ret.append(",");
            }
            ret.append(folders.toString());
        }
        if (!documents.isEmpty()) {
            ret.append("D=");
            for (String d : documents) {
                ret.append(d);
                ret.append(",");
            }
        }
        return ret.substring(0, ret.length() - 1);
    }
}
