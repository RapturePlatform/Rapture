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
package rapture.kernel.cache.config;

import static org.junit.Assert.assertEquals;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

public class DocRepoConfigFactoryTest {

    @Test
    public void testMem() throws RecognitionException {
        String template = "REP {} USING MEMORY {}";
        String config = new DocRepoConfigFactory().createConfig(template, "myAuth");
        assertEquals(template, config);

        config = new DocRepoConfigFactory().createConfig(template, "idp.order");
        assertEquals(template, config);

    }

    @Test
    public void testMongo() throws RecognitionException {
        String template = "NREP {} USING MONGODB {prefix=\"\"}";
        String config = new DocRepoConfigFactory().createConfig(template, "myAuth");
        assertEquals("NREP {} USING MONGODB {prefix=\"myAuth\"}", config);

        config = new DocRepoConfigFactory().createConfig(template, "idp.order");
        assertEquals("NREP {} USING MONGODB {prefix=\"idp.order\"}", config);

    }

    @Test
    public void testCassandra() throws RecognitionException {
        String template = "NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"%s\", rowPrefix=\"%s\"}";
        String config = new DocRepoConfigFactory().createConfig(template, "myAuth");
        assertEquals("NREP {} USING CASSANDRA { keyspace=\"rapture\", cf=\"myAuth\", rowPrefix=\"\"}", config);

        template = "NREP {} USING CASSANDRA { keyspace=\"rapture\", authority=\"%s\"}";
        config = new DocRepoConfigFactory().createConfig(template, "idp.order");
        assertEquals("NREP {} USING CASSANDRA { keyspace=\"rapture\", authority=\"idp.order\"}", config);

    }
}