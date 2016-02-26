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
package rapture.common;

import java.net.HttpURLConnection;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentAttribute;
import rapture.common.model.LinkDocumentAttribute;
import rapture.common.model.MetaDocumentAttribute;

/**
 * This factory is used to generate classes that implement DocumentAttribute. If
 * someone needs to add a new type of attribute to be parsed from the
 * displayName they add it here. The attribute is parsed in the form of
 * type/doc/$attType/key
 * 
 * @author dukenguyen
 * 
 */
public class DocumentAttributeFactory {

    /*
     * type used for links to other documents
     */
    public static final String LINK_TYPE = "link";

    /*
     * type used for arbitrary meta-data about a document e.g. mime-type
     */
    public static final String META_TYPE = "meta";

    /**
     * This method is called by passing in a type e.g.
     * DocumentAttributeFactory.create(DocumentAttributeFactory.LINK_TYPE);
     * 
     * @param type
     *            - the requested implementation to be returned
     * @return DocumentAttribute representing the underlying implementation
     */
    public static DocumentAttribute create(String type) {
        if (type == null) {
            return null;
        } else if (type.equals(LINK_TYPE)) {
            return new LinkDocumentAttribute();
        } else if (type.equals(META_TYPE)) {
            return new MetaDocumentAttribute();
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported attribute type specified: " + type);
        }
    }
}
