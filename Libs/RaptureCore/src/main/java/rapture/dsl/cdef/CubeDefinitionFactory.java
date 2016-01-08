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
package rapture.dsl.cdef;

import java.net.HttpURLConnection;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.log4j.Logger;

import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.exception.RaptureExceptionFormatter;
import rapture.generated.CubeDefLexer;
import rapture.generated.CubeDefParser;
import rapture.generated.CubeDefParser.cubeSchemaDefinition_return;

/**
 * Given a string, convert to an IndexDefinition
 * 
 * @author alanmoore
 * 
 */
public class CubeDefinitionFactory {
    private static final Logger log = Logger.getLogger(CubeDefinitionFactory.class);
    public static CubeSchemaDefinition getDefinition(String config) {
        CubeDefLexer lexer = new CubeDefLexer();
        log.debug("Config = "+config);
        lexer.setCharStream(new ANTLRStringStream(config));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CubeDefParser parser = new CubeDefParser(tokens);
        try {
            cubeSchemaDefinition_return ret = parser.cubeSchemaDefinition();
            return ret.def;
        } catch (RecognitionException e) {
            RaptureException raptException = RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Bad cube schema definition");
            log.error(RaptureExceptionFormatter.getExceptionMessage(raptException, e));
            throw raptException;
        }
    }
}
