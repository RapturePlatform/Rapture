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
package rapture.server.web.servlet;

import rapture.common.api.ScriptingApi;
import rapture.kernel.ContextFactory;
import rapture.kernel.script.KernelScript;
import rapture.script.reflex.ReflexDataHelper;
import reflex.BadReflexSuspendHandler;
import reflex.cache.DefaultReflexCacheHandler;
import reflex.DummyReflexDebugHandler;
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
import reflex.NullReflexIOHandler;
import reflex.NullReflexInputHandler;

public class ReflexScriptPageHandler implements IReflexHandler {

    private StringBuilder output = new StringBuilder();
    private IReflexDebugHandler debugHandler = new DummyReflexDebugHandler();
    private IReflexSuspendHandler suspendHandler = new BadReflexSuspendHandler();

    private IReflexDataHandler dataHandler = new ReflexDataHelper(ContextFactory.ADMIN);
    private IReflexOutputHandler outputHandler = new ReflexOutputHandler(output);
    private IReflexInputHandler inputHandler = new NullReflexInputHandler();
    private IReflexCacheHandler cacheHandler = new DefaultReflexCacheHandler();
    private IReflexScriptHandler scriptHandler = null;
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

    public String getOutput() {
        return output.toString();
    }

    @Override
    public IReflexPortHandler getPortHandler() {
        return null;
    }

    @Override
    public IReflexScriptHandler getScriptHandler() {
       return scriptHandler;
    }

    @Override
    public void setApi(ScriptingApi api) {
        this.api = api;
        this.originalApi = api;
    }

    @Override
    public void setDataHandler(IReflexDataHandler reflexDataHelper) {
        dataHandler = reflexDataHelper;
    }

    public void setScriptApi(KernelScript scriptApi) {
        setApi(scriptApi);
        this.scriptHandler = new ScriptHandler(scriptApi);
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

	private IReflexIOHandler ioHandler = new NullReflexIOHandler();
	
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
