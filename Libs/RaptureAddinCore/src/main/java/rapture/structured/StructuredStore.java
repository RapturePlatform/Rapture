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
package rapture.structured;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import rapture.common.CallingContext;
import rapture.common.StoredProcedureParams;
import rapture.common.StoredProcedureResponse;
import rapture.common.TableMeta;

public interface StructuredStore {

    DataSource getDataSource();

    SqlGenerator getSqlGenerator();

    void setInstance(String instance);

    void setConfig(Map<String, String> config, String authority);

    void drop();

    boolean commit(String txId);

    boolean rollback(String txId);

    Boolean createTableUsingSql(CallingContext context, String sql);

    Boolean createTable(String tableName, Map<String, String> columns);

    Boolean dropTable(String tableName);

    Boolean tableExists(String tableName);

    TableMeta describeTable(String tableName);

    Boolean addTableColumns(String tableName, Map<String, String> columns);

    Boolean deleteTableColumns(String tableName, List<String> columnNames);

    Boolean updateTableColumns(String tableName, Map<String, String> columns);

    Boolean renameTableColumns(String tableName, Map<String, String> columnNames);

    List<Map<String, Object>> selectUsingSql(CallingContext context, String sql);

    List<Map<String, Object>> selectJoinedRows(List<String> tables, List<String> columnNames, String from, String where,
                                               List<String> order, Boolean ascending, int limit);

    List<Map<String, Object>> selectRows(String tableName, List<String> columnNames, String where, List<String> order, Boolean ascending, int limit);

    Boolean insertUsingSql(CallingContext context, String sql);

    Boolean insertRow(String tableName, Map<String, ?> values);

    Boolean insertRows(String tableName, List<? extends Map<String, ?>> values);

    Boolean deleteUsingSql(CallingContext context, String sql);

    Boolean deleteRows(String tableName, String where);

    Boolean updateUsingSql(CallingContext context, String sql);

    Boolean updateRows(String tableName, Map<String, ?> values, String where);

    Boolean createIndex(String tableName, String indexName, List<String> columnNames);

    Boolean dropIndex(String indexName);

    String getDdl(String tableName, Boolean includeTableData);

    String getCursorUsingSql(CallingContext context, String sql);

    String getCursor(String tableName, List<String> columnNames, String where, List<String> order, Boolean ascending, int limit);

    String getCursorForJoin(List<String> tables, List<String> columnNames, String from, String where, List<String> order, Boolean ascending, int limit);

    List<Map<String, Object>> next(String tableName, String cursorId, int count);

    List<Map<String, Object>> previous(String tableName, String cursorId, int count);

    Boolean closeCursor(String tableName, String cursorId);

    void executeDdl(String ddl);

    Boolean createProcedureCallUsingSql(CallingContext context, String sql);

    StoredProcedureResponse callProcedure(CallingContext context, String procName, StoredProcedureParams params);

    Boolean dropProcedureUsingSql(CallingContext context, String rawSql);
}
