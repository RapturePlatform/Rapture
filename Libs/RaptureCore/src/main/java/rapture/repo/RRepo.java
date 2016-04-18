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
package rapture.repo;

import rapture.common.RaptureDNCursor;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureNativeQueryResult;
import rapture.common.RaptureQueryResult;
import rapture.common.RaptureScript;
import rapture.common.exception.RaptNotSupportedException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentWithMeta;
import rapture.dsl.dparse.BaseDirective;
import rapture.index.IndexHandler;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import reflex.ReflexExecutor;

public class RRepo extends BaseSimpleRepo implements Repository {

    private Map<String, Object> scriptMap;
    private Map<String, String> config;

    public RRepo(Map<String, String> config) {
        // The config should have a string "config" that defines a document in Rapture that contains a set of
        // key/value pairs that define scripts that implement the functions below.
        // If not present, the implementation will throw an exception up.
        this.config = config;
        String configUrl = config.get("config");
        String val = Kernel.getDoc().getDoc(ContextFactory.getKernelUser(), configUrl);
        if (val == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, configUrl + " cannot be found");
        }
        scriptMap = JacksonUtil.getMapFromJson(val);
    }

    @Override
    public DocumentWithMeta addDocument(String key, String value, String user,
            String comment, boolean mustBeNew) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDocuments(List<String> dispNames, String content,
            String user, String comment) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addToStage(String stage, String key, String value,
            boolean mustBeNew) {
        // TODO Auto-generated method stub

    }

    @Override
    public long countDocuments()
            throws RaptNotSupportedException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void drop() {
        // TODO Auto-generated method stub

    }

    private Map<String, Object> getStandardParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("config", config);
        return params;
    }

    @Override
    public String getDocument(String key) {
        // We ignore perspective here
        Map<String, Object> params = getStandardParams();
        params.put("key", key);
        Object ret = callScript("getDocument", params);
        return ret.toString();
    }

    private Object callScript(String key, Map<String, Object> params) {
        // Call the script with the key passed in.
        if (!scriptMap.containsKey(key)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "No config implementation found for " + key);
        }
        RaptureScript script = Kernel.getScript().getScript(ContextFactory.getKernelUser(), scriptMap.get(key).toString());
        if (script == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, scriptMap.get(key) + " cannot be found");
        }
        // Now execute the script, passing the parameters in
        KernelScript kScript = new KernelScript();
        kScript.setCallingContext(ContextFactory.getKernelUser());
        RRepReflexHandler handler = new RRepReflexHandler();
        handler.setScriptApi(kScript);
        try {
            return ReflexExecutor.runReflexProgram(script.getScript(), handler, params);
        } catch (Exception e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error when invoking handler script " + e.getMessage());
        }
    }

    @Override
    public String getDocument(String key, BaseDirective directive) {
        return getDocument(key);
    }

    @Override
    public List<String> getDocuments(List<String> keys) {
        List<String> ret = new ArrayList<String>();
        for (String k : keys) {
            ret.add(getDocument(k));
        }
        return ret;
    }

    private boolean getExistence(String key) {
        Map<String, Object> params = getStandardParams();
        params.put("key", key);
        Object ret = callScript("getExistence", params);
        return (Boolean) ret;
    }

    @Override
    public boolean[] getExistence(List<String> displays) {
        boolean[] ret = new boolean[displays.size()];
        int pos = 0;
        for (String k : displays) {
            ret[pos] = getExistence(k);
            pos++;
        }
        return ret;
    }

    @Override
    public RaptureDNCursor getNextDNCursor(RaptureDNCursor cursor, int count) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");
    }

    @Override
    public boolean isVersioned() {
        return false;
    }

    @Override
    public boolean removeDocument(String key, String user,
            String comment) {
        Map<String, Object> params = getStandardParams();
        params.put("key", key);
        params.put("user", user);
        params.put("comment", comment);
        Object ret = callScript("removeDocument", params);
        return (Boolean) ret;
    }

    @Override
    public boolean removeFromStage(String stage, String key) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");

    }

    @Override
    public RaptureQueryResult runNativeQuery(String repoType,
            List<String> queryParams) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");

    }

    @Override
    public RaptureNativeQueryResult runNativeQueryWithLimitAndBounds(
            String repoType, List<String> queryParams, int limit, int offset) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");

    }

    @Override
    public void visitAll(String prefix, BaseDirective directive, RepoVisitor visitor) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");

    }

    @Override
    public void visitFolder(String folder, BaseDirective directive, RepoVisitor visitor) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");

    }

    @Override
    public void visitFolders(String folderPrefix, BaseDirective directive, RepoFolderVisitor visitor) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");

    }

    @Override
    public DocumentWithMeta addDocumentWithVersion(String disp, String content,
            String user, String comment, boolean mustBeNew, int expectedVersion) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<RaptureFolderInfo> removeChildren(String area, Boolean force) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");
    }

    @Override
    public List<RaptureFolderInfo> getChildren(String displayNamePart) {
        Map<String, Object> params = getStandardParams();
        params.put("prefix", displayNamePart);
        Object ret = callScript("getChildren", params);
        if (ret instanceof String) {
            List<Object> children = JacksonUtil.objectFromJson(ret.toString(), List.class);
            List<RaptureFolderInfo> r = new ArrayList<RaptureFolderInfo>();
            for (Object x : children) {
                if (x instanceof Map) {
                    Map<String, Object> v = (Map<String, Object>) x;
                    RaptureFolderInfo rfi = new RaptureFolderInfo();
                    rfi.setFolder((Boolean) v.get("folder"));
                    rfi.setName(v.get("name").toString());
                    r.add(rfi);
                } else {
                    System.out.println("Ignore " + x.toString());
                }
            }
            return r;
        }
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Ret not of correct type " + ret.toString());
    }

    @Override
    public List<String> getAllChildren(String area) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not supported");
    }

    @Override
    public Boolean validate() {
        return true;
    }

    @Override
    public Optional<IndexHandler> getIndexHandler() {
        return Optional.absent();
    }

	@Override
	public DocumentWithMeta addTagToDocument(String user, String docPath,
			String tagUri, String value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentWithMeta addTagsToDocument(String user, String docPath,
			Map<String, String> tagMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentWithMeta removeTagFromDocument(String user, String docPath,
			String tagUri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentWithMeta removeTagsFromDocument(String user, String docPath,
			List<String> tags) {
		// TODO Auto-generated method stub
		return null;
	}

}
