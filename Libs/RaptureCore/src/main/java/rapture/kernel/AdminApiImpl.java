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

import com.google.common.collect.Lists;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rapture.batch.ScriptParser;
import rapture.batch.kernel.ContextCommandExecutor;
import rapture.common.*;
import rapture.common.api.AdminApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.*;
import rapture.common.storable.helpers.RaptureUserHelper;
import rapture.dsl.tparse.TemplateF;
import rapture.kernel.repo.TypeConversionExecutor;
import rapture.object.storage.ObjectFilter;
import rapture.repo.RepoVisitor;
import rapture.repo.Repository;
import rapture.util.IDGenerator;
import rapture.util.RaptureURLCoder;
import rapture.util.encode.RaptureURLCoderFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

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
    private static final String ERROR = Messages.getString("Admin.CouldNotLoadDoc"); //$NON-NLS-1$

    private Map<String, String> templates = new HashMap<String, String>();

    public AdminApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
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
        RaptureIPWhiteListStorage.add(wlist, context.getUser(), Messages.getString("Admin.AddedToWhiteList")); //$NON-NLS-1$
    }

    @Override
    public RaptureRemote addRemote(CallingContext context, String name, String description, String url, String apiKey, String optPass) {
        checkParameter(NAME, name);
        checkParameter("Url", url); //$NON-NLS-1$
        checkParameter("ApiKey", apiKey); //$NON-NLS-1$
        RaptureRemote ret = new RaptureRemote();
        ret.setName(name);
        ret.setDescription(description);
        ret.setUrl(url);
        ret.setApiKey(apiKey);
        ret.setOptionalPass(optPass);
        RaptureRemoteStorage.add(ret, context.getUser(), Messages.getString("Admin.AddRemote")); //$NON-NLS-1$
        return ret;
    }

    @Override
    public void addTemplate(CallingContext context, String name, String template, Boolean overwrite) {
        if (templates.containsKey(name) && !templates.get(name).isEmpty() && !overwrite) {
            log.info(Messages.getString("Admin.NoOverwriteTemplate") + name); //$NON-NLS-1$
        } else {
            log.info(Messages.getString("Admin.AddingTemplate") + name + Messages.getString("Admin.Value") + template); //$NON-NLS-1$ //$NON-NLS-2$
            templates.put(name, template);
        }
    }

    @Override
    public void addUser(CallingContext context, String userName, String description, String hashPassword, String email) {
        checkParameter("User", userName); //$NON-NLS-1$
        // Does the user already exist?
        RaptureUser usr = getUser(context, userName);
        if (usr == null) {
            usr = new RaptureUser();
            usr.setUsername(userName);
            usr.setDescription(description);
            usr.setHashPassword(hashPassword);
            usr.setEmailAddress(email);
            RaptureUserHelper.validateSalt(usr);
            usr.setInactive(false);
            RaptureUserStorage.add(usr, context.getUser(), Messages.getString("Admin.AddedUser") + userName); //$NON-NLS-1$
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.UserAlreadyExists")); //$NON-NLS-1$
        }
    }

    @Override
    public void clearRemote(CallingContext context, String raptureURIString) {
        RaptureURI internalURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", internalURI.toShortString())); //$NON-NLS-1$
        }
        checkParameter(AUTHORITYNAME, internalURI.getAuthority());
        Repository repository = getKernel().getRepo(internalURI.getAuthority()); //$NON-NLS-1$
        repository.clearRemote();
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
                    log.info(Messages.getString("Admin.Copying") + name); //$NON-NLS-1$
                    targRepo.addDocument(name, content.getContent(), "$copy", //$NON-NLS-1$
                            Messages.getString("Admin.CopyRepo"), wipe); //$NON-NLS-1$
                } catch (RaptureException e) {
                    log.info(Messages.getString("Admin.NoAddDoc") + name); //$NON-NLS-1$
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
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.NoDeleteYourself")); //$NON-NLS-1$
        }
        log.info(Messages.getString("Admin.RemovingUser") + userName); //$NON-NLS-1$
        RaptureUser usr = getUser(context, userName);
        if (!usr.getInactive()) {
            if (usr.getHasRoot()) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.NoDeleteRoot")); //$NON-NLS-1$
            }
            usr.setInactive(true);
            RaptureUserStorage.add(usr, context.getUser(),
                    Messages.getString("Admin.Made") + userName + Messages.getString("Admin.Inactive")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    @Override
    public void restoreUser(CallingContext context, String userName) {
        checkParameter("User", userName); //$NON-NLS-1$
        log.info(Messages.getString("Admin.RestoringUser") + userName); //$NON-NLS-1$
        RaptureUser usr = getUser(context, userName);
        if (usr.getInactive()) {
            usr.setInactive(false);
            RaptureUserStorage
                    .add(usr, context.getUser(), Messages.getString("Admin.Made") + userName + Messages.getString("Admin.Active")); //$NON-NLS-1$ //$NON-NLS-2$
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
        RaptureUserStorage.add(usr, context.getUser(), Messages.getString("Admin.CreatedApi")); //$NON-NLS-1$
        return usr;
    }

    @Override
    public List<String> getIPWhiteList(CallingContext context) {
        RaptureIPWhiteList wlist = RaptureIPWhiteListStorage.readByFields();
        return wlist.getIpWhiteList();
    }

    @Override
    public List<RaptureRemote> getRemotes(CallingContext context) {
        return RaptureRemoteStorage.readAll();
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
    public List<String> getTags(CallingContext context, String raptureURIString) {
        RaptureURI internalURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        checkParameter(NAME, internalURI.getDocPath());
        Repository repository = getKernel().getRepo(internalURI.getFullPath()); //$NON-NLS-1$
        return repository.getTags();
    }

    @Override
    public String getTemplate(CallingContext context, String name) {
        return templates.get(name);
    }

    private void logError(String name) {
        log.error(String.format(ERROR, name));
    }

    @Override
    public void pullRemote(CallingContext context, String raptureURIString) {
        RaptureURI internalURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", internalURI.toShortString())); //$NON-NLS-1$
        }
        checkParameter(NAME, internalURI.getDocPath());

        log.info(Messages.getString("Admin.PullPerspective") + Messages.getString("Admin.OnType") + internalURI.getDocPath() //$NON-NLS-1$ //$NON-NLS-2$
                + Messages.getString("Admin.InAuthority") + internalURI.getAuthority()); //$NON-NLS-1$
        // NOW this is the fun one
        // Here are the steps
        // (1) Look at the local, does it have a remote defined? If
        // so, what is the latest commit known about
        // for that remote?
        // (2) Make a remote call to retrieve the commits up to that commit from
        // the RemoteLink
        // (3) For each commit, look at all of the references, retrieve them
        // from the remote and persist them
        // into the repo.
        // (4) Update the local perspective to point at the latest commit, and
        // update the Remote commit to point at that.

        getKernel().getRepo(internalURI.getFullPath()); //$NON-NLS-1$
    }

    @Override
    public void removeIPFromWhiteList(CallingContext context, String ipAddress) {
        RaptureIPWhiteList wlist = RaptureIPWhiteListStorage.readByFields();
        wlist.getIpWhiteList().remove(ipAddress);
        RaptureIPWhiteListStorage.add(wlist, context.getUser(), Messages.getString("Admin.RemoveWhiteList")); //$NON-NLS-1$
    }

    @Override
    public void deleteRemote(CallingContext context, String name) {
        RaptureRemoteStorage.deleteByFields(name, context.getUser(), Messages.getString("Admin.RemoveRemote")); //$NON-NLS-1$ //$NON-NLS-2$
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
            RaptureUserStorage.add(usr, context.getUser(), Messages.getString("Admin.PasswordChange") + userName); //$NON-NLS-1$
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.NoExistUser")); //$NON-NLS-1$
        }
    }

    @Override
    public String createPasswordResetToken(CallingContext context, String username) {
        checkParameter("User", username);
        RaptureUser user = getUser(context, username);
        if(user == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.NoExistUser")); //$NON-NLS-1$
        }
        String token = generateSecureToken();
        user.setPasswordResetToken(token);
        user.setTokenExpirationTime(DateTime.now().plusDays(1).getMillis());
        RaptureUserStorage.add(user, context.getUser(), "Generate password reset token for user " + username); //$NON-NLS-1$
        return token;
    }

    private String generateSecureToken() {
        try {
            // get secure random
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            byte bytes[] = new byte[128];
            random.nextBytes(bytes);
            //get its digest
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] result =  sha.digest(bytes);
            // encode to hex
            return (new Hex()).encodeHexString(result);
        } catch (NoSuchAlgorithmException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    public void cancelPasswordResetToken(CallingContext context, String username) {
        checkParameter("User", username);
        RaptureUser user = getUser(context, username);
        if(user == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.NoExistUser")); //$NON-NLS-1$
        }
        // expire token now
        user.setTokenExpirationTime(System.currentTimeMillis());
        RaptureUserStorage.add(user, context.getUser(), "Cancel password reset token for user " + username); //$NON-NLS-1$
    }

    @Override
    public void updateUserEmail(CallingContext context, String userName, String newEmail) {
        checkParameter("User", userName); //$NON-NLS-1$
        RaptureUser user = getUser(context, userName);
        if (user != null) {
            user.setEmailAddress(newEmail);
            RaptureUserStorage.add(user, context.getUser(), Messages.getString("Admin.UpdateEmail") + userName);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.NoExistUser")); //$NON-NLS-1$
        }
    }

    @Override
    public String runBatchScript(CallingContext context, String script) {
        ScriptParser parser = new ScriptParser(new ContextCommandExecutor(context));
        ByteArrayOutputStream so = new ByteArrayOutputStream();
        try {
            parser.parseScript(new StringReader(script), so);
        } catch (IOException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error running batch script");
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }
        return so.toString();
    }

    @Override
    public String runTemplate(CallingContext context, String name, String params) {
        String template = templates.get(name);
        if (template == null) {
            log.info("Null template execution - " + name + Messages.getString("Admin.NoExist")); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        log.info(Messages.getString("Admin.RunTemplate") + template + Messages.getString("Admin.Params") + params); //$NON-NLS-1$ //$NON-NLS-2$
        String ret = TemplateF.parseTemplate(template, params);
        log.info(Messages.getString("Admin.TemplateOutput") + ret); //$NON-NLS-1$
        return ret;
    }

    /**
     * This method is used to setup a link between a local repo and a remote one. The remoteName
     */
    @Override
    public void setRemote(CallingContext context, String raptureURIString, String remote, String remoteURIString) {
        RaptureURI internalURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", internalURI.toShortString())); //$NON-NLS-1$
        }
        checkParameter(NAME, internalURI.getAuthority());
        checkParameter("Remote", remote); //$NON-NLS-1$
        RaptureURI remoteURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        if (remoteURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", remoteURI.toShortString())); //$NON-NLS-1$
        }
        checkParameter(NAME, remoteURI.getAuthority());

        // This is set in the repo
        Repository repository = getKernel().getRepo(internalURI.getAuthority()); //$NON-NLS-1$
        repository.setRemote(remote, remoteURI.getAuthority());
        Kernel.typeChanged(internalURI);
    }

    @Override
    public void updateRemoteApiKey(CallingContext context, String name, String apiKey) {
        checkParameter(NAME, name);
        checkParameter("ApiKey", apiKey); //$NON-NLS-1$
        RaptureRemote ret = Kernel.INSTANCE.getRemote(name);
        if (ret == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Admin.NoFindRemote") + name); //$NON-NLS-1$
        } else {
            ret.setApiKey(apiKey);
            RaptureRemoteStorage.add(ret, context.getUser(), Messages.getString("Admin.UpdatedApi")); //$NON-NLS-1$
        }
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
            String error = "User '" + userName + "' not found.  Cannot destroy";
            log.error(error);
            throw RaptureExceptionFactory.create("User '" + userName + "' not found.  Cannot destroy");
        }
        if (usr.getInactive()) {
            String error = "User '" + userName + "' has not been disabled.  Cannot Destroy";
            log.error(error);
            throw RaptureExceptionFactory.create(error);
        }
        RaptureUserStorage.deleteByFields(userName, context.getUser(), "Destroying user record");
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
    public List<String> findGroupNamesByUser(CallingContext context, String username) {
        List<String> result = Lists.newArrayList();
        FindGroupsByUserFilter filter = new FindGroupsByUserFilter(username);
        List<RaptureEntitlementGroup> groups = RaptureEntitlementGroupStorage.filterAll(filter);
        for (RaptureEntitlementGroup group : groups) {
            result.add(group.getName());
        }
        return result;
    }

    private class FindGroupsByUserFilter implements ObjectFilter<RaptureEntitlementGroup> {
        private String username;

        private FindGroupsByUserFilter(String username) {
            this.username = username;
        }

        @Override
        public boolean shouldInclude(RaptureEntitlementGroup obj) {
            return obj.getUsers().contains(username);
        }
    }
}
