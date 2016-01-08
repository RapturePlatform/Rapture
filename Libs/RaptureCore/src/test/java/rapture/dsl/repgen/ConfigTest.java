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
package rapture.dsl.repgen;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import rapture.generated.RapGenLexer;
import rapture.generated.RapGenParser;

public class ConfigTest {
    private RapGenLexer lexer = new RapGenLexer();

    public void parseMessage(String message) throws RecognitionException {
        lexer.setCharStream(new ANTLRStringStream(message));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RapGenParser parser = new RapGenParser(tokens);
        parser.repinfo();
    }

    @Test
    public void testMongo() throws RecognitionException {
        parseMessage("VREP { test=\"one\" } using MONGO { prefix = \"test\"}");
        parseMessage("VREP { test=\"one\" } using MONGODB { prefix = \"test\"}");
    }

    @Test
    public void testMongoWithCache() throws RecognitionException {
        parseMessage("VREP { test=\"one\" } using MONGO { prefix=\"tyest\"} WITH EHCACHE { test=\"one\"}");
    }

    @Test
    public void testReadOnly() throws RecognitionException {
        parseMessage("READONLY REP { test=\"one\" } using MEMORY {}");
    }

    @Test
    public void testShadow() throws RecognitionException {
        parseMessage("VREP { test=\"one\" } using MEMORY {} SHADOW MEMORY {}");
    }

    @Test
    public void testSimple() throws RecognitionException {
        parseMessage("VREP { test=\"one\" } using REDIS { prefix = \"test\"}");
    }
}
