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
package rapture.series.children;

public class ChildKeyUtil {
    private static final String CHILDREN_KEY = "..children_";
    private static final String FOLDER_PREFIX = "//FOLDER//";
    private static final String FILE_PREFIX = "//FILE//";

    public static String createRowKey(String name) {
        return CHILDREN_KEY + name;
    }

    public static boolean isRowKey(String potential) {
        return potential != null && potential.startsWith(CHILDREN_KEY);
    }

    public static String fromRowKey(String key) {
        if (!isRowKey(key)) {
            return "";
        } else {
            return key.replace(CHILDREN_KEY, "");
        }
    }

    public static String createColumnFolder(String childName) {
        return FOLDER_PREFIX + childName;
    }

    public static String createColumnFile(String childName) {
        return FILE_PREFIX + childName;
    }

    public static boolean isColumnFolder(String columnName) {
        return columnName != null && columnName.startsWith(FOLDER_PREFIX);
    }

    public static String fromColumnFolder(String columnName) {
        if (columnName != null && columnName.length() > FOLDER_PREFIX.length()) {
            return columnName.substring(FOLDER_PREFIX.length());
        } else {
            return "";
        }
    }

    public static String fromColumnFile(String columnName) {
        if (columnName != null && columnName.length() > FILE_PREFIX.length()) {
            return columnName.substring(FILE_PREFIX.length());
        } else {
            return "";
        }
    }

}
