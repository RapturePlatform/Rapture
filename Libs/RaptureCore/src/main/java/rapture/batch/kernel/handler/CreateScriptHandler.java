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
package rapture.batch.kernel.handler;

import java.io.OutputStream;
import java.util.Map;

import rapture.common.CallingContext;
import rapture.common.RaptureScriptLanguage;
import rapture.common.RaptureScriptPurpose;
import rapture.kernel.Kernel;

public class CreateScriptHandler extends BaseHandler {

    @Override
    public void execute(CallingContext ctx, Map<String, String> params, OutputStream output) {
        if (validateParamsPresent(params, new String[] { HandlerConstants.NAME, HandlerConstants.AUTHORITY, HandlerConstants.SCRIPT, HandlerConstants.LANGUAGE,
                HandlerConstants.PURPOSE })) {

            Kernel.getScript().createScript(ctx, "//" + params.get(HandlerConstants.AUTHORITY) + "/" + params.get(HandlerConstants.NAME),
                    RaptureScriptLanguage.valueOf(params.get(HandlerConstants.LANGUAGE)), RaptureScriptPurpose.valueOf(params.get(HandlerConstants.PURPOSE)),
                    params.get(HandlerConstants.SCRIPT));
        }
    }

}
