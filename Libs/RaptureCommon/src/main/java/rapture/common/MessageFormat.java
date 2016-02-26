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
package rapture.common;

import java.util.Locale;

public class MessageFormat extends java.text.MessageFormat implements Formattable {
    private static final long serialVersionUID = 5048448763068980951L;
    Object[] arguments = null;

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    public void setArgument(Object argument) {
        this.arguments = new Object[] { argument };
    }
    
    public MessageFormat(String pattern) {
        super(pattern);
    }
  
    public MessageFormat(String pattern, Object argument) {
        super(pattern);
        arguments = new Object[] { argument };
    }

    public MessageFormat(String pattern, Object[] arguments) {
        super(pattern);
        this.arguments = arguments;
    }

    public MessageFormat(String pattern, Locale locale) {
        super(pattern);
        this.setLocale(locale);
    }
  
    public MessageFormat(String pattern, Object argument, Locale locale) {
        super(pattern);
        this.setLocale(locale);
        arguments = new Object[] { argument };
    }

    public MessageFormat(String pattern, Object[] arguments, Locale locale) {
        super(pattern);
        this.arguments = arguments;
        this.setLocale(locale);
    }

    @Override
    public String format() {
        return format(arguments);
    }
    
    @Override
    public String toString() {
        return format();
    }
}
