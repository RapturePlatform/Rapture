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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import rapture.common.CallingContext;
import rapture.common.ForeignKey;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.StoredProcedureParams;
import rapture.common.StoredProcedureResponse;
import rapture.common.StructuredRepoConfig;
import rapture.common.StructuredRepoConfigStorage;
import rapture.common.TableIndex;
import rapture.common.api.StructuredApi;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.repo.StructuredRepo;
import rapture.structured.DefaultValidator;
import rapture.structured.Validator;

public class StructuredApiImpl extends KernelBase implements StructuredApi {

    private Validator validator;

    public StructuredApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        validator = new DefaultValidator();
    }

    @Override
    public void createStructuredRepo(CallingContext context, String repoURI, String config) {
        checkParameter("URI", repoURI);
        checkParameter("Config", config);

        RaptureURI internalURI = new RaptureURI(repoURI, Scheme.STRUCTURED);
        String authority = internalURI.getAuthority();
        if ((authority == null) || authority.isEmpty()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoAuthority")); //$NON-NLS-1$
        }
        if (internalURI.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", repoURI)); //$NON-NLS-1$
        }

        // TODO write a config validator

        if (structuredRepoExists(context, repoURI)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("Exists", internalURI.toShortString())); //$NON-NLS-1$
        }

        StructuredRepoConfig structured = new StructuredRepoConfig();
        structured.setAuthority(authority);
        structured.setConfig(config);
        StructuredRepoConfigStorage.add(structured, context.getUser(), "Create Structured repository");
    }

    @Override
    public void deleteStructuredRepo(CallingContext context, String repoURI) {
        RaptureURI uri = new RaptureURI(repoURI, Scheme.STRUCTURED);
        if (uri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", repoURI)); //$NON-NLS-1$
        }
        getRepoOrFail(uri.getAuthority()).drop();
        removeRepoFromCache(uri.getAuthority());
        StructuredRepoConfigStorage.deleteByAddress(uri, context.getUser(), "Delete structured repo");
    }

    @Override
    public Boolean structuredRepoExists(CallingContext context, String repoURI) {
        RaptureURI uri = new RaptureURI(repoURI, Scheme.STRUCTURED);
        if (uri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", repoURI)); //$NON-NLS-1$
        }
        return getRepoFromCache(uri.getAuthority()) != null;
    }

    @Override
    public StructuredRepoConfig getStructuredRepoConfig(CallingContext ctx, String uriStr) {
        RaptureURI uri = new RaptureURI(uriStr, Scheme.STRUCTURED);
        if (uri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    "Structured repository URI " + uriStr + " can't have a doc path: '" + uri.getDocPath() + "'");
        }
        return StructuredRepoConfigStorage.readByAddress(uri);
    }

    @Override
    public List<StructuredRepoConfig> getStructuredRepoConfigs(CallingContext context) {
        return StructuredRepoConfigStorage.readAll();
    }

    private StructuredRepo getRepoOrFail(String authority) {
        StructuredRepo repo = getRepoFromCache(authority);
        if (repo == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NoSuchRepo", Scheme.STRUCTURED+"://"+authority)); //$NON-NLS-1$
        } else {
            return repo;
        }
    }

    private StructuredRepo getRepoFromCache(String authority) {
        return Kernel.getRepoCacheManager().getStructuredRepo(authority);
    }

    private void removeRepoFromCache(String authority) {
        Kernel.getRepoCacheManager().removeRepo(Scheme.STRUCTURED.toString(), authority);
    }

    @Override
    public void createTableUsingSql(CallingContext context, String schema, String rawSql) {
        StructuredRepo repo = getRepoOrFail(schema);
        registerWithTxManager(context, repo);
        try {
            repo.createTableUsingSql(context, rawSql);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void createTable(CallingContext context, String tableUri, Map<String, String> columns) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        // We do not allow tables with dots in them. Standard SQL does not allow this and we won't either.
        // The complexity involved to go back and forth and text-parse is not worth it.
        validateTable(uri.getDocPath());
        validateColumns(columns.keySet());
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.createTable(uri.getDocPath(), columns);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void dropTable(CallingContext context, String tableUri) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.dropTable(uri.getDocPath());
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public Boolean tableExists(CallingContext context, String tableUri) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        try {
            return getRepoOrFail(uri.getAuthority()).tableExists(uri.getDocPath());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getSchemas(CallingContext context) {
        List<StructuredRepoConfig> configs = getStructuredRepoConfigs(context);
        return Lists.transform(configs, new Function<StructuredRepoConfig, String>() {
            @Nullable
            @Override
            public String apply(StructuredRepoConfig structuredRepoConfig) {
                return structuredRepoConfig.getAuthority();
            }
        });
    }

    @Override
    public List<String> getTables(CallingContext context, String repoURI) {
        RaptureURI uri = new RaptureURI(repoURI, Scheme.STRUCTURED);
        if (uri.hasDocPath()) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, apiMessageCatalog.getMessage("NoDocPath", repoURI)); //$NON-NLS-1$
        }
        return getRepoOrFail(uri.getAuthority()).getTables();
    }

    @Override
    public Map<String, String> describeTable(CallingContext context, String tableUri) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        return getRepoOrFail(uri.getAuthority()).describeTable(uri.getDocPath()).getRows();
    }

    @Override
    public void addTableColumns(CallingContext context, String tableUri, Map<String, String> columns) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        validateColumns(columns.keySet());
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.addTableColumns(uri.getDocPath(), columns);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteTableColumns(CallingContext context, String tableUri, List<String> columnNames) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.deleteTableColumns(uri.getDocPath(), columnNames);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void updateTableColumns(CallingContext context, String tableUri, Map<String, String> columns) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        validateColumns(columns.keySet());
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.updateTableColumns(uri.getDocPath(), columns);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void renameTableColumns(CallingContext context, String tableUri, Map<String, String> columnNames) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.renameTableColumns(uri.getDocPath(), columnNames);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void createIndex(CallingContext context, String tableUri, String indexName, List<String> columnNames) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.createIndex(uri.getDocPath(), indexName, columnNames);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void dropIndex(CallingContext context, String tableUri, String indexName) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.dropIndex(indexName);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<TableIndex> getIndexes(CallingContext context, String tableUri) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        return repo.getIndexes(uri.getDocPath());
    }

    @Override
    public String getPrimaryKey(CallingContext context, String tableUri) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        return repo.getPrimaryKey(uri.getDocPath());
    }

    @Override
    public List<ForeignKey> getForeignKeys(CallingContext context, String tableUri) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        return repo.getForeignKeys(uri.getDocPath());
    }

    @Override
    public List<Map<String, Object>> selectJoinedRows(CallingContext context, List<String> tableUris, List<String> columnNames, String from,
            String where, List<String> order, Boolean ascending, int limit) {
        Pair<String, List<String>> pair = getAuthorityAndDocPaths(tableUris);
        return getRepoOrFail(pair.getLeft()).selectJoinedRows(pair.getRight(), columnNames, from, where, order, ascending, limit);
    }

    @Override
    public List<Map<String, Object>> selectUsingSql(CallingContext context, String schema, String rawSql) {
        return getRepoOrFail(schema).selectUsingSql(context, rawSql);
    }

    @Override
    public List<Map<String, Object>> selectRows(CallingContext context, String tableUri, List<String> columnNames, String where, List<String> order,
            Boolean ascending, int limit) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        return getRepoOrFail(uri.getAuthority()).selectRows(uri.getDocPath(), columnNames, where, order, ascending, limit);
    }

    @Override
    public void insertUsingSql(CallingContext context, String schema, String rawSql) {
        StructuredRepo repo = getRepoOrFail(schema);
        registerWithTxManager(context, repo);
        try {
            repo.insertUsingSql(context, rawSql);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void insertRow(CallingContext context, String tableUri, Map<String, Object> values) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.insertRow(uri.getDocPath(), values);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void insertRows(CallingContext context, String tableUri, List<Map<String, Object>> values) {
        // TODO: validate input, each map has same number of keys
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.insertRows(uri.getDocPath(), values);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void updateUsingSql(CallingContext context, String schema, String rawSql) {
        StructuredRepo repo = getRepoOrFail(schema);
        registerWithTxManager(context, repo);
        try {
            repo.updateUsingSql(context, rawSql);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteUsingSql(CallingContext context, String schema, String rawSql) {
        StructuredRepo repo = getRepoOrFail(schema);
        registerWithTxManager(context, repo);
        try {
            repo.deleteUsingSql(context, rawSql);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void updateRows(CallingContext context, String tableUri, Map<String, Object> values, String where) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.updateRows(uri.getDocPath(), values, where);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteRows(CallingContext context, String tableUri, String where) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.deleteRows(uri.getDocPath(), where);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    private void registerWithTxManager(CallingContext context, StructuredRepo repo) {
        String txId = getTxId(context);
        if (TransactionManager.isTransactionFailed(txId)) {
            throw RaptureExceptionFactory.create("Transaction " + txId + " already failed, abort now");
        }
        if (TransactionManager.isTransactionActive(txId)) {
            TransactionManager.registerThread(txId);
            TransactionManager.registerRepo(txId, repo);
        }
    }

    @Override
    public Boolean begin(CallingContext context) {
        return TransactionManager.begin(getTxId(context));
    }

    @Override
    public Boolean commit(CallingContext context) {
        return TransactionManager.commit(getTxId(context));
    }

    @Override
    public Boolean rollback(CallingContext context) {
        return TransactionManager.rollback(getTxId(context));
    }

    @Override
    public Boolean abort(CallingContext context, String transactionId) {
        return TransactionManager.rollback(transactionId);
    }

    @Override
    public List<String> getTransactions(CallingContext context) {
        return ImmutableList.copyOf(TransactionManager.getTransactions());
    }

    private String getTxId(CallingContext context) {
        return context.getContext();
    }

    private void validateTable(String docPath) {
        if (!validator.isTableValid(docPath)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid table name provided");
        }
    }

    private void validateColumns(Set<String> columns) {
        for (String column : columns) {
            if (!validator.isColumnValid(column)) {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid column name provided");
            }
        }
    }

    private void validateCursorCount(int count) {
        if (!validator.isCursorCountValid(count)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Cursor count must be greater than 0");
        }
    }

    @Override
    public String getDdl(CallingContext context, String uriStr, Boolean includeTableData) {
        RaptureURI uri = new RaptureURI(uriStr, Scheme.STRUCTURED);
        return getRepoOrFail(uri.getAuthority()).getDdl(uri.getDocPath(), includeTableData);
    }

    @Override
    public String getCursorUsingSql(CallingContext context, String schema, String rawSql) {
        return getRepoOrFail(schema).getCursorUsingSql(context, rawSql);
    }

    @Override
    public String getCursor(CallingContext context, String tableUri, List<String> columnNames, String where, List<String> order, Boolean ascending, int limit) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        return getRepoOrFail(uri.getAuthority()).getCursor(uri.getDocPath(), columnNames, where, order, ascending, limit);
    }

    @Override
    public String getCursorForJoin(CallingContext context, List<String> tableUris, List<String> columnNames, String from,
            String where, List<String> order, Boolean ascending, int limit) {
        Pair<String, List<String>> pair = getAuthorityAndDocPaths(tableUris);
        return getRepoOrFail(pair.getLeft()).getCursorForJoin(pair.getRight(), columnNames, from, where, order, ascending, limit);
    }

    @Override
    public List<Map<String, Object>> next(CallingContext context, String tableUri, String cursorId, int count) {
        validateCursorCount(count);
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        return getRepoOrFail(uri.getAuthority()).next(uri.getDocPath(), cursorId, count);
    }

    ;

    @Override
    public List<Map<String, Object>> previous(CallingContext context, String tableUri, String cursorId, int count) {
        validateCursorCount(count);
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        return getRepoOrFail(uri.getAuthority()).previous(uri.getDocPath(), cursorId, count);
    }

    ;

    @Override
    public void closeCursor(CallingContext context, String tableUri, String cursorId) {
        RaptureURI uri = new RaptureURI(tableUri, Scheme.STRUCTURED);
        getRepoOrFail(uri.getAuthority()).closeCursor(uri.getDocPath(), cursorId);
    }

    ;

    @Override
    public void createProcedureCallUsingSql(CallingContext context, String procUri, String rawSql) {
        RaptureURI uri = new RaptureURI(procUri);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.createProcedureCallUsingSql(context, rawSql);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public StoredProcedureResponse callProcedure(CallingContext context, String procUri, StoredProcedureParams params) {
        RaptureURI uri = new RaptureURI(procUri);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        String procName = uri.getDocPath();
        registerWithTxManager(context, repo);
        try {
            return repo.callProcedure(context, procName, params);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void dropProcedureUsingSql(CallingContext context, String procUri, String rawSql) {
        RaptureURI uri = new RaptureURI(procUri);
        StructuredRepo repo = getRepoOrFail(uri.getAuthority());
        registerWithTxManager(context, repo);
        try {
            repo.dropProcedureUsingSql(context, rawSql);
        } catch (Exception e) {
            TransactionManager.transactionFailed(getTxId(context));
            throw RaptureExceptionFactory.create(e.getMessage(), e.getCause());
        }
    }

    /**
     * Trusted method not exposed via the API. Mainly used for Feature Installer to re-install schemas and tables
     *
     * @param uriStr - uri representing the structured repo
     * @param ddl    - full SQL string with CREATE and INSERT statements
     */
    public void executeDdl(String uriStr, String ddl, boolean alter) {
        getRepoOrFail(new RaptureURI(uriStr, Scheme.STRUCTURED).getAuthority()).executeDdl(ddl, alter);
    }

    /**
     * Used in joins to get a single authority and the docpaths as a List
     *
     * @param tableUris
     * @return
     */
    private Pair<String, List<String>> getAuthorityAndDocPaths(List<String> tableUris) {
        if (CollectionUtils.isEmpty(tableUris)) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,  apiMessageCatalog.getMessage("NullEmpty", "list of tableUris")); //$NON-NLS-1$ //$NON-NLS-2$
        }
        // we assume the join is across the same DB provider
        RaptureURI uri = new RaptureURI(tableUris.get(0), Scheme.STRUCTURED);
        List<String> docPaths = new ArrayList<>();
        for (String tableUri : tableUris) {
            RaptureURI ruri = new RaptureURI(tableUri, Scheme.STRUCTURED);
            docPaths.add(ruri.getDocPath());
        }
        return Pair.of(uri.getAuthority(), docPaths);
    }

}
