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
