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
package rapture.dsl.tparse;

import java.util.Map;

import rapture.util.StringUtil;

public final class TemplateF {
    public static String parseTemplate(String path, String subs) {
        Map<String, Object> sub = StringUtil.getMapFromString(subs);
        StringBuilder ret = new StringBuilder();
        int point = 0;
        while (point < path.length()) {
            int pos = path.indexOf('$', point);
            if (pos == -1) {
                ret.append(path.substring(point));
                break;
            } else {
                ret.append(path.substring(point, pos));
                int nextPoint = path.indexOf('}', pos);
                if (nextPoint == -1) {
                    ret.append(path.substring(pos));
                    point = path.length();
                } else {
                    String var = path.substring(pos + 2, nextPoint);
                    if (sub.containsKey(var)) {
                        ret.append(sub.get(var));
                    } else {
                        ret.append(var.toUpperCase());
                    }
                    point = nextPoint + 1;
                }
            }
        }
        return ret.toString();
    }

    private TemplateF() {
    }
}
