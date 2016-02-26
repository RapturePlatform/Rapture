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
package reflex.debug;

import reflex.Scope;
import reflex.node.ReflexNode;
import reflex.util.function.LanguageRegistry;
import reflex.value.ReflexValue;

/**
 * This "debugger" simply prints out the steps as they are traversed
 * 
 * @author amkimian
 * 
 */
public class ReflexPrintingDebugger implements IReflexDebugger {
    private int stepLevel = 0;

    @Override
    public boolean isDebug() {
        return true;
    }

    @Override
    public void setRegistry(LanguageRegistry functions) {
    }

    @Override
    public void setProgram(String program) {
        program.split("\n");
    }

    @Override
    public void stepEnd(ReflexNode node, ReflexValue value, Scope scope) {
        stepLevel--;
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < stepLevel; i++) {
            msg.append("<");
        }
        msg.append(" ");
        msg.append(node.toString());
        System.out.println(msg.toString());
        System.out.println("Returning " + value.toString());
    }

    @Override
    public void stepStart(ReflexNode node, Scope scope) {
        stepLevel++;
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < stepLevel; i++) {
            msg.append(">");
        }
        msg.append(" ");
        msg.append(node.toString());
        System.out.println(msg.toString());
    }

    @Override
    public void recordMessage(String message) {
        System.out.println(message);
    }

}
