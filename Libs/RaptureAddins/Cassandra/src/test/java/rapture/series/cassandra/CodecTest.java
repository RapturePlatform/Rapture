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
package rapture.series.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.codehaus.jackson.JsonParseException;
import org.junit.Test;

import rapture.cassandra.AstyanaxCassandraBase;
import rapture.common.SeriesValue;
import rapture.common.StructureSeriesValue;
import rapture.dsl.serfun.DecimalSeriesValue;
import rapture.dsl.serfun.LongSeriesValue;
import rapture.dsl.serfun.SeriesValueCodec;
import rapture.dsl.serfun.StringSeriesValue;
import rapture.dsl.serfun.StructureSeriesValueImpl;

public class CodecTest {
    @Test
    public void testDouble() throws JsonParseException, IOException {
        double d = 2.354;
        byte[] bytes = SeriesValueCodec.encodeValue(new DecimalSeriesValue(d, "ignore"));
        SeriesValue val = SeriesValueCodec.decode("example", bytes);
        assertTrue(val.isDouble());
        assertFalse(val.isString());
        assertEquals(d, val.asDouble(), 0.0);
    }
    
    @Test
    public void testNaN() {
        double d = Double.NaN;
        double d2 = Double.parseDouble("NaN");
        assertEquals(d, d2, 0.0);
    }
    
    @Test
    public void testLong() throws JsonParseException, IOException {
        long l = 284;
        byte[] bytes = SeriesValueCodec.encodeValue(new LongSeriesValue(l, "whatever"));
        SeriesValue val = SeriesValueCodec.decode("another example", bytes);
        assertTrue(val.isLong());
        assertFalse(val.isDouble());
        assertFalse(val.isString());
        assertEquals(l, val.asLong());
    }
    
    @Test
    public void testString() throws JsonParseException, IOException {
        String value = "simple data";
        byte[] bytes = SeriesValueCodec.encodeValue(new StringSeriesValue(value, "column"));
        SeriesValue val = SeriesValueCodec.decode("more examples", bytes);
        assertTrue(val.isString());
        assertFalse(val.isNumber());
        assertEquals(value, val.asString());
    }
    
    @Test
    public void testDecodeSimpleJson() throws JsonParseException, IOException {
        String json = "{\"dub\":0.3,\"lng\":42,\"str\":\"Hello Cruel World\"}";
        StructureSeriesValue val = StructureSeriesValueImpl.unmarshal(json, "made it up");
        assertTrue(val.isStructure());
        assertTrue(val.getFields().size() == 3);
        assertEquals("Hello Cruel World", val.getField("str").asString());
        assertEquals(0.3, val.getField("dub").asDouble(), 0.0);
        assertEquals(42, val.getField("lng").asLong());
    }
    
    @Test
    public void testDecodeNestedJson() throws JsonParseException, IOException {
        String json = "{ \"int\":5, \"nest\": { \"int\": 8 } }";

        StructureSeriesValue val = StructureSeriesValueImpl.unmarshal(json, "column");
        assertTrue(val.isStructure());
        assertEquals(5, val.getField("int").asLong());
        assertTrue(val.getField("nest").isStructure());
        assertEquals(8, val.getField("nest").asStructure().getField("int").asLong());
        assertEquals(8, val.getField("nest.int").asLong());
    }
    
    @Test
    public void testAlphaNumOnly() {
        assertTrue("Blah_Blah".matches(AstyanaxCassandraBase.ALPHA_NUM_UNDERSCRORE));
        assertFalse("Blah.Blah".matches(AstyanaxCassandraBase.ALPHA_NUM_UNDERSCRORE));
        
        //length
        assertFalse("SDFSL12345ASLITDACLEIASDFJSTDHJSF".matches(AstyanaxCassandraBase.ALPHA_NUM_UNDERSCRORE));
        assertTrue("asdfghjklqwertyuiopsdwthecosaliw".matches(AstyanaxCassandraBase.ALPHA_NUM_UNDERSCRORE));
    }
}
