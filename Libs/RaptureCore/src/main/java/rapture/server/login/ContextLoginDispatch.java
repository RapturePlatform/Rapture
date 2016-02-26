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
package rapture.server.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import rapture.common.DispatchReturn;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.ContextLoginData;
import rapture.common.model.ContextResponseData;
import rapture.common.model.GeneralResponse;
import rapture.kernel.Kernel;
import rapture.server.BaseDispatcher;

public class ContextLoginDispatch extends BaseDispatcher {
    private static Logger log = Logger.getLogger(ContextLoginDispatch.class);

    @Override
    public DispatchReturn dispatch(String params, HttpServletRequest req, HttpServletResponse resp) {
        try {
            ContextLoginData data = JacksonUtil.objectFromJson(params, ContextLoginData.class);
            log.info("Executing ContextLogin with user " + data.getUser());
            // Here we need to get the general login singleton, which will
            // validate that the user exists,
            // generate a session, store that session and return the id
            ContextResponseData rsp = Kernel.getLogin().getContextForUser(data.getUser());
            return new DispatchReturn(JacksonUtil.jsonFromObject(new GeneralResponse(rsp)));
        } catch (RaptureException e) {
            return new DispatchReturn(error(e));
        }
    }

}
