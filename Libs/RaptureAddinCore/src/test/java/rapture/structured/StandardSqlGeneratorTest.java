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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.ddlutils.Platform;
import org.apache.ddlutils.platform.SqlBuilder;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class StandardSqlGeneratorTest {

    private StandardSqlGenerator sqlGenerator;

    @Before
    public void setup() {
        sqlGenerator = new StandardSqlGenerator() {
            @Override
            public String constructRenameTableColumns(String schema, String table, Map<String, String> columns) {
                return null;
            }

            @Override
            protected SqlBuilder getDdlSqlBuilder(String schema, Platform platform) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Test
    public void testGetColumnExpressionFromList() {
        String template = "DROP COLUMN %s";
        List<String> columnNames = Arrays.asList("col1", "col2", "col3");
        String columnExpression = sqlGenerator.getColumnExpression(template, columnNames);
        String expected = "DROP COLUMN col1,DROP COLUMN col2,DROP COLUMN col3";
        assertEquals(expected, columnExpression);
    }

    @Test
    public void testGetColumnExpressionFromMap() {
        String template = "ALTER COLUMN %s TYPE %s";
        Map<String, String> columns = ImmutableMap.of("col1", "type1", "col2", "type2", "col3", "type3");
        String columnExpression = sqlGenerator.getColumnExpression(template, columns);
        String expected = "ALTER COLUMN col1 TYPE type1,ALTER COLUMN col2 TYPE type2,ALTER COLUMN col3 TYPE type3";
        assertEquals(expected, columnExpression);
    }

    @Test
    public void testConstructSelect() {
        String sql = sqlGenerator.constructSelect("public", "mytable", Arrays.asList("id", "name", "age"), "age=2", Arrays.asList("name", "age"), false, 10);
        assertEquals("SELECT id,name,age FROM public.mytable WHERE age=2 ORDER BY name,age DESC LIMIT 10 ", sql);
        sql = sqlGenerator.constructSelect("schema", "table2", null, null, null, null, -1);
        assertEquals("SELECT * FROM schema.table2 ", sql);
    }

    @Test
    public void testConstructSelectJoin() {
        String sql = sqlGenerator.constructSelectJoin(Arrays.asList("table1", "table2"), Arrays.asList("table1.x", "table2.y"),
                "table1 OUTER JOIN table2 ON table1.x=table2.y", null, null, null, -1);
        assertEquals("SELECT table1.x,table2.y FROM table1 OUTER JOIN table2 ON table1.x=table2.y ", sql);
    }
}