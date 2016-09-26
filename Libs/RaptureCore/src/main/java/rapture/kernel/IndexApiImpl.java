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

import static rapture.common.Scheme.DOCUMENT;

import java.util.List;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TableConfig;
import rapture.common.TableConfigStorage;
import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.api.IndexApi;
import rapture.common.model.IndexConfig;
import rapture.common.model.IndexConfigStorage;
import rapture.index.IndexHandler;
import rapture.repo.Repository;

public class IndexApiImpl extends KernelBase implements IndexApi {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(IndexApiImpl.class);

    public IndexApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public IndexConfig createIndex(CallingContext context, String indexUri, String config) {

        RaptureURI internalUri = new RaptureURI(indexUri, Scheme.INDEX);
        if (internalUri.hasDocPath()) log.warn(indexUri + " has unexpected docPath " + internalUri.getDocPath() + " - ignored");

        IndexConfig newP = new IndexConfig();
        newP.setName(internalUri.getAuthority());
        newP.setConfig(config);
        IndexConfigStorage.add(newP, context.getUser(), "Created index config");
        return newP;
    }

    @Override
    public void deleteIndex(CallingContext context, String indexUri) {
        RaptureURI internalUri = new RaptureURI(indexUri, Scheme.INDEX);
        IndexConfigStorage.deleteByAddress(internalUri, context.getUser(), "Delete index");
    }

    @Override
    public IndexConfig getIndex(CallingContext context, String indexUri) {
        RaptureURI internalUri = new RaptureURI(indexUri, Scheme.INDEX);
        return IndexConfigStorage.readByAddress(internalUri);
    }

    @Override
    public TableQueryResult findIndex(CallingContext context, String indexUri, String query) {
        // TODO Pass this straight down to the appropriate document repo defined
        // by the indexUri (which should match a document repo)
        RaptureURI internalUri = new RaptureURI(indexUri, DOCUMENT);
        Repository repository = getKernel().getRepo(internalUri.getAuthority());
        return repository.findIndex(query);
    }
    
    @Override
    public TableConfig createTable(CallingContext context, String indexURI, String config) {

        RaptureURI internalURI = new RaptureURI(indexURI, Scheme.TABLE);
        TableConfig newP = new TableConfig();
        newP.setName(internalURI.getDocPath());
        newP.setAuthority(internalURI.getAuthority());
        newP.setConfig(config);
        TableConfigStorage.add(newP, context.getUser(), "Created index config");
        return newP;
    }

    @Override
    public void deleteTable(CallingContext context, String indexURI) {
        TableConfigStorage.deleteByAddress(new RaptureURI(indexURI), context.getUser(), "Remove table");
    }

    @Override
    public List<TableRecord> queryTable(CallingContext context, String indexURI, TableQuery query) {
        IndexHandler indexHandler = getIndexHandler(indexURI);
        return indexHandler.queryTable(query);
    }

    
    @Override
    public TableConfig getTable(CallingContext context, String indexURI) {

        RaptureURI internalURI = new RaptureURI(indexURI, Scheme.TABLE);
        return TableConfigStorage.readByAddress(internalURI);
    }
    
    private IndexCache indexCache = new IndexCache();

    public IndexHandler getIndexHandler(String indexURI) {
    	        return indexCache.getIndex(indexURI);
    }
}
