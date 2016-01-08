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
package rapture.common.model;

/**
 * This is a reference to another folder
 * 
 * @author alan
 * 
 */
public class RestFolderInfo {

    private String name;

    private String url;

    private boolean folder;

    private String restType;
    private RestAccessInfo accessInfo = new RestAccessInfo();

    public RestFolderInfo() {

    }

    public RestFolderInfo(String root, String name, String part, String restType, boolean folder) {
        this.name = name;
        this.url = root + "/" + part;
        this.setRestType(restType);
        this.folder = folder;
    }

    public RestAccessInfo getAccessInfo() {
        return accessInfo;
    }

    public String getName() {
        return name;
    }

    public String getRestType() {
        return restType;
    }

    public String getUrl() {
        return url;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setAccessInfo(RestAccessInfo accessInfo) {
        this.accessInfo = accessInfo;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRestType(String restType) {
        this.restType = restType;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
