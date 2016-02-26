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
package rapture.util;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, ConfigRetriever.class })
public class ConfigRetrieverTest {

    
    @Test
    public void testGetSettingIntReturnDefault(){
        Map<String, String> config = new HashMap<String, String>();
        Assert.assertEquals(5000, ConfigRetriever.getSettingInt(config, "MAXELEMS", 5000));
    }
    
    @Test
    public void testGetSettingLongReturnDefault(){
        Map<String, String> config = new HashMap<String, String>();
        Assert.assertEquals(5000, ConfigRetriever.getSettingLong(config, "MAXELEMS", 5000));
    }
    
    @Test
    public void testGetSettingIntInMap(){
        Map<String, String> config = new HashMap<String, String>();
        config.put("MAXELEMS", "2500");
        Assert.assertEquals(2500, ConfigRetriever.getSettingInt(config, "MAXELEMS", 5000));
    }
    
    @Test
    public void testGetSettingLongInMap(){
        Map<String, String> config = new HashMap<String, String>();
        config.put("MAXELEMS", "2500");
        Assert.assertEquals(2500, ConfigRetriever.getSettingLong(config, "MAXELEMS", 5000));
    }
    
    @Test
    public void testGetSettingIntReturnDefaultFromException(){
        Map<String, String> config = new HashMap<String, String>();
        config.put("MAXELEMS", "%^@E%^@");
        Assert.assertEquals(1250, ConfigRetriever.getSettingInt(config, "MAXELEMS", 1250));
    }
    
    @Test
    public void testGetSettingLongReturnDefaultFromException(){
        Map<String, String> config = new HashMap<String, String>();
        config.put("MAXELEMS", "%^@E%^@");
        Assert.assertEquals(1250, ConfigRetriever.getSettingLong(config, "MAXELEMS", 1250));
    }
    
    @Test
    public void testGetSetting() throws Exception {
        Map<String, String> primaryMap = new HashMap<String, String>();
        primaryMap.put("TESTKEY1", "TESTVALUE1");
        Assert.assertEquals("TESTVALUE1", ConfigRetriever.getSetting(primaryMap, "TESTKEY1", null));
    }

    @Test
    public void testGetPropertyKeySetting() throws Exception {
        System.setProperty("TESTKEY2", "TESTVALUE2");
        Assert.assertEquals("TESTVALUE2", ConfigRetriever.getSetting(null, "TESTKEY2", null));
        System.clearProperty("TESTKEY2");
    }

    @Test
    public void testGetPropertyNullDefaultKeySetting() throws Exception {
        mockStaticPartial(System.class, "getProperty");
        expect(System.getProperty("TESTKEY3")).andStubReturn(null);
        replay(System.class);
        Assert.assertEquals("TESTVALUE_DEF", ConfigRetriever.getSetting(null, "TESTKEY3", "TESTVALUE_DEF"));
        reset(System.class);
    }

    @Test
    public void testGetEnvKeySetting() throws Exception {
        mockStaticPartial(System.class, "getProperty");
        expect(System.getProperty("TESTKEY10")).andStubReturn("TESTVALUE10");
        replay(System.class);

        Assert.assertEquals("TESTVALUE10", ConfigRetriever.getSetting(null, "TESTKEY10", null));
        reset(System.class);
    }

    
}
