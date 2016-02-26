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
package rapture.script.reflex;

import rapture.common.CallingContext;
import rapture.common.api.ScriptingApi;
import rapture.kernel.script.KernelScript;
import reflex.cache.DefaultReflexCacheHandler;
import reflex.DefaultReflexIOHandler;
import reflex.DummyPortHandler;
import reflex.DummyReflexDebugHandler;
import reflex.DummyReflexOutputHandler;
import reflex.IReflexCacheHandler;
import reflex.IReflexDataHandler;
import reflex.IReflexDebugHandler;
import reflex.IReflexHandler;
import reflex.IReflexIOHandler;
import reflex.IReflexInputHandler;
import reflex.IReflexOutputHandler;
import reflex.IReflexPortHandler;
import reflex.IReflexScriptHandler;
import reflex.IReflexSuspendHandler;
import reflex.NullReflexInputHandler;
import reflex.NullReflexSuspendHandler;

public class ReflexHandler implements IReflexHandler {
    private IReflexDataHandler dataHandler;
    private IReflexDebugHandler debugHandler;
    private IReflexIOHandler ioHandler;
    private IReflexPortHandler portHandler;
    private IReflexScriptHandler scriptHandler;
    private IReflexSuspendHandler suspendHandler;
    private IReflexOutputHandler outputHandler;
    private IReflexCacheHandler cacheHandler;
    private IReflexInputHandler inputHandler;

    public void setOutputHandler(IReflexOutputHandler outputHandler) {
        this.outputHandler = outputHandler;
    }

    private ScriptingApi api;

    public ReflexHandler(CallingContext ctx) {
        dataHandler = new ReflexDataHelper(ctx);
        debugHandler = new DummyReflexDebugHandler();
        ioHandler = new DefaultReflexIOHandler();
        scriptHandler = new ReflexIncludeHelper(ctx);
        portHandler = new DummyPortHandler();
        suspendHandler = new NullReflexSuspendHandler();
        outputHandler = new DummyReflexOutputHandler();
        cacheHandler = new DefaultReflexCacheHandler();
        inputHandler = new NullReflexInputHandler();
        KernelScript kh = new KernelScript();
        kh.setCallingContext(ctx);
        api = kh;
        originalApi = api;
    }

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
        this.dataHandler = reflexDataHelper;
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

	private ScriptingApi originalApi;
	
    @Override
    public void switchApi(ScriptingApi api) {
        this.api = api;
        this.dataHandler.switchApi(api);
    }

    @Override
    public void resetApi() {
        this.api = originalApi;
        this.dataHandler.resetApi();
    }

}
