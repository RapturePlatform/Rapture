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
package reflex.importer;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.debug.IReflexDebugger;

public class ModuleFactory {
    private static Object syncPoint = new Object();

    private ModuleFactory() {

    }

    public static Module createModule(String name, String alias, IReflexHandler handler, IReflexDebugger debugger) {
        synchronized (syncPoint) {
            Module ret = null;
            ret = createModuleFromClassName("reflex.module." + name.substring(0, 1).toUpperCase() + name.substring(1), handler, debugger);
            if (ret != null) {
                return ret;
            }
            ret = createModuleFromClassName(name, handler, debugger);
            if (ret != null) {
                return ret;
            }
        }

        throw new ReflexException(-1, "Cannot find module named " + name);
    }

    private static Module createModuleFromClassName(String className, IReflexHandler handler, IReflexDebugger debugger) {
        Module ret = null;
        try {
            Class<?> moduleClass = Module.class.getClassLoader().loadClass(className);
            // if (Module.class.isAssignableFrom(moduleClass)) {
            ret = (Module) moduleClass.newInstance();

            ret.setReflexHandler(handler);
            ret.setReflexDebugger(debugger);
            return ret;
            // } else {
            // System.out.println("Object is not instance of Module, it is an "
            // + moduleClass.toString());
            // return null;
            // }
        } catch (Exception e) {
            handler.getOutputHandler().printOutput("Error - " + e.getMessage() + " - " + e.getClass().toString());
            return null;
        }
    }
}
