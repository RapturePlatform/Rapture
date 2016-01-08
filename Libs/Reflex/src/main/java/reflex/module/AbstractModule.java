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
package reflex.module;

import java.util.List;

import reflex.IReflexHandler;
import reflex.debug.IReflexDebugger;
import reflex.importer.Module;
import reflex.value.ReflexValue;

public abstract class AbstractModule implements Module {

    @Override
    public ReflexValue keyholeCall(String name, List<ReflexValue> parameters) {
        return null;
    }

    @Override
    public boolean handlesKeyhole() {
        return false;
    }

    @Override
    public boolean canUseReflection() {
        return true;
    }

    @Override
    public void configure(List<ReflexValue> parameters) {
        
    }

    protected IReflexHandler handler;
    protected IReflexDebugger debugger;
    
    @Override
    public void setReflexHandler(IReflexHandler handler) {
        this.handler = handler;
    }

    @Override
    public void setReflexDebugger(IReflexDebugger debugger) {
        this.debugger = debugger;
    }
    
}