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
package reflex;

import java.util.ArrayList;
import java.util.List;

import rapture.common.api.ScriptingApi;

public class CombinedOutputHandler implements IReflexOutputHandler {
	private List<IReflexOutputHandler> handlers = new ArrayList<IReflexOutputHandler>();
	@Override
	public boolean hasCapability() {
		return true;
	}

	@Override
	public void printLog(String text) {
		for(IReflexOutputHandler h : handlers) {
			h.printLog(text);
		}
	}

	@Override
	public void printOutput(String text) {
		for(IReflexOutputHandler h : handlers) {
			h.printOutput(text);
		}
	}

	public void addHandler(IReflexOutputHandler handler) {
		handlers.add(handler);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getHandlerLike(Class<T> clazz) {
		for(IReflexOutputHandler handler : handlers) {
			if (handler.getClass() == clazz) {
				return (T) handler;
				
			}
		}
		return null;
	}

    @Override
    public void setApi(ScriptingApi api) {
       for(IReflexOutputHandler h : handlers) {
           h.setApi(api);
       }
    }

}
