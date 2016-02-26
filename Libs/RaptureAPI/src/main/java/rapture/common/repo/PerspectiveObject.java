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

import rapture.common.model.RemoteLink;

public class PerspectiveObject extends BaseObject {
    private String baseCommit;

    private String latestCommit;

    private String owner;

    private String basePerspective;

    private String description;

    private Date when;

    // Optional, can be null
    private RemoteLink remoteLink = null;

    public String getBaseCommit() {
        return baseCommit;
    }

    public String getBasePerspective() {
        return basePerspective;
    }

    public String getDescription() {
        return description;
    }

    public String getLatestCommit() {
        return latestCommit;
    }

    public String getOwner() {
        return owner;
    }

    public RemoteLink getRemoteLink() {
        return remoteLink;
    }

    public Date getWhen() {
        return when;
    }

    public void setBaseCommit(String baseCommit) {
        this.baseCommit = baseCommit;
    }

    public void setBasePerspective(String basePerspective) {
        this.basePerspective = basePerspective;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLatestCommit(String latestCommit) {
        this.latestCommit = latestCommit;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRemoteLink(RemoteLink remoteLink) {
        this.remoteLink = remoteLink;
    }

    public void setWhen(Date when) {
        this.when = when;
    }
}
