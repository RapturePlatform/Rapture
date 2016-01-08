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
package rapture.dsl.entparser;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.apache.log4j.Logger;

import rapture.common.IEntitlementsContext;
import rapture.generated.EntLexer;
import rapture.generated.EntParser;

public final class ParseEntitlementPath {
    private static Logger log = Logger.getLogger(ParseEntitlementPath.class);

    public static String getEntPath(String path, IEntitlementsContext entCtx) {
        EntLexer lexer = new EntLexer();
        lexer.setCharStream(new ANTLRStringStream(path));
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);
        EntParser parser = new EntParser(tokens);
        parser.setEctx(entCtx);

        try {
            parser.entpath();
            return parser.getPath();
        } catch (RecognitionException e) {
            // If we don't recognize it, ignore it and return the path
            log.error("Could not parse path " + path + ", error was " + e.getMessage());
        }
        return path;
    }

    private ParseEntitlementPath() {
    }
}
