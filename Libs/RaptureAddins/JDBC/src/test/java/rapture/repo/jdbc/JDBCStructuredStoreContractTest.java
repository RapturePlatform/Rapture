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
package rapture.repo.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.structured.StructuredStore;

public abstract class JDBCStructuredStoreContractTest {

    protected StructuredStore ss;

    private String table = "people";

    private String table2 = "address";

    private CallingContext context;

    public abstract StructuredStore getStructuredStore();

    public abstract String getSchema();

    @Before
    public void setup() {
        ss = getStructuredStore();
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("firstname", "varchar(255)");
        columns.put("lastname", "varchar(255)");
        columns.put("age", "int");
        assertTrue(ss.createTable(table, columns));

        Map<String, String> columnsTable2 = new LinkedHashMap<>();
        columnsTable2.put("id", "int");
        columnsTable2.put("street", "varchar(255)");
        columnsTable2.put("city", "varchar(255)");
        columnsTable2.put("zip", "int");
        assertTrue(ss.createTable(table2, columnsTable2));

        context = ContextFactory.getKernelUser();
    }

    @After
    public void teardown() {
        ss.dropTable(table);
        assertFalse(ss.tableExists(table));
        ss.dropTable(table2);
        assertFalse(ss.tableExists(table2));
        ss.drop();
    }

    @Test
    public void testCreateTableUsingSql() {
        String newTable = "company";
        String sql = String.format("create table %s.%s (id int, name varchar(30), address varchar(100), phone varchar(15))", getSchema(), newTable);
        assertTrue(ss.createTableUsingSql(context, sql));

        Map<String, String> tableDefinition = ss.describeTable(newTable).getRows();
        assertEquals(4, tableDefinition.size());

        ss.dropTable(newTable);
    }

    @Test
    public void testSelectRows() {
        List<String> columnNames = Arrays.asList("id", "firstname", "lastname", "age");
        String where = "firstname='Duke'";
        List<String> order = Arrays.asList("firstname");
        Boolean ascending = true;
        int limit = 1;
        List<Map<String, Object>> result = ss.selectRows(table, columnNames, where, order, ascending, limit);
        assertTrue(result.isEmpty());
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("firstname", "Duke");
        vals.put("lastname", "Nguyen");
        assertTrue(ss.insertRow(table, vals));
        result = ss.selectRows(table, columnNames, where, order, ascending, limit);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("iD"));
        assertNull(result.get(0).get("AGe"));
        assertEquals("Duke", result.get(0).get("firstname"));
        assertEquals("Nguyen", result.get(0).get("LASTNAME"));

        vals.clear();
        vals.put("id", 2);
        vals.put("firstname", "Walter");
        vals.put("lastname", "White");
        vals.put("age", 40);
        assertTrue(ss.insertRow(table, vals));

