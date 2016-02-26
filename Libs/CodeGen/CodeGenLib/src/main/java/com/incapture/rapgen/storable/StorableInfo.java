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
package com.incapture.rapgen.storable;

import com.google.common.base.Optional;

/**
 * @author bardhi
 * @since 4/15/15.
 */
public class StorableInfo {
    private String storableName;
    private String storablePackage;
    private Optional<String> prefix = Optional.absent();
    private Optional<String> repoName = Optional.absent();

    public Optional<String> getRepoName() {
        return repoName;
    }

    public void setRepoName(Optional<String> repoName) {
        this.repoName = repoName;
    }

    public Optional<String> getPrefix() {
        return prefix;
    }

    public void setPrefix(Optional<String> prefix) {
        this.prefix = prefix;
    }

    public String getStorablePackage() {
        return storablePackage;
    }

    public void setStorablePackage(String storablePackage) {
        this.storablePackage = storablePackage;
    }

    public String getStorableName() {
        return storableName;
    }

    public void setStorableName(String storableName) {
        this.storableName = storableName;
    }

}
