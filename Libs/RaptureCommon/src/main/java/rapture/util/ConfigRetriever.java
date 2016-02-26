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
package rapture.util;

import java.util.Map;

public final class ConfigRetriever {

    public static String getSetting(Map<String, String> primary, String key, String def) {
        if (primary != null && primary.containsKey(key)) {
            return primary.get(key);
        }
        String ret = System.getenv(key);
        if (ret == null) {
            ret = System.getProperty(key);
        }
        if (ret == null) {
            ret = def;
        }
        return ret;
    }

    public static int getSettingInt(Map<String, String> config, String string, int def) {
        String val = getSetting(config, string, null);
        if (val == null) {
            return def;
        } else {
            try {
                return Integer.parseInt(val);
            } catch (Exception e) {
                return def;
            }
        }
    }

    public static long getSettingLong(Map<String, String> config, String string, long def) {
        String val = getSetting(config, string, null);
        if (val == null) {
            return def;
        } else {
            try {
                return Long.parseLong(val);
            } catch (Exception e) {
                return def;
            }
        }
    }

    private ConfigRetriever() {

    }
}