        result = ss.selectRows(table, columnNames, null, order, ascending, -1);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).get("iD"));
        assertNull(result.get(0).get("AGe"));
        assertEquals("Duke", result.get(0).get("firstname"));
        assertEquals("Nguyen", result.get(0).get("LASTNAME"));
        assertEquals(2, result.get(1).get("iD"));
        assertEquals(40, result.get(1).get("AGe"));
        assertEquals("Walter", result.get(1).get("firstname"));
        assertEquals("White", result.get(1).get("LASTNaME"));
    }

    @Test
    public void testSelectUsingSql() {
        String sql = "select firstname, age from " + getSchema() + ".people where age between -1 and 100 and firstname like 'first%'";
        List<Map<String, Object>> result = ss.selectUsingSql(context, sql);
        assertTrue(result.isEmpty());

        for (int i = 1; i <= 5; i++) {
            ss.insertRow(table, ImmutableMap.of("id", i, "firstname", "first" + i, "lastname", "last" + i, "age", i * 10));
        }
        result = ss.selectUsingSql(context, sql);
        assertEquals(5, result.size());
    }

    @Test
    public void testGetDeleteRowsWithSubQuery() {
        for (int i = 1; i <= 5; i++) {
            String sql = String.format("insert into %s.people (id, firstname, lastname, age) values (%d, 'first%d', 'last%d', %d)", getSchema(), i, i, i, i);
            assertTrue(ss.insertUsingSql(context, sql));
        }
        String sql = String.format("select * from %s.people where age = (select max(age) from %s.people)", getSchema(), getSchema());
        List<Map<String, Object>> result = ss.selectUsingSql(context, sql);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).get("age"));

        sql = String.format("create table %s.employed (employee varchar(255), employer varchar(255))", getSchema());
        assertTrue(ss.createTableUsingSql(context, sql));
        ss.insertUsingSql(context, String.format("insert into %s.employed values ('first1', 'company1')", getSchema()));
        ss.insertUsingSql(context, String.format("insert into %s.employed values ('first2', 'company2')", getSchema()));

        sql = String.format("select * from %s.people where firstname in (select distinct employee from %s.employed)", getSchema(), getSchema());
        result = ss.selectUsingSql(context, sql);
        assertEquals(2, result.size());

        sql = String.format("select * from %s.people, %s.employed where firstname=employee and age > 1", getSchema(), getSchema());
        result = ss.selectUsingSql(context, sql);
        assertEquals(1, result.size());

        sql = String.format("delete from %s.people where firstname not in (select distinct employee from %s.employed)", getSchema(), getSchema());
        assertTrue(ss.deleteUsingSql(context, sql));
        result = ss.selectUsingSql(context, "select * from " + getSchema() + ".people");
        assertEquals(2, result.size());

        ss.dropTable("employed");
    }

    @Test
    public void testInsertUsingSql() {
        for (int i = 1; i <= 5; i++) {
            String sql = String.format("insert into %s.people (id, firstname, lastname, age) values (%d, 'first%d', 'last%d', %d)", getSchema(), i, i, i, i);
            assertTrue(ss.insertUsingSql(context, sql));
        }

        List<Map<String, Object>> result = ss.selectUsingSql(context, "select * from " + getSchema() + ".people");
        assertEquals(5, result.size());
    }

    @Test
    public void testInsertRows() {
        List<String> columnNames = Arrays.asList("id", "firstname", "lastname", "age");
        String where = null;
        List<String> order = Arrays.asList("id");
        Boolean ascending = true;
        int limit = -1;
        int num = 100;
        List<Map<String, Object>> result = ss.selectRows(table, columnNames, where, order, ascending, limit);
        assertTrue(result.isEmpty());
        List<Map<String, Object>> vals = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i);
            m.put("firstname", "first_" + i);
            m.put("lastname", "last_" + i);
            m.put("age", i);
            vals.add(m);
        }
        assertTrue(ss.insertRows(table, vals));
        result = ss.selectRows(table, columnNames, where, order, ascending, limit);
        assertEquals(num, result.size());
        for (int i = 0; i < num; i++) {
            Map<String, Object> row = result.get(i);
            assertEquals(i, row.get("id"));
            assertEquals("first_" + i, row.get("firstname"));
            assertEquals("last_" + i, row.get("lastname"));
            assertEquals(i, row.get("age"));
        }
    }

    @Test
    public void testUpdateTableColumns() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("age", 24);
        assertTrue(ss.insertRow(table, vals));
        List<Map<String, Object>> result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
        assertEquals(24, result.get(0).get("Age"));
        assertTrue(result.get(0).get("id") instanceof Integer);
        assertTrue(result.get(0).get("AGE") instanceof Integer);
        Map<String, String> newColumns = new HashMap<>();
        newColumns.put("id", "varchar(255)");
        newColumns.put("age", "varchar(255)");
        assertTrue(ss.updateTableColumns(table, newColumns));
        result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).get("id"));
        assertEquals("24", result.get(0).get("age"));
        assertTrue(result.get(0).get("id") instanceof String);
        assertTrue(result.get(0).get("AgE") instanceof String);
    }

    @Test
    public void testRenameTableColumns() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("age", 24);
        assertTrue(ss.insertRow(table, vals));
        List<Map<String, Object>> result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
        assertEquals(24, result.get(0).get("Age"));
        assertTrue(ss.renameTableColumns(table, ImmutableMap.of("id", "xid")));
        result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("xid"));
        assertEquals(24, result.get(0).get("Age"));
        assertNull(result.get(0).get("id"));
    }

    @Test
    public void testAddDeleteTableColumns() {
        List<Map<String, Object>> result = ss.selectRows(table, null, null, null, null, -1);
        assertTrue(result.isEmpty());
        Map<String, String> newColumns = new HashMap<>();
        newColumns.put("newcol1", "int");
        newColumns.put("newcol2", "varchar(255)");
        assertTrue(ss.addTableColumns(table, newColumns));
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("firstname", "Duke");
        vals.put("lastname", "Nguyen");
        vals.put("newcol1", 100);
        vals.put("newcol2", "blah");
        assertTrue(ss.insertRow(table, vals));
        result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).get("newcol1"));
        assertEquals("blah", result.get(0).get("newcol2"));
        assertTrue(ss.deleteTableColumns(table, new ArrayList<>(newColumns.keySet())));
        result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals("Duke", result.get(0).get("FIRSTName"));
        assertNull(result.get(0).get("newcol1"));
        assertNull(result.get(0).get("newcol2"));
    }

    @Test
    public void testUpdateRows() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 2);
        vals.put("firstname", "Walter");
        vals.put("lastname", "White");
        vals.put("age", 40);
        assertTrue(ss.insertRow(table, vals));

        List<Map<String, Object>> result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).get("id"));
        assertEquals("Walter", result.get(0).get("firstname"));
        assertEquals("White", result.get(0).get("lastname"));
        assertEquals(40, result.get(0).get("age"));

        vals.clear();
        vals.put("firstname", "Duke");
        vals.put("lastname", "Nguyen");
        vals.put("age", 34);
        assertTrue(ss.updateRows(table, vals, null));
        result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).get("id"));
        assertEquals("Duke", result.get(0).get("firstname"));
        assertEquals("Nguyen", result.get(0).get("lastname"));
        assertEquals(34, result.get(0).get("age"));
        assertFalse(ss.updateRows(table, vals, "id=6"));

        vals.clear();
        vals.put("firstname", "Name");
        assertTrue(ss.updateRows(table, vals, "firstname='Duke'"));
        result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).get("id"));
        assertEquals("Name", result.get(0).get("firstname"));

    }

    @Test
    public void testUpdateUsingSql() {
        int i = 1;
        String sql = String.format("insert into %s.people (id, firstname, lastname, age) values (%d, 'first%d', 'last%d', %d)", getSchema(), i, i, i, i);
        assertTrue(ss.insertUsingSql(context, sql));

        sql = String.format("update %s.people set firstname='first%s', age=%d where id = %d", getSchema(), i * 11, i * 11, i);
        assertTrue(ss.updateUsingSql(context, sql));

        sql = String.format("select * from %s.people where id=%d", getSchema(), i);
        List<Map<String, Object>> result = ss.selectUsingSql(context, sql);
        assertEquals("first" + (i * 11), result.get(0).get("firstname"));
        assertEquals(i * 11, result.get(0).get("age"));
    }

    @Test
    public void testDeleteRows() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 2);
        vals.put("firstname", "Walter");
        vals.put("lastname", "White");
        vals.put("age", 40);
        assertTrue(ss.insertRow(table, vals));

        List<Map<String, Object>> result = ss.selectRows(table, null, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).get("id"));
        assertEquals("Walter", result.get(0).get("firstname"));
        assertEquals("White", result.get(0).get("lastname"));
        assertEquals(40, result.get(0).get("age"));

        assertTrue(ss.deleteRows(table, null));
        result = ss.selectRows(table, null, null, null, null, -1);
        assertTrue(result.isEmpty());
        assertTrue(ss.insertRow(table, vals));
        assertFalse(ss.deleteRows(table, "id=3"));
    }

    @Test
    public void testDeleteUsingSql() {
        for (int i = 1; i <= 5; i++) {
            String sql = String.format("insert into %s.people (id, firstname, lastname, age) values (%d, 'first%d', 'last%d', %d)", getSchema(), i, i, i, i);
            assertTrue(ss.insertUsingSql(context, sql));
        }

        String sql = String.format("delete from %s.people where id<=3", getSchema());
        assertTrue(ss.deleteUsingSql(context, sql));

        List<Map<String, Object>> result = ss.selectUsingSql(context, String.format("select * from %s.people", getSchema()));
        assertEquals(2, result.size());
    }

    @Test
    public void testCreateIndex() {
        assertTrue(ss.createIndex(table, "myindex", Arrays.asList("id")));
    }

    @Test
    public void testDropIndex() {
        assertTrue(ss.createIndex(table, "myindex", Arrays.asList("id")));
        assertTrue(ss.dropIndex("myindex"));
    }

    @Test
    public void testTableExists() {
        assertTrue(ss.tableExists("people"));
        assertFalse(ss.tableExists("nonexistentTable"));
        assertTrue(ss.dropTable("people"));
        assertFalse(ss.tableExists("people"));
    }

    @Test
    public void testSelectJoinedRows() {
        String tablea = "table_a";
        String tableb = "table_b";

        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("firstname", "varchar(255)");
        columns.put("lastname", "varchar(255)");
        columns.put("age", "int");
        assertTrue(ss.createTable(tablea, columns));

        Map<String, String> columnsTable2 = new LinkedHashMap<>();
        columnsTable2.put("id", "int");
        columnsTable2.put("street", "varchar(255)");
        columnsTable2.put("city", "varchar(255)");
        columnsTable2.put("zip", "int");
        assertTrue(ss.createTable(tableb, columnsTable2));

        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 2);
        vals.put("firstname", "Walter");
        vals.put("lastname", "White");
        vals.put("age", 40);
        assertTrue(ss.insertRow(tablea, vals));

        Map<String, Object> vals2 = new HashMap<>();
        vals2.put("id", 2);
        vals2.put("street", "Montgomery");
        vals2.put("city", "SF");
        vals2.put("zip", 94105);
        assertTrue(ss.insertRow(tableb, vals2));

        List<String> tables = Arrays.asList(tablea, tableb);
        List<String> columnNames = Arrays.asList(tablea + ".firstname", tableb + ".city");
        String from = String.format("%s.%s INNER JOIN %s.%s ON %s.id=%s.id", getSchema(), tablea, getSchema(), tableb, tablea, tableb);
        List<Map<String, Object>> result = ss.selectJoinedRows(tables, columnNames, from, null, null, null, -1);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals("Walter", result.get(0).get("firstname"));
        assertEquals("SF", result.get(0).get("city"));
    }

    @Test
    public void testGetCursorForJoin() {
        String tablea = "table_a";
        String tableb = "table_b";

        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("firstname", "varchar(255)");
        columns.put("lastname", "varchar(255)");
        columns.put("age", "int");
        assertTrue(ss.createTable(tablea, columns));

        Map<String, String> columnsTable2 = new LinkedHashMap<>();
        columnsTable2.put("id", "int");
        columnsTable2.put("street", "varchar(255)");
        columnsTable2.put("city", "varchar(255)");
        columnsTable2.put("zip", "int");
        assertTrue(ss.createTable(tableb, columnsTable2));

        List<Map<String, Object>> tableaVals = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> vals = new HashMap<>();
            vals.put("id", i);
            vals.put("firstname", "firstname_" + i);
            vals.put("lastname", "lastname_" + i);
            vals.put("age", i);
            tableaVals.add(vals);
        }
        assertTrue(ss.insertRows(tablea, tableaVals));

        List<Map<String, Object>> tablebVals = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> vals = new HashMap<>();
            vals.put("id", i);
            vals.put("street", "street_" + i);
            vals.put("city", "city_" + i);
            vals.put("zip", i);
            tablebVals.add(vals);
        }
        assertTrue(ss.insertRows(tableb, tablebVals));

        List<String> tables = Arrays.asList(tablea, tableb);
        List<String> columnNames = Arrays.asList(tablea + ".firstname", tableb + ".city", tableb + ".zip");
        String from = String.format("%s.%s INNER JOIN %s.%s ON %s.id=%s.id", getSchema(), tablea, getSchema(), tableb, tablea, tableb);
        String where = tableb + ".zip >= 0 AND " + tablea + ".firstname LIKE 'firstname_%'";
        String cursorId = ss.getCursorForJoin(tables, columnNames, from, where, null, null, -1);
        assertNotNull(cursorId);
        for (int i = 0; i < 100; i++) {
            List<Map<String, Object>> res = ss.next(table, cursorId, 1);
            assertNotNull(res);
            assertEquals(1, res.size());
            Map<String, Object> row = res.get(0);
            assertEquals("firstname_" + i, row.get("firstname"));
            assertEquals("city_" + i, row.get("city"));
            assertEquals(i, row.get("zip"));
        }

        for (int i = 0; i < 342; i++) {
            assertNull(ss.next(table, cursorId, 1));
        }
        assertTrue(ss.closeCursor(table, cursorId));

        cursorId = ss.getCursorForJoin(tables, columnNames, from, where, null, null, -1);
        assertNotNull(cursorId);
        for (int i = 0; i < 50; i++) {
            List<Map<String, Object>> res = ss.next(table, cursorId, 2);
            assertNotNull(res);
            assertEquals(2, res.size());
            Map<String, Object> row1 = res.get(0);
            Map<String, Object> row2 = res.get(1);
            int x = i * 2;
            int y = x + 1;
            assertEquals("firstname_" + x, row1.get("firstname"));
            assertEquals("city_" + x, row1.get("city"));
            assertEquals(x, row1.get("zip"));
            assertEquals("firstname_" + y, row2.get("firstname"));
            assertEquals("city_" + y, row2.get("city"));
            assertEquals(y, row2.get("zip"));
        }
        assertTrue(ss.closeCursor(table, cursorId));

        cursorId = ss.getCursorForJoin(tables, columnNames, from, where, null, null, -1);
        assertNotNull(cursorId);
        for (int i = 0; i < 40; i++) {
            List<Map<String, Object>> res = ss.next(table, cursorId, 1);
            assertNotNull(res);
            assertEquals(1, res.size());
            Map<String, Object> row = res.get(0);
            assertEquals("firstname_" + i, row.get("firstname"));
            assertEquals("city_" + i, row.get("city"));
            assertEquals(i, row.get("zip"));
        }
        for (int i = 0; i < 39; i++) {
            List<Map<String, Object>> res = ss.previous(table, cursorId, 1);
            assertNotNull(res);
            assertEquals(1, res.size());
            Map<String, Object> row = res.get(0);
            int x = 38 - i;
            assertEquals("firstname_" + x, row.get("firstname"));
            assertEquals("city_" + x, row.get("city"));
            assertEquals(x, row.get("zip"));
        }

    }

    @Test
    public void testSelectJoinedRowsDifferentJoinTypes() {
        String tablea = "table_a";
        String tableb = "table_b";

        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("firstname", "varchar(255)");
        columns.put("lastname", "varchar(255)");
        columns.put("age", "int");
        assertTrue(ss.createTable(tablea, columns));

        Map<String, String> columnsTable2 = new LinkedHashMap<>();
        columnsTable2.put("id", "int");
        columnsTable2.put("street", "varchar(255)");
        columnsTable2.put("city", "varchar(255)");
        columnsTable2.put("zip", "int");
        assertTrue(ss.createTable(tableb, columnsTable2));

        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("firstname", "Walter");
        vals.put("lastname", "White");
        vals.put("age", 40);
        assertTrue(ss.insertRow(tablea, vals));

        vals.clear();
        vals.put("id", 2);
        vals.put("firstname", "Jesse");
        vals.put("lastname", "Pinkman");
        vals.put("age", 20);
        assertTrue(ss.insertRow(tablea, vals));

        vals.clear();
        vals.put("id", 3);
        vals.put("firstname", "Gustavo");
        vals.put("lastname", "Fring");
        vals.put("age", 45);
        assertTrue(ss.insertRow(tablea, vals));

        vals.clear();
        vals.put("id", 1);
        vals.put("street", "Montgomery");
        vals.put("city", "SF");
        vals.put("zip", 94105);
        assertTrue(ss.insertRow(tableb, vals));

        vals.clear();
        vals.put("id", 2);
        vals.put("street", "Powell");
        vals.put("city", "SF");
        vals.put("zip", 94105);
        assertTrue(ss.insertRow(tableb, vals));

        vals.clear();
        vals.put("id", 3);
        vals.put("street", "Van Ness");
        vals.put("city", "SF");
        vals.put("zip", 94105);
        assertTrue(ss.insertRow(tableb, vals));

        vals.clear();
        vals.put("id", 4);
        vals.put("street", "Church");
        vals.put("city", "SF");
        vals.put("zip", 94105);
        assertTrue(ss.insertRow(tableb, vals));

        List<String> tables = Arrays.asList(tablea, tableb);
        List<String> columnNames = Arrays.asList(tablea + ".firstname", tableb + ".street AS MYSTREET");
        List<String> orderBy = Arrays.asList(tableb + ".id");

        String from = String.format("%s.%s RIGHT JOIN %s.%s ON %s.id=%s.id", getSchema(), tablea, getSchema(), tableb, tablea, tableb);
        List<Map<String, Object>> result = ss.selectJoinedRows(tables, columnNames, from, null, orderBy, null, -1);
        assertEquals(4, result.size());
        for (Map<String, Object> row : result) {
            assertEquals(2, row.size());
            assertTrue(row.containsKey("firstname"));
            assertTrue(row.containsKey("MYSTREET"));

        }
        assertEquals("Walter", result.get(0).get("firstname"));
        assertEquals("Montgomery", result.get(0).get("MYSTREET"));
        assertEquals("Jesse", result.get(1).get("firstname"));
        assertEquals("Powell", result.get(1).get("MYSTREET"));
        assertEquals("Gustavo", result.get(2).get("firstname"));
        assertEquals("Van Ness", result.get(2).get("MYSTREET"));
        assertNull(result.get(3).get("firstname"));
        assertEquals("Church", result.get(3).get("MYSTREET"));

        from = String.format("%s.%s LEFT JOIN %s.%s ON %s.id=%s.id", getSchema(), tablea, getSchema(), tableb, tablea, tableb);
        result = ss.selectJoinedRows(tables, columnNames, from, null, null, null, -1);
        assertEquals(3, result.size());
        for (Map<String, Object> row : result) {
            assertEquals(2, row.size());
            assertTrue(row.containsKey("firstname"));
            assertTrue(row.containsKey("MYSTREET"));
        }
        assertEquals("Walter", result.get(0).get("firstname"));
        assertEquals("Montgomery", result.get(0).get("MYSTREET"));
        assertEquals("Jesse", result.get(1).get("firstname"));
        assertEquals("Powell", result.get(1).get("MYSTREET"));
        assertEquals("Gustavo", result.get(2).get("firstname"));
        assertEquals("Van Ness", result.get(2).get("MYSTREET"));

        from = String.format("%s.%s FULL OUTER JOIN %s.%s ON %s.id=%s.id", getSchema(), tablea, getSchema(), tableb, tablea, tableb);
        result = ss.selectJoinedRows(tables, columnNames, from, null, null, null, -1);
        assertEquals(4, result.size());
        for (Map<String, Object> row : result) {
            assertEquals(2, row.size());
            assertTrue(row.containsKey("firstname"));
            assertTrue(row.containsKey("MYSTREET"));
        }
        assertEquals("Walter", result.get(0).get("firstname"));
        assertEquals("Montgomery", result.get(0).get("MYSTREET"));
        assertEquals("Jesse", result.get(1).get("firstname"));
        assertEquals("Powell", result.get(1).get("MYSTREET"));
        assertEquals("Gustavo", result.get(2).get("firstname"));
        assertEquals("Van Ness", result.get(2).get("MYSTREET"));
        assertNull(result.get(3).get("firstname"));
        assertEquals("Church", result.get(3).get("MYSTREET"));

        from = String.format("%s.%s INNER JOIN %s.%s ON %s.id=%s.id", getSchema(), tablea, getSchema(), tableb, tablea, tableb);
        result = ss.selectJoinedRows(tables, columnNames, from, null, null, null, -1);
        assertEquals(3, result.size());
        for (Map<String, Object> row : result) {
            assertEquals(2, row.size());
            assertTrue(row.containsKey("firstname"));
            assertTrue(row.containsKey("MYSTREET"));
        }
        assertEquals("Walter", result.get(0).get("firstname"));
        assertEquals("Montgomery", result.get(0).get("MYSTREET"));
        assertEquals("Jesse", result.get(1).get("firstname"));
        assertEquals("Powell", result.get(1).get("MYSTREET"));
        assertEquals("Gustavo", result.get(2).get("firstname"));
        assertEquals("Van Ness", result.get(2).get("MYSTREET"));
    }

    @Test
    public void testGetContentWithAliases() {
        List<String> columnNames = Arrays.asList("id as IDENT", "firstname As THE_NAME", "lastname AS LAST", "age as THE_AGE");
        String where = "firstname='Duke'";
        List<String> order = Arrays.asList("firstname");
        Boolean ascending = true;
        int limit = 1;
        List<Map<String, Object>> result = ss.selectRows(table, columnNames, where, order, ascending, limit);
        assertTrue(result.isEmpty());
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("firstname", "Duke");
        vals.put("lastname", "Nguyen");
        assertTrue(ss.insertRow(table, vals));
        result = ss.selectRows(table, columnNames, where, order, ascending, limit);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("ident"));
        assertNull(result.get(0).get("The_age"));
        assertEquals("Duke", result.get(0).get("THE_NAME"));
        assertEquals("Nguyen", result.get(0).get("LAST"));
    }

    @Test
    public void testGetDdlForEntireSchema() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("firstname", "Duke");
        vals.put("lastname", "Nguyen");
        assertTrue(ss.insertRow(table, vals));
        vals = new HashMap<>();
        vals.put("id", 2);
        vals.put("firstname", "Walter");
        vals.put("lastname", "White");
        assertTrue(ss.insertRow(table, vals));

        String s = ss.getDdl(null, false);
        // output varies too much between providers, can't assert too tightly
        assertNotNull(s);
        assertTrue(s.indexOf("INSERT") == -1);
        assertTrue(s.indexOf("CREATE") != -1);

        s = ss.getDdl(null, true);
        // output varies too much between providers, can't assert too tightly
        assertNotNull(s);
        assertTrue(s.indexOf("INSERT") != -1);
        assertTrue(s.indexOf("CREATE") != -1);
    }

    @Test
    public void testGetDdlForSingleTable() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", 1);
        vals.put("firstname", "Duke");
        vals.put("lastname", "Nguyen");
        assertTrue(ss.insertRow(table, vals));
        vals = new HashMap<>();
        vals.put("id", 2);
        vals.put("firstname", "Walter");
        vals.put("lastname", "White");
        assertTrue(ss.insertRow(table, vals));

        String s = ss.getDdl(table, false);
        String expected = String.format("CREATE TABLE %s.PEOPLE\n" +
                "(\n" +
                "    ID INTEGER,\n" +
                "    FIRSTNAME VARCHAR(255),\n" +
                "    LASTNAME VARCHAR(255),\n" +
                "    AGE INTEGER\n" +
                ");\n\n"+
                "// BEGIN ALTER_BLOCK DO NOT REMOVE THIS LINE\n", getSchema());
        assertEqualsIgnoreCase(expected, s.substring(0, expected.length()));
        s = ss.getDdl(table, true);
        expected = String.format("CREATE TABLE %s.PEOPLE\n" +
                "(\n" +
                "    ID INTEGER,\n" +
                "    FIRSTNAME VARCHAR(255),\n" +
                "    LASTNAME VARCHAR(255),\n" +
                "    AGE INTEGER\n" +
                ");\n" +
                "\n" +
                "INSERT INTO %s.PEOPLE (ID, FIRSTNAME, LASTNAME, AGE) VALUES ('1', 'Duke', 'Nguyen', NULL)\n" +
                "INSERT INTO %s.PEOPLE (ID, FIRSTNAME, LASTNAME, AGE) VALUES ('2', 'Walter', 'White', NULL)\n"+
                "// BEGIN ALTER_BLOCK DO NOT REMOVE THIS LINE\n", getSchema(), getSchema(), getSchema());
        assertEqualsIgnoreCase(expected, s.substring(0, expected.length()));
    }

    private void assertEqualsIgnoreCase(String s1, String s2) {
        assertTrue(s1.equalsIgnoreCase(s2));
    }

    @Test
    public void testGetCursor() {
        Map<String, Object> vals = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            vals.clear();
            vals.put("id", i);
            vals.put("firstname", "first_" + i);
            vals.put("lastname", "last_" + i);
            vals.put("age", i);
            assertTrue(ss.insertRow(table, vals));
        }

        List<String> columnNames = Arrays.asList("id", "firstname as f", "lastname as l", "age");
        String where = null;
        List<String> order = Arrays.asList("id");
        Boolean ascending = true;
        int limit = -1;

        List<Map<String, Object>> result = ss.selectRows(table, columnNames, where, order, ascending, limit);
        assertEquals(100, result.size());

        String cursorId = ss.getCursor(table, columnNames, where, order, ascending, limit);
        assertNotNull(cursorId);
        for (int i = 0; i < 100; i++) {
            List<Map<String, Object>> res = ss.next(table, cursorId, 1);
            assertNotNull(res);
            assertEquals(1, res.size());
            Map<String, Object> row = res.get(0);
            assertEquals(i, row.get("id"));
            assertEquals("first_" + i, row.get("f"));
            assertEquals("last_" + i, row.get("l"));
            assertEquals(i, row.get("age"));
        }

        for (int i = 0; i < 342; i++) {
            assertNull(ss.next(table, cursorId, 1));
        }
        assertTrue(ss.closeCursor(table, cursorId));

        cursorId = ss.getCursor(table, columnNames, where, order, ascending, limit);
        assertNotNull(cursorId);
        for (int i = 0; i < 50; i++) {
            List<Map<String, Object>> res = ss.next(table, cursorId, 2);
            assertNotNull(res);
            assertEquals(2, res.size());
            Map<String, Object> row1 = res.get(0);
            Map<String, Object> row2 = res.get(1);
            int x = i * 2;
            int y = x + 1;
            assertEquals(x, row1.get("id"));
            assertEquals("first_" + x, row1.get("f"));
            assertEquals("last_" + x, row1.get("l"));
            assertEquals(x, row1.get("age"));
            assertEquals(y, row2.get("id"));
            assertEquals("first_" + y, row2.get("f"));
            assertEquals("last_" + y, row2.get("l"));
            assertEquals(y, row2.get("age"));
        }
        assertTrue(ss.closeCursor(table, cursorId));

        cursorId = ss.getCursor(table, columnNames, where, order, ascending, limit);
        assertNotNull(cursorId);
        for (int i = 0; i < 50; i++) {
            List<Map<String, Object>> res = ss.next(table, cursorId, 1);
            assertNotNull(res);
            assertEquals(1, res.size());
            Map<String, Object> row1 = res.get(0);
            int x = i;
            assertEquals(x, row1.get("id"));
            assertEquals("first_" + x, row1.get("f"));
            assertEquals("last_" + x, row1.get("l"));
            assertEquals(x, row1.get("age"));
        }
        for (int i = 0; i < 49; i++) {
            List<Map<String, Object>> res = ss.previous(table, cursorId, 1);
            assertNotNull(res);
            assertEquals(1, res.size());
            Map<String, Object> row1 = res.get(0);
            int x = 48 - i;
            assertEquals(x, row1.get("id"));
            assertEquals("first_" + x, row1.get("f"));
            assertEquals("last_" + x, row1.get("l"));
            assertEquals(x, row1.get("age"));
        }
        List<Map<String, Object>> res = ss.next(table, cursorId, 1);
        assertNotNull(res);
        assertEquals(1, res.size());
        Map<String, Object> row1 = res.get(0);
        int x = 1;
        assertEquals(x, row1.get("id"));
        assertEquals("first_" + x, row1.get("f"));
        assertEquals("last_" + x, row1.get("l"));
        assertEquals(x, row1.get("age"));
        assertTrue(ss.closeCursor(table, cursorId));
    }

    @Test
    public void testGetCursorUsingSql() {
        for (int i = 1; i <= 100; i++) {
            String sql = String.format("insert into %s.people (id, firstname, lastname, age) values (%d, 'first%d', 'last%d', %d)", getSchema(), i, i, i, i);
            assertTrue(ss.insertUsingSql(context, sql));
        }
        String sql = String.format("select id, firstname as f, lastname as l, age from %s.people where id>=10", getSchema());
        String cursorId = ss.getCursorUsingSql(context, sql);
        assertNotNull(cursorId);
        for (int i = 10; i <= 100; i++) {
            List<Map<String, Object>> res = ss.next(table, cursorId, 1);
            assertNotNull(res);
            assertEquals(1, res.size());
            Map<String, Object> row = res.get(0);
            assertEquals(i, row.get("id"));
            assertEquals("first" + i, row.get("f"));
            assertEquals("last" + i, row.get("l"));
            assertEquals(i, row.get("age"));
        }
        assertNull(ss.next(table, cursorId, 1));
    }

    @Test
    public abstract void testCreateStoredProcedure();

    @Test
    public abstract void testDeleteStoredProcedure();

    // Bad test removed

    public abstract Map<String, String> getStoredProcCreations();

    public abstract void cleanUpStoredProc();

}
