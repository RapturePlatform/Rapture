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
package rapture.server.web.servlet.rest;

import java.util.ArrayList;
import java.util.List;

public class RestCollectionResponse {
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public List<RestFolder> getFolders() {
        return folders;
    }
    public void setFolders(List<RestFolder> folders) {
        this.folders = folders;
    }
    public List<RestFile> getFiles() {
        return files;
    }
    public void setFiles(List<RestFile> files) {
        this.files = files;
    }
    private String path;
    private List<RestFolder> folders = new ArrayList<RestFolder>();
    private List<RestFile> files = new ArrayList<RestFile>();
}
