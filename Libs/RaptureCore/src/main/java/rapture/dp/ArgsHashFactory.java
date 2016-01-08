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
package rapture.dp;

import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import rapture.common.dp.ContextVariables;

/**
 * @author bardhi
 * @since 1/27/15.
 */
public class ArgsHashFactory {

    private static final Logger log = Logger.getLogger(ArgsHashFactory.class);

    public static String createHashValue(Map<String, String> contextMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
            String key = entry.getKey();
            if (!ContextVariables.TIMESTAMP.equals(key) && !ContextVariables.LOCAL_DATE.equals(key)) {
                //(exclude timestamps from hash, since they change all the time)
                sb.append("key=").append(key).append("; value=").append(entry.getValue()).append(";");
            }
        }
        String data = sb.toString();
        String hash = DigestUtils.sha256Hex(data);
        if (log.isTraceEnabled()) {
            log.trace(String.format("sb is %s, hash is %s", data, hash));
        }
        return hash;
    }
}
