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
package rapture.json;

import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;

import rapture.generated.JSONLexer;
import rapture.generated.JSONParser;
import rapture.generated.JSONTree;

import java.io.IOException;

public abstract class AbstractJSONTests {

    protected void testViaStringTree(String source, String expected) throws IOException, RecognitionException {
        JSONParser parser = createParser(source);
        ParserRuleReturnScope result = parser.value();
        String st = toStringTree(result);
        System.out.println(st);
        assert st.equals(expected) : "Expected " + expected + ", but found " + st;
    }

    protected JSONTree createTreeParser(String testString) throws IOException, RecognitionException {
        JSONLexer lexer = createLexer(testString);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(tokens);
        // Invoke the program rule in get return value
        JSONParser.value_return r = parser.value();
        CommonTree t = (CommonTree) r.getTree();
        // Walk resulting tree; create treenode stream first
        CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
        // AST nodes have payloads that point into token stream
        nodes.setTokenStream(tokens);
        // Create a tree Walker attached to the nodes stream
        return new JSONTree(nodes);
    }

    protected JSONParser createParser(String testString) throws IOException {
        JSONLexer lexer = createLexer(testString);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(tokens);
        return parser;
    }

    private JSONLexer createLexer(String testString) throws IOException {
        CharStream stream = new ANTLRStringStream(testString);
        JSONLexer lexer = new JSONLexer(stream);
        return lexer;
    }

    private String toStringTree(ParserRuleReturnScope result) {
        assert result != null;
        String st = ((Tree) result.getTree()).toStringTree();
        return st;
    }
}
