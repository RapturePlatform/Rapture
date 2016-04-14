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

import com.google.common.base.Preconditions;
import org.apache.log4j.Logger;
import rapture.common.*;
import rapture.common.api.JarApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.config.ConfigLoader;
import rapture.repo.BlobRepo;

import static rapture.common.Scheme.BLOB;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zanniealvarez on 9/21/15.
 */
public class JarApiImpl extends KernelBase implements JarApi {
    private static Logger logger = Logger.getLogger(JarApiImpl.class);

    final static String JAR_REPO_URI = "blob://" + RaptureConstants.JAR_REPO + RaptureURI.Parser.SEPARATOR_CHAR;
    final static String CONTENT_TYPE = "application/java-archive";
    final static String ENABLED_HEADER = "Jar-Enabled";

    private BlobApiImpl blobApi;

    public JarApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        blobApi = Kernel.getBlob().getTrusted();
    }

    @Override
    public Boolean jarExists(CallingContext context, String jarUri) {
        return blobApi.blobExists(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri));
    }

    @Override
    public void putJar(CallingContext context, String jarUri, byte[] jarContent) {
        try {
            blobApi.putBlob(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri), jarContent, CONTENT_TYPE);
            disableJar(ContextFactory.getKernelUser(), jarUri);
        }
        catch (RaptureException e) {
            if (!isNoSuchRepoException(e))
                throw e;

            createJarRepo();
            blobApi.putBlob(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri), jarContent, CONTENT_TYPE);
            disableJar(ContextFactory.getKernelUser(), jarUri);
        }
    }

    @Override
    public BlobContainer getJar(CallingContext context, String jarUri) {
        try {
            return blobApi.getBlob(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri));
        }
        catch (RaptureException e) {
            if (!isNoSuchRepoException(e))
                throw e;

            return null;
        }
    }

    @Override
    public void deleteJar(CallingContext context, String jarUri) {
        BlobRepo blobRepo = Kernel.getRepoCacheManager().getBlobRepo(RaptureConstants.JAR_REPO);
        if (blobRepo == null) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoSuchRepo", "Jar")); //$NON-NLS-1$

        blobApi.deleteBlob(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri));
    }

    @Override
    public Long getJarSize(CallingContext context, String jarUri) {
        try {
            return blobApi.getBlobSize(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri));
        }
        catch (RaptureException e) {
            if (!isNoSuchRepoException(e))
                throw e;

            return null;
        }
    }

    @Override
    public Map<String, String> getJarMetaData(CallingContext context, String jarUri) {
        try {
            return blobApi.getBlobMetaData(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri));
        }
        catch (RaptureException e) {
            if (!isNoSuchRepoException(e))
                throw e;

            return null;
        }
    }

    @Override
    public Map<String, RaptureFolderInfo> listJarsByUriPrefix(CallingContext context, String uriPrefix, int depth) {
        try {
            Map<String, RaptureFolderInfo> blobsByPrefix = blobApi.listBlobsByUriPrefix(ContextFactory.getKernelUser(), getBlobUriFromJarUri(uriPrefix), depth);

            Map<String, RaptureFolderInfo> jarsByPrefix = new HashMap<>();
            for (Map.Entry<String, RaptureFolderInfo> entry : blobsByPrefix.entrySet()) {
                jarsByPrefix.put(getJarUriFromBlobUri(entry.getKey()), entry.getValue());
            }

            return jarsByPrefix;
        }
        catch (RaptureException e) {
            if (!isNoSuchRepoException(e))
                throw e;

            return null;
        }
    }

    @Override
    public Boolean jarIsEnabled(CallingContext context, String jarUri) {
        Map<String, String> meta = blobApi.getBlobMetaData(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri));
        String enabled = meta.get(ENABLED_HEADER);
        return Boolean.valueOf(enabled);
    }

    @Override
    public void enableJar(CallingContext context, String jarUri) {
        setIsEnabled(context, jarUri, true);
    }

    @Override
    public void disableJar(CallingContext context, String jarUri) {
        setIsEnabled(context, jarUri, false);
    }

    private void setIsEnabled(CallingContext context, String jarUri, Boolean enabled) {
        RaptureURI interimUri = new RaptureURI(getBlobUriFromJarUri(jarUri), Scheme.BLOB);
        Preconditions.checkNotNull(interimUri);

        Map<String, String> meta = blobApi.getBlobMetaData(ContextFactory.getKernelUser(), getBlobUriFromJarUri(jarUri));
        meta.put(ENABLED_HEADER, "" + enabled);
        blobApi.putMeta(meta, interimUri, ContextFactory.getKernelUser());
    }

    private void createJarRepo() {
        if (blobApi.blobRepoExists(ContextFactory.getKernelUser(), JAR_REPO_URI)) {
            logger.info("Attempted to create JAR repo when it already exists. Skipping.");
            return;
        }

        logger.info("JAR repo does not exist, creating");

        String configBase = " {} USING " + ConfigLoader.getConf().JarStorage +
                " { prefix=\"" + RaptureConstants.JAR_REPO + "\"}";

        String config = "BLOB" + configBase;
        String metaConfig = "REP" + configBase;

        blobApi.createBlobRepo(ContextFactory.getKernelUser(), JAR_REPO_URI, config, metaConfig);
    }

    private Boolean isNoSuchRepoException(RaptureException e) {
        return e.getMessage().equals(
                apiMessageCatalog.getMessage("NoSuchRepo", "blob://" + RaptureConstants.JAR_REPO).toString());
    }

    String getBlobUriFromJarUri(String jarUri) {
        RaptureURI raptureURI = new RaptureURI(jarUri);
        return JAR_REPO_URI + raptureURI.getShortPath();
    }

    String getJarUriFromBlobUri(String blobUri) {
        return blobUri.replace(JAR_REPO_URI, "jar://");
    }
}
