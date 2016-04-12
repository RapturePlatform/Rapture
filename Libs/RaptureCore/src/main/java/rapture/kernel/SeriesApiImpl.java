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
package rapture.kernel;

import static rapture.common.Scheme.SERIES;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.EntitlementSet;
import rapture.common.Hose;
import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.SeriesDouble;
import rapture.common.SeriesPoint;
import rapture.common.SeriesRepoConfig;
import rapture.common.SeriesRepoConfigStorage;
import rapture.common.SeriesString;
import rapture.common.SeriesValue;
import rapture.common.api.SeriesApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.shared.doc.GetDocPayload;
import rapture.common.shared.series.DeleteSeriesPayload;
import rapture.common.shared.series.ListSeriesByUriPrefixPayload;
import rapture.dsl.serfun.HoseArg;
import rapture.dsl.serfun.HoseProgram;
import rapture.dsl.serfun.LoadHose;
import rapture.kernel.context.ContextValidator;
import rapture.repo.SeriesRepo;
import rapture.series.config.ConfigValidatorService;
import rapture.series.config.InvalidConfigException;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class SeriesApiImpl extends KernelBase implements SeriesApi {

    private static Logger log = Logger.getLogger(SeriesApiImpl.class);
    
    public SeriesApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }
    
        
    @Override
    public void createSeriesRepo(CallingContext context, String seriesURI, String config) {
        checkParameter("Repository URI", seriesURI);
        checkParameter("Config", config);

        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        String authority = internalURI.getAuthority();
        if ((authority == null) || authority.isEmpty()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoAuthority")); //$NON-NLS-1$
        }
        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", seriesURI)); //$NON-NLS-1$
        }
        try {
            ConfigValidatorService.validateConfig(config);
        } catch (InvalidConfigException | RecognitionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ConfigNotValid", config), e); //$NON-NLS-1$
        }

        if (seriesRepoExists(context, seriesURI)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("Exists", seriesURI)); //$NON-NLS-1$
        }

        // Repo config
        SeriesRepoConfig series = new SeriesRepoConfig();
        series.setAuthority(internalURI.getAuthority());
        series.setConfig(config);
        SeriesRepoConfigStorage.add(series, context.getUser(), "Create series repo");

        // series repo will be cached when accessed the first time
    }

    @Override
    public Boolean seriesRepoExists(CallingContext context, String seriesURI) {
        RaptureURI uri = new RaptureURI(seriesURI, Scheme.SERIES);
        if (uri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", seriesURI)); //$NON-NLS-1$
 //$NON-NLS-1$
        }
        SeriesRepo series = getRepoFromCache(uri.getAuthority());
        return series != null;
    }

    @Override
    public SeriesRepoConfig getSeriesRepoConfig(CallingContext context, String seriesURI) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);

        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", seriesURI)); //$NON-NLS-1$
        }

        return SeriesRepoConfigStorage.readByAddress(internalURI);
    }

    @Override
    public void deleteSeriesRepo(CallingContext context, String seriesURI) {
        // Need to drop the data, then drop the type definition

        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);

        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", seriesURI)); //$NON-NLS-1$
        }

        SeriesRepo repo = getRepoFromCache(internalURI.getAuthority());
        if (repo != null) {
            repo.drop();
        }

        SeriesRepoConfigStorage.deleteByAddress(internalURI, context.getUser(), Messages.getString("Admin.RemoveType")); //$NON-NLS-1$ //$NON-NLS-2$
        removeRepoFromCache(internalURI.getAuthority());
    }

    @Override
    public void deletePointsFromSeriesByPointKey(CallingContext context, String seriesURI, List<String> columns) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repo = getRepoOrFail(internalURI);
        repo.deletePointsFromSeriesByColumn(internalURI.getDocPath(), columns);
    }

    @Override
    public void deletePointsFromSeries(CallingContext context, String seriesURI) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repo = getRepoOrFail(internalURI);
        repo.deletePointsFromSeries(internalURI.getDocPath());
    }

    @Override
    public List<SeriesPoint> getPoints(CallingContext context, String seriesURI) {
        return Lists.transform(getPointsAsSeriesValues(context, seriesURI), sv2xsf);
    }

    private List<SeriesValue> getPointsAsSeriesValues(CallingContext context, String seriesURI) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repo = getRepoOrFail(internalURI);
        List<SeriesValue> raw = repo.getPoints(internalURI.getDocPath());
        return raw;
    }

    private Function<SeriesValue, SeriesPoint> sv2xsf = new Function<SeriesValue, SeriesPoint>() {
        public SeriesPoint apply(SeriesValue in) {
            SeriesPoint result = new SeriesPoint();
            result.setColumn(in.getColumn());
            result.setValue(in.isString() ? ("'" + in.asString()) : in.asString());
            return result;
        }
    };

    private Function<SeriesValue, SeriesDouble> sv2sd = new Function<SeriesValue, SeriesDouble>() {
        public SeriesDouble apply(SeriesValue in) {
            SeriesDouble result = new SeriesDouble();
            result.setKey(in.getColumn());
            result.setValue(in.asDouble());
            return result;
        }
    };

    private Function<SeriesValue, SeriesString> sv2ss = new Function<SeriesValue, SeriesString>() {
        public SeriesString apply(SeriesValue in) {
            SeriesString result = new SeriesString();
            result.setKey(in.getColumn());
            result.setValue(in.asString());
            return result;
        }
    };

    private Function<String, HoseArg> xsf2sv = new Function<String, HoseArg>() {
        public HoseArg apply(String in) {
            if (Character.isDigit(in.charAt(0))) {
                return in.contains(".") ? HoseArg.makeDecimal(in) : HoseArg.makeLong(in);
            } else if (in.charAt(0) == '\'') {
                return HoseArg.makeString(in.substring(1));
            } else if (in.charAt(0) == '@') {
                return HoseArg.makeStream(LoadHose.make(in));
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("BadHose"));
            }
        }
    };

    @Override
    public SeriesPoint getLastPoint(CallingContext context, String seriesURI) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repo = getRepoOrFail(internalURI);
        SeriesValue lastPoint = repo.getLastPoint(internalURI.getDocPath());
        return sv2xsf.apply(lastPoint);
    }

    @Override
    public List<SeriesPoint> getPointsAfter(CallingContext context, String seriesURI, String startColumn, int maxNumber) {
        return Lists.transform(getPointsAfterAsSeriesValues(context, seriesURI, startColumn, maxNumber), sv2xsf);
    }

    @Override
    public List<SeriesPoint> getPointsAfterReverse(CallingContext context, String seriesURI, String startColumn, int maxNumber) {
        return Lists.transform(getPointsAfterReverseAsSeriesValues(context, seriesURI, startColumn, maxNumber), sv2xsf);
    }

    private List<SeriesValue> getPointsAfterReverseAsSeriesValues(CallingContext context, String seriesURI, String startColumn, int maxNumber) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repo = getRepoOrFail(internalURI);
        return repo.getPointsAfterReverse(internalURI.getDocPath(), startColumn, maxNumber);
    }

    private List<SeriesValue> getPointsAfterAsSeriesValues(CallingContext context, String seriesURI, String startColumn, int maxNumber) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repo = getRepoOrFail(internalURI);
        return repo.getPointsAfter(internalURI.getDocPath(), startColumn, maxNumber);
    }

    /**
     * Return the {@link SeriesRepo} for this {@link RaptureURI} or throw a {@link RaptureException} with an error message
     *
     * @param uri
     * @return
     */
    private SeriesRepo getRepoOrFail(RaptureURI uri) {
        SeriesRepo repo = getRepoFromCache(uri.getAuthority());
        if (repo == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", uri.toAuthString())); //$NON-NLS-1$
        } else {
            return repo;
        }
    }

    @Override
    public List<SeriesPoint> getPointsInRange(CallingContext context, String seriesURI, String startColumn, String endColumn, int maxNumber) {
        return Lists.transform(getPointsInRangeAsSeriesValues(context, seriesURI, startColumn, endColumn, maxNumber), sv2xsf);
    }

    private List<SeriesValue> getPointsInRangeAsSeriesValues(CallingContext context, String seriesURI, String startColumn, String endColumn, int maxNumber) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repo = getRepoOrFail(internalURI);
        return repo.getPointsAfter(internalURI.getDocPath(), startColumn, endColumn, maxNumber);
    }

    @Override
    public void addDoubleToSeries(CallingContext context, String seriesURI, String pointKey, Double pointValue) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addDoubleToSeries(internalURI.getDocPath(), pointKey, pointValue);
    }

    @Override
    public void addLongToSeries(CallingContext context, String seriesURI, String pointKey, Long pointValue) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addLongToSeries(internalURI.getDocPath(), pointKey, pointValue);
    }

    @Override
    public void addStringToSeries(CallingContext context, String seriesURI, String pointKey, String pointValue) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addStringToSeries(internalURI.getDocPath(), pointKey, pointValue);
    }

    @Override
    public void addStructureToSeries(CallingContext context, String seriesURI, String pointKey, String pointValue) {
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addStructureToSeries(internalURI.getDocPath(), pointKey, pointValue);
    }

    @Override
    public void addDoublesToSeries(CallingContext context, String seriesURI, List<String> pointKeys, List<Double> pointValues) {
        checkParameter("URI", seriesURI);
        checkParameter("pointKeys", pointKeys);
        checkParameter("pointValues", pointValues);
        if (pointKeys.size() != pointValues.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ArgSizeNotEqual")); //$NON-NLS-1$
        }
        for (Double v : pointValues) {
            if (v == null)
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NullEmpty", "pointValue")); //$NON-NLS-1$
        }
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addDoublesToSeries(internalURI.getDocPath(), pointKeys, pointValues);
    }

    @Override
    public void addLongsToSeries(CallingContext context, String seriesURI, List<String> pointKeys, List<Long> pointValues) {
        checkParameter("URI", seriesURI);
        checkParameter("pointKeys", pointKeys);
        checkParameter("pointValues", pointValues);
        if (pointKeys.size() != pointValues.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ArgSizeNotEqual")); //$NON-NLS-1$
        }
        for (Long v : pointValues) {
            if (v == null)
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NullEmpty", "pointValue")); //$NON-NLS-1$
        }
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addLongsToSeries(internalURI.getDocPath(), pointKeys, pointValues);
    }

    @Override
    public void addStringsToSeries(CallingContext context, String seriesURI, List<String> pointKeys, List<String> pointValues) {
        checkParameter("URI", seriesURI);
        checkParameter("pointKeys", pointKeys);
        checkParameter("pointValues", pointValues);
        if (pointKeys.size() != pointValues.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ArgSizeNotEqual")); //$NON-NLS-1$
        }
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addStringsToSeries(internalURI.getDocPath(), pointKeys, pointValues);
    }

    @Override
    public void addStructuresToSeries(CallingContext context, String seriesURI, List<String> pointKeys, List<String> pointValues) {
        checkParameter("URI", seriesURI);
        checkParameter("pointKeys", pointKeys);
        checkParameter("pointValues", pointValues);

        if (pointKeys.size() != pointValues.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ArgSizeNotEqual")); //$NON-NLS-1$
        }
        for (String v : pointValues) {
            if (v == null)
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NullEmpty", "pointValue")); //$NON-NLS-1$
        }
        RaptureURI internalURI = new RaptureURI(seriesURI, Scheme.SERIES);
        getRepoOrFail(internalURI).addStructuresToSeries(internalURI.getDocPath(), pointKeys, pointValues);
    }

    @Override
    public List<SeriesPoint> runSeriesScript(CallingContext context, String scriptContent, List<String> arguments) {
        Hose h = assemble(scriptContent, arguments);
        List<SeriesPoint> result = Lists.newArrayList();
        while (true) {
            SeriesValue v = h.pullValue();
            if (v == null) break;
            result.add(sv2xsf.apply(v));
        }
        return result;
    }

    @Override
    public void runSeriesScriptQuiet(CallingContext context, String scriptContent, List<String> arguments) {
        Hose h = assemble(scriptContent, arguments);
        while (h.pullValue() != null)
            ;
    }

    private Hose assemble(String program, List<String> args) {
        List<HoseArg> decodedArgs = Lists.transform(args, xsf2sv);
        try {
            return HoseProgram.compile(program).make(decodedArgs);
        } catch (RecognitionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("BadHose"), e);
        }
    }

    @Override
    public List<SeriesDouble> getPointsAsDoubles(CallingContext context, String seriesURI) {
        return Lists.transform(getPointsAsSeriesValues(context, seriesURI), sv2sd);
    }

    @Override
    public List<SeriesDouble> getPointsAfterAsDoubles(CallingContext context, String seriesURI, String startColumn, int maxNumber) {
        return Lists.transform(getPointsAfterAsSeriesValues(context, seriesURI, startColumn, maxNumber), sv2sd);
    }

    @Override
    public List<SeriesDouble> getPointsInRangeAsDoubles(CallingContext context, String seriesURI, String startColumn, String endColumn, int maxNumber) {
        return Lists.transform(getPointsInRangeAsSeriesValues(context, seriesURI, startColumn, endColumn, maxNumber), sv2sd);
    }

    @Override
    public List<SeriesString> getPointsAsStrings(CallingContext context, String seriesURI) {
        return Lists.transform(getPointsAsSeriesValues(context, seriesURI), sv2ss);
    }

    @Override
    public List<SeriesString> getPointsAfterAsStrings(CallingContext context, String seriesURI, String startColumn, int maxNumber) {
        return Lists.transform(getPointsAfterAsSeriesValues(context, seriesURI, startColumn, maxNumber), sv2ss);
    }

    @Override
    public List<SeriesString> getPointsInRangeAsStrings(CallingContext context, String seriesURI, String startColumn, String endColumn, int maxNumber) {
        return Lists.transform(getPointsInRangeAsSeriesValues(context, seriesURI, startColumn, endColumn, maxNumber), sv2ss);
    }

    @Override
    public List<SeriesRepoConfig> getSeriesRepoConfigs(CallingContext context) {
        List<SeriesRepoConfig> result = SeriesRepoConfigStorage.readAll();
        if (result == null) result = Lists.newArrayList();
        return result;
    }

    @Override
    public Map<String, RaptureFolderInfo> listSeriesByUriPrefix(CallingContext context, String uriPrefix, int depth) {
        RaptureURI internalUri = new RaptureURI(uriPrefix, SERIES);
        String authority = internalUri.getAuthority();
        Map<String, RaptureFolderInfo> ret = new HashMap<String, RaptureFolderInfo>();
        
        // Schema level is special case.
        if (authority.isEmpty()) {
            --depth;
            try {
                List<SeriesRepoConfig> configs = getSeriesRepoConfigs(context);
                for (SeriesRepoConfig config : configs) {
                     authority = config.getAuthority();
                     // NULL or empty string should not exist. 
                     if ((authority == null) || authority.isEmpty()) {
                         log.warn("Invalid authority (null or empty string) found for "+JacksonUtil.jsonFromObject(config));
                         continue;
                     }
                     String uri = SERIES+"://"+authority;
                     ret.put(uri, new RaptureFolderInfo(authority, true));
                     if (depth != 0) {
                         ret.putAll(listSeriesByUriPrefix(context, uri, depth));
                     }
                }

            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission for "+uriPrefix);
            }
            return ret;
        }

        SeriesRepo repo = getRepoFromCache(internalUri.getAuthority());
        Boolean getAll = false;
        if (repo == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        String parentDocPath = internalUri.getDocPath() == null ? "" : internalUri.getDocPath();
        int startDepth = StringUtils.countMatches(parentDocPath, "/");

        if (log.isDebugEnabled()) {
            log.debug("Loading all children from repo " + internalUri.getAuthority() + " with " + internalUri.getDocPath());
        }
        if (depth <= 0) getAll = true;

        Stack<String> parentsStack = new Stack<String>();
        parentsStack.push(parentDocPath);

        while (!parentsStack.isEmpty()) {
            String currParentDocPath = parentsStack.pop();
            int currDepth = StringUtils.countMatches(currParentDocPath, "/") - startDepth;
            if (!getAll && currDepth >= depth) continue;
            boolean top = currParentDocPath.isEmpty();
            // Make sure that you have permission to read the folder.
            try {
                ListSeriesByUriPrefixPayload requestObj = new ListSeriesByUriPrefixPayload();
                requestObj.setContext(context);
                requestObj.setSeriesUri(currParentDocPath);
                ContextValidator.validateContext(context, EntitlementSet.Series_listSeriesByUriPrefix, requestObj); 
            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission on folder " + currParentDocPath);
                continue;
            }

            List<RaptureFolderInfo> children = repo.listSeriesByUriPrefix(currParentDocPath);
            if ((children == null) || (children.isEmpty()) && (currDepth == 0) && (internalUri.hasDocPath())) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchSeries", internalUri.toString())); //$NON-NLS-1$
            } else {
                for (RaptureFolderInfo child : children) {
                    String childDocPath = currParentDocPath + (top ? "" : "/") + child.getName();
                    if (child.getName().isEmpty()) continue;
                    String childUri = Scheme.SERIES+"://" + authority + "/" + childDocPath + (child.isFolder() ? "/" : "");
                    ret.put(childUri, child);
                    if (child.isFolder()) {
                        parentsStack.push(childDocPath);
                    }
                }
            }
            if (top) startDepth--; // special case
        }
        return ret;
    }

    private SeriesRepo getRepoFromCache(String authority) {
        return Kernel.getRepoCacheManager().getSeriesRepo(authority);
    }

    private void removeRepoFromCache(String authority) {
        Kernel.getRepoCacheManager().removeRepo(Scheme.SERIES.toString(), authority);
    }

    @Override
    public List<String> deleteSeriesByUriPrefix(CallingContext context, String uriPrefix) {
        
        Map<String, RaptureFolderInfo> map = listSeriesByUriPrefix(context, uriPrefix, Integer.MAX_VALUE);
        List<String> folders = new ArrayList<>();
        Set<String> notEmpty = new HashSet<>();
        List<String> removed = new ArrayList<>();

        DeleteSeriesPayload requestObj = new DeleteSeriesPayload();
        requestObj.setContext(context);

        folders.add(uriPrefix.endsWith("/") ? uriPrefix : uriPrefix + "/");
        for (Map.Entry<String, RaptureFolderInfo> entry : map.entrySet()) {
            String uri = entry.getKey();
            boolean isFolder = entry.getValue().isFolder();
            try {
                requestObj.setSeriesUri(uri);
                if (isFolder) {
                    ContextValidator.validateContext(context, EntitlementSet.Series_deleteSeriesByUriPrefix, requestObj);
                    folders.add(0, uri.substring(0, uri.length() - 1));
                } else {
                    ContextValidator.validateContext(context, EntitlementSet.Series_deleteSeries, requestObj);
                    deletePointsFromSeries(context, uri);
                    deleteSeries(context, uri);
                    removed.add(uri);
                }
            } catch (RaptureException e) {
                // permission denied
                log.debug("Unable to delete " + uri + " : " + e.getMessage());
                int colon = uri.indexOf(":") + 3;
                while (true) {
                    int slash = uri.lastIndexOf('/');
                    if (slash < colon) break;
                    uri = uri.substring(0, slash);
                    notEmpty.add(uri);
                }
            }
        }
        for (String uri : folders) {
            if (notEmpty.contains(uri)) continue;
            deleteSeries(context, uri);
        }
        return removed;

    }

    static final String DUMMY = "dUmMy__dUmMy";
    static final List<String> DUMMYLIST = new ArrayList<String>(1);

    static {
        DUMMYLIST.add(DUMMY);
    }

    /**
     * Wonder if this is faster than calling getContent for docs and blobs?
     */
    @Override
    public Boolean seriesExists(CallingContext context, String seriesURI) {
        try {
            RaptureURI uri = new RaptureURI(seriesURI, Scheme.SERIES);
            // no authority or no doc path
            if (uri.getAuthority().isEmpty() || uri.getDocPathDepth() < 2) {
                return false;
            }
            if (!listSeriesByUriPrefix(context, seriesURI, 1).isEmpty()) {
                return true;
            }
            int lastSlash = seriesURI.lastIndexOf("/");
            if (lastSlash < 0 || lastSlash == seriesURI.length() - 1) {
                return false;
            }
            String parentUri = seriesURI.substring(0, lastSlash);
            String name = seriesURI.substring(lastSlash + 1, seriesURI.length());

            log.debug("parentUri: " + parentUri);
            log.debug("seriesUri: " + seriesURI);
            for (RaptureFolderInfo folder : listSeriesByUriPrefix(context, parentUri, 1).values()) {
                if (folder.getName().equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.info(e);
            return false;
        }
    }

    @Override
    public void deleteSeries(CallingContext context, String seriesURI) {
        RaptureURI uri = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repository = getRepoOrFail(uri);
        repository.deleteSeries(uri.getDocPath());
    }

    @Override
    public void createSeries(CallingContext context, String seriesURI) {
        RaptureURI uri = new RaptureURI(seriesURI, Scheme.SERIES);
        SeriesRepo repository = getRepoOrFail(uri);
        repository.createSeries(uri.getDocPath());
    }
}
