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
package rapture.common.model;

import rapture.common.ErrorWrapper;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.impl.jackson.JsonContent;

public class GeneralResponse {
    private JsonContent response;
    private boolean inError = false;
    private boolean success = true;

    public GeneralResponse() {

    }

    public GeneralResponse(ErrorWrapper ew, boolean b) {
        setInError(b);
        response = new JsonContent(JacksonUtil.jsonFromObject(ew));
    }

    public GeneralResponse(Object resp) {
        response = new JsonContent(JacksonUtil.jsonFromObject(resp));
    }

    public JsonContent getResponse() {
        return response;
    }

    public boolean isInError() {
        return inError;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setInError(boolean inError) {
        this.inError = inError;
        this.success = !inError;
    }

    public void setResponse(JsonContent response) {
        this.response = response;
    }

    public void setSuccess(boolean success) {
        this.success = success;
        this.inError = !success;
    }

}
