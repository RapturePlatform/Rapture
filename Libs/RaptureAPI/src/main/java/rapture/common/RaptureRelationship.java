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
package rapture.common;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Note that this class is referenced in types.api - any changes to this file should be reflected there.
 **/

/*
 * TODO 
 * We should convert this to extend {@link BaseDocument}, but for now just leave it
 *
 */
public class RaptureRelationship implements RaptureTransferObject {

    private RaptureURI fromURI;
    private RaptureURI toURI;
    private String label;
    private long createDateUTC;
    private String user;
    private Map<String, String> properties;
    private RaptureURI uri;
    private UUID uuid;

    public RaptureRelationship() {
        uuid = UUID.randomUUID();
        createDateUTC = System.currentTimeMillis();
    }

    public RaptureURI getFromURI() {
        return fromURI;
    }

    public void setFromURI(RaptureURI fromURI) {
        this.fromURI = fromURI;
    }

    public RaptureURI getToURI() {
        return toURI;
    }

    public void setToURI(RaptureURI toURI) {
        this.toURI = toURI;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public long getCreateDateUTC() {
        return createDateUTC;
    }

    public void setCreateDateUTC(long createDateUTC) {
        this.createDateUTC = createDateUTC;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public RaptureURI getURI() {
        return uri;
    }

    public void setURI(RaptureURI uri) {
        this.uri = uri;
    }

    @JsonIgnore
    public void setAuthorityURI(String raptureAuthorityURI) {
        RaptureURI parsedURI = new RaptureURI(raptureAuthorityURI, Scheme.RELATIONSHIP);
        this.uri = RaptureURI.builder(parsedURI).docPath(uuid.toString()).build();
    }

}
