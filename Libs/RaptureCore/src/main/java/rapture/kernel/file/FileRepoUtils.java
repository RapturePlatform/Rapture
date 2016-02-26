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
package rapture.kernel.file;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.log4j.Logger;

import rapture.blob.BlobStoreFactory;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.config.ConfigLoader;

/**
 * Common code for file based repositories
 * 
 * @author qqqq
 *
 */
public abstract class FileRepoUtils {

    public static final String PREFIX = "prefix";

    static char encodingChar = '-';
    // Decode function assumes 2 character hex code for prohibited characters.
    static char[] prohibitedChars = new char[] { encodingChar, '<', '>', '\\', '\'', '|', '?', '*' };
    static String[] replacementString;
    private static Logger log = Logger.getLogger(FileRepoUtils.class);

    static {
        replacementString = new String[prohibitedChars.length];
        for (int i = 0; i < prohibitedChars.length; i++) {
            replacementString[i] = encodingChar + Integer.toHexString(prohibitedChars[i]);
        }
    }

    public static String encode(String str) {
        int j = 0;
        if (str.startsWith(".")) str = encodingChar + str.substring(++j);

        for (int i = 0; i < prohibitedChars.length;) {
            j = str.indexOf(prohibitedChars[i], j);
            if (j >= 0) {
                str = str.substring(0, j) + replacementString[i] + str.substring(j+1);
                j += 2;
            } else {
                i++;
                j = 0;
            }
        }
        return str;
    }

    public static String decode(String str) {
        if (str.length() < 3 && str.indexOf(encodingChar) == 0) {
            // This must have been because it began with a dot, and we'll get index out of bounds below
            return "." + str.substring(1);
        }

        int encodedCharIndex = -1;
        while ((encodedCharIndex = str.indexOf(encodingChar, encodedCharIndex + 1)) >= 0) {
            // This assumes that the hex code is exactly 2 characters long.
            String encodedChar = str.substring(encodedCharIndex + 1, encodedCharIndex + 3);
            if (Arrays.asList(replacementString).contains(encodingChar + encodedChar)) {
                char originalChar = (char)Integer.parseInt(encodedChar, 16);

                String prefix = "";
                if (originalChar == '\\') {
                    prefix = "\\"; // Have to escape backslash with escaped backslash
                }
                str = str.replaceFirst(encodingChar + encodedChar, prefix + originalChar);
            }
            else {
                // If it wasn't a prohibited character, the string must have begun with a dot.
                str = str.replaceFirst("" + encodingChar, ".");
            }
        }

        return str;
    }

    public static File makeGenericFile(File parent, String path) {
        return new File(parent, encode(path));
    }

    public static File ensureDirectory(String dirName) {
        String fullName = encode((Paths.get(dirName).isAbsolute()) ? dirName :  ConfigLoader.getConf().FileRepoDirectory + dirName);

        File file = new File(fullName);
        if (file.isDirectory()) 
            log.info("Parent directory " + fullName+" already exists ");

        if (!file.isDirectory() && !file.mkdirs()) 
            throw RaptureExceptionFactory.create("Cannot create parent directory " + fullName);
        return file;
    }
}
