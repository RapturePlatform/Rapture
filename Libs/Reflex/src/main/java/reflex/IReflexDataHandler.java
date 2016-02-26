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

import java.util.List;
import java.util.Map;

import rapture.common.RaptureURI;
import rapture.common.api.ScriptingApi;
import reflex.value.ReflexValue;

import com.google.common.net.MediaType;

/**
 * A ReflexDataHandler is passed by the program invoking the walk, and actions
 * like pull and push that interact with Rapture Data will invoke the methods on
 * this interface to perform their action.
 * 
 * @author amkimian
 * 
 */
public interface IReflexDataHandler extends ICapability {
    List<Map<String, Object>> batchPullData(List<String> displayNames);

    @Deprecated
    Map<String, Object> pullData(String displayName);

    @Deprecated
    String pushData(String displayName, Map<String, Object> data);

    ReflexValue pullData(RaptureURI displayName);

    void pushData(RaptureURI raptureURI, ReflexValue data, MediaType type);

    String rawPushData(String displayName, String content);

    List<String> batchPushData(List<String> displayNames, List<Map<String, Object>> datas);

    List<String> batchPushRawData(List<String> displayNames, List<String> datas);

    void remove(String displayName);

    Map<String, Object> metaPullData(String asString);

    void switchApi(ScriptingApi api);
    void resetApi();
}
