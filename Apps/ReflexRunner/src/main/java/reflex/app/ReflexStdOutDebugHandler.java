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
package reflex.app;

import org.apache.log4j.Logger;

import reflex.DebugLevel;
import reflex.IReflexDebugHandler;

public class ReflexStdOutDebugHandler implements IReflexDebugHandler {
    private Logger log = Logger.getLogger(this.getClass().getName());

    @Override
    public void debugOut(int arg0, DebugLevel arg1, String arg2) {
        String message = "Line: " + arg0 + ", " + arg2;
        switch (arg1) {
        case SPAM:
            log.info(message);
            break;
        case INFO:
            log.info(message);
            break;
        case WARNING:
            log.warn(message);
            break;
        case ERROR:
            log.error(message);
            break;
        }
    }

    @Override
    public void statementReached(int arg0, DebugLevel arg1, String arg2) {
        debugOut(arg0, arg1, arg2);
    }

    @Override
    public boolean hasCapability() {
        return true;
    }

}
