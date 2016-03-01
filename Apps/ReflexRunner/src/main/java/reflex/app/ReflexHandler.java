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
package reflex.app;

import rapture.common.api.ScriptingApi;
import rapture.common.client.ScriptClient;
import reflex.cache.DefaultReflexCacheHandler;
import reflex.DefaultReflexIOHandler;
import reflex.DummyPortHandler;
import reflex.DummyReflexInputHandler;
import reflex.IReflexCacheHandler;
import reflex.IReflexDataHandler;
import reflex.IReflexDebugHandler;
import reflex.IReflexHandler;
import reflex.IReflexIOHandler;
import reflex.IReflexInputHandler;
import reflex.IReflexOutputHandler;
import reflex.IReflexPortHandler;
import reflex.IReflexScriptHandler;
//import reflex.IReflexStreamHandler;
import reflex.IReflexSuspendHandler;
import reflex.NullReflexSuspendHandler;
import reflex.app.handler.ReflexDataHandler;
import reflex.app.handler.ReflexDebugHandler;
import reflex.app.handler.ReflexScriptHandler;

public class ReflexHandler implements IReflexHandler {
    private ScriptingApi api;
    private IReflexDataHandler dataHandler;
    private IReflexDebugHandler debugHandler;
    private IReflexIOHandler ioHandler = new DefaultReflexIOHandler();
    private IReflexScriptHandler scriptHandler;
    private IReflexPortHandler portHandler = new DummyPortHandler();
    private IReflexSuspendHandler suspendHandler = new NullReflexSuspendHandler();
    private IReflexOutputHandler outputHandler = new ReflexStdOutHandler();
    private IReflexInputHandler inputHandler = new DummyReflexInputHandler();
    private IReflexCacheHandler cacheHandler = new DefaultReflexCacheHandler();
    private ScriptingApi originalApi;

    public ReflexHandler(ScriptClient scriptClient, boolean b) {
        api = scriptClient;
        originalApi = api;
        scriptHandler = new ReflexScriptHandler(scriptClient);
        dataHandler = new ReflexDataHandler(scriptClient);
        if (b) {
            debugHandler = new ReflexStdOutDebugHandler();
        } else {
            debugHandler = new ReflexDebugHandler();
        }
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
    public void setApi(ScriptingApi arg0) {
        this.api = arg0;
    }

    @Override
    public void setDataHandler(IReflexDataHandler arg0) {
        this.dataHandler = arg0;
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
        this.dataHandler.switchApi(api);
    }

    @Override
    public void resetApi() {
        this.api = this.originalApi;
        this.dataHandler.resetApi();
    }

/**
    @Override
    public IReflexStreamHandler getStreamHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setStreamHandler(IReflexStreamHandler streamHandler) {
        // TODO Auto-generated method stub
        
    }
**/
}
