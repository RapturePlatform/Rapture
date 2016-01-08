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
package rapture.util;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, PropertyConfigRetriever.class, EnvConfigRetriever.class, DefaultConfigRetriever.class })
public class DefaultConfigRetrieverTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetPropertyFromSystem() {
        mockStaticPartial(System.class, "getProperty");
        DefaultConfigRetriever dcr = new DefaultConfigRetriever();
        
        expect(System.getProperty("TESTKEYFROMsystem","DEFAULTVAL")).andStubReturn("TESTVALUEFROMsystem");
        expect(System.getProperty("TESTKEYFROMsystem")).andStubReturn("TESTVALUEFROMsystem");
        replayAll();
        
        Assert.assertEquals("TESTVALUEFROMsystem", dcr.getProperty("TESTKEYFROMsystem", "DEFAULTVAL"));
        
        resetAll();

    }
  
    @Test
    public void testGetPropertyFromEnv() {
        mockStatic(System.class);
        PropertyConfigRetriever mockConfigRet = createPartialMock(PropertyConfigRetriever.class, "hasProperty");
        DefaultConfigRetriever dcr = new DefaultConfigRetriever();
        
        expect(System.getProperty("TESTKEYFROMENV2")).andStubReturn(null);
        expect(System.getenv("TESTKEYFROMENV2")).andStubReturn("TESTVALUEFROMENV2");
        expect(mockConfigRet.hasProperty("TESTKEYFROMENV2")).andStubReturn(true);
        
        replayAll();
        Assert.assertEquals("TESTVALUEFROMENV2", dcr.getProperty("TESTKEYFROMENV2", "DEFAULTENVVAL2"));
        
        resetAll();
    }
    
    @Test
    public void testGetPropertyInnerDefaultFromEnv() {
        mockStatic(System.class);
        PropertyConfigRetriever mockConfigRet = createPartialMock(PropertyConfigRetriever.class, "hasProperty");
        EnvConfigRetriever mockEnvRet = createPartialMock(EnvConfigRetriever.class, "hasProperty");
        DefaultConfigRetriever dcr = new DefaultConfigRetriever();
        
        expect(System.getProperty("TESTKEYFROMENV30")).andStubReturn(null);
        expect(System.getenv("TESTKEYFROMENV30")).andStubReturn(null);
        expect(mockConfigRet.hasProperty("TESTKEYFROMENV30")).andStubReturn(false);
        expect(mockEnvRet.hasProperty("TESTKEYFROMENV30")).andStubReturn(true);
        expect(mockEnvRet.getProperty("TESTKEYFROMENV30", "DEFAULTENVVAL30")).andStubReturn(null);
        
        replayAll();
        Assert.assertEquals("DEFAULTENVVAL30", dcr.getProperty("TESTKEYFROMENV30", "DEFAULTENVVAL30"));
        
        resetAll();
    }
    
    @Test
    public void testGetPropertyDefaultFromEnv() {
        mockStaticPartial(System.class, "getProperty");
        DefaultConfigRetriever dcr = new DefaultConfigRetriever();
        
        expect(System.getProperty("TESTKEYFROMENV1")).andReturn(null);
        replayAll();
        Assert.assertEquals("DEFAULTENVVAL1", dcr.getProperty("TESTKEYFROMENV1", "DEFAULTENVVAL1"));
        
        resetAll();
    }
    
    @Test
    public void testHasPropertyFromEnv() {
        mockStatic(System.class);
        
        DefaultConfigRetriever dcr = new DefaultConfigRetriever();
        expect(System.getenv("TESTKEYFROMENV20")).andStubReturn("TESTVALUEFROMENV20");
        expect(System.getProperty("TESTKEYFROMENV20")).andStubReturn(null);
        replayAll();
        
        Assert.assertTrue(dcr.hasProperty("TESTKEYFROMENV20"));
        resetAll();
    }
    
    @Test
    public void testHasPropertyFromSystem() {
        System.setProperty("TESTKEYFROMENV10", "TESTVALUEFROMENV10");
        DefaultConfigRetriever dcr = new DefaultConfigRetriever();
        Assert.assertTrue(dcr.hasProperty("TESTKEYFROMENV10"));
        System.clearProperty("TESTKEYFROMENV10");
    }
  
}
