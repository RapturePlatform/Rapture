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
package reflex.util;

import java.util.Stack;

public class NamespaceStack {

    private Stack<String> stack;
    private String prefixCache;

    public NamespaceStack() {
        this.stack = new Stack<String>();
        cachePrefix();
    }
    
    @SuppressWarnings("unchecked")
    public NamespaceStack(NamespaceStack other) {
        this.stack = (Stack<String>) other.stack.clone();
        this.prefixCache = other.prefixCache;
    }

    private void cachePrefix() {
        StringBuilder builder = new StringBuilder();
        for (String current : stack) {
            builder.append(current).append('.');
        }
        prefixCache = builder.toString();
    }
    
    public String asPrefix() {
        return prefixCache;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public String peek() {
        return stack.peek();
    }

    public void pop() {
        stack.pop();
        cachePrefix();
    }

    public void push(java.lang.String string) {
        stack.push(string);
        cachePrefix();
    }

}
