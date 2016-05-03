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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.common.DocumentAttributeFactory;
import rapture.common.EntitlementSet;
import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureIdGenConfig;
import rapture.common.RaptureScript;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.XferDocumentAttribute;
import rapture.common.api.DocApi;
import rapture.common.event.DocEventConstants;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocWriteHandle;
import rapture.common.model.DocumentAttribute;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentRepoConfig;
import rapture.common.model.DocumentRepoConfigStorage;
import rapture.common.model.DocumentWithMeta;
import rapture.common.model.IndexScriptPair;
import rapture.common.model.RaptureDocConfig;
import rapture.common.model.RunEventHandle;
import rapture.common.shared.doc.DeleteDocPayload;
import rapture.common.shared.doc.GetDocPayload;
import rapture.dsl.dparse.AbsoluteVersion;
import rapture.dsl.dparse.AsOfTimeDirective;
import rapture.dsl.dparse.BaseDirective;
import rapture.index.IndexHandler;
import rapture.kernel.cache.DocRepoCache;
import rapture.kernel.context.ContextValidator;
import rapture.kernel.pipeline.SearchPublisher;
import rapture.kernel.schemes.RaptureScheme;
import rapture.repo.NVersionedRepo;
import rapture.repo.Repository;
import rapture.repo.VersionedRepo;
import rapture.script.reflex.ReflexHandler;
import rapture.script.reflex.ReflexRaptureScript;
import rapture.table.TableScriptCache;
import reflex.IReflexHandler;
import reflex.ReflexTreeWalker;
import reflex.value.ReflexValue;

public class DocApiImpl extends KernelBase implements DocApi, RaptureScheme {
    private static Logger log = Logger.getLogger(DocApiImpl.class);
    private static final String NAME = "Name"; //$NON-NLS-1$
    private static final String IDGEN_AUTHORITY = "documentRepo";
    private TableScriptCache indexScriptCache = new TableScriptCache();

    public static final String DELETED = DocRepoCache.DELETED;

    public DocApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public void createDocRepo(CallingContext context, String docRepoUri, String config) {
        checkParameter("Repository URI", docRepoUri);
        checkParameter("Config", config); //$NON-NLS-1$

        RaptureURI internalUri = new RaptureURI(docRepoUri, Scheme.DOCUMENT);
        String authority = internalUri.getAuthority();

        if ((authority == null) || authority.isEmpty()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoAuthority")); //$NON-NLS-1$
        }
        if (internalUri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", docRepoUri)); //$NON-NLS-1$
        }
        if (docRepoExists(context, docRepoUri)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("Exists", internalUri.toShortString())); //$NON-NLS-1$
        }

        // save repo config
        RaptureDocConfig dc = new RaptureDocConfig();
        dc.setConfig(config);
        dc.setAuthority(authority);

        DocumentRepoConfig documentRepo = new DocumentRepoConfig();
        documentRepo.setAuthority(authority);
        documentRepo.setDocumentRepo(dc);
        DocumentRepoConfigStorage.add(documentRepo, context.getUser(), apiMessageCatalog.getMessage("CreatedType", new String[] { internalUri.getDocPath(), authority }).format()); //$NON-NLS-1$

