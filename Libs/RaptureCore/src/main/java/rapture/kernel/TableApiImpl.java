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
import rapture.common.RaptureTableConfig;
import rapture.common.RaptureTableConfigStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TableQuery;
import rapture.common.TableRecord;
import rapture.common.api.TableApi;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.index.IndexHandler;
import rapture.repo.RepoVisitor;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class TableApiImpl extends KernelBase implements TableApi {
    private static Logger log = Logger.getLogger(TableApiImpl.class);

    public TableApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

    @Override
    public RaptureTableConfig createTable(CallingContext context, String indexURI, String config) {

        RaptureURI internalURI = new RaptureURI(indexURI, Scheme.TABLE);
        RaptureTableConfig newP = new RaptureTableConfig();
        newP.setName(internalURI.getDocPath());
        newP.setAuthority(internalURI.getAuthority());
        newP.setConfig(config);
        RaptureTableConfigStorage.add(newP, context.getUser(), "Created index config");
        return newP;
    }

    @Override
    public Boolean deleteTable(CallingContext context, String indexURI) {
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Not yet implemented");
    }

    @Override
    public RaptureTableConfig getTable(CallingContext context, String indexURI) {

        RaptureURI internalURI = new RaptureURI(indexURI, Scheme.TABLE);
        return RaptureTableConfigStorage.readByAddress(internalURI);
    }

    @Override
    public List<RaptureTableConfig> getTablesForAuthority(CallingContext context, final String authority) {

        final RaptureURI internalURI = new RaptureURI(authority, Scheme.TABLE);

        final List<RaptureTableConfig> ret = new ArrayList<RaptureTableConfig>();

        RaptureTableConfigStorage.visitAll(new RepoVisitor() {

            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    log.info("Visiting " + name);
                    RaptureTableConfig index;
                    try {
                        index = RaptureTableConfigStorage.readFromJson(content);
                        if (index.getAuthority().equals(internalURI.getAuthority())) {
                            ret.add(index);
                        }
                    } catch (RaptureException e) {
                        log.error("Could not load document " + name + ", continuing anyway");
                    }
                }
                return true;
            }

        });
        return ret;
    }

    /**
     * TODO: is this needed? look into whether we can use the one inside
     * {@link Kernel}
     */
    private IndexCache indexCache = new IndexCache();

    public IndexHandler getIndexHandler(String indexURI) {
        return indexCache.getIndex(indexURI);
    }

    /**
     * Given an index, query for data in it...
     */
    @Override
    public List<TableRecord> queryTable(CallingContext context, String indexURI, TableQuery query) {
        IndexHandler indexHandler = getIndexHandler(indexURI);
        return indexHandler.queryTable(query);
    }

}
