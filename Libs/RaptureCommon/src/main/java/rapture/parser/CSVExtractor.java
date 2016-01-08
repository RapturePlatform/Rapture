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
package rapture.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import rapture.generated.CSVLexer;
import rapture.generated.CSVParser;

public class CSVExtractor {
    public static List<List<String>> getCSV(String content) throws RecognitionException {
        StandardCallback callback = new StandardCallback();

        getCSV(content, callback);

        return callback.getOutput();
    }

    public static void getCSV(String content, CSVCallback callback) throws RecognitionException {
        CSVLexer lexer = new CSVLexer();
        lexer.setCharStream(new ANTLRStringStream(content));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CSVParser parser = new CSVParser(tokens);
        parser.setCallback(callback);
        parser.file();
    }

    public static void streamCSV(String fileName, CSVCallback callback) throws FileNotFoundException, RecognitionException {
        FileStreamer fs = null;
        try {
            fs = FileStreamer.lines(fileName);
            for (String line : fs) {
                getCSV(line, callback);
            }
        } finally {
            if (fs != null) {
                fs.close();
            }
        }
    }

    /**
     * Given an InputStream that represents a CSV, stream each line into the
     * CSVParser and call the callback for each line. This should encourage use
     * of streams to prevent loading up large CSV's into String objects in
     * memory.
     * 
     * This method does not flush or close the stream. This is to avoid making
     * non-portable assumptions about the stream's origin and further use. Thus
     * the caller is still responsible for closing the InputStream after use.
     * 
     * @param stream
     *            - The InputStream for reading a CSV file line by line
     * @param callback
     *            - The Callback that is called after each line is read
     * @throws IOException
     *             - If there was an IO problem while reading the InputStream
     * @throws RecognitionException
     *             - If there was a parsing issue related to the CSV
     */
    public static void streamCSV(InputStream stream, CSVCallback callback) throws IOException, RecognitionException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            getCSV(line, callback);
        }
    }
}
