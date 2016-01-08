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
package rapture.kernel.schemes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import rapture.common.CallingContext;
import rapture.common.ContentEnvelope;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.kernel.RepoApiImpl;

/**
 * @deprecated TODO(RAP-942) This class should be combined into the general CRUD class.
 */
@Deprecated
public class SchemeProxyImpl implements RaptureScheme {

    private String baseURI;
    private RepoApiImpl repoImpl;
    private Class<?> clazz;

    public SchemeProxyImpl(RepoApiImpl repo, String baseURI, Class<?> clazz) {
        this.repoImpl = repo;
        this.baseURI = baseURI;
        this.clazz = clazz;
    }

    @Override
    public ContentEnvelope getContent(CallingContext context, RaptureURI raptureURI) {
        String targetURI = baseURI + raptureURI.getFullPath();
        RaptureURI raptureTargetURI = new RaptureURI(targetURI, Scheme.DOCUMENT);
        ContentEnvelope ce = repoImpl.getContent(context, targetURI);
        if (ce.getContent() != null) {

            //TODO: Ben - DRY this out. Plus, is this disgusting?
            if (raptureTargetURI.getElement() != null) {
                Object objectFromJson = JacksonUtil.objectFromJson((String) ce.getContent(), clazz);
                String methodName = "get" + raptureTargetURI.getElement();
                try {
                    Method method = objectFromJson.getClass().getMethod(methodName);
                    ce.setContent(method.invoke(objectFromJson));
                } catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                ce.setContent(JacksonUtil.objectFromJson((String) ce.getContent(), clazz));
            }

        }
        return ce;
    }

    @Override
    public void putContent(CallingContext context, RaptureURI raptureURI, Object content, String comment) {
        String targetURI = baseURI + raptureURI.getFullPath();
        String stringContent = JacksonUtil.jsonFromObject(content);
        repoImpl.putContent(context, targetURI, stringContent, comment);
    }

    @Override
    public void deleteContent(CallingContext context, RaptureURI raptureURI, String comment) {
        String targetURI = baseURI + raptureURI.getFullPath();
        repoImpl.deleteContent(context, targetURI, comment);
    }

}
