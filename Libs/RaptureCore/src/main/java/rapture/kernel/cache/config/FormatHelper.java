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
package rapture.kernel.cache.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author bardhi
 * @since 3/9/15.
 */
public class FormatHelper {
    public int getNumExpected(String standardTemplate) {
        return StringUtils.countMatches(standardTemplate, "%s");
    }

    /**
     * If we expect more args than we have, return a new list with empty args to make up for more args. Else, return current list.
     *
     * @param numExpected
     * @param args
     */
    public List<String> padExtras(int numExpected, List<String> args) {
        List<String> list = new ArrayList<>(numExpected);
        for (String arg : args) {
            list.add(arg);
        }
        for (int i = args.size(); i < numExpected; i++) {
            list.add("");
        }
        return list;

    }

    /**
     * Some standard templates have no placeholders, e.g. USING MONGODB "". Fill in a Java-friendly %s in there.
     * @param standardTemplate
     * @return
     */
    public String fillInPlaceholders(String standardTemplate) {
        return standardTemplate.replaceAll("\"\"", "\"%s\"");
    }
}
