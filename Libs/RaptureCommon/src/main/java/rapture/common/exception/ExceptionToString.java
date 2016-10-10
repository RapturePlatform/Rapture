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
/**
 * ExceptionToString - simple class that dumps out the full exception stack trace as a string.
 * Can be useful for logging.
 */
package rapture.common.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import rapture.common.Formattable;

public class ExceptionToString implements Formattable{

    Throwable toss = null;
    
    public ExceptionToString(Throwable t) {
        toss = t;
    }
    
    @Override
    public String toString() {        
        return format(toss);
    }
    
    @Override
    public String format() {
        return toString();
    }

    // for testing only
    public static void main(String[] args) {
        Throwable t = new Throwable("FOO", new Throwable("Bar", new Throwable("Baz")));
        System.out.println(ExceptionToString.format(t));
    }
    
    public static final String format(Throwable toss) {        
        StringWriter sw = new StringWriter();
        
        // Improved nested exception handling
        PrintWriter pw = new PrintWriter(sw) {
            boolean stop = false;

            @Override
            public void print(String s) {
                if (s.startsWith("Caused by")) stop = true;
                else if (!s.startsWith(" ") && !s.startsWith("\t")) stop = false;
                if (!stop) super.print(s);
            }
            
            @Override
            public void println() {
                if (!stop) super.println();
            }
            
        };
        for (Throwable t = toss; t != null;) {
            t.printStackTrace(pw);
            t = t.getCause();
            if (t != null) pw.append("Caused by: ");
        }
        return sw.toString();
    }

    public static final String summary(Throwable toss) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = toss; t != null;) {
            String clas = t.getClass().toString();
            if (clas.startsWith("class ")) clas = clas.substring(6);
            sb.append(clas).append(": ");
            sb.append(t.getMessage()).append("\n");
            t = t.getCause();
            if (t != null) sb.append("Caused by: ");
        }
        return sb.toString();
    }
}
