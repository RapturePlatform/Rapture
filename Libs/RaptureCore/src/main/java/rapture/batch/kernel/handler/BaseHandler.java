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
package rapture.batch.kernel.handler;

import java.util.Map;

import org.apache.log4j.Logger;

import rapture.batch.kernel.ICommandHandler;

public abstract class BaseHandler implements ICommandHandler {
    private static Logger log = Logger.getLogger(BaseHandler.class);

    protected String getWithDefault(Map<String, String> params, String paramName, String def) {
        if (!params.containsKey(paramName)) {
            return def;
        }
        return params.get(paramName);
    }

    protected boolean validateParamsPresent(Map<String, String> params, String[] paramsToCheck) {
        boolean ret = true;
        for (String param : paramsToCheck) {
            if (!params.containsKey(param)) {
                ret = false;
                log.error("Parameter named " + param + " not present");
            }
        }
        return ret;
    }

}
