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
package rapture.dsl;

import java.util.HashMap;
import java.util.Map;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;

public abstract class BaseDslParser extends Parser {
    private Map<String, String> currentMap = new HashMap<String, String>();
    private ConfigDef processorConfig;
    private ConfigDef cacheConfig;
    private ConfigDef shadowConfig;

    private Token cacheToken;
    private Token storeToken;
    private Token shadowToken;

    private ConfigDef config;

    public Map<String, String> getImplementionConfig() {
        return config.getConfig();
    }

    public String getImplementationName() {
        return config.getName();
    }

    public int getStoreType() {
        return storeToken.getType();
    }

    /**
     * Create a new parser instance, pre-supplying the input token stream.
     * 
     * @param input
     *            The stream of tokens that will be pulled from the lexer
     */
    protected BaseDslParser(TokenStream input) {
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
    protected BaseDslParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    protected void addCacheConfig(String name) {
        cacheConfig = getNewConfigDefFromCurrent(name);
    }

    protected void addConfig(String name) {
        config = getNewConfigDefFromCurrent(name);
    }

    protected void addConfig(String name, String value) {
        if (value.startsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        currentMap.put(name, value);
    }

    protected void addProcessorConfig(String name) {
        processorConfig = getNewConfigDefFromCurrent(name);
    }

    protected void addShadowConfig(String name) {
        shadowConfig = getNewConfigDefFromCurrent(name);
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

    public Token getCache() {
        return cacheToken;
    }

    public ConfigDef getCacheConfig() {
        return cacheConfig;
    }

    public ConfigDef getConfig() {
        return config;
    }

    private ConfigDef getNewConfigDefFromCurrent(String name) {
        ConfigDef ret = new ConfigDef();
        ret.setName(name);
        ret.setConfig(currentMap);
        currentMap = new HashMap<String, String>();
        return ret;
    }

    public ConfigDef getProcessorConfig() {
        return processorConfig;
    }

    public Token getShadow() {
        return shadowToken;
    }

    public ConfigDef getShadowConfig() {
        return shadowConfig;
    }

    public Token getStore() {
        return storeToken;
    }

    public void setCache(Token c) {
        cacheToken = c;
    }

    public void setConfig(ConfigDef config) {
        this.config = config;
    }

    public void setProcessorConfig(ConfigDef processorConfig) {
        this.processorConfig = processorConfig;
    }

    public void setShadow(Token shadowToken) {
        this.shadowToken = shadowToken;
    }

    public void setStore(Token s) {
        storeToken = s;
    }

}
