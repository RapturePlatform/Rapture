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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;
import org.apache.ddlutils.platform.SqlBuilder;

import rapture.common.exception.RaptureExceptionFactory;

/**
 * Hand-crafted sql until I can think of (or find) a better way to do this.
 * 
 * @author dukenguyen
 * 
 */
public abstract class StandardSqlGenerator implements SqlGenerator {

    protected CaseConverter caseConverter = new DefaultCaseConverter();
    protected NameSanitizer nameSanitizer = new DefaultNameSanitizer();

    @Override
    public String constructSelectJoin(List<String> tableNames, List<String> columnNames, String from, String where,
            List<String> order, Boolean ascending, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append(String.format("SELECT %s FROM %s ",
                CollectionUtils.isEmpty(columnNames) ? "*" : StringUtils.join(columnNames, ","),
                caseConverter.convert(from)));
        if (!StringUtils.isBlank(where)) {
            sql.append(String.format("WHERE %s ", where));
        }
        if (!CollectionUtils.isEmpty(order)) {
            sql.append(String.format("ORDER BY %s ", StringUtils.join(order, ",")));
            if (ascending != null && !ascending) {
                sql.append("DESC ");
            } else {
                sql.append("ASC ");
            }
        }
        if (limit > 0) {
            sql.append(String.format("LIMIT %d ", limit));
        }
        return sql.toString();
    }

    @Override
    public String constructSelect(String schema, String tableName, List<String> columnNames, String where, List<String> order, Boolean ascending, int limit) {
        return constructSelectJoin(Arrays.asList(tableName), columnNames, getSafeFullName(schema, tableName), where, order, ascending,
                limit);
    }

    @Override
    public String constructInsertPreparedStatement(String schema, String table, List<List<String>> columnNames) {
        List<String> columns = columnNames.get(0);
        String s = String.format("INSERT INTO %s (%s) VALUES %s", getSafeFullName(schema, table), StringUtils.join(columns, ','),
                getPreparedStatementQuestionMarks(columnNames.size(), columns.size()));
        return s;
    }

    @Override
    public String constructUpdatePreparedStatement(String schema, String table, List<String> columnNames, String where) {
        String ret = String.format("UPDATE %s SET %s", getSafeFullName(schema, table), getPreparedStatement(columnNames));
        if (!StringUtils.isBlank(where)) {
            ret += String.format(" WHERE %s", where);
        }
        return ret;
    }

    @Override
    public String constructCreateTable(String schema, String table, Map<String, String> columns) {
        return String.format("CREATE TABLE IF NOT EXISTS %s (%s)", getSafeFullName(schema, table), getColumnExpression("%s %s", columns));
    }

    @Override
    public String constructCreateSchema(String schema) {
        return String.format("CREATE SCHEMA IF NOT EXISTS %s", makeSqlSafe(schema));
    }

    @Override
    public String constructDropSchema(String schema) {
        return String.format("DROP SCHEMA IF EXISTS %s CASCADE", makeSqlSafe(schema));
    }

    @Override
    public String constructDropTable(String schema, String table) {
        return String.format("DROP TABLE IF EXISTS %s", getSafeFullName(schema, table));
    }

    @Override
    public String constructDescribeTable(String schema, String tableName) {
        return String.format(
                "SELECT column_name, data_type, character_maximum_length FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema='%s' and table_name='%s'",
                makeSqlSafe(schema), makeSqlSafe(tableName));
    }

    @Override
    public String constructAddTableColumns(String schema, String table, Map<String, String> columns) {
        return String.format("ALTER TABLE %s %s", getSafeFullName(schema, table), getColumnExpression("ADD COLUMN %s %s", columns));
    }

    @Override
    public String constructDeleteTableColumns(String schema, String table, List<String> columns) {
        return String.format("ALTER TABLE %s %s", getSafeFullName(schema, table), getColumnExpression("DROP COLUMN %s", columns));
    }

    @Override
    public String constructUpdateTableColumns(String schema, String table, Map<String, String> columns) {
        return String.format("ALTER TABLE %s %s", getSafeFullName(schema, table), getColumnExpression("ALTER COLUMN %s %s", columns));
    }

    @Override
    public String constructCreateIndex(String schema, String table, String index, List<String> columns) {
        return String.format("CREATE INDEX %s ON %s (%s)", index, getSafeFullName(schema, table), getColumnExpression("%s", columns));
    }

    @Override
    public String constructDropIndex(String schema, String index) {
        return String.format("DROP INDEX IF EXISTS %s", getSafeFullName(schema, index));
    }

    String getColumnExpression(String template, List<String> columnNames) {
        StringBuilder sb = new StringBuilder();
        for (String columnName : columnNames) {
            sb.append(",").append(String.format(template, makeSqlSafe(columnName)));
        }
        return sb.substring(1);
    }

    protected String getColumnExpression(String template, Map<String, String> columns) {
        StringBuilder sb = new StringBuilder();
        for (String columnName : columns.keySet()) {
            sb.append(",").append(String.format(template, makeSqlSafe(columnName), columns.get(columnName)));
        }
        return sb.substring(1);
    }

