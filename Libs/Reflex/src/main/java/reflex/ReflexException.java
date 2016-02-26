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

import reflex.value.ReflexValue;

/**
 * Exception thrown by Reflex during parsing. Note the derivation from
 * RuntimeException - it is unchecked so needs to be explicitly caught.
 * 
 * @author amkimian
 * 
 */
public class ReflexException extends RuntimeException {
    private static final long serialVersionUID = 4946965139832937450L;
    private int lineNumber;
    private ReflexValue value = null;

    public ReflexException(int lineNumber, String message) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public ReflexException(int lineNumber, String string, Exception e) {
        super(string, e);
        this.lineNumber = lineNumber;
    }

    public ReflexException(ReflexValue value, int lineNumber) {
        super(String.format(Messages.getString("ReflexException.LineException"), lineNumber, value.toString())); //$NON-NLS-1$
        this.value = value;
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public ReflexValue getValue() {
        return value;
    }

    public String toString() {
        return String.format(Messages.getString("ReflexException.LineException"), lineNumber, super.toString()); //$NON-NLS-1$
    }
    
    public void setLineNumber(int newValue) {
        this.lineNumber = newValue;
    }
}
