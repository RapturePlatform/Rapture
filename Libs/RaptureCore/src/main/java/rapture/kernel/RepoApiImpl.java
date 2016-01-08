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
import rapture.common.ContentEnvelope;
import rapture.common.RaptureJob;
import rapture.common.RaptureJobPathBuilder;
import rapture.common.RaptureScript;
import rapture.common.RaptureScriptPathBuilder;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.RepoApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.schemes.RaptureScheme;
import rapture.kernel.schemes.SchemeProxyImpl;
import rapture.kernel.schemes.SeriesSchemeImpl;
import rapture.kernel.schemes.StructuredSchemeImpl;

import java.util.HashMap;
import java.util.Map;

public class RepoApiImpl extends KernelBase implements RepoApi {

    private Map<Scheme, RaptureScheme> schemeHandlerMap = new HashMap<Scheme, RaptureScheme>();

    @Deprecated
    public RepoApiImpl(Kernel kernel) {
        super(kernel);

        schemeHandlerMap.put(Scheme.DOCUMENT, new DocApiImpl(kernel));
        schemeHandlerMap.put(Scheme.BLOB, new BlobApiImpl(kernel));
        schemeHandlerMap.put(Scheme.SERIES, new SeriesSchemeImpl());
        schemeHandlerMap.put(Scheme.STRUCTURED, new StructuredSchemeImpl());

        // schemeHandlerMap.put(RaptureURI.Scheme.EVENT, new
        // EventApiImpl(kernel));
        // schemeHandlerMap.put(RaptureURI.Scheme.FIELD, new
        // FieldsApiImpl(kernel));

        String jobURI = RaptureURI.builder(Scheme.DOCUMENT, RaptureJobPathBuilder.getRepoName()).docPath(RaptureJobPathBuilder.getPrefix()).build().toString();
        schemeHandlerMap.put(Scheme.JOB, new SchemeProxyImpl(this, jobURI, RaptureJob.class));
        String scriptURI = RaptureURI.builder(Scheme.DOCUMENT, RaptureScriptPathBuilder.getRepoName()).docPath(RaptureScriptPathBuilder.getPrefix()).build()
                .toString();
        schemeHandlerMap.put(Scheme.SCRIPT, new SchemeProxyImpl(this, scriptURI, RaptureScript.class));
    }

    @Override
    @Deprecated
    public ContentEnvelope getContent(CallingContext context, String documentURIString) {
        RaptureURI raptureURI = new RaptureURI(documentURIString, Scheme.DOCUMENT);
        Scheme scheme = raptureURI.getScheme();
        ContentEnvelope contentEnvelope = schemeHandlerMap.get(scheme).getContent(context, raptureURI);
        if (contentEnvelope != null && contentEnvelope.getContent() != null) {
            contentEnvelope.setContentType(contentEnvelope.getContent().getClass().getSimpleName());
            contentEnvelope.setRaptureURI(documentURIString);
        }
        return contentEnvelope;
    }

    @Override
    @Deprecated
    public void putContent(CallingContext context, String documentURIString, Object content, String comment) {
        RaptureURI raptureURI = new RaptureURI(documentURIString, Scheme.DOCUMENT);
        if (raptureURI.getScheme().isPrimitive() && !raptureURI.hasDocPath()) {
            schemeHandlerMap.get(raptureURI.getScheme()).putContent(context, raptureURI, JacksonUtil.jsonFromObject(content), comment);
        } else {
            schemeHandlerMap.get(raptureURI.getScheme()).putContent(context, raptureURI, content, comment);
        }
    }

    @Override
    @Deprecated
    public void deleteContent(CallingContext context, String raptureURIString, String comment) {
        RaptureURI raptureURI = new RaptureURI(raptureURIString, Scheme.DOCUMENT);
        schemeHandlerMap.get(raptureURI.getScheme()).deleteContent(context, raptureURI, comment);
    }
}
