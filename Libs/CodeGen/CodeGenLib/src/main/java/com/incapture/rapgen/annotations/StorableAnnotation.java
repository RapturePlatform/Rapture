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
package com.incapture.rapgen.annotations;

import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;

import com.google.common.base.Optional;
import com.incapture.rapgen.annotations.storable.StorableField;
import com.incapture.rapgen.annotations.storable.StorableFieldType;

public class StorableAnnotation implements Annotation {

    private String separator = "\"/\"";
    private List<StorableField> fields = new LinkedList<StorableField>();
    private String encodingType = "";
    private Long ttlMillis;
    private Optional<String> prefix = Optional.absent();
    private Optional<String> repoName = Optional.absent();
    private Optional<String> repoConstant = Optional.absent();

    public Optional<String> getRepoConstant() {
        return repoConstant;
    }

    public void setRepoConstant(String repoConstant) {
        this.repoConstant = Optional.fromNullable(repoConstant);
    }

    public String getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(String encodingType) {
        if (encodingType.length() > 2) {
            encodingType = encodingType.substring(1, encodingType.length() - 1);
        }
        this.encodingType = encodingType;
    }

    public List<StorableField> getFields() {
        return fields;
    }

    public void addField(String name, StorableFieldType type) {
        this.fields.add(new StorableField(name, type));
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public Long getTtl() {
        return ttlMillis;
    }

    public void setTtlDays(CommonTree treeElement) {
        String ttlString = treeElement.getText();
        if (ttlString == null) {
            throw new IllegalArgumentException(String.format("Cannot set ttl to null at %s:%s", treeElement.getLine(), treeElement.getCharPositionInLine()));
        } else {
            try {
                this.ttlMillis = Long.parseLong(ttlString) * 3600 * 24 * 1000;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Error parsing TTL at %s:%s", treeElement.getLine(), treeElement.getCharPositionInLine()), e);
            }
        }
    }

    public Optional<String> getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = Optional.fromNullable(prefix);
    }

    public Optional<String> getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = Optional.fromNullable(repoName);
    }
}
