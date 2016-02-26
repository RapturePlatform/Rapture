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

import org.apache.log4j.Logger;

import rapture.common.api.ScriptingApi;
import rapture.common.impl.jackson.JacksonUtil;

public class DummyReflexOutputHandler implements IReflexOutputHandler {
    private static final Logger logger = Logger.getLogger(DummyReflexOutputHandler.class);

    @Override
    public void printLog(String text) {
        printOutput("Log: " + text);
    }

    @Override
    public void printOutput(String text) {
        logger.info(text);
        // per Alan: Comment out that line for now.
        // It's used for websockets code which we can do in a different way in the future.
//       if (api != null && api.getPipeline() != null) {
//           api.getPipeline().publishTopicMessage("main", "raptureTopic", "reflexOut", text);
//       }
    }

    @Override
    public boolean hasCapability() {
        return false;
    }

    private ScriptingApi api;
    
    public void setApi(ScriptingApi api) {
        this.api = api;
    }
}
