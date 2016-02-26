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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * This is just a simple wrapper around the Scanner class to provide a file as a stream of lines
 * so we can parse huge CSVs without blowing out memory.
 * 
 * @author mel
 */
public class FileStreamer implements Iterable<String>, Iterator<String> {
    public static final FileStreamer lines(String filename) throws FileNotFoundException {
        return lines(new File(filename));
    }

    public static final FileStreamer lines(File f) throws FileNotFoundException {        
        FileStreamer fs = new FileStreamer();
        fs.s = new Scanner(f);
        fs.next();
        return fs;
    }
    
    private FileStreamer() {}

    private boolean used = false;
    private Scanner s;
    private String nextLine;

    @Override
    public Iterator<String> iterator() {
        if (used) throw new IllegalStateException("Cannot rewind input");
        return this;
    }

    @Override
    public boolean hasNext() {
        return nextLine != null;
    }

    @Override
    public String next() {
        String result = nextLine;
        try {
            nextLine = s.nextLine();
        } catch (NoSuchElementException e) {
            nextLine = null;
        }
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }  

    public void close() {
        s.close();
    }
}
