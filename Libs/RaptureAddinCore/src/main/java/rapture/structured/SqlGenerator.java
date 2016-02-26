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
package rapture.structured;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

public interface SqlGenerator {

    String constructCreateTable(String schema, String table, Map<String, String> columns);

    String constructSelect(String schema, String table, List<String> columnNames, String where, List<String> order, Boolean ascending, int limit);

    String constructSelectJoin(List<String> tableNames, List<String> columnNames, String from, String where, List<String> order,
            Boolean ascending, int limit);

    String constructInserts(StructuredStore store, String schema, String table);

    String constructInsertPreparedStatement(String schema, String table, List<List<String>> columnNames);

    String constructUpdatePreparedStatement(String schema, String tableName, List<String> columnNames, String where);

    String constructCreateSchema(String schema);

    String constructDropSchema(String schema);

    String constructDropTable(String schema, String table);

    String constructListTables(String schema);

    String constructDescribeTable(String schema, String tableName);

    String constructAddTableColumns(String schema, String table, Map<String, String> columns);

    String constructDeleteTableColumns(String schema, String table, List<String> columns);

    String constructUpdateTableColumns(String schema, String table, Map<String, String> columns);

    String constructRenameTableColumns(String schema, String table, Map<String, String> columns);

    String constructCreateIndex(String schema, String table, String index, List<String> columns);

    String constructDropIndex(String schema, String index);

    String constructGetIndexes(String schema, String table);

    String constructTableExists(String schema, String table);

    String constructDelete(String schema, String table, String where);

    String constructCreateTable(DataSource dataSource, String schema, String table, Boolean includeTableData);

    String constructGetPrimaryKey(String schema, String table);

    String constructGetForeignKeys(String schema, String table);

    void setCaseConverter(CaseConverter caseConverter);

    CaseConverter getCaseConverter();

    void setNameSanitizer(NameSanitizer nameSanitizer);

    NameSanitizer getNameSanitizer();

}