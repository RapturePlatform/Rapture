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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;

public class ContextStackContainer {
    private Map<String, ContextStack> stackMap = new HashMap<String, ContextStack>();
    private static Logger log = Logger.getLogger(ContextStackContainer.class);

    private boolean rejectContext(CallingContext ctx) {
        if (ctx == ContextFactory.getKernelUser()) {
            log.debug("Would reject " + ctx.getContext() + " as it is the kernel user");
            return false; // was true
        }
        return false;
    }

    public void pushStack(CallingContext ctx, String uri) {
        if (rejectContext(ctx)) {
            return;
        }
        String key = ctx.getContext();
        synchronized (stackMap) {
            if (!stackMap.containsKey(key)) {
                stackMap.put(key, new ContextStack(key));
            }
            stackMap.get(key).push(uri);
        }
    }

    public void popStack(CallingContext ctx) {
        if (rejectContext(ctx)) {
            return;
        }
        String key = ctx.getContext();
        synchronized (stackMap) {
            if (stackMap.containsKey(key)) {
                stackMap.get(key).pop();
                if (stackMap.get(key).isEmpty()) {
                    stackMap.remove(key);
                }
            }
        }
    }

    public String peekStack(CallingContext ctx) {
        if (rejectContext(ctx)) {
            return null;
        }
        String key = ctx.getContext();
        synchronized (stackMap) {
            if (stackMap.containsKey(key)) {
                return stackMap.get(key).peekTop();
            }
        }
        return null;
    }
}
