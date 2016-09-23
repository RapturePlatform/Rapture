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

import java.util.HashMap;
import java.util.Map;

import rapture.common.BlobContainer;
import rapture.common.CallingContext;
import rapture.common.RaptureConstants;
import rapture.common.RaptureFolderInfo;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.BlobApi;
import rapture.common.api.JarApi;
import rapture.common.mime.MimeJarCacheUpdate;

/**
 * Created by zanniealvarez on 9/21/15.
 */
public class JarApiImpl extends KernelBase implements JarApi {

    private final static String JAR_REPO_URI = "blob://" + RaptureConstants.JAR_REPO + RaptureURI.Parser.SEPARATOR_CHAR;
    public final static String CONTENT_TYPE = "application/java-archive";

    private BlobApi blobApi;

    public JarApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        blobApi = Kernel.getBlob();
    }

    @Override
    public Boolean jarExists(CallingContext context, String jarUri) {
        return blobApi.blobExists(context, getBlobUriFromJarUri(jarUri));
    }

    @Override
    public void putJar(CallingContext context, String jarUri, byte[] jarContent) {
        String blobUri = getBlobUriFromJarUri(jarUri);
        blobApi.putBlob(context, blobUri, jarContent, CONTENT_TYPE);
        sendCacheUpdateMessage(context, jarUri, false);
    }

    @Override
    public BlobContainer getJar(CallingContext context, String jarUri) {
        return blobApi.getBlob(context, getBlobUriFromJarUri(jarUri));
    }

    @Override
    public void deleteJar(CallingContext context, String jarUri) {
        blobApi.deleteBlob(context, getBlobUriFromJarUri(jarUri));
        sendCacheUpdateMessage(context, jarUri, true);

    }

    @Override
    public Long getJarSize(CallingContext context, String jarUri) {
        return blobApi.getBlobSize(context, getBlobUriFromJarUri(jarUri));
    }

    @Override
    public Map<String, String> getJarMetaData(CallingContext context, String jarUri) {
        return blobApi.getBlobMetaData(context, getBlobUriFromJarUri(jarUri));
    }

    @Override
    public Map<String, RaptureFolderInfo> listJarsByUriPrefix(CallingContext context, String uriPrefix, int depth) {
        Map<String, RaptureFolderInfo> blobsByPrefix = blobApi.listBlobsByUriPrefix(context, getBlobUriFromJarUri(uriPrefix), depth);
        Map<String, RaptureFolderInfo> jarsByPrefix = new HashMap<>();
        for (Map.Entry<String, RaptureFolderInfo> entry : blobsByPrefix.entrySet()) {
            jarsByPrefix.put(getJarUriFromBlobUri(entry.getKey()), entry.getValue());
        }
        return jarsByPrefix;
    }

    String getBlobUriFromJarUri(String jarUri) {
        return JAR_REPO_URI + new RaptureURI(jarUri, Scheme.JAR).getShortPath();
    }

    String getJarUriFromBlobUri(String blobUri) {
        return blobUri.replace(JAR_REPO_URI, "jar://");
    }

    /**
     * Tell all Rapture Kernels to update their JarCache instance
     * 
     * @param ctx
     * @param jarUri
     *            - the uri of the jar that has been updated
     * @param isDeletion
     *            - true, if this is for removal of a jarUri
     */
    private void sendCacheUpdateMessage(CallingContext ctx, String jarUri, boolean isDeletion) {
        RapturePipelineTask task = new RapturePipelineTask();
        MimeJarCacheUpdate mime = new MimeJarCacheUpdate();
        mime.setJarUri(new RaptureURI(jarUri, Scheme.JAR));
        mime.setDeletion(isDeletion);
        task.setContentType(MimeJarCacheUpdate.getMimeType());
        task.addMimeObject(mime);
        Kernel.getPipeline().broadcastMessageToAll(ctx, task);
    }
}
