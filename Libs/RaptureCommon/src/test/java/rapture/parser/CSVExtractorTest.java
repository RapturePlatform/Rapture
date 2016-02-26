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
package rapture.parser;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CSVExtractorTest {
    private static class Counter implements CSVCallback {
        int lines = 0;
        int cells = 0;

        @Override
        public void startNewLine() {
            lines++;
        }

        @Override
        public void addCell(String cell) {
            cells++;
        }

        public int getLineCount() {
            return lines;
        }

        public int getCellCount() {
            return cells;
        }
    }

    @Test
    @Ignore
    public void testSimpleFile() throws FileNotFoundException, RecognitionException {
        Counter counter = new Counter();
        CSVExtractor.streamCSV("/tmp/test.csv", counter);
        assertEquals(2, counter.getLineCount());
        assertEquals(10, counter.getCellCount());
    }

    @Test
    @Ignore
    public void testNoNewline() throws FileNotFoundException, RecognitionException {
        Counter counter = new Counter();
        CSVExtractor.streamCSV("/tmp/nnl.csv", counter);
        assertEquals(3, counter.getLineCount());
        assertEquals(15, counter.getCellCount());
    }

    @Test
    public void testStream() {
        String testcsv = "1,2,3 , 4, 5\na,b, c, d ,e \n I am, the, one, who,     knocks";
        InputStream is = new ByteArrayInputStream(testcsv.getBytes());
        Counter counter = new Counter();
        try {
            CSVExtractor.streamCSV(is, counter);
        } catch (IOException e) {
            fail(e.toString());
        } catch (RecognitionException e) {
            fail(e.toString());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                fail(e.toString());
            }
        }
        assertEquals(3, counter.getLineCount());
        assertEquals(15, counter.getCellCount());
    }

}
