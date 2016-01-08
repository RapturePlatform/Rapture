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
package rapture.kernel;

import rapture.common.CallingContext;
import rapture.common.RaptureField;
import rapture.common.RaptureFieldPath;
import rapture.common.RaptureFieldStorage;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.FieldsApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.util.StringUtil;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class FieldsApiImpl extends KernelBase implements FieldsApi {

    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(FieldsApiImpl.class);

    public FieldsApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    private List<String> getFieldValue(Map<String, Object> map, String type, RaptureField f) {
        // displayname_return display =
        // DisplayNameParser.parseDisplayName(displayName);

        List<String> ret = new ArrayList<String>();
        // TODO: Here we also need to worry about composite fields
        for (RaptureFieldPath fp : f.getFieldPaths()) {
            if (fp.getTypeName().equals(type)) {
                // This will be something like x.y.z
                List<String> pathParts = StringUtil.list(fp.getPath(), "\\.");
                ret.add(getFromMap(map, pathParts));
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private String getFromMap(Map<String, Object> map, List<String> pathParts) {
        String part = pathParts.remove(0);
        Object v = map.get(part);
        if (!pathParts.isEmpty() && v instanceof Map) {
            Map<String, Object> innerV = (Map<String, Object>) v;
            return getFromMap(innerV, pathParts);
        } else {
            if (v != null) {
                return v.toString();
            } else {
                return "";
            }
        }
    }

    /**
     * If a field is a banded type, it will (should) be invoked as bandEntity(valueEntity). E.g. durationBand(amount) What this means is this - load the field
     * "durationBand" and load the field "amount". Add amount as the band amount field for durationBand Then get the fieldValue of the outerBand, get the value
     * of the innerValue, and create n values (one for each band). The value of each entry is either the amount (if the band comparison returns true) or null
     * (zero) if the comparison returns false.
     *
     * The name of the column (when we get that far) is actually n columns, one for each band, as bandname.inneramountname
     *
     * @param context
     * @param docURI
     * @param fieldParts
     * @param docCache
     * @return @
     */
    private List<String> getValue(CallingContext context, String docURI, List<String> fieldParts, Map<String, Map<String, Object>> docCache) {
        // Look for content in the cache
        List<String> ret = new ArrayList<String>();
        if (!docCache.containsKey(docURI)) {
            String content = Kernel.getDoc().getDoc(context, docURI);
            // Convert content to a map, for easier access
            Map<String, Object> treeStruct = (Map<String, Object>) JacksonUtil.getMapFromJson(content);
            docCache.put(docURI, treeStruct);
        }
        String thisField = fieldParts.remove(0);

        // TODO: here check field for the magic structure x(y) to create a
        // composite field

        RaptureURI internalUri = new RaptureURI(docURI, Scheme.DOCUMENT);
        RaptureField f = getField(context, "//" + internalUri.getAuthority() + "/" + thisField);

        if (f != null) {
            ret = getFieldValue(docCache.get(docURI), internalUri.getAuthority(), f);
            if (f.getAuthority() != null && !f.getAuthority().isEmpty() && !fieldParts.isEmpty()) {
                List<String> ret2 = new ArrayList<String>();
                for (String v : ret) {
                    String lowerDisplayName = "//" + internalUri.getAuthority() + "/" + v;
                    ret2.addAll(getValue(context, lowerDisplayName, fieldParts, docCache));
                }
                return ret2;
            }
        }
        return ret;
    }

    private static String addSchemeIfNeeded(String path, Scheme scheme) {
        if (path.startsWith(scheme.toString())) {
            return path;
        } else if (path.startsWith("//")) {
            return scheme.toString() + ":" + path;
        } else if (path.contains("://")) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Trying to use " + scheme.toString() + " with invalid URI scheme: "
                    + path);
        } else {
            return scheme.toString() + "://" + path;
        }
    }

    @Override
    public List<String> getDocumentFields(CallingContext context, String docUri, List<String> fields) {
        docUri = addSchemeIfNeeded(docUri, Scheme.DOCUMENT);
        Map<String, Map<String, Object>> docCache = new HashMap<String, Map<String, Object>>();
        List<String> ret = new ArrayList<String>();
        for (String field : fields) {
            List<String> fieldParts = StringUtil.list(field, "\\.");
            ret.addAll(getValue(context, docUri, fieldParts, docCache));
        }
        return ret;
    }

    @Override
    public List<String> putDocumentAndGetDocumentFields(CallingContext context, String docURI, String content, List<String> fields) {
        Map<String, Map<String, Object>> docCache = new HashMap<String, Map<String, Object>>();
        docCache.put(docURI, (Map<String, Object>) JacksonUtil.getMapFromJson(content));
        List<String> ret = new ArrayList<String>();
        for (String field : fields) {
            List<String> fieldParts = StringUtil.list(field, "\\.");
            ret.addAll(getValue(context, docURI, fieldParts, docCache));
        }
        return ret;
    }

    @Override
    public Map<String, RaptureFolderInfo> listFieldsByUriPrefix(CallingContext context, String authority, int depth) {
        RaptureURI addressUri = new RaptureURI(authority, Scheme.FIELD);
        RaptureURI storageLocation = RaptureFieldStorage.addressToStorageLocation(addressUri);
        return Kernel.getDoc().listDocsByUriPrefix(context, storageLocation.toString(), depth);
    }

    @Override
    public RaptureField getField(CallingContext context, String fieldUri) {
        RaptureURI internalUri = new RaptureURI(fieldUri, Scheme.FIELD);
        return RaptureFieldStorage.readByAddress(internalUri);
    }

    @Override
    public void putField(CallingContext context, RaptureField field) {
        RaptureFieldStorage.add(field, context.getUser(), "Adding field");
    }

    @Override
    public Boolean fieldExists(CallingContext context, String fieldUri) {
        RaptureField field = getField(context, fieldUri);
        if (field == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void deleteField(CallingContext context, String fieldUri) {
        RaptureURI addressUri = new RaptureURI(fieldUri, Scheme.FIELD);
        RaptureFieldStorage.deleteByAddress(addressUri, context.getUser(), "Removing field");
    }
}
