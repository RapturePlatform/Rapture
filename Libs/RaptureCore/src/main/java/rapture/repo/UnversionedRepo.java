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
package rapture.repo;

import java.util.Map;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.DocumentWithMeta;
import rapture.dsl.dparse.VersionDirective;
import rapture.lock.ILockingHandler;
import rapture.repo.meta.AbstractMetaBasedRepo;
import rapture.repo.meta.handler.UnversionedMetaHandler;

/**
 * An unversioned repo does not have any versions - each document may
 * overwrite others.
 *
 * @author amkimian
 */
public class UnversionedRepo extends AbstractMetaBasedRepo<UnversionedMetaHandler> {

    public UnversionedRepo(Map<String, String> config, KeyStore store, KeyStore metaKeyStore, KeyStore attributeKeyStore, ILockingHandler locker) {
        super(store, locker);
        metaHandler = new UnversionedMetaHandler(store, metaKeyStore, attributeKeyStore);
    }

    @Override public boolean isVersioned() {
        return false;
    }

    @Override public DocumentWithMeta addDocumentWithVersion(String disp, String content, String user, String comment, boolean mustBeNew, int expectedVersion) {
        throw RaptureExceptionFactory.create("Not supported!");
    }

    @Override protected DocumentWithMeta getVersionedDocumentWithMeta(String docPath, VersionDirective directive) {
        throw RaptureExceptionFactory.create("Not supported!");
    }
}
