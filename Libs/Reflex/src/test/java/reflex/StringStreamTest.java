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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import reflex.file.FileReadAdapterFactory;
import reflex.node.io.FileReadAdapter;
import reflex.value.ReflexStringStreamValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class StringStreamTest {
    @Test
    public void testStringStream() {
        String content = "ONE,TWO,THREE\nA,B,C\n";
        ReflexStringStreamValue val = new ReflexStringStreamValue();
        val.setContent(content);
        FileReadAdapter fra = FileReadAdapterFactory.create("CSV", null, null);
        val.setFileReadAdapter(fra);

        DefaultReflexIOHandler handler = new DefaultReflexIOHandler();
        ReflexValue cnt = handler.getContent(val);
        assertEquals(cnt.toString(), content);

        handler.forEachLine(val, new IReflexLineCallback() {

            @Override
            public ReflexValue callback(String line) {
                System.out.println("Line = " + line);
                return new ReflexVoidValue();
            }

        });

    }
}
