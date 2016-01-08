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
package rapture.api.hooks.impl;

import rapture.common.CallingContext;
import rapture.common.hooks.CallName;
import rapture.common.hooks.HookType;

public abstract class AbstractApiHook {
    private String id;

    public final void execute(HookType hookType, CallingContext context, CallName callName) {
        loadConfig(hookType, context, callName);
        doExecute(hookType, context, callName);
    }

    protected abstract void doExecute(HookType hookType, CallingContext context, CallName callName);

    /**
     * Override if a particular type of hook needs to load any config from storage
     * @param callName 
     * @param context 
     * @param hookType 
     */
    protected void loadConfig(HookType hookType, CallingContext context, CallName callName) {
    }
    
    /**
     * Override if a particular hook type needs to persist config 
     */
    protected void saveConfig() {
        
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
}