    @Override
    public String constructTableExists(String schema, String table) {
        return String.format("SELECT EXISTS ( SELECT 1 FROM information_schema.tables WHERE table_schema = '%s' AND table_name = '%s')",
                makeSqlSafe(schema), makeSqlSafe(table));
    }

    @Override
    public String constructDelete(String schema, String table, String where) {
        String ret = String.format("DELETE FROM %s", getSafeFullName(schema, table));
        if (!StringUtils.isBlank(where)) {
            ret += String.format(" WHERE %s", where);
        }
        return ret;
    }

    protected abstract SqlBuilder getDdlSqlBuilder(String schema, Platform platform);

    @Override
    public String constructInserts(StructuredStore store, String schema, String table) {
        Pair<Database, SqlBuilder> pair = getDatabaseAndSqlBuilderPair(schema, store.getDataSource());
        StringBuilder ret = new StringBuilder();
        // if 'table' is blank they want inserts for the entire schema across all tables
        if (!StringUtils.isBlank(table)) {
            for (Table t : pair.getLeft().getTables()) {
                if (t.getName().equalsIgnoreCase(table)) {
                    ret.append(constructInsertSingleTable(t, store, pair.getRight()));
                    return ret.toString();
                }
            }
        } else {
            for (Table t : pair.getLeft().getTables()) {
                ret.append(constructInsertSingleTable(t, store, pair.getRight()));
            }
            return ret.toString();
        }
        throw RaptureExceptionFactory.create(String.format("Failed to find table [%s] in schema [%s]", table, schema));
    }

    private String constructInsertSingleTable(Table table, StructuredStore store, SqlBuilder sqlBuilder) {
        StringBuilder ret = new StringBuilder();
        List<Map<String, Object>> allData = store.selectRows(table.getName(), null, null, null, null, -1);
        for (Map<String, Object> row : allData) {
            ret.append(sqlBuilder.getInsertSql(table, row, false) + "\n");
        }
        return ret.toString();
    }

    @Override
    public String constructCreateTable(DataSource dataSource, String schema, String table, Boolean includeTableData) {
        Pair<Database, SqlBuilder> pair = getDatabaseAndSqlBuilderPair(schema, dataSource);
        // if table unspecified they want DDL for the entire schema which includes all the tables in the schema
        if (StringUtils.isBlank(table)) {
            StringWriter sw = new StringWriter();
            pair.getRight().setWriter(sw);
            try {
                pair.getRight().createTables(pair.getLeft());
                return sw.toString();
            } catch (IOException e) {
                throw RaptureExceptionFactory.create(String.format("Failed to generate DDL for schema [%s].  Msg is [%s]", schema, e.getMessage()));
            }
        } else {
            for (Table t : pair.getLeft().getTables()) {
                if (t.getName().equalsIgnoreCase(table)) {
                    StringWriter sw = new StringWriter();
                    pair.getRight().setWriter(sw);
                    try {
                        pair.getRight().createTable(pair.getLeft(), t);
                        return sw.toString();
                    } catch (IOException e) {
                        throw RaptureExceptionFactory.create(String.format("Failed to generate DDL for schema [%s] for table [%s].  Msg is [%s]", schema,
                                table, e.getMessage()));
                    }
                }
            }
            throw RaptureExceptionFactory.create(String.format("Failed to find table [%s] in schema [%s]", table, schema));
        }
    }

    @Override
    public void setCaseConverter(CaseConverter caseConverter) {
        this.caseConverter = caseConverter;
    }

    @Override
    public CaseConverter getCaseConverter() {
        return caseConverter;
    }

    @Override
    public NameSanitizer getNameSanitizer() {
        return nameSanitizer;
    }

    @Override
    public void setNameSanitizer(NameSanitizer nameSanitizer) {
        this.nameSanitizer = nameSanitizer;
    }

    protected String getSafeFullName(String schema, String tableName) {
        return String.format("%s.%s", makeSqlSafe(schema), makeSqlSafe(tableName));
    }

    private String makeSqlSafe(String name) {
        return caseConverter.convert(nameSanitizer.sanitize(name));
    }

    private String getPreparedStatement(List<String> columnNames) {
        List<String> pstmts = new LinkedList<>();
        for (String columnName : columnNames) {
            pstmts.add(String.format("%s=?", columnName));
        }
        return StringUtils.join(pstmts, ",");
    }

    private String getPreparedStatementQuestionMarks(int rows, int num) {
        return StringUtils.join(Collections.nCopies(rows, String.format("(%s)", StringUtils.join(Collections.nCopies(num, "?"), ","))), ",");
    }

    private Pair<Database, SqlBuilder> getDatabaseAndSqlBuilderPair(String schema, DataSource dataSource) {
        Platform platform = PlatformFactory.createNewPlatformInstance(dataSource);
        return Pair.of(platform.readModelFromDatabase(null, null, caseConverter.convert(schema), null), getDdlSqlBuilder(schema, platform));
    }
}
