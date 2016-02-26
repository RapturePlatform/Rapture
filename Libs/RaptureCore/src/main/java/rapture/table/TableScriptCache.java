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
package rapture.table;

import java.util.HashMap;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.ReflexExecutor;
import reflex.ReflexTreeWalker;

/**
 * This class holds pre-compiled index scripts, on the assumption that (a) the
 * same index will be used many times (b) the index code will not change that
 * often
 * 
 * @author amkimian
 * 
 */
public class TableScriptCache {
    private Map<String, ReflexTreeWalker> reflexScriptCache = new HashMap<String, ReflexTreeWalker>();

    public TableScriptCache() {

    }

    public boolean doesCacheExist(String fullName) {
        return reflexScriptCache.containsKey(fullName);
    }

    public ReflexTreeWalker getReflexScript(String fullName, String script, IReflexHandler handler) {
        if (reflexScriptCache.containsKey(fullName)) {
            return reflexScriptCache.get(fullName);
        } else {
            ReflexTreeWalker walker = ReflexExecutor.getWalkerForProgram(script, handler);
            // reflexScriptCache.put(fullName, walker);
            return walker;
        }
    }
}
