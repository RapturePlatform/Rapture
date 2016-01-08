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
package reflex;

import rapture.common.api.ScriptingApi;

import reflex.cache.DefaultReflexCacheHandler;

public class StandardReflexHandler implements IReflexHandler {
    private ScriptingApi originalApi;

    public StandardReflexHandler(ScriptingApi api) {
        this.api = api;
        this.originalApi = api;
        this.apiHandler = new ReflexScriptDataHandler(api);
        this.dataHandler = this.apiHandler;
        this.scriptHandler = this.apiHandler;
        ((DummyReflexOutputHandler)outputHandler).setApi(api);
    }

    private ReflexScriptDataHandler apiHandler;
    private IReflexDataHandler dataHandler;
    private IReflexScriptHandler scriptHandler;
    private IReflexDebugHandler debugHandler = new DummyReflexDebugHandler();
    private IReflexIOHandler ioHandler = new DefaultReflexIOHandler();
    private IReflexPortHandler portHandler = new DummyPortHandler();
    private IReflexSuspendHandler suspendHandler = new NullReflexSuspendHandler();
    private IReflexOutputHandler outputHandler = new DummyReflexOutputHandler();
    private IReflexInputHandler inputHandler = new DummyReflexInputHandler();
    private IReflexCacheHandler cacheHandler = new DefaultReflexCacheHandler();

    private ScriptingApi api;

    @Override
    public ScriptingApi getApi() {
        return api;
    }

    @Override
    public IReflexDataHandler getDataHandler() {
        return dataHandler;
    }

    @Override
    public IReflexDebugHandler getDebugHandler() {
        return debugHandler;
    }

    @Override
    public IReflexIOHandler getIOHandler() {
        return ioHandler;
    }

    @Override
    public IReflexPortHandler getPortHandler() {
        return portHandler;
    }

    @Override
    public IReflexScriptHandler getScriptHandler() {
        return scriptHandler;
    }

    @Override
    public void setApi(ScriptingApi api) {
        this.api = api;
    }

    @Override
    public void setDataHandler(IReflexDataHandler reflexDataHelper) {
        dataHandler = reflexDataHelper;
    }

    @Override
    public IReflexSuspendHandler getSuspendHandler() {
        return suspendHandler;
    }

    @Override
    public void setSuspendHandler(IReflexSuspendHandler suspendHandler) {
        this.suspendHandler = suspendHandler;
    }

    @Override
    public IReflexOutputHandler getOutputHandler() {
        return outputHandler;
    }

    @Override
    public void setOutputHandler(IReflexOutputHandler outputHandler) {
        this.outputHandler = outputHandler;
    }

    @Override
    public IReflexCacheHandler getCacheHandler() {
        return cacheHandler;
    }

    @Override
    public void setCacheHandler(IReflexCacheHandler cacheHandler) {
        this.cacheHandler = cacheHandler;
    }

	@Override
	public IReflexInputHandler getInputHandler() {
		return inputHandler;
	}

	@Override
	public void setInputHandler(IReflexInputHandler inputHandler) {
		this.inputHandler = inputHandler;
	}

	@Override
	public void setIOHandler(IReflexIOHandler reflexIOHandler) {
		this.ioHandler = reflexIOHandler;
	}

    @Override
    public void switchApi(ScriptingApi api) {
        this.api = api;
        this.getDataHandler().switchApi(api);
    }

    @Override
    public void resetApi() {
        this.api = originalApi;
        this.getDataHandler().resetApi();
    }
}
