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
package rapture.repo;

import java.util.ArrayList;
import java.util.List;

import rapture.common.DocumentAttributeFactory;
import rapture.common.RaptureURI;
import rapture.common.model.DocumentAttribute;

/**
 * All the primary functions related to operating on KeyStore's for
 * DocumentAttributes are in this class. This functionality is similar across
 * all Repo implementations, hence it is provided once here.
 * 
 * @author dukenguyen
 * 
 */
public class DocumentAttributeHandler {

    /**
     * Given a KeyStore, simply insert the attribute's value using the full
     * uri as the key. An example of a uri for an attribute is
     * doc://authority/doc/$meta/mime-type
     * 
     * @param attributeStore
     * @param uri
     * @param attribute
     */
    public void setDocAttribute(KeyStore attributeStore, RaptureURI uri, DocumentAttribute attribute) {
        attributeStore.put(uri.toString(), attribute.getValue());
    }

    /**
     * Get a single attribute given a full uri, which is the key in the
     * KeyStore. Returns null if not found
     * 
     * @param attributeStore
     * @param uri
     * @return - DocumentAttribute if found, else null
     */
    public DocumentAttribute getDocAttribute(KeyStore attributeStore, RaptureURI uri) {
        String storeValue = attributeStore.get(uri.toString());
        if (storeValue == null) {
            return null;
        }
        return generateDocumentAttribute(uri, storeValue);
    }

    /**
     * Get a list of all attributes for a given uri e.g. //authority/doc/$link
     * will return all links for that document.
     * 
     * @param attributeStore
     * @param uri
     * @return
     */
    public List<DocumentAttribute> getDocAttributes(KeyStore attributeStore, final RaptureURI uri) {
        final List<DocumentAttribute> ret = new ArrayList<DocumentAttribute>();
        attributeStore.visitKeys(uri.toString(), new StoreKeyVisitor() {
            @Override
            public boolean visit(String key, String value) {
                DocumentAttribute att = generateDocumentAttribute(uri.getAttributeName(), key, value);
                ret.add(att);
                return true;
            }
        });
        return ret;
    }

    /**
     * Remove a single attribute from a document given a full uri e.g.
     * //authority/doc/$meta/mime-type
     * 
     * @param attributeStore
     * @param uri
     * @return
     */
    public Boolean deleteDocAttribute(KeyStore attributeStore, RaptureURI uri) {
        return attributeStore.delete(uri.toString());
    }
    
    private DocumentAttribute generateDocumentAttribute(RaptureURI uri, String value) {
        return generateDocumentAttribute(uri.getAttributeName(), uri.toString(), value);
    }
    
    private DocumentAttribute generateDocumentAttribute(String type, String key, String value) {
        DocumentAttribute ret = DocumentAttributeFactory.create(type);
        ret.setKey(key);
        ret.setValue(value);
        return ret;
    }

}
