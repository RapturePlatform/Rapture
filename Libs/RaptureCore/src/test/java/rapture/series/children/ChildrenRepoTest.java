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
package rapture.series.children;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;

import rapture.common.SeriesValue;

public class ChildrenRepoTest {

    private Queue<String> children;
    private Queue<String> parents;

    @Before
    public void setUp() throws Exception {
        children = new LinkedList<String>();
        parents = new LinkedList<String>();
    }

    @Test
    public void testParentage1() throws Exception {
        testABCS1Parentage("a/b/c/s1");
        testABCS1Parentage("/a/b/c///s1//");
        testABCS1Parentage("a/b/c///s1");
        testABCS1Parentage("/a/b/c///s1");
        testABCS1Parentage("//a/b/c///s1");
        testABCS1Parentage("/a/b/c///s1/");
    }

    private void testABCS1Parentage(String input) throws Exception {
        ChildrenRepo repo = getRepo();

        repo.registerParentage(input);
        assertEquals("", parents.poll());
        assertEquals("a", parents.poll());
        assertEquals("a/b", parents.poll());
        assertEquals("a/b/c", parents.poll());
        assertTrue(parents.isEmpty());

        assertEquals("a", children.poll());
        assertEquals("b", children.poll());
        assertEquals("c", children.poll());
        assertEquals("s1", children.poll());
        assertTrue(children.isEmpty());
    }

    private ChildrenRepo getRepo() {
        return new ChildrenRepo() {

            @Override
            public List<SeriesValue> getPoints(String key) {
                return null;
            }

            @Override
            public boolean addPoint(String key, SeriesValue value) {
                parents.add(ChildKeyUtil.fromRowKey(key));
                String columnName = value.getColumn();
                if (ChildKeyUtil.isColumnFolder(columnName)) {
                    children.add(ChildKeyUtil.fromColumnFolder(columnName));
                } else {
                    children.add(ChildKeyUtil.fromColumnFile(columnName));
                }
                return true;
            }

            @Override
            public boolean dropPoints(String key, List<String> points) {
                return false;
            }

            @Override
            public void dropRow(String key) {
                
            }
        };
    }
}
