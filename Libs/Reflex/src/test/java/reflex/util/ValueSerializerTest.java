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
package reflex.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rapture.common.RaptureFolderInfo;
import reflex.value.ReflexValue;

public class ValueSerializerTest {
    @Test
    public void testComplex() throws ClassNotFoundException {
        RaptureFolderInfo fi = new RaptureFolderInfo();
        fi.setFolder(true);
        fi.setName("Hello");
        ReflexValue one = new ReflexValue(fi);
        RaptureFolderInfo fi2 = new RaptureFolderInfo();
        fi2.setFolder(false);
        fi2.setName("Goodbye");
        ReflexValue two = new ReflexValue(fi2);
        List<ReflexValue> items = new ArrayList<ReflexValue>();
        items.add(one);
        items.add(two);
        ReflexValue toTest = new ReflexValue(items);
        ReflexValue back = testTwoWay(toTest);
        
        assertTrue(back.isList());
        for(ReflexValue v : back.asList()) {
            assertTrue(v.isComplex());
            RaptureFolderInfo rfi = (RaptureFolderInfo) v.asObject();
            System.out.println(rfi.getName());
        }
    }
    
    @Test
    public void testSimple() throws ClassNotFoundException {
        ReflexValue back = testTwoWay(new ReflexValue(5));
        assertTrue(back.isNumber());
        assertTrue(back.asInt() == 5);
    }
    
    @Test
    public void testSimple2() throws ClassNotFoundException {
        ReflexValue back = testTwoWay(new ReflexValue("Hello world"));
        assertTrue(back.isString());
        assertTrue(back.asString().equals("Hello world"));
    }
    
    private ReflexValue testTwoWay(ReflexValue toTest) throws ClassNotFoundException {
        String serialVersion = ValueSerializer.serialize(toTest);
        System.out.println("Serial version = " + serialVersion);
        
        ReflexValue back = ValueSerializer.deserialize(serialVersion);
        return back;
        
    }
}
