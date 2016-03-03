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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rapture.batch.ScriptParser;
import rapture.batch.kernel.ContextCommandExecutor;
import rapture.common.CallingContext;
import rapture.common.CallingContextStorage;
import rapture.common.EnvironmentInfo;
import rapture.common.EnvironmentInfoStorage;
import rapture.common.MessageFormat;
import rapture.common.Messages;
import rapture.common.RaptureConstants;
import rapture.common.RaptureIPWhiteList;
import rapture.common.RaptureIPWhiteListStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TypeArchiveConfig;
import rapture.common.TypeArchiveConfigStorage;
import rapture.common.api.AdminApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.common.model.RaptureEntitlementGroupStorage;
import rapture.common.model.RaptureUser;
import rapture.common.model.RaptureUserStorage;
import rapture.common.model.RepoConfig;
import rapture.common.model.RepoConfigStorage;
import rapture.common.storable.helpers.RaptureUserHelper;
import rapture.dsl.tparse.TemplateF;
import rapture.kernel.repo.TypeConversionExecutor;
import rapture.mail.Mailer;
import rapture.object.storage.ObjectFilter;
import rapture.repo.RepoVisitor;
import rapture.repo.Repository;
import rapture.util.IDGenerator;
import rapture.util.RaptureURLCoder;
import rapture.util.encode.RaptureURLCoderFilter;

import com.google.common.collect.Lists;

/**
 * Admin is really dealing with the topLevel admin needs of a RaptureServer
 *
 * @author amkimian
 */
public class AdminApiImpl extends KernelBase implements AdminApi {
    private static final String NAME = "Name"; //$NON-NLS-1$
    private static final String AUTHORITYNAME = "Authority"; //$NON-NLS-1$
    private static final String TEMPLATE = "TEMPLATE"; //$NON-NLS-1$
    private static Logger log = Logger.getLogger(AdminApiImpl.class);

    private Map<String, String> templates = new HashMap<String, String>();
    Messages adminMessageCatalog;

