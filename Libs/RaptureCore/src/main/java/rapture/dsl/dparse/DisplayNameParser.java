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
package rapture.dsl.dparse;

import java.net.HttpURLConnection;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import rapture.common.exception.RaptureExceptionFactory;
import rapture.generated.DParseLexer;
import rapture.generated.DParseParser;
import rapture.generated.DParseParser.displayname_return;

public final class DisplayNameParser {
    public static displayname_return parseDisplayName(String displayName) {
        DParseLexer lexer = new DParseLexer();
        lexer.setCharStream(new ANTLRStringStream(displayName));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DParseParser parser = new DParseParser(tokens);
        displayname_return ret;
        try {
            ret = parser.displayname();
            return ret;
        } catch (RecognitionException e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "Could not parse displayname " + e.getMessage());
        }
    }

    private DisplayNameParser() {
    }

}
