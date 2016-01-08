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
package <package>;

import java.util.List;
import <typePackage>.<name>;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.object.storage.CrudStorableInfo;
import rapture.object.storage.ObjectStorage;
import rapture.persistence.storable.mapper.impl.jackson.StorableObjectMapper;
import rapture.repo.Repository;
import rapture.kernel.Kernel;
import org.apache.log4j.Logger;
import rapture.common.RaptureFolderInfo;
import static rapture.common.Scheme.DOCUMENT;

public class <name>ApiImpl extends <name>ApiBase {
    private static Logger log = Logger.getLogger(<name>ApiImpl.class);
    
    @Override
    public <name> get<name>(CallingContext context, String uri) {
        log.info("Attempting to retrieve crud document at " + uri);
        RaptureURI theRealUri = new RaptureURI(uri);
        log.info("Crud document storage at " + theRealUri.getDocPath());
        return ObjectStorage.read(theRealUri, <name>.class, new CrudStorableInfo(), StorableObjectMapper.getMapper());
    }

    @Override
    public Boolean delete<name>(CallingContext context, String uri) {
        ObjectStorage.delete(context.getUser(), new RaptureURI(uri), null);
        return true;
    }

    @Override
    public <name> put<name>(CallingContext context, <name> content) {
        RaptureURI storageLocation = content.getStorageLocation();
        log.info("Crud document storage at " + storageLocation.getDocPath());
        ObjectStorage.write(content, context.getUser(), new CrudStorableInfo(), "Adding", StorableObjectMapper.getMapper());
        return content;
    }

    @Override
    public List\<RaptureFolderInfo\> getChildren(CallingContext context, String uri) {
        RaptureURI internalURI = new RaptureURI(uri, DOCUMENT);
        Repository repo = Kernel.INSTANCE.getRepo(internalURI.getAuthority());
        String area = internalURI.getDocPath() == null ? "" : internalURI.getDocPath();
        log.info("Loading children from repo " + internalURI.getAuthority() + " with area: " + area);
        return repo.getChildren(area);
    }
}
