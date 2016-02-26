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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class FileRepoUtilsTest {
    ArrayList<Pair> pairs = null;

    @Before
    public void setUp() throws Exception {
        pairs = new ArrayList<>();

        pairs.add(new Pair("-2d-2d-2d", "---"));
        pairs.add(new Pair("/fred/jim/sharon/даве", "/fred/jim/sharon/даве"));
        pairs.add(new Pair("/ /!@#$%^-2d-2a();:-3c-3e-7c-5c/dave", "/ /!@#$%^-*();:<>|\\/dave"));
        pairs.add(new Pair("-", "."));
        pairs.add(new Pair("-.", ".."));
        pairs.add(new Pair("-..", "..."));
        pairs.add(new Pair("--2d", ".-"));
        pairs.add(new Pair("c:/foo/bar", "c:/foo/bar"));
    }

    @Test
    public void testEncode() {
        for (Pair pair: pairs) {
            assertEquals(pair.encoded, FileRepoUtils.encode(pair.decoded));
        }
    }

    @Test
    public void testDecode() {
        for (Pair pair: pairs) {
            assertEquals(pair.decoded, FileRepoUtils.decode(pair.encoded));
        }
    }

    private class Pair {
        public String encoded;
        public String decoded;

        Pair(String encoded, String decoded) {
            this.encoded = encoded;
            this.decoded = decoded;
        }
    }
}
