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

import java.util.List;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.ForeignKey;
import rapture.common.StoredProcedureParams;
import rapture.common.StoredProcedureResponse;
import rapture.common.TableIndex;
import rapture.common.TableMeta;
import rapture.structured.StructuredStore;

/**
 * Created by seanchen on 7/1/15.
 */
public class StructuredRepo {

    private StructuredStore store;

    public StructuredRepo (StructuredStore store) {
        this.store = store;
    }

    public void drop(){
        store.drop();
    }

    public boolean commit(String txId){
        return store.commit(txId);
    }

    public boolean rollback(String txId){
        return store.rollback(txId);
    }

    public Boolean createTableUsingSql(CallingContext context, String rawSql){
        return store.createTableUsingSql(context, rawSql);
    }

    public Boolean createTable(String tableName, Map<String, String> columns){
        return store.createTable(tableName, columns);
    }

    public Boolean dropTable(String tableName){
        return store.dropTable(tableName);
    }

    public Boolean tableExists(String tableName){
        return store.tableExists(tableName);
    }

    public List<String> getTables() {
        return store.getTables();
    }
    public TableMeta describeTable(String tableName){
        return store.describeTable(tableName);
    }

    public Boolean addTableColumns(String tableName, Map<String, String> columns){
        // TODO RAP-3141: Does this count as a write???
        return store.addTableColumns(tableName, columns);
    }

    public Boolean deleteTableColumns(String tableName, List<String> columnNames){
        return store.deleteTableColumns(tableName, columnNames);
    }

    public Boolean updateTableColumns(String tableName, Map<String, String> columns){
        // TODO RAP-3141: Does this count as a write???
        return store.updateTableColumns(tableName, columns);
    }

    public Boolean renameTableColumns(String tableName, Map<String, String> columnNames){
        return store.renameTableColumns(tableName, columnNames);
    }

    public Boolean createIndex(String tableName, String indexName, List<String> columnNames){
        return store.createIndex(tableName, indexName, columnNames);
    }

    public Boolean dropIndex(String indexName){
        return store.dropIndex(indexName);
    }

    public List<TableIndex> getIndexes(String tablename) {
        return store.getIndexes(tablename);
    }

    public String getPrimaryKey(String tableName) {
        return store.getPrimaryKey(tableName);
    }

    public List<ForeignKey> getForeignKeys(String tableName) {
        return store.getForeignKeys(tableName);
    }

    public List<Map<String, Object>> selectJoinedRows(List<String> tables, List<String> columnNames, String from, String where, List<String> order, Boolean ascending, int limit){
        return store.selectJoinedRows(tables, columnNames, from, where, order, ascending, limit);
    }

    public List<Map<String, Object>> selectUsingSql(CallingContext context, String rawSql){
        return store.selectUsingSql(context, rawSql);
    }

    public List<Map<String, Object>> selectRows(String tableName, List<String> columnNames, String where, List<String> order, Boolean ascending, int limit){
        return store.selectRows(tableName, columnNames, where, order, ascending, limit);
    }

    public Boolean insertUsingSql(CallingContext context, String rawSql){
        // TODO RAP-3141: Figure out how to measure data size and report to graphite
        return store.insertUsingSql(context, rawSql);
    }

    public Boolean insertRow(String tableName, Map<String, Object> values){
        // TODO RAP-3141: Figure out how to measure data size and report to graphite
        return store.insertRow(tableName, values);
    }

    public Boolean insertRows(String tableName, List<Map<String, Object>> values){
        // TODO RAP-3141: Figure out how to measure data size and report to graphite
        return store.insertRows(tableName, values);
    }

    public Boolean updateUsingSql(CallingContext context, String rawSql){
        // TODO RAP-3141: Does this count as a write???
        return store.updateUsingSql(context, rawSql);
    }

    public Boolean deleteUsingSql(CallingContext context, String rawSql){
        return store.deleteUsingSql(context, rawSql);
    }

    public Boolean updateRows(String tableName, Map<String, Object> values, String where){
        // TODO RAP-3141: Does this count as a write???
        return store.updateRows(tableName, values, where);
    }

    public Boolean deleteRows(String tableName, String where){
        return store.deleteRows(tableName, where);
    }

    public String getDdl(String tableName, Boolean includeTableData){
        return store.getDdl(tableName, includeTableData);
    }

    public void executeDdl(String ddl, boolean alter) {
        store.executeDdl(ddl, alter);
    }

    public String getCursorUsingSql(CallingContext context, String rawSql){
        return store.getCursorUsingSql(context, rawSql);
    }

    public String getCursor(String tableName, List<String> columnNames, String where, List<String> order, Boolean ascending, int limit){
        return store.getCursor(tableName, columnNames, where, order, ascending, limit);
    }

    public String getCursorForJoin(List<String> tables, List<String> columnNames, String from, String where, List<String> order, Boolean ascending, int limit){
        return store.getCursorForJoin(tables, columnNames, from, where, order, ascending, limit);
    }

    public List<Map<String, Object>> next(String tableName, String cursorId, int count){
        return store.next(tableName, cursorId, count);
    }

    public List<Map<String, Object>> previous(String tableName, String cursorId, int count){
        return store.previous(tableName, cursorId, count);
    }

    public Boolean closeCursor(String tableName, String cursorId){
        return store.closeCursor(tableName, cursorId);
    }

    public Boolean createProcedureCallUsingSql(CallingContext context, String rawSql){
        return store.createProcedureCallUsingSql(context, rawSql);
    }

    public StoredProcedureResponse callProcedure(CallingContext context, String procName, StoredProcedureParams params){
        return store.callProcedure(context, procName, params);
    }

    public Boolean dropProcedureUsingSql(CallingContext context, String rawSql){
        return store.dropProcedureUsingSql(context, rawSql);
    }

}
