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
package rapture.kernel;

import java.util.Stack;

/**
 * A context stack is associated with a context - and api implementations can
 * push things onto a stack and pop it off.
 * 
 * Then relationship calls can use this stack to determine relationships.
 * 
 * E.g. if I putDoc(docURI) and the top of this stack is a script, there's a
 * put relationship between the script and the docURI. If the script executes
 * another script, there's a relationship between the script and that script,
 * *and* any puts in the bottom script are referencing that one
 * 
 * @author alanmoore
 * 
 */
public class ContextStack {
    private String contextUUID;
    private Stack<String> contextStack;

    public ContextStack(String contextUUID) {
        this.contextUUID = contextUUID;
        this.contextStack = new Stack<String>();
    }

    public String getContextUUID() {
        return contextUUID;
    }

    public String peekTop() {
        return contextStack.peek();
    }

    public void push(String uri) {
        contextStack.push(uri);
    }

    public void pop() {
        contextStack.pop();
    }

    public boolean isEmpty() {
        return contextStack.isEmpty();
    }
}
