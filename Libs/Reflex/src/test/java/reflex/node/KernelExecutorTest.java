package reflex.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import reflex.value.ReflexValue;

public class KernelExecutorTest {

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

    // Need to convert
    @Test
    public void testConvertValueToType() {
        List<ReflexValue> list = new ArrayList<>();
        Map<String, ReflexValue> map1 = new LinkedHashMap<>();
        map1.put("id", new ReflexValue(1));
        map1.put("name", new ReflexValue("Foo"));

        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("id", new ReflexValue(2));
        map2.put("name", new String("Bar"));

        Map<String, ReflexValue> map3 = new LinkedHashMap<>();
        map3.put("id", new ReflexValue(3));
        map3.put("name", new ReflexValue("Baz"));

        list.add(new ReflexValue(map1));
        list.add(new ReflexValue(map2));
        list.add(new ReflexValue(map3));
        Type listmap = null;
        Type maplist = null;
        Type listlistmap = null;

        for (Method man : this.getClass().getDeclaredMethods()) {
            if (man.getName().equals("ListMap")) {
                listmap = man.getGenericParameterTypes()[0];
            }
            if (man.getName().equals("MapList")) {
                maplist = man.getGenericParameterTypes()[0];
            }
            if (man.getName().equals("ListListMap")) {
                listlistmap = man.getGenericParameterTypes()[0];
            }
        }
        assertNotNull(listmap);
        Object o = KernelExecutor.convertValueToType(new ReflexValue(list), listmap);
        assertNotNull(o);

        assertNotNull(listlistmap);
        o = KernelExecutor.convertValueToType(new ReflexValue(list), listlistmap);
        assertTrue(o instanceof List);
        for (Object ob : ((List<Object>) o))
            assertNull(ob);

        List<ReflexValue> list2 = new ArrayList<>();
        list2.add(new ReflexValue(list));
        o = KernelExecutor.convertValueToType(new ReflexValue(list2), listlistmap);
        assertNotNull(o);
        assertEquals("[[{id=1, name=Foo}, {id=2, name=Bar}, {id=3, name=Baz}]]", o.toString());

        Map<String, Object> map4 = new LinkedHashMap<>();
        List<Object> l1 = new ArrayList<>();
        l1.add(new ReflexValue("A"));
        l1.add(new String("B"));
        List<Object> l2 = new ArrayList<>();
        l2.add(new ReflexValue("C"));
        l2.add(new String("D"));
        map4.put("id", new ReflexValue(l1));
        map4.put("name", new ReflexValue(l2));

        o = KernelExecutor.convertValueToType(new ReflexValue(map4), maplist);
        assertNotNull(o);
        assertEquals("{id=[A, B], name=[C, D]}", o.toString());
    }

    public void ListMap(List<Map<String, Object>> arg) {
    }

    public void MapList(Map<String, List<Object>> arg) {
    }

    public void ListListMap(List<List<Map<String, Object>>> arg) {
    }
}