        // The repo will be created as needed in the cache when accessed the first time
        log.info("Creating Repository " + docRepoUri + " with config " + config);
    }

    @Override
    public Boolean validateDocRepo(CallingContext context, String docRepoUri) {
        RaptureURI internalUri = new RaptureURI(docRepoUri, Scheme.DOCUMENT);
        if (internalUri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", docRepoUri)); //$NON-NLS-1$
        }
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        return repository.validate();
    }

    // @Override
    public Boolean removeDocumentRepo(CallingContext context, String docRepoUri, Boolean destroy) {
        if (destroy) deleteDocRepo(context, docRepoUri);
        return true;
    }

    @Override
    public void archiveRepoDocs(CallingContext context, String docRepoUri, int versionLimit, long timeLimit, Boolean ensureVersionLimit) {
        RaptureURI internalUri = new RaptureURI(docRepoUri, Scheme.DOCUMENT);
        checkParameter(NAME, internalUri.getAuthority());
        log.info("Archive versions " + internalUri.getFullPath());

        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        } else if (repository instanceof VersionedRepo) {
            archiveVersionedRepo(context, repository, internalUri, versionLimit, timeLimit, ensureVersionLimit);
        } else if (repository instanceof NVersionedRepo) {
            archiveNVersionedRepo(context, repository, internalUri, versionLimit, timeLimit, ensureVersionLimit);
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ArchiveUnsupported", repository.getClass().getSimpleName())); //$NON-NLS-1$
        }
    }

    private void archiveVersionedRepo(CallingContext context, Repository repo, RaptureURI internalUri, int versionLimit, long timeLimit, Boolean ensureVersionLimit) {
        try {
            ((VersionedRepo) repo).archiveRepoVersions(versionLimit, timeLimit, ensureVersionLimit, ContextFactory.getKernelUser().getUser());
        } catch (RaptureException e) {
            if (e.getMessage().contains("no version left")) {
                deleteDocRepo(context, internalUri.getAuthority());
            }
        }
    }

    private boolean archiveNVersionedRepo(CallingContext context, Repository repo, RaptureURI internalUri, int versionLimit, long timeLimit, Boolean ensureVersionLimit) {
        if (internalUri.hasDocPath()) {
            return ((NVersionedRepo) repo).archiveDocumentVersions(internalUri.getDocPath(), versionLimit, timeLimit, ensureVersionLimit, ContextFactory.getKernelUser().getUser());
        } else {
            return ((NVersionedRepo) repo).archiveRepoVersions(internalUri.getAuthority(), versionLimit, timeLimit, ensureVersionLimit, ContextFactory.getKernelUser().getUser());
        }
    }

    @Override
    public void deleteDocRepo(CallingContext context, String docRepoUri) {
        RaptureURI internalUri = new RaptureURI(docRepoUri, Scheme.DOCUMENT);
        if (internalUri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", docRepoUri)); //$NON-NLS-1$
        }
        checkParameter(NAME, internalUri.getAuthority());

        // First drop the data, then drop the repo config

        Repository repository = getRepoFromCache(internalUri.getAuthority());
        log.info(Messages.getString("Admin.DropRepo") + internalUri.getAuthority()); //$NON-NLS-1$
        if (repository != null) {
            repository.drop();
        }

        // Remove jobs associated with this authority.
        for (String jobUri : Kernel.getSchedule().getTrusted().getJobs(context)) {
            RaptureURI uri = new RaptureURI(Kernel.getSchedule().getTrusted().retrieveJob(context, jobUri).getJobURI());
            if (uri.getAuthority().equals(internalUri.getAuthority())) {
                Kernel.getSchedule().getTrusted().deleteJob(context, jobUri);
            }
        }
        // Yeah this is like "delete everything that is prefixed by //docRepoUri
        SearchPublisher.publishDropMessage(context, internalUri.toString());
        // We can't just delete the repo. If we do then
        // DocRepoCache.reloadConfig will just create it.
        // So mark the config as having been deleted.
        // DocumentRepoConfigStorage.deleteByAddress(internalUri,
        // context.getUser(), "Drop document repo");
        DocumentRepoConfig drc = DocumentRepoConfigStorage.readByAddress(internalUri);
        drc.setConfig(DELETED);
        DocumentRepoConfigStorage.add(internalUri, drc, context.getUser(), "Mark config as deleted");
        System.out.println("Config for " + internalUri.toString() + " marked as deleted ");
        removeRepoFromCache(internalUri.getAuthority());
    }

    public void updateDocumentRepo(CallingContext context, DocumentRepoConfig data) {
        DocumentRepoConfigStorage.add(data, context.getUser(), "updated");
        removeRepoFromCache(data.getAuthority());
    }

    @Override
    public Boolean docRepoExists(CallingContext context, String docRepoUri) {
        DocumentRepoConfig documentRepo = getDocRepoConfig(context, docRepoUri);
        return documentRepo != null;
    }

    @Override
    public DocumentRepoConfig getDocRepoConfig(CallingContext context, String docRepoUri) {
        RaptureURI uri = new RaptureURI(docRepoUri, Scheme.DOCUMENT);
        if (uri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", docRepoUri)); //$NON-NLS-1$
        }
        DocumentRepoConfig documentRepo = DocumentRepoConfigStorage.readByAddress(uri);
        if ((documentRepo != null) && StringUtils.equals(DELETED, documentRepo.getConfig())) return null;
        return documentRepo;

    }

    @Override
    public List<DocumentRepoConfig> getDocRepoConfigs(CallingContext context) {
        List<DocumentRepoConfig> configs = DocumentRepoConfigStorage.readAll();
        if ((configs == null) || configs.isEmpty()) return configs;
        int i = configs.size();
        do {
            i--;
            DocumentRepoConfig drc = configs.get(i);
            if (StringUtils.equals(DELETED, drc.getConfig())) configs.remove(i);
        } while (i >= 0);
        return configs;
    }

    @Override
    /**
     * Note: We no longer remove duplicates or modify the docUris list
     */
    public List<Boolean> docsExist(CallingContext context, List<String> docUris) {
        List<Boolean> retVal = new ArrayList<Boolean>(docUris.size());
        for (String uri : docUris) {
            try {
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                requestObj.setDocUri(uri);
                ContextValidator.validateContext(context, EntitlementSet.Doc_docExists, requestObj);

                retVal.add(exists(context, uri));
            } catch (RaptureException e) {
                retVal.add(false);
            }
        }
        return retVal;
    }

    public Boolean exists(CallingContext context, String docUri) {
        // Slow - it gets the whole doc just to verify existence
        return getDoc(context, docUri) != null;
    }

    @Override
    public Map<String, String> getDocs(CallingContext context, List<String> docUris) {
        if (docUris == null) {
            return null;
        } else if (docUris.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> ret = new LinkedHashMap<String, String>();
        Map<String, String> result = getDocsInternal(context, docUris);

        for (String uri : docUris) {
            try {
                // Not so fast. Need to check your entitlement first
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                requestObj.setDocUri(uri);
                ContextValidator.validateContext(context, EntitlementSet.Doc_getDoc, requestObj);

                String toAdd = result.get(uri);
                if (toAdd == null) {
                    ret.put(uri, result.get(new RaptureURI(uri, Scheme.DOCUMENT).toString()));
                } else {
                    ret.put(uri, toAdd);
                }
            } catch (RaptureException e) {
                log.error("Unable to access " + uri, e);
            }
        }
        return ret;
    }

    public Map<String, String> getDocsInternal(CallingContext context, List<String> docUris) {
        Map<String, String> ret = new LinkedHashMap<>();
        ListMultimap<String, RaptureURI> repoToUriMap = ArrayListMultimap.create();

        // Group all the documents by the correct repo
        for (String docUri : docUris) {
            if (StringUtils.isBlank(docUri)) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NullOrEmpty"));
            }
            RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
            repoToUriMap.put(internalUri.getAuthority(), internalUri);
        }

        // Request each set of the documents from the repo at once
        for (final String authority : repoToUriMap.keySet()) {
            Repository repository = getRepoFromCache(authority);
            if (repository != null) {
                List<RaptureURI> uris = repoToUriMap.get(authority);
                List<String> docs = repository.getDocuments(Lists.transform(uris, new Function<RaptureURI, String>() {
                    @Override
                    public String apply(RaptureURI uri) {
                        return uri.getDocPath();
                    }
                }));
                for (int i = 0; i < docs.size(); i++) {
                    ret.put(uris.get(i).toString(), docs.get(i));
                }
            }
        }
        return ret;
    }

    @Override
    public List<DocumentWithMeta> getDocAndMetas(CallingContext context, List<String> docUris) {
        ListMultimap<String, RaptureURI> repoToUriMap = ArrayListMultimap.create();
        List<DocumentWithMeta> documents = new ArrayList<DocumentWithMeta>();

        if (docUris == null) {
            return null;
        } else if (docUris.isEmpty()) {
            return new ArrayList<DocumentWithMeta>();
        }

        for (String docUri : docUris) {
            try {
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                requestObj.setDocUri(docUri);
                ContextValidator.validateContext(context, EntitlementSet.Doc_getDoc, requestObj);

                RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
                repoToUriMap.put(internalUri.getAuthority(), internalUri);
            } catch (RaptureException e) {
                log.error("Unable to access " + docUri, e);
            }
        }

        for (String typePath : repoToUriMap.keySet()) {
            Repository repository = getRepoFromCache(typePath);
            if (repository != null && repository.hasMetaContent()) {
                documents.addAll(repository.getDocAndMetas(repoToUriMap.get(typePath)));
            } else if (repository != null) {
                List<String> contents = repository.getDocuments(Lists.transform(repoToUriMap.get(typePath), new Function<RaptureURI, String>() {
                    @Override
                    public String apply(RaptureURI input) {
                        return input.toString();
                    }
                }));
                for (int i = 0; i < contents.size(); i++) {
                    documents.add(constructDefaultDocumentWithMeta(contents.get(i), repoToUriMap.get(typePath).get(i).getDocPath()));
                }
            } else {
                log.error("repository is null for " + typePath);
            }
        }
        return documents;
    }

    private DocumentWithMeta constructDefaultDocumentWithMeta(String content, String displayName) {
        DocumentWithMeta ret = new DocumentWithMeta();
        ret.setContent(content);
        ret.setDisplayName(displayName);
        ret.setMetaData(new DocumentMetadata());
        ret.getMetaData().setComment("Repo does not support metadata");
        ret.getMetaData().setUser("unknown");
        ret.getMetaData().setVersion(-1);
        long now = System.currentTimeMillis();
        ret.getMetaData().setCreatedTimestamp(now);
        ret.getMetaData().setModifiedTimestamp(now);
        ret.getMetaData().setWriteTime(new Date(now));
        return ret;
    }

    @Override
    public Boolean deleteDoc(CallingContext context, String docUri) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        DocumentRepoConfig type = getConfigFromCache(internalUri.getAuthority());

        boolean ret = repository.removeDocument(internalUri.getDocPath(), context.getUser(), "");
        if (ret) {
            SearchPublisher.publishDeleteMessage(context, type, internalUri);
        }
        return ret;
    }

    @Override
    public String getDoc(CallingContext context, String docUri) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }

        // check if they specified an attribute in the display name e.g.
        // order/1/$link/linkeddoc
        if (internalUri.hasAttribute()) {
            DocumentAttribute att = repository.getDocAttribute(internalUri);
            return (att == null) ? null : att.getValue();
        }

        BaseDirective baseDirective = getDirective(internalUri, repository);
        return repository.getDocument(internalUri.getDocPath(), baseDirective);
    }

    @Override
    public DocumentWithMeta getDocAndMeta(CallingContext context, String docUri) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        if (repository.hasMetaContent()) {
            BaseDirective directive = getDirective(internalUri, repository);
            return repository.getDocAndMeta(internalUri.getDocPath(), directive);
        } else {
            return constructDefaultDocumentWithMeta(getDoc(context, docUri), internalUri.getDocPath());
        }
    }

    @Override
    public DocumentMetadata getDocMeta(CallingContext context, String docUri) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        BaseDirective directive = getDirective(internalUri, repository);
        return repository.getMeta(internalUri.getDocPath(), directive);
    }

    private BaseDirective getDirective(RaptureURI internalUri, Repository repository) {
        BaseDirective directive = null;

        if ((internalUri.hasVersion() || internalUri.hasAsOfTime()) && !repository.isVersioned()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("InvalidVersionURI", internalUri.toString())); //$NON-NLS-1$
        }

        if (internalUri.hasVersion()) {
            AbsoluteVersion av = new AbsoluteVersion();
            av.setVersion(internalUri.getVersion());
            directive = av;
        } else if (internalUri.hasAsOfTime()) {
            AsOfTimeDirective asOfTimeDirective = new AsOfTimeDirective();
            asOfTimeDirective.setAsOfTime(internalUri.getAsOfTime());
            directive = asOfTimeDirective;
        }

        return directive;
    }

    @Override
    public DocumentWithMeta revertDoc(CallingContext context, String docUri) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        return repository.revertDoc(internalUri.getDocPath(), null);
    }

    @Override
    public Map<String, String> getDocRepoStatus(CallingContext context, String docRepoUri) {
        RaptureURI internalUri = new RaptureURI(docRepoUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        return repository.getStatus();
    }

    @Override
    public List<Object> putDocs(CallingContext context, List<String> docUris, List<String> contents) {
        // For now, implement this as a series of putContentPs - this will be a bit faster as we are not worrying about the transport costs of this call.
        if (docUris.size() != contents.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ArgSizeNotEqual"));
        }
        List<Object> retList = new ArrayList<Object>();
        for (int i = 0; i < docUris.size(); i++) {
            try {
                // go back out to wrapper in ordere to verify entitlement
                // See RAP-3715
                retList.add(Kernel.getDoc().putDoc(context, docUris.get(i), contents.get(i)));
            } catch (RaptureException e) {
                retList.add(e);
            }
        }
        return retList;
    }

    @Override
    public String renameDoc(CallingContext context, String fromDocUri, String toDocUri) {

        // Note: we can reuse the payload because the calls are effectively the same; only the entitlement set changed.
        GetDocPayload requestObj = new GetDocPayload();
        requestObj.setContext(context);
        requestObj.setDocUri(fromDocUri);
        ContextValidator.validateContext(context, EntitlementSet.Doc_getDoc, requestObj);
        ContextValidator.validateContext(context, EntitlementSet.Doc_deleteDoc, requestObj);

        requestObj.setDocUri(toDocUri);
        // Requiring that you be able to read the target isn't necessarily a requirement but it might not be a bad idea to warn the user if they don't have
        // EntitlementSet.Doc_getDoc on the target
        ContextValidator.validateContext(context, EntitlementSet.Doc_putDoc, requestObj);

        String content = getDoc(context, fromDocUri);
        String retval = null;
        if (content != null) {
            retval = putDoc(context, toDocUri, content);
            deleteDoc(context, fromDocUri);
        }
        return retval;
    }

    @Override
    public List<String> renameDocs(CallingContext context, String authority, String comment, List<String> docUris, List<String> toDocUris) {
        if (docUris.size() != toDocUris.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("ArgSizeNotEqual"));
        }
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < docUris.size(); i++) {
            try {
                // renameDoc now handles its own entitlements since there are several in play
                ret.add(renameDoc(context, docUris.get(i), toDocUris.get(i)));
            } catch (RaptureException e) {
                log.error("renameDocs failed when renaming " + docUris.get(i) + " to " + toDocUris.get(i) + " with error: " + e.getMessage());
                ret.add(null);
            }
        }
        return ret;
    }

    private DocWriteHandle saveDocument(CallingContext context, RaptureURI internalUri, String content, boolean mustBeNew, DocumentRepoConfig type, int expectedCurrentVersion,
            Map<String, String> extraEventContextMap) {
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        if (type.getStrictCheck()) {
            // Validate that the content is a json document
            try {
                JacksonUtil.getMapFromJson(content);
            } catch (RaptureException e) {
                log.error("Attempt to save non-json content in a strict type - " + e.getMessage());
                throw e;
            }
        }

        DocumentWithMeta newDoc = null;

        if (expectedCurrentVersion == -1) {
            newDoc = repository.addDocument(internalUri.getDocPathWithElement(), content, context.getUser(), "", mustBeNew);
            // note: the repository throws an exception when it fails, so success can be implied by reaching here
        } else {
            newDoc = repository.addDocumentWithVersion(internalUri.getDocPathWithElement(), content, context.getUser(), "", mustBeNew, expectedCurrentVersion);
        }

        DocWriteHandle handle = new DocWriteHandle();
        if (newDoc != null) {
            // TODO: Ben - this should be in the wrapper
            Map<String, String> eventContextMap = new HashMap<>();
            eventContextMap.put(DocEventConstants.VERSION, "" + expectedCurrentVersion);
            if (extraEventContextMap != null) {
                eventContextMap.putAll(extraEventContextMap);
            }
            RunEventHandle eventHandle = Kernel.getEvent().getTrusted().runEventWithContext(context, "//" + internalUri.getAuthority() + "/data/update", internalUri.getDocPath(),
                    eventContextMap);
            handle.setEventHandle(eventHandle);

            for (IndexScriptPair indexScriptPair : type.getIndexes()) {
                runIndex(context, indexScriptPair, internalUri.getAuthority(), internalUri.getDocPath(), content);
            }
            newDoc.setDisplayName(internalUri.getFullPath());
            SearchPublisher.publishCreateMessage(context, type, newDoc);
        }
        handle.setDocumentURI(internalUri.toString());
        handle.setIsSuccess(newDoc != null);
        return handle;
    }

    @Override
    public Boolean putDocWithVersion(CallingContext context, String docUri, String content, int versionNumber) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);

        DocumentRepoConfig type = getConfigFromCache(internalUri.getAuthority());
        return saveDocument(context, internalUri, content, false, type, versionNumber, null).getIsSuccess();
    }

    @Override
    public DocWriteHandle putDocWithEventContext(CallingContext context, String docUri, String content, Map<String, String> extraEventContextMap) {
        boolean mustBeNew = false;

        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        DocumentRepoConfig config = getConfigFromCache(internalUri.getAuthority());

        if (config == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }

        DocWriteHandle handle;
        if (internalUri.hasAttribute()) {
            handle = addDocumentAttributeWithHandle(internalUri, content);
        } else {
            if (internalUri.getElement() != null && internalUri.getElement().equals(UserApiImpl.AUTOID) && config.getIdGenUri() != null && !config.getIdGenUri().isEmpty()) {
                // update display name with idgen
                String newId = Kernel.getIdGen().nextIds(context, config.getIdGenUri(), 1L);
                String oldPath = internalUri.getDocPath();
                String newPath = (oldPath == null || oldPath.isEmpty()) ? newId : oldPath + "/" + newId;
                log.debug("New path is " + newPath);

                // element will be still #id unless we clear it.
                internalUri = RaptureURI.builder(internalUri).docPath(newPath).element(null).build();
                mustBeNew = true;
                // Now we must replace any #UserApiImpl.AUTOID with this newly generated idgen id
                content = content.replace("#" + UserApiImpl.AUTOID, newId);
            }

            Kernel.getStackContainer().pushStack(context, internalUri.toString());

            int versionNumber = -1;
            if (internalUri.hasVersion()) {
                try {
                    versionNumber = Integer.parseInt(internalUri.getVersion());
                } catch (NumberFormatException e) {
                    throw RaptureExceptionFactory.create(HttpStatus.SC_BAD_REQUEST,
                            String.format("Bad version specified in uri. version='%s', uri='%s'", internalUri.getVersion(), internalUri));
                }
            }
            handle = saveDocument(context, internalUri, content, mustBeNew, config, versionNumber, extraEventContextMap);
            Kernel.getStackContainer().popStack(context);
        }

        return handle;
    }

    @Override
    public String putDoc(CallingContext context, String docUri, String content) {
        return putDocWithEventContext(context, docUri, content, null).getDocumentURI();
    }

    public void putDocEphemeral(CallingContext context, String docUri, String content) {
        RaptureURI uri = new RaptureURI(docUri);
        Repository ephemeral = getEphemeralRepo();
        ephemeral.createStage(uri.getAuthority());
        ephemeral.addToStage(uri.getAuthority(), docUri, content, false);
        ephemeral.commitStage(uri.getAuthority(), "admin", "Temporary storage");
    }

    public String getDocEphemeral(CallingContext context, String docUri) {
        // RaptureURI uri = new RaptureURI(docUri);
        Repository ephemeral = getEphemeralRepo();
        return ephemeral.getDocument(docUri);
    }

    @SuppressWarnings("unchecked")
    public void runIndex(CallingContext context, IndexScriptPair indexScriptPair, String authority, String displayName, String content) {
        if (log.isDebugEnabled()) {
            log.debug("Would run index for " + indexScriptPair.getIndex());
            log.debug("And " + indexScriptPair.getScript());
        }
        String contentKey = authority + "/" + indexScriptPair.getScript();
        final RaptureScript script = Kernel.getScript().getScript(context, "//" + authority + "/" + indexScriptPair.getScript());
        IReflexHandler handler = new ReflexHandler(context);
        ReflexTreeWalker walker = indexScriptCache.getReflexScript(contentKey, script.getScript(), handler);
        walker.reset();
        ReflexRaptureScript rs = new ReflexRaptureScript();
        Map<String, Object> extraVals = new HashMap<String, Object>();
        extraVals.put("document", JacksonUtil.getMapFromJson(content));
        extraVals.put("displayName", displayName);
        ReflexValue value = rs.runProgram(context, walker, null, extraVals, script);
        log.debug("Value is " + value.toString());

        IndexHandler indexHandler = Kernel.getIndex().getTrusted().getIndexHandler(RaptureURI.builder(Scheme.INDEX, authority).docPath(indexScriptPair.getIndex()).asString());
        if (value.isMap()) {
            Map<String, Object> entries = value.asMap();
            for (Map.Entry<String, Object> entryVals : entries.entrySet()) {
                Object v = entryVals.getValue();
                if (v instanceof Map) {
                    indexHandler.updateRow(entryVals.getKey(), (Map<String, Object>) v);
                }
            }
        }
    }

    @Override
    public List<String> deleteDocsByUriPrefix(CallingContext context, String uriPrefix) {
        Map<String, RaptureFolderInfo> docs = listDocsByUriPrefix(context, uriPrefix, Integer.MAX_VALUE);
        List<RaptureURI> folders = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        RaptureURI docURI = new RaptureURI(uriPrefix, Scheme.DOCUMENT);

        DeleteDocPayload requestObj = new DeleteDocPayload();
        requestObj.setContext(context);

        folders.add(docURI);
        for (Entry<String, RaptureFolderInfo> entry : docs.entrySet()) {
            String uri = entry.getKey();
            RaptureURI ruri = new RaptureURI(uri);
            boolean isFolder = entry.getValue().isFolder();
            try {
                requestObj.setDocUri(uri);
                if (isFolder) {
                    ContextValidator.validateContext(context, EntitlementSet.Doc_deleteDocsByUriPrefix, requestObj);
                    folders.add(0, ruri);
                } else {
                    ContextValidator.validateContext(context, EntitlementSet.Doc_deleteDoc, requestObj);
                    if (deleteDoc(context, uri)) {
                        removed.add(uri);
                    }
                }
            } catch (RaptureException e) {
                // permission denied
                log.debug("Unable to delete " + uri + " : " + e.getMessage());
            }
        }
        return removed;
    }

    @Override
    public Map<String, RaptureFolderInfo> listDocsByUriPrefix(CallingContext context, String uriPrefix, int depth) {
        RaptureURI internalUri = new RaptureURI(uriPrefix, Scheme.DOCUMENT);
        String authority = internalUri.getAuthority();
        Map<String, RaptureFolderInfo> ret = new HashMap<String, RaptureFolderInfo>();

        // Schema level is special case.
        if (authority.isEmpty()) {
            --depth;
            try {
                List<DocumentRepoConfig> configs = this.getDocRepoConfigs(context);
                for (DocumentRepoConfig config : configs) {
                    authority = config.getAuthority();
                    // NULL or empty string should not exist.
                    if ((authority == null) || authority.isEmpty()) {
                        log.warn("Invalid authority (null or empty string) found for " + JacksonUtil.jsonFromObject(config));
                        continue;
                    }
                    String uri = Scheme.DOCUMENT + "://" + authority;
                    ret.put(uri, new RaptureFolderInfo(authority, true));
                    if (depth != 0) {
                        ret.putAll(listDocsByUriPrefix(context, uri, depth));
                    }
                }

            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission for " + uriPrefix);
            }
            return ret;
        }

        Repository repository = getRepoFromCache(authority);
        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }

        String parentDocPath = internalUri.getDocPath() == null ? "" : internalUri.getDocPath();
        int startDepth = StringUtils.countMatches(parentDocPath, "/");

        if (log.isDebugEnabled()) {
            log.debug("Loading all children from repo " + internalUri.getAuthority() + " with " + internalUri.getDocPath());
        }

        Boolean getAll = (depth <= 0);

        Stack<String> parentsStack = new Stack<String>();
        parentsStack.push(parentDocPath);

        while (!parentsStack.isEmpty()) {
            String currParentDocPath = parentsStack.pop();
            int currDepth = StringUtils.countMatches(currParentDocPath, "/") - startDepth;
            if (!getAll && currDepth >= depth) continue;
            boolean top = currParentDocPath.isEmpty();

            // Make sure that you have permission to read the folder.
            try {
                GetDocPayload requestObj = new GetDocPayload();
                requestObj.setContext(context);
                requestObj.setDocUri(currParentDocPath);
                ContextValidator.validateContext(context, EntitlementSet.Doc_listDocsByUriPrefix, requestObj);
            } catch (RaptureException e) {
                // permission denied
                log.debug("No read permission on folder " + currParentDocPath);
                continue;
            }

            List<RaptureFolderInfo> children = repository.getChildren(currParentDocPath);
            if (((children == null) || children.isEmpty()) && (currDepth == 0) && internalUri.hasDocPath()) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    apiMessageCatalog.getMessage("NoSuchFolder", internalUri.toString())); //$NON-NLS-1$
            else if (children != null) {
                for (RaptureFolderInfo child : children) {
                    String name = child.getName();
                    String childDocPath = currParentDocPath + (top ? "" : "/") + name;
                    if (name.isEmpty()) continue;
                    String uri = RaptureURI.builder(Scheme.DOCUMENT, authority).docPath(childDocPath).asString() + (child.isFolder() ? "/" : "");
                    ret.put(uri, child);
                    if (child.isFolder()) {
                        parentsStack.push(childDocPath);
                    }
                }
            }
            if (top) startDepth--; // special case
        }
        return ret;
    }

    @Override
    public Boolean setDocAttribute(CallingContext context, String attributeUri, String value) {
        RaptureURI docUri = new RaptureURI(attributeUri, Scheme.DOCUMENT);
        return addDocumentAttributeWithHandle(docUri, value).getIsSuccess();
    }

    private DocWriteHandle addDocumentAttributeWithHandle(RaptureURI docUri, String value) {
        Repository repository = getRepoFromCache(docUri.getAuthority());

        if (!docUri.hasAttribute() || docUri.getAttributeKey() == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid arguments supplied");
        }

        DocumentAttribute attribute = DocumentAttributeFactory.create(docUri.getAttributeName());
        attribute.setKey(docUri.toString());
        attribute.setValue(value);

        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", docUri.toAuthString())); //$NON-NLS-1$
        }
        repository.setDocAttribute(docUri, attribute);

        DocWriteHandle handle = new DocWriteHandle();
        handle.setDocumentURI(docUri.toString());
        handle.setIsSuccess(true);
        return handle;
    }

    @Override
    public Map<String, Boolean> setDocAttributes(CallingContext context, String attributeUri, List<String> keys, List<String> values) {
        if (keys.size() != values.size()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    "Cannot bulk add multiple attributes since the size of the list of keys is not equal to the size of the list of values");
        }
        Map<String, Boolean> ret = new HashMap<String, Boolean>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String interimUri = attributeUri + "/" + key;
            try {
                String value = values.get(i);
                // go back out to wrapper in ordere to verify entitlement
                // See RAP-3715
                Boolean success = Kernel.getDoc().setDocAttribute(context, interimUri, value);
                ret.put(key, success);
            } catch (RaptureException e) {
                log.error("setDocAttributes failed with error: " + e.getMessage());
                ret.put(key, false);
            }
        }
        return ret;
    }

    @Override
    public List<XferDocumentAttribute> getDocAttributes(CallingContext context, String attributeUri) {
        RaptureURI internalUri = new RaptureURI(attributeUri, Scheme.DOCUMENT);

        if (!internalUri.hasAttribute()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid arguments supplied");
        }

        // check if a valid attribute type was provided
        DocumentAttributeFactory.create(internalUri.getAttributeName());

        Repository repository = getRepoFromCache(internalUri.getAuthority());

        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        return Lists.transform(repository.getDocAttributes(internalUri), xferFunc);
    }

    @Override
    public Boolean deleteDocAttribute(CallingContext context, String attributeUri) {
        RaptureURI internalUri = new RaptureURI(attributeUri, Scheme.DOCUMENT);

        if (!internalUri.hasAttribute() || internalUri.getAttributeKey() == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid arguments supplied -- need attribute and attribute key to be set");
        }

        Repository repository = getRepoFromCache(internalUri.getAuthority());

        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }
        return repository.deleteDocAttribute(internalUri);
    }

    @Override
    public XferDocumentAttribute getDocAttribute(CallingContext context, String attributeUri) {
        RaptureURI internalUri = new RaptureURI(attributeUri, Scheme.DOCUMENT);

        if (!internalUri.hasAttribute() || internalUri.getAttributeKey() == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid arguments supplied: attribute and attribute key must be set");
        }

        Repository repository = getRepoFromCache(internalUri.getAuthority());

        if (repository == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", internalUri.toAuthString())); //$NON-NLS-1$
        }

        return xferFunc.apply(repository.getDocAttribute(internalUri));
    }

    /**
     * Convert DocumentAttribute to XferDocumentAttribute so that it can be passed over the wire using our HTTP interface
     */
    private Function<DocumentAttribute, XferDocumentAttribute> xferFunc = new Function<DocumentAttribute, XferDocumentAttribute>() {
        @Override
        public XferDocumentAttribute apply(DocumentAttribute in) {
            if (in == null) {
                return null;
            }
            XferDocumentAttribute x = new XferDocumentAttribute();
            x.setAttributeType(in.getAttributeType());
            x.setKey(in.getKey());
            x.setValue(in.getValue());
            return x;
        }
    };

    @Override
    public ContentEnvelope getContent(CallingContext context, RaptureURI raptureUri) {
        ContentEnvelope retVal = new ContentEnvelope();
        String content = getDoc(context, raptureUri.toString());
        if (content != null) {
            retVal.setContent(content);
        }
        return retVal;
    }

    @Deprecated
    @Override
    public void putContent(CallingContext context, RaptureURI raptureUri, Object content, String comment) {
        log.warn("Deprecated method putContent with arguments CallingContext, RaptureURI, Object, String called by " + new Throwable().getStackTrace()[1].getFileName());
        putDoc(context, raptureUri.toString(), content.toString());
    }

    @Override
    public void deleteContent(CallingContext context, RaptureURI raptureUri, String comment) {
        deleteDoc(context, raptureUri.toString());
    }

    @Override
    public DocumentRepoConfig setDocRepoIdGenConfig(CallingContext context, String docRepoUri, String idGenConfig) {
        String idGenUri = getDocRepoIdGenUri(context, docRepoUri);
        Kernel.getIdGen().createIdGen(context, idGenUri, idGenConfig);

        DocumentRepoConfig documentRepo = getDocRepoConfig(context, docRepoUri);
        documentRepo.setIdGenUri(idGenUri);
        updateDocumentRepo(context, documentRepo);
        RaptureURI internalUri = new RaptureURI(docRepoUri, Scheme.DOCUMENT);
        removeRepoFromCache(internalUri.getAuthority());

        return documentRepo;
    }

    /**
     * Return the {@link RaptureURI} that's associated with the idgen that belongs to this doc repo.
     *
     * @param stringUri
     * @return
     */
    @Override
    public String getDocRepoIdGenUri(CallingContext context, String stringUri) {
        RaptureURI documentRepoUri = new RaptureURI(stringUri, Scheme.DOCUMENT);
        return RaptureURI.builder(Scheme.IDGEN, IDGEN_AUTHORITY).docPath(documentRepoUri.getAuthority()).build().toString();
    }

    @Override
    public RaptureIdGenConfig getDocRepoIdGenConfig(CallingContext context, String docRepoUri) {
        String idGenUri = getDocRepoIdGenUri(context, docRepoUri);
        return Kernel.getIdGen().getIdGenConfig(context, idGenUri);
    }

    private Repository getRepoFromCache(String authority) {
        return Kernel.getRepoCacheManager().getDocRepo(authority);
    }

    private DocumentRepoConfig getConfigFromCache(String authority) {
        return Kernel.getRepoCacheManager().getDocConfig(authority);
    }

    private void removeRepoFromCache(String authority) {
        Kernel.getRepoCacheManager().removeRepo(Scheme.DOCUMENT.toString(), authority);
    }

    @Override
    public Boolean docExists(CallingContext context, String docUri) {

        // TODO TECHDEBT This is inefficient. Can we find a better way?

        try {
            return (getDoc(context, docUri) != null);
        } catch (RaptureException e) {
            return false;
        }
    }

    public DocumentMetadata applyTag(CallingContext context, String docUri, String tagUri, String value) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        DocumentWithMeta dwm = repository.addTagToDocument(context.getUser(), internalUri.getDocPath(), tagUri, value);
        // Now apply search update
        DocumentRepoConfig type = getConfigFromCache(internalUri.getAuthority());
        dwm.setDisplayName(internalUri.getFullPath());
        SearchPublisher.publishCreateMessage(context, type, dwm);
        return dwm.getMetaData();
    }

    public DocumentMetadata applyTags(CallingContext context, String docUri, Map<String, String> tagMap) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        DocumentWithMeta dwm = repository.addTagsToDocument(context.getUser(), internalUri.getDocPath(), tagMap);
        // Now apply search update
        DocumentRepoConfig type = getConfigFromCache(internalUri.getAuthority());
        dwm.setDisplayName(internalUri.getFullPath());
        SearchPublisher.publishCreateMessage(context, type, dwm);
        return dwm.getMetaData();
    }

    public DocumentMetadata removeTag(CallingContext context, String docUri, String tagUri) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        DocumentWithMeta dwm = repository.removeTagFromDocument(context.getUser(), internalUri.getDocPath(), tagUri);
        // Now apply search update
        DocumentRepoConfig type = getConfigFromCache(internalUri.getAuthority());
        dwm.setDisplayName(internalUri.getFullPath());
        SearchPublisher.publishCreateMessage(context, type, dwm);
        return dwm.getMetaData();
    }

    public DocumentMetadata removeTags(CallingContext context, String docUri, List<String> tags) {
        RaptureURI internalUri = new RaptureURI(docUri, Scheme.DOCUMENT);
        Repository repository = getRepoFromCache(internalUri.getAuthority());
        DocumentWithMeta dwm = repository.removeTagsFromDocument(context.getUser(), internalUri.getDocPath(), tags);
        // Now apply search update
        DocumentRepoConfig type = getConfigFromCache(internalUri.getAuthority());
        dwm.setDisplayName(internalUri.getFullPath());
        SearchPublisher.publishCreateMessage(context, type, dwm);
        return dwm.getMetaData();
    }

}
