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
package rapture.series.children;

import java.util.Arrays;
import java.util.List;

public class SeriesPathParser {

    /**
     * Get each part of the path in a list of strings, e.g. a/b/s1 will return a
     * list containing "a", "b", and "s1". The last entry in the list is always
     * the series file name
     * 
     * @param path
     * @return
     */
    public static List<String> getPathParts(String path) {
        path = massagePath(path);
        return Arrays.asList(path.split(PathConstants.PATH_SEPARATOR));
    }

    /**
     * Get rid of double slashes in path, remove leading and trailing slashes
     * 
     * @param path
     * @return
     */
    private static String massagePath(String path) {
        return path.replaceAll("/+", PathConstants.PATH_SEPARATOR).replaceAll("^/+", "").replaceAll("/$", "");
    }

    public static String getParent(String fullPath) {
        String path = massagePath(fullPath);
        int lastSlash = path.lastIndexOf(PathConstants.PATH_SEPARATOR);
        if (lastSlash == -1) {
            return "";
        } else {
            return path.substring(0, lastSlash);
        }
    }
    
    public static String getChild(String fullPath) {
        String path = massagePath(fullPath);
        int lastSlash = path.lastIndexOf(PathConstants.PATH_SEPARATOR);
        if (lastSlash == -1) {
            return path;
        }
        else {
            return path.substring(lastSlash + 1);
        }
    }
}
