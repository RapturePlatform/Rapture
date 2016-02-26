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
package rapture.plugin.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import rapture.plugin.install.TopoSort;

public class TopoSortTest {

    @Test
    public void testABC() {
        TopoSort<String> topo = new TopoSort<String>();
        topo.addConstraint("A", "B");
        topo.addConstraint("B", "C");
        Set<String> entries = topo.getEntries();
        Set<TopoSort.Constraint<String>> constraints = topo.getConstraints();
        List<String> result = topo.sort();
        assertEquals(3, entries.size());
        assertTrue(entries.contains("A"));
        assertTrue(entries.contains("B"));
        assertTrue(entries.contains("C"));
        assertEquals(2, constraints.size());
        verify(entries, constraints, result);
    }
    
    @Test
    public void testMultiroot() {
        TopoSort<String> topo = new TopoSort<String>();
        topo.addConstraint("D", "B");
        topo.addConstraint("A", "C");
        topo.addConstraint("A", "B");
        verify(topo.getEntries(), topo.getConstraints(), topo.sort());
    }
    
    @Test
    public void testDisjoint() {
        TopoSort<String> topo = new TopoSort<String>();
        topo.addConstraint("D", "B");
        topo.addConstraint("A", "C");
        verify(topo.getEntries(), topo.getConstraints(), topo.sort());
    }
    
    @Test
    public void testUnconstrained() {
        TopoSort<String> topo = new TopoSort<String>();
        topo.addConstraint("D", "B");
        topo.addEntry("E");
        topo.addConstraint("A", "C");
        verify(topo.getEntries(), topo.getConstraints(), topo.sort());
    }
    
    @Test
    public void testCycle() {
        TopoSort<String> topo = new TopoSort<String>();
        topo.addConstraint("A", "B");
        topo.addConstraint("B", "C");
        topo.addConstraint("C", "A");
        try {
            topo.sort();
        } catch (Exception ex) {
            return; //Expected
        }
        fail();
    }
    
    @Test
    public void testDuplicate() {
        TopoSort<String> topo = new TopoSort<String>();
        topo.addConstraint("A", "B");
        topo.addConstraint("A", "B");
        verify(topo.getEntries(), topo.getConstraints(), topo.sort());
    }
    
    @Test
    public void testComplex() {
        TopoSort<String> topo = new TopoSort<String>();
        topo.addConstraint("A", "B");
        topo.addConstraint("C", "A");
        topo.addConstraint("Z", "E");
        topo.addConstraint("A", "Z");
        topo.addConstraint("A", "B");
        topo.addConstraint("X", "B");
        topo.addConstraint("Y", "B");
        topo.addConstraint("W", "S");
        topo.addConstraint("S", "Q");
        topo.addConstraint("T", "Q");
        topo.addConstraint("B", "Q");
        topo.addConstraint("A", "Q");
        topo.addConstraint("L", "M");
        topo.addConstraint("Q", "L");
        verify(topo.getEntries(), topo.getConstraints(), topo.sort());
    }

    private static <T> void verify(Set<T> entries, Set<TopoSort.Constraint<T>> constraints, List<T>result) {
        assertEquals(entries.size(), result.size());
        Map<T,Integer> key2index = new HashMap<T,Integer>();
        int index = 0;
        for(T t: result) {
            key2index.put(t, index);
            index++;
        }
        for(TopoSort.Constraint<T> c: constraints) {
            assertTrue(key2index.get(c.getBefore()) < key2index.get(c.getAfter()));
        }
        for(T t: entries) {
            assertTrue(key2index.get(t) >= 0);
        }
    }
}
