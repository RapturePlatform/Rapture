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

import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import rapture.common.*;
import rapture.common.api.UserApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.RaptureUser;
import rapture.common.model.RaptureUserStorage;
import rapture.common.version.ApiVersion;
import rapture.repo.Repository;
import rapture.server.ServerApiVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import static rapture.common.Scheme.DOCUMENT;

public class UserApiImpl extends KernelBase implements UserApi {
    private static Logger log = Logger.getLogger(UserApiImpl.class);

    public static final String AUTOID = "id";

    public UserApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public RaptureUser changeMyPassword(CallingContext context, String oldHashPassword, String newHashPassword) {
        RaptureUser usr = Kernel.getAdmin().getTrusted().getUser(context, context.getUser());
        if (usr != null) {
            if (usr.getHashPassword().equals(oldHashPassword)) {
                usr.setHashPassword(newHashPassword);
                RaptureUserStorage.add(usr, context.getUser(), "Updated my password");
                return usr;
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_UNAUTHORIZED, "Bad Password");
            }
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Could not find user record");
        }
    }

    @Override
    public RaptureUser getWhoAmI(CallingContext context) {
        RaptureUser usr = Kernel.getAdmin().getTrusted().getUser(context, context.getUser());
        if (usr != null) {
            return usr;
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Could not find this user");
        }
    }

    @Override
    public RaptureUser changeMyEmail(CallingContext context, String newAddress) {
        RaptureUser usr = Kernel.getAdmin().getTrusted().getUser(context, context.getUser());
        if (usr != null) {
            usr.setEmailAddress(newAddress);
            RaptureUserStorage.add(usr, context.getUser(), "Updated my email");
            return usr;
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Could not find this user");
        }
    }

    /**
     * This is similar to runView, except that we don't use a predefined view.
     * However we do use a filter function to filter records, and then pass the
     * document through a field grabber to retrieve the values. We then place
     * the results into a RaptureCubeResult that does the grouping and totalling
     * automatically, particularly as we also pass the field definitions to the
     * cube result so it knows how to create sub totals/totals etc.
     */
    @Override
    public RaptureCubeResult runFilterCubeView(final CallingContext context, final String typeURI, final String scriptURI, String filterParams,
            String groupFields, String columnFields) {

        final RaptureURI internalURI = new RaptureURI(typeURI, DOCUMENT);

        final RaptureCubeResult ret = new RaptureCubeResult();
        //        final RaptureScript filterScript = Kernel.getScript().getScript(context, scriptURI);
        //        final IRaptureScript scriptHandler = ScriptFactory.getScript(filterScript);
        //        // Get fields
        //        final List<String> colFields = StringUtil.list(columnFields);
        //        final List<String> grpFields = StringUtil.list(groupFields);
        //        // Load fields
        //
        //        List<RaptureFolderInfo> children = Kernel.getFields().listSheetByUriPrefix(context, "//" + internalURI.getAuthority() + "/");
        //        final List<RaptureField> colFieldInfo = new ArrayList<RaptureField>();
        //        for (RaptureFolderInfo rfi : children) {
        //            RaptureField rf = Kernel.getFields().getField(context, "//" + internalURI.getAuthority() + "/" + rfi.getName());
        //            if (rf != null && colFields.contains(rfi.getName())) {
        //                colFieldInfo.add(rf);
        //            }
        //        }
        //
        //        ret.setColFieldInfo(colFieldInfo);
        //        // Now, run the filter and off we go
        //        final Map<String, Object> realParams = StringUtil.getMapFromString(filterParams);
        //
        //        // script://test/trueFn
        //        final Repository repository = Kernel.getKernel().getRepo(internalURI.getAuthority());
        //
        //        // Hello this is a change
        //        repository.visitAll("", null, new RepoVisitor() {
        //
        //            @Override
        //            public boolean visit(final String name, JsonContent content, boolean isFolder) {
        //                if (!isFolder) {
        //                    // Load the document, convert to a RaptureDataContext, pass
        //                    // it to the filterScript, if that
        //                    // returns true, run it through the Map function.
        //
        //                    // Check the entitlement for this document
        //
        //                    try {
        //                        Kernel.getKernel().validateContext(context, EntitlementConst.DOC_DATABATCH, new IEntitlementsContext() {
        //
        //                            @Override
        //                            public String getDocPath() {
        //                                return internalURI.getDocPath();
        //                            }
        //
        //                            @Override
        //                            public String getFullPath() {
        //                                return internalURI.getFullPath();
        //                            }
        //
        //                            @Override
        //                            public String getAuthority() {
        //                                return internalURI.getAuthority();
        //                            }
        //
        //                        });
        //                    } catch (RaptureException e) {
        //                        log.info("Failed validation check for " + name + " so not using it in view");
        //                    }
        //
        //                    RaptureDataContext dataContext = new RaptureDataContext();
        //                    dataContext.setDisplayName(name);
        //                    dataContext.putJsonContent(content);
        //                    dataContext.setUser(context.getUser());
        //                    dataContext.setWhen(new Date());
        //
        //                    // With the results, add to the RaptureViewResult.
        //                    try {
        //                        if (scriptHandler.runFilter(context, filterScript, dataContext, realParams)) {
        //                            // Run to get the fields (twice), then create the
        //                            // grouping and add it to the cuve
        //                            internalURI.getDocPath();
        //                            RaptureURI fieldURI = RaptureURI.builder(DOCUMENT, internalURI.getAuthority()).docPath(name).build();
        //                            List<String> colResults = Kernel.getFields().getFieldsFromContent(context, fieldURI.toString(), content.getContent(),
        //                                    colFields);
        //                            List<String> grpResults = Kernel.getFields().getFieldsFromContent(context, fieldURI.toString(), content.getContent(),
        //                                    grpFields);
        //                            ret.addEntry(name, grpResults, colResults);
        //                        }
        //                    } catch (NullPointerException e) {
        //                        log.info("Failed to handle content during filter cube view " + ExceptionToString.format(e));
        //                    }
        //                }
        //                return true;
        //            }
        //
        //        });
        //
        //        ret.finish();
        return ret;
    }

    @Override
    public RaptureCubeResult runNativeFilterCubeView(CallingContext context, String docURI, String repoType, List<String> queryParams, String groupFields,
            String columnFields) {
        final RaptureCubeResult ret = new RaptureCubeResult();
        //        RaptureURI internalURI = new RaptureURI(docURI, DOCUMENT);
        //        Repository repository = getKernel().getRepo(internalURI.getAuthority());
        //        // Get fields
        //        final List<String> colFields = StringUtil.list(columnFields);
        //        final List<String> grpFields = StringUtil.list(groupFields);
        //        // Load fields
        //
        //        List<RaptureFolderInfo> children = Kernel.getFields().listSheetByUriPrefix(context, "//" + internalURI.getAuthority() + "/");
        //        final List<RaptureField> colFieldInfo = new ArrayList<RaptureField>();
        //        for (RaptureFolderInfo rfi : children) {
        //            RaptureField rf = Kernel.getFields().getField(context, rfi.getName());
        //            if (rf != null && colFields.contains(rfi.getName())) {
        //                colFieldInfo.add(rf);
        //            }
        //        }
        //
        //        ret.setColFieldInfo(colFieldInfo);
        //        if (repository != null) {
        //            // We need to keep on running a specific nativeQuery
        //            // "with limit and bounds" until we get
        //            // no more records.
        //            RaptureNativeQueryResult currentResult = null;
        //            int limit = 100;
        //            int offset = 0;
        //            currentResult = repository.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
        //            while (!currentResult.getRows().isEmpty()) {
        //                for (RaptureNativeRow row : currentResult.getRows()) {
        //                    // Do something with this result
        //                    RaptureURI fieldURI = RaptureURI.builder(DOCUMENT, internalURI.getAuthority()).docPath(row.getName()).build();
        //
        //                    // RaptureURI fieldURI = RaptureURI.builder(FIELD,
        //                    // internalURI.getAuthority()).addDocPath(internalURI.getDocPath()).addDocPath(row.getName()).build();
        //                    List<String> colResults = Kernel.getFields().getFieldsFromContent(context, fieldURI.toString(), row.getContent().getContent(),
        //                            colFields);
        //                    List<String> grpResults = Kernel.getFields().getFieldsFromContent(context, fieldURI.toString(), row.getContent().getContent(),
        //                            grpFields);
        //                    ret.addEntry(row.getName(), grpResults, colResults);
        //                }
        //                offset += currentResult.getRows().size();
        //                currentResult = repository.runNativeQueryWithLimitAndBounds(repoType, queryParams, limit, offset);
        //            }
        //        } else {
        //            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "No such repo");
        //        }
        return ret;
    }

    @Override
    public RaptureQueryResult runNativeQuery(CallingContext context, String typeURI, String repoType, List<String> queryParams) {
        // Run a native query against the lower level repo (which must be
        // an unversioned repo)
        RaptureURI internalURI = new RaptureURI(typeURI, DOCUMENT);
        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", internalURI.toShortString())); //$NON-NLS-1$
        }
        Repository repository = getKernel().getRepo(internalURI.getAuthority());
        if (repository != null) {
            return repository.runNativeQuery(repoType, queryParams);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NoSuchRepo", internalURI.toAuthString())); //$NON-NLS-1$
        }
    }

    @Override
    public RaptureUser updateMyDescription(CallingContext context, String description) {
        RaptureUser usr = Kernel.getAdmin().getTrusted().getUser(context, context.getUser());
        if (usr != null) {
            usr.setDescription(description);
            RaptureUserStorage.add(usr, context.getUser(), "Updated my description");
            return usr;
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Could not find user record");
        }
    }

    @Override
    public void logoutUser(CallingContext context) {
        // Remove cookie, or actually remove session
        CallingContextStorage.deleteByFields(context.getContext(), context.getUser(), "Logout user");
    }

    private String getPreferenceKey(String user, String category, String name) {
        return String.format("preference/%s/%s/%s", user, category, name);
    }

    private String getPreferenceCatPrefix(String user) {
        return String.format("preference/%s", user);
    }

    private String getPreferenceCatPrefix(String user, String category) {
        return String.format("preference/%s/%s", user, category);
    }

    @Override
    public void storePreference(CallingContext context, String category, String name, String content) {
        // Preferences are stored in the settings repo, in
        // preference/[user]/[category]/[name]
        getSettingsRepo().addDocument(getPreferenceKey(context.getUser(), category, name), content, context.getUser(), "Store preference", false);
    }

    @Override
    public String getPreference(CallingContext context, String category, String name) {
        return getSettingsRepo().getDocument(getPreferenceKey(context.getUser(), category, name));
    }

    @Override
    public void removePreference(CallingContext context, String category, String name) {
        getSettingsRepo().removeDocument(getPreferenceKey(context.getUser(), category, name), context.getUser(), "Removed preference");
    }

    @Override
    public List<String> getPreferenceCategories(CallingContext context) {
        List<RaptureFolderInfo> categories = getSettingsRepo().getChildren(getPreferenceCatPrefix(context.getUser()));
        List<String> ret = new ArrayList<String>(categories == null ? 0 : categories.size());
        if (categories == null) {
            return ret;
        }
        for (RaptureFolderInfo cat : categories) {
            if (cat.isFolder()) {
                ret.add(cat.getName());
            }
        }
        return ret;
    }

    @Override
    public List<String> getPreferencesInCategory(CallingContext context, String category) {
        List<RaptureFolderInfo> preferences = getSettingsRepo().getChildren(getPreferenceCatPrefix(context.getUser(), category));
        List<String> ret = new ArrayList<String>(preferences.size());
        for (RaptureFolderInfo pref : preferences) {
            if (!pref.isFolder()) {
                ret.add(pref.getName());
            }
        }
        return ret;
    }

    @Override
    public ApiVersion getServerApiVersion(final CallingContext context) {
        return ServerApiVersion.getApiVersion();
    }

    @Override
    public Boolean isPermitted(CallingContext context, String apiCallOrEntitlement, String callParam) {

        String entitlementString = null;
        if (apiCallOrEntitlement.startsWith("/")) {
            entitlementString = apiCallOrEntitlement;
        } else {
            String[] elements = apiCallOrEntitlement.split("\\.");
            if (elements.length == 2) {
                String key = WordUtils.capitalize(elements[0] + "_" + elements[1]);
                try {
                    entitlementString = EntitlementSet.valueOf(key).getPath();
                } catch (Exception e) {
                    log.warn("Method " + apiCallOrEntitlement + " unknown");
                }
            } else if (elements.length == 3) {
                // If not in rapture.common then we need to use reflection here instead
                // Expect <sdkname>.<api>.<method> 
                // EntitlementSet is rapture.<sdkname>.server.EntitlementSet

                String entitlementSetClassName = "rapture." + elements[0] + ".server.EntitlementSet";
                String key = WordUtils.capitalize(elements[1] + "_" + elements[2]);

                try {
                    Class<?> entitlementSetClass = Class.forName(entitlementSetClassName);
                    try {
                        Method entitlementSetValueOf = entitlementSetClass.getMethod("valueOf", String.class);
                        Object entitlement = entitlementSetValueOf.invoke(entitlementSetClass, key);
                        Method entitlementGetPath = entitlement.getClass().getMethod("getPath", (Class<?>[]) null);
                        Object entitlementStringObject = entitlementGetPath.invoke(entitlement, (Object[]) null);
                        if (entitlementStringObject != null)
                            entitlementString = entitlementStringObject.toString();
                    } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        log.warn("Method " + apiCallOrEntitlement + " unknown " + e.getMessage());
                        log.trace(ExceptionToString.format(e));
                    }
                } catch (ClassNotFoundException | SecurityException e) {
                    log.warn("Cannot find Class " + entitlementSetClassName);
                }
            } else {
                log.warn("Illegal method name format " + apiCallOrEntitlement);
            }
        }
        if (entitlementString == null) return false;

        int i1 = entitlementString.indexOf("$");
        if (i1 > 0) {
            if (callParam == null) callParam = "";
            int i2 = callParam.indexOf(":");
            while (callParam.charAt(++i2) == '/') ;
            entitlementString = entitlementString.substring(0, i1) + callParam.substring(i2);
        }
        try {
            Kernel.getKernel().validateContext(context, entitlementString, null);
            return true;
        } catch (RaptureException e) {
            // Expected if not entitled
            log.trace(ExceptionToString.format(e));
            return false;
        }
    }
}
