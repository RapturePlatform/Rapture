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
package reflex.value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import reflex.ReflexException;

public class ReflexProcessValue {
    private Process p;

    public ReflexProcessValue(Process p) {
        this.p = p;
    }

    public ReflexValue getOutput() {
        // Get the output from the program
        List<ReflexValue> vals = new ArrayList<ReflexValue>();

        InputStream is = p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String line;
        int exit = -1;

        try {
            while ((line = br.readLine()) != null) {
                // Outputs your process execution
                vals.add(new ReflexValue(line));
                try {
                    exit = p.exitValue();
                    if (exit == 0) {
                        break;
                    }
                } catch (IllegalThreadStateException t) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new ReflexException(-1, "cannot read process output", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
            }
        }

        return new ReflexValue(vals);
    }

    public int waitFor() {
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            return -1;
        }
    }
}