    public AdminApiImpl(Kernel raptureKernel) {
        super(raptureKernel);

        adminMessageCatalog = new Messages("Admin");

        // Update templates from the command line (Environment and Defines)
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().startsWith(TEMPLATE)) {
                String templateName = entry.getKey().substring(TEMPLATE.length() + 1);
                templates.put(templateName, entry.getValue());
            }
        }

        Enumeration<Object> e = System.getProperties().keys();
        while (e.hasMoreElements()) {
            String name = e.nextElement().toString();
            if (name.startsWith(TEMPLATE)) {
                String templateName = name.substring(TEMPLATE.length() + 1);
                templates.put(templateName, System.getProperty(name));
            }
        }
    }

    /**
     * The ip white list is a document that is stored in the settings repo
     */
    @Override
    public void addIPToWhiteList(CallingContext context, String ipAddress) {
        RaptureIPWhiteList wlist = RaptureIPWhiteListStorage.readByFields();
        wlist.getIpWhiteList().add(ipAddress);
        RaptureIPWhiteListStorage.add(wlist, context.getUser(), adminMessageCatalog.getMessage("AddedToWhiteList").toString()); //$NON-NLS-1$
    }

    @Override
    public void addTemplate(CallingContext context, String name, String template, Boolean overwrite) {
        if (templates.containsKey(name) && !templates.get(name).isEmpty() && !overwrite) {
            log.info(adminMessageCatalog.getMessage("NoOverwriteTemplate", name)); //$NON-NLS-1$
        } else {
            log.info(adminMessageCatalog.getMessage("AddingTemplate", new String[] { name, template }));
        }
    }

    @Override
    public void addUser(CallingContext context, String userName, String description, String hashPassword, String email) {
        checkParameter("User", userName); //$NON-NLS-1$
        // Does the user already exist?
        RaptureUser usr = getUser(context, userName);
        if (usr == null) {
            String iAm = context.getUser();
            // High-level audit log message.
            Kernel.getAudit().writeAuditEntry(context, RaptureConstants.DEFAULT_AUDIT_URI, "admin", 2, "New user "+userName+" added by "+iAm);
            usr = new RaptureUser();
            usr.setUsername(userName);
            usr.setDescription(description);
            usr.setHashPassword(hashPassword);
            usr.setEmailAddress(email);
            RaptureUserHelper.validateSalt(usr);
            usr.setInactive(false);
            RaptureUserStorage.add(usr, context.getUser(), adminMessageCatalog.getMessage("AddedUser", userName).toString()); //$NON-NLS-1$
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getMessage("Admin", "UserAlreadyExists", null, null)); //$NON-NLS-1$
        }
    }


    /**
     * Clone the data from the src type to the target
     */
    @Override
    public void copyDocumentRepo(CallingContext context, String srcAuthority, String targAuthority, final Boolean wipe) {
        Repository srcRepo = Kernel.getKernel().getRepo(srcAuthority); //$NON-NLS-1$
        final Repository targRepo = Kernel.getKernel().getRepo(targAuthority); //$NON-NLS-1$
        if (wipe) {
            targRepo.drop();
        }

        srcRepo.visitAll("", null, new RepoVisitor() { //$NON-NLS-1$

                    @Override
                    public boolean visit(String name, JsonContent content, boolean isFolder) {
                        try {
                            log.info(adminMessageCatalog.getMessage("Copying", name).toString()); //$NON-NLS-1$
                            targRepo.addDocument(name, content.getContent(), "$copy", //$NON-NLS-1$
                                    adminMessageCatalog.getMessage("CopyRepo", name).toString(), wipe); //$NON-NLS-1$
                        } catch (RaptureException e) {
                            log.info(adminMessageCatalog.getMessage("NoAddDoc", name)); //$NON-NLS-1$
                        }
                        return true;
                    }

                });
    }

    /**
     * Some type specific calls
     */
    @Override
    public void deleteUser(CallingContext context, String userName) {
        checkParameter("User", userName); //$NON-NLS-1$
        // Assuming the userName is not "you", mark the user as inactive
        if (userName.equals(context.getUser())) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoDeleteYourself")); //$NON-NLS-1$
        }
        log.info(adminMessageCatalog.getMessage("RemovingUser", userName)); //$NON-NLS-1$
        RaptureUser usr = getUser(context, userName);
        if (!usr.getInactive()) {
            if (usr.getHasRoot()) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoDeleteRoot")); //$NON-NLS-1$
            }
            usr.setInactive(true);
            RaptureUserStorage.add(usr, context.getUser(), adminMessageCatalog.getMessage("Inactive", userName).toString()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public void restoreUser(CallingContext context, String userName) {
        checkParameter("User", userName); //$NON-NLS-1$
        log.info(adminMessageCatalog.getMessage("RestoringUser", userName)); //$NON-NLS-1$
        RaptureUser usr = getUser(context, userName);
        if (usr.getInactive()) {
            usr.setInactive(false);
            RaptureUserStorage.add(usr, context.getUser(),  adminMessageCatalog.getMessage("Active", userName).toString()); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public Boolean doesUserExist(CallingContext context, String userName) {
        RaptureUser usr = getUser(context, userName);
        return usr != null;
    }

    @Override
    public RaptureUser getUser(CallingContext context, String userName) {
        return RaptureUserStorage.readByFields(userName);
    }

    @Override
    public RaptureUser generateApiUser(CallingContext context, String prefix, String description) {
        // Special treatment of prefix "debug"
        checkParameter("Prefix", prefix); //$NON-NLS-1$

        String userId = "zz-" + prefix; //$NON-NLS-1$

        if (!prefix.equals("debug")) { //$NON-NLS-1$
            userId = prefix + "-" + IDGenerator.getUUID(); //$NON-NLS-1$
        }
        RaptureUser usr = new RaptureUser();
        usr.setUsername(userId);
        usr.setDescription(description);
        usr.setHashPassword(""); //$NON-NLS-1$
        usr.setInactive(false);
        usr.setApiKey(true);
        RaptureUserStorage.add(usr, context.getUser(), adminMessageCatalog.getMessage("CreatedApi").toString()); //$NON-NLS-1$
        return usr;
    }

    @Override
    public List<String> getIPWhiteList(CallingContext context) {
        RaptureIPWhiteList wlist = RaptureIPWhiteListStorage.readByFields();
        return wlist.getIpWhiteList();
    }



    /**
     * @return @
     */
    public List<RepoConfig> getRepoConfig(CallingContext context) {
        return RepoConfigStorage.readAll();
    }

    @Override
    public List<CallingContext> getSessionsForUser(CallingContext context, final String user) {
        // Get the sessions for this user. Visit and test the content before
        // adding it
        checkParameter("User", user); //$NON-NLS-1$
        final List<CallingContext> ret = new ArrayList<CallingContext>();
        getEphemeralRepo().visitAll("session", //$NON-NLS-1$
                null, new RepoVisitor() {

                    @Override
                    public boolean visit(String name, JsonContent content, boolean isFolder) {
                        if (!isFolder) {
                            CallingContext ctx;
                            try {
                                ctx = CallingContextStorage.readFromJson(content);
                                if (ctx.getUser().equals(user)) {
                                    ret.add(ctx);
                                }
                            } catch (RaptureException e) {
                                logError(name);
                            }
                        }
                        return true;
                    }

                });
        return ret;
    }

    @Override
    public Map<String, String> getSystemProperties(CallingContext context, List<String> keys) {
        Map<String, String> ret = new TreeMap<String, String>();
        if (keys.isEmpty()) {
            ret.putAll(System.getenv());
            Properties p = System.getProperties();
            for (Map.Entry<Object, Object> prop : p.entrySet()) {
                ret.put(prop.getKey().toString(), prop.getValue().toString());
            }
        } else {
            for (String k : keys) {
                String val = System.getenv(k);
                if (val != null) {
                    ret.put(k, System.getenv(k));
                } else {
                    String prop = System.getProperty(k);
                    if (prop != null) {
                        ret.put(k, prop);
                    }
                }
            }
        }
        return ret;
    }



    @Override
    public String getTemplate(CallingContext context, String name) {
        return templates.get(name);
    }

    private void logError(String name) {
        log.error(adminMessageCatalog.getMessage("CouldNotLoadDoc", name).toString());
    }


    @Override
    public void removeIPFromWhiteList(CallingContext context, String ipAddress) {
        RaptureIPWhiteList wlist = RaptureIPWhiteListStorage.readByFields();
        wlist.getIpWhiteList().remove(ipAddress);
        RaptureIPWhiteListStorage.add(wlist, context.getUser(), adminMessageCatalog.getMessage("RemoveWhiteList", ipAddress).toString()); //$NON-NLS-1$
    }


    @Override
    public void resetUserPassword(CallingContext context, String userName, String newHashPassword) {
        checkParameter("User", userName); //$NON-NLS-1$
        checkParameter("Password", newHashPassword); //$NON-NLS-1$
        // Set a new password for this user
        RaptureUser usr = getUser(context, userName);
        if (usr != null) {
            usr.setInactive(false);
            usr.setHashPassword(newHashPassword);
            RaptureUserStorage.add(usr, context.getUser(), adminMessageCatalog.getMessage("PasswordChange", userName).toString()); //$NON-NLS-1$
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoExistUser", userName)); //$NON-NLS-1$
        }
    }

    @Override
    public String createPasswordResetToken(CallingContext context, String userName) {
        checkParameter("User", userName);
        RaptureUser user = getUser(context, userName);
        if (user == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoExistUser", userName)); //$NON-NLS-1$
        }
        String token = generateSecureToken();
        user.setPasswordResetToken(token);
        user.setTokenExpirationTime(DateTime.now().plusDays(1).getMillis());

        RaptureUserStorage.add(user, context.getUser(), adminMessageCatalog.getMessage("GenReset", userName).toString()); //$NON-NLS-1$
        return token;
    }

    @Override
    public String createRegistrationToken(CallingContext context, String userName) {
        checkParameter("User", userName);
        RaptureUser user = getUser(context, userName);
        if (user == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoExistUser", userName)); //$NON-NLS-1$
        }
        String token = generateSecureToken();
        user.setRegistrationToken(token);
        user.setVerified(false);
        // Registration Token doesn't
        RaptureUserStorage.add(user, context.getUser(), adminMessageCatalog.getMessage("GenReg", userName).toString()); //$NON-NLS-1$
        return token;
    }

    @Override
    public Boolean verifyUser(CallingContext context, String userName, String token) {
        checkParameter("User", userName);
        checkParameter("Token", token);
        RaptureUser user = getUser(context, userName);
        boolean match = user.getVerified();

        if (user == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoExistUser", userName)); //$NON-NLS-1$
        }
        if (!match) {
            match = token.equals(user.getRegistrationToken());
            if (match) {
                user.setRegistrationToken("");
                user.setVerified(true);
                RaptureUserStorage.add(user, context.getUser(), adminMessageCatalog.getMessage("CreatedApi").toString()); //$NON-NLS-1$
            }
        }
        return match;
    }

    private String generateSecureToken() {
        try {
            // get secure random
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            byte bytes[] = new byte[128];
            random.nextBytes(bytes);
            // get its digest
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] result = sha.digest(bytes);
            // encode to hex
            return (new Hex()).encodeHexString(result);
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    public void cancelPasswordResetToken(CallingContext context, String userName) {
        checkParameter("User", userName);
        RaptureUser user = getUser(context, userName);
        if (user == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoExistUser", userName)); //$NON-NLS-1$
        }
        // expire token now
        user.setTokenExpirationTime(System.currentTimeMillis());
        RaptureUserStorage.add(user, context.getUser(), "Cancel password reset token for user " + userName); //$NON-NLS-1$
    }

    @Override
    public void updateUserEmail(CallingContext context, String userName, String newEmail) {
        checkParameter("User", userName); //$NON-NLS-1$
        RaptureUser user = getUser(context, userName);
        if (user != null) {
            user.setEmailAddress(newEmail);
            RaptureUserStorage.add(user, context.getUser(), adminMessageCatalog.getMessage("UpdateEmail") + userName);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoExistUser", userName)); //$NON-NLS-1$
        }
    }

    @Override
    public void emailUser(CallingContext context, String userName, String emailTemplate, Map<String, Object> templateValues) {
        checkParameter("User", userName); //$NON-NLS-1$
        RaptureUser user = getUser(context, userName);
        if (user != null) {
            templateValues.put("user", user);
            Mailer.email(context, emailTemplate, templateValues);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, adminMessageCatalog.getMessage("NoExistUser", userName)); //$NON-NLS-1$
        }
    }



    @Override
    public String runTemplate(CallingContext context, String name, String params) {
        String template = templates.get(name);
        if (template == null) {
            log.info(adminMessageCatalog.getMessage("NoExist", name)); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        String args[] = { template, params };
        log.info(adminMessageCatalog.getMessage("RunTemplate", args).toString()); //$NON-NLS-1$ //$NON-NLS-2$
        String ret = TemplateF.parseTemplate(template, params);
        log.info(adminMessageCatalog.getMessage("TemplateOutput", ret).toString()); //$NON-NLS-1$
        return ret;
    }


    @Override
    public List<RaptureUser> getAllUsers(CallingContext context) {
        return RaptureUserStorage.readAll();
    }

    TypeConversionExecutor tExecutor = new TypeConversionExecutor();

    @Override
    public void initiateTypeConversion(CallingContext context, String raptureURIString, String newConfig, int versionsToKeep) {
        RaptureURI internalURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        checkParameter(NAME, internalURI.getDocPath());
        tExecutor.runRebuildFor(internalURI.getAuthority(), internalURI.getDocPath(), newConfig, versionsToKeep);
    }

    @Override
    public void putArchiveConfig(CallingContext context, String raptureURIString, TypeArchiveConfig config) {
        RaptureURI internalURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        checkParameter(NAME, internalURI.getDocPath());
        config.setAuthority(internalURI.getAuthority());
        config.setTypeName(internalURI.getDocPath());
        TypeArchiveConfigStorage.add(config, context.getUser(), "Created type archive config");
    }

    @Override
    public TypeArchiveConfig getArchiveConfig(CallingContext context, String raptureURIString) {
        RaptureURI addressURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        checkParameter(NAME, addressURI.getDocPath());
        return TypeArchiveConfigStorage.readByAddress(addressURI);
    }

    @Override
    public void deleteArchiveConfig(CallingContext context, String raptureURIString) {
        RaptureURI addressURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        checkParameter(NAME, addressURI.getDocPath());
        TypeArchiveConfigStorage.deleteByAddress(addressURI, context.getUser(), "Removed archive config");
    }

    /**
     * A call to simply test that the system is working.
     */
    @Override
    public Boolean ping(CallingContext context) {
        return true;
    }

    @Override
    public void addMetadata(CallingContext context, Map<String, String> values, Boolean overwrite) {
        if ((values == null) || values.isEmpty()) return;

        Map<String, String> metadata = context.getMetadata();
        if (metadata == null) metadata = new HashMap<String, String>();
        for (String key : values.keySet()) {
            if (!overwrite && metadata.containsKey(key)) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, key + " exists and overwrite was disallowed");
            }
            metadata.put(key, values.get(key));
        }
        context.setMetadata(metadata);
        getEphemeralRepo().addToStage(RaptureConstants.OFFICIAL_STAGE, "session/" + context.getContext(), JacksonUtil.jsonFromObject(context), false);
    }

    // Message of the day and environment, these are all in the same document.

    private EnvironmentInfo getEnvInfo(CallingContext context) {
        EnvironmentInfo info = EnvironmentInfoStorage.readByFields();
        if (info == null) {
            info = new EnvironmentInfo();
            info.setMotd("Welcome to Rapture");
            info.setName("Rapture");
            info.getProperties().put("BANNER_COLOR", "blue");
        }
        return info;
    }

    private void putEnvInfo(CallingContext context, EnvironmentInfo info) {
        EnvironmentInfoStorage.add(info, context.getUser(), "Updated environment info");
    }

    @Override
    public void setMOTD(CallingContext context, String message) {
        EnvironmentInfo info = getEnvInfo(context);
        info.setMotd(message);
        putEnvInfo(context, info);
    }

    @Override
    public String getMOTD(CallingContext context) {
        EnvironmentInfo info = getEnvInfo(context);
        return info.getMotd();
    }

    @Override
    public void setEnvironmentName(CallingContext context, String name) {
        EnvironmentInfo info = getEnvInfo(context);
        info.setName(name);
        putEnvInfo(context, info);
    }

    @Override
    public void setEnvironmentProperties(CallingContext context, Map<String, String> properties) {
        EnvironmentInfo info = getEnvInfo(context);
        info.getProperties().putAll(properties);
        putEnvInfo(context, info);
    }

    @Override
    public String getEnvironmentName(CallingContext context) {
        EnvironmentInfo info = getEnvInfo(context);
        return info.getName();
    }

    @Override
    public Map<String, String> getEnvironmentProperties(CallingContext context) {
        EnvironmentInfo info = getEnvInfo(context);
        return info.getProperties();

    }

    @Override
    public void destroyUser(CallingContext context, String userName) {
        checkParameter("User", userName); //$NON-NLS-1$
        log.info("Destroying user: " + userName);
        RaptureUser usr = getUser(context, userName);
        if (usr == null) {
            MessageFormat error = adminMessageCatalog.getMessage("NoExistUser", userName);
            log.error(error.toString());
            throw RaptureExceptionFactory.create(error);
        }
        if (usr.getInactive()) {
            MessageFormat error = adminMessageCatalog.getMessage("UserNotDestroyed", userName);
            log.error(error.toString());
            throw RaptureExceptionFactory.create(error);
        }
        RaptureUserStorage.deleteByFields(userName, context.getUser(), adminMessageCatalog.getMessage("UserDestroyed", userName).toString());
    }

    @Override
    public String encode(CallingContext context, String toEncode) {
        return RaptureURLCoder.encode(toEncode);
    }

    private static final RaptureURLCoderFilter allowDotSlash = new RaptureURLCoderFilter("./");

    @Override
    public String createURI(CallingContext context, String path, String leaf) {
        return RaptureURLCoder.encode(path, allowDotSlash) + "/" + RaptureURLCoder.encode(leaf);
    }

    @Override
    public String createMultipartURI(CallingContext context, List<String> elements) {
        if (elements == null) return null;

        StringBuilder sb = new StringBuilder();

        sb.append("/");
        for (String element : elements)
            sb.append("/").append(RaptureURLCoder.encode(element));
        return sb.toString();
    }

    @Override
    public String decode(CallingContext context, String encoded) {
        return RaptureURLCoder.encode(encoded);
    }

    @Override
    public List<String> findGroupNamesByUser(CallingContext context, String userName) {
        List<String> result = Lists.newArrayList();
        FindGroupsByUserFilter filter = new FindGroupsByUserFilter(userName);
        List<RaptureEntitlementGroup> groups = RaptureEntitlementGroupStorage.filterAll(filter);
        for (RaptureEntitlementGroup group : groups) {
            result.add(group.getName());
        }
        return result;
    }

    private class FindGroupsByUserFilter implements ObjectFilter<RaptureEntitlementGroup> {
        private String userName;

        private FindGroupsByUserFilter(String userName) {
            this.userName = userName;
        }

        @Override
        public boolean shouldInclude(RaptureEntitlementGroup obj) {
            return obj.getUsers().contains(userName);
        }
    }
}
