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
package rapture.cli;

import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.TokenStream;

import rapture.cli.exec.ConcreteType;
import rapture.common.CallingContext;
import rapture.common.exception.RaptureException;

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
public abstract class AbstractCliParser

extends Parser

{
    private ConcreteType ct;

    private FunctionStyle fnType;

    private CallingContext ctx;

    private boolean all = false;

    private String conceptName;

    private String authority;

    private Map<String, String> currentMap = new HashMap<String, String>();

    /**
     * Create a new parser instance, pre-supplying the input token stream.
     * 
     * @param input
     *            The stream of tokens that will be pulled from the lexer
     */
    protected AbstractCliParser(TokenStream input) {
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
    protected AbstractCliParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    protected void addConfig(String name, String value) {
        if (value.startsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        currentMap.put(name, value);
    }

    /**
     * Creates the error/warning message that we need to show users/IDEs when
     * ANTLR has found a parsing error, has recovered from it and is now telling
     * us that a parsing exception occurred.
     * 
     * @param tokenNames
     *            token names as known by ANTLR (which we ignore)
     * @param e
     *            The exception that was thrown
     */
    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {

        // This is just a place holder that shows how to override this method
        //
        super.displayRecognitionError(tokenNames, e);
    }

    /**
     * Execute the current built command and put it into the current result
     * buffer
     */
    protected void execute() {
        try {
            switch (fnType) {
            case SHOW:
                ct.executeShow(this);
                break;
            default:
                break;
            }
        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    public String getConceptName() {
        return conceptName;
    }

    public CallingContext getCtx() {
        return ctx;
    }

    public String getAuthority() {
        return authority;
    }

    public boolean isAll() {
        return all;
    }

    protected void setAll(boolean value) {
        all = value;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public void setCtx(CallingContext ctx) {
        this.ctx = ctx;
    }

    protected void setFunctionType(FunctionStyle st) {
        fnType = st;
    }

    protected void setLowerLevelType(ConcreteType t) {
        ct = t;
    }

    protected void setLowerName(String name) {
        this.conceptName = name;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}
