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
package reflex;

import org.antlr.runtime.RecognitionException;

public class ReflexParseException extends ReflexException {
    /**
	 * 
	 */
    private static final long serialVersionUID = 4415323081653743714L;
    private int lineNumber;
    private int posInLine;
    private String errorMessage;

    public ReflexParseException(RecognitionException e, String errorMessage) {
        super(e.line, String.format(Messages.getString("ReflexParseException.parseError"), errorMessage), e); //$NON-NLS-1$
        lineNumber = e.line;
        posInLine = e.charPositionInLine;
        this.errorMessage = errorMessage;
    }

    public String toString() {
        return String.format(Messages.getString("ReflexParseException.parseException"), lineNumber, posInLine, errorMessage); //$NON-NLS-1$
    }
}
