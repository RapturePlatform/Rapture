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
package reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import reflex.value.ReflexValue;

public class AbstractReflexDataHandlerTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testBadInputs() {
        Map<String, Object> map;
        List<ReflexValue> rvlist1 = new ArrayList<>();
        // empty list
        map = AbstractReflexDataHandler.rvListToMap(rvlist1);
        assertTrue(map.isEmpty());
        rvlist1.add(new ReflexValue("foo"));
        map = AbstractReflexDataHandler.rvListToMap(rvlist1);
        assertTrue(map.isEmpty());
    }

    @Test
    public void test() {
        List<ReflexValue> rvlist1 = new ArrayList<>();
        rvlist1.add(new ReflexValue("A"));
        rvlist1.add(new ReflexValue("B"));
        rvlist1.add(new ReflexValue("C"));
        rvlist1.add(new ReflexValue("D"));
        
        List<ReflexValue> rvlist2 = new ArrayList<>();
        rvlist2.add(new ReflexValue("A"));
        rvlist2.add(new ReflexValue("P"));
        rvlist2.add(new ReflexValue("Q"));
        rvlist2.add(new ReflexValue("R"));
        
        List<ReflexValue> rvlist3 = new ArrayList<>();
        rvlist3.add(new ReflexValue("A"));
        rvlist3.add(new ReflexValue("P"));
        rvlist3.add(new ReflexValue("C"));
        rvlist3.add(new ReflexValue("D"));
        
        List<ReflexValue> rvlist4 = new ArrayList<>();
        rvlist4.add(new ReflexValue("A"));
        rvlist4.add(new ReflexValue("B"));
        rvlist4.add(new ReflexValue("X"));
        rvlist4.add(new ReflexValue("Y"));
        
        List<ReflexValue> rvlist5 = new ArrayList<>();
        rvlist5.add(new ReflexValue("B"));
        rvlist5.add(new ReflexValue("X"));
        rvlist5.add(new ReflexValue("Y"));
        rvlist5.add(new ReflexValue("Z"));

        List<ReflexValue> rvlist = new ArrayList<>();
        rvlist.add(new ReflexValue(rvlist1));
        rvlist.add(new ReflexValue(rvlist2));
        rvlist.add(new ReflexValue(rvlist3));
        rvlist.add(new ReflexValue(rvlist4));
        rvlist.add(new ReflexValue(rvlist5));
        
        Map<String, Object> map = AbstractReflexDataHandler.rvListToMap(rvlist);
        
        List<ReflexValue> newList = AbstractReflexDataHandler.mapToRVList(map);
        assertEquals(rvlist.size(), newList.size());
        
        // newList and rvList should now be equivalent, but the ordering of lines might be different.
        // So use string comparison to verify
        List<String> elementsAsStrings = new ArrayList<>();
        for (ReflexValue rv : newList) {
            elementsAsStrings.add(rv.toString());
        }
        for (ReflexValue rv : rvlist) {
            assertTrue(elementsAsStrings.contains(rv.toString()));
        }
    }
}
