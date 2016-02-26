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
package rapture.repo.cassandra;

import static org.junit.Assert.*;

import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.junit.Before;
import org.junit.Test;

import rapture.generated.RapGenLexer;
import rapture.generated.RapGenParser;

import com.google.common.base.Optional;

public class ConfigParserTest {

    private static final String CONFIG1 = "REP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"rapture_bootstrap\" }";
    private static final String CONFIG2 = "NREP {} USING CASSANDRA { keyspace=\"rapture\", authority=\"someAuth\"}";
    private static final String CONFIG3 = "NREP {} USING CASSANDRA { keyspace=\"rapture\", authority=\"someAuth.\"}";
    private static final String CONFIG4 = "NREP {} USING CASSANDRA { keyspace=\"rapture\", authority=\"idp.orders\"}";
    private static final String CONFIG5 = "NREP {} USING CASSANDRA { keyspace=\"rapture\", authority=\"idp.orders.some.other.stuff\"}";
    private static final String CONFIG6 = "NREP {} USING CASSANDRA { keyspace=\"rapture\", authority=\"idp.orders.some.other.stuff\", cf=\"this\", partitionKeyPrefix=\"prefix\"}";
    private ConfigParser parser1;
    private ConfigParser parser2;
    private ConfigParser parser3;
    private ConfigParser parser4;
    private ConfigParser parser5;
    private ConfigParser parser6;

    private static Map<String, String> parseString(String configString) throws RecognitionException {
        RapGenLexer lexer = new RapGenLexer();
        lexer.setCharStream(new ANTLRStringStream(configString));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RapGenParser parser = new RapGenParser(tokens);
        parser.repinfo();
        return parser.getImplementionConfig();
    }

    @Before
    public void setUp() throws Exception {
        parser1 = new ConfigParser(parseString(CONFIG1));
        parser1.parse();
        parser2 = new ConfigParser(parseString(CONFIG2));
        parser2.parse();
        parser3 = new ConfigParser(parseString(CONFIG3));
        parser3.parse();
        parser4 = new ConfigParser(parseString(CONFIG4));
        parser4.parse();
        parser5 = new ConfigParser(parseString(CONFIG5));
        parser5.parse();
        parser6 = new ConfigParser(parseString(CONFIG6));
        parser6.parse();

    }

    @Test
    public void testGetKeyspaceName() throws Exception {
        assertEquals("rapture", parser1.getKeyspaceName());
        assertEquals("rapture", parser2.getKeyspaceName());
        assertEquals("rapture", parser3.getKeyspaceName());
        assertEquals("rapture", parser4.getKeyspaceName());
        assertEquals("rapture", parser5.getKeyspaceName());
        assertEquals("rapture", parser6.getKeyspaceName());
    }

    @Test
    public void testGetCfName() throws Exception {
        assertEquals("rapture_bootstrap", parser1.getCfName());
        assertEquals("someAuth", parser2.getCfName());
        assertEquals("someAuth", parser3.getCfName());
        assertEquals("idp", parser4.getCfName());
        assertEquals("idp", parser5.getCfName());
        assertEquals("this", parser6.getCfName());
    }

    @Test
    public void testGetRowPrefix() throws Exception {
        assertEquals(Optional.<String>absent(), parser1.getpKeyPrefix());
        assertEquals(Optional.<String>absent(), parser2.getpKeyPrefix());
        assertEquals(Optional.<String>absent(), parser3.getpKeyPrefix());
        assertEquals("orders", parser4.getpKeyPrefix().get());
        assertEquals("orders.some.other.stuff", parser5.getpKeyPrefix().get());
        assertEquals("prefix", parser6.getpKeyPrefix().get());
    }
}