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
package rapture.dsl.qgen;

import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.TokenStream;

import rapture.dsl.BaseDslParser;

/**
 * The super class of the generated parser. It is extended by the generated code
 * because of the superClass optoin in the .g file.
 * 
 * This class contains any helper functions used within the parser grammar
 * itself, as well as any overrides of the standard ANTLR Java runtime methods,
 * such as an implementation of a custom error reporting method, symbol table
 * populating methods and so on.
 * 
 * 
 */
public abstract class AbstractQParser

extends BaseDslParser

{

    /**
     * Create a new parser instance, pre-supplying the input token stream.
     * 
     * @param input
     *            The stream of tokens that will be pulled from the lexer
     */
    protected AbstractQParser(TokenStream input) {
        super(input);
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream and
     * the shared state.
     * 
     * This is only used when a grammar is imported into another grammar, but we
     * must supply this constructor to satisfy the super class contract.
     * 
     * @param input
     *            The stream of tokesn that will be pulled from the lexer
     * @param state
     *            The shared state object created by an interconnectd grammar
     */
    protected AbstractQParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    private String instanceName = "default";

    protected void setInstance(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstance() {
        return instanceName;
    }
}
