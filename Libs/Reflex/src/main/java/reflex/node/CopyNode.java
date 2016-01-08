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
package reflex.node;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;

public class CopyNode extends BaseNode {

    private ReflexNode source;
    private ReflexNode target;

    public CopyNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode source, ReflexNode target) {
        super(lineNumber, handler, s);
        this.source = source;
        this.target = target;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {

        debugger.stepStart(this, scope);
        ReflexValue sourceVal = source.evaluate(debugger, scope);
        ReflexValue targetVal = target.evaluate(debugger, scope);
        ReflexValue retVal = new ReflexNullValue(lineNumber);;

        if (sourceVal.isString() && targetVal.isFile()) {
            String blobEndPoint = handler.getApi().getEndPoint() + "/blob/";
            String sourceString = sourceVal.asString();
            if (sourceString.startsWith("blob://")) {
                sourceString = sourceString.substring(7);
            } else if (sourceString.startsWith("//")) {
                sourceString = sourceString.substring(2);
            }
            FileOutputStream fos = null;
            try {
                String downloadPoint = blobEndPoint + sourceString;
                URL website = new URL(downloadPoint);
                // Need to provide a context to this
                // Need to send some cookies (raptureContext = serialized context)
                URLConnection conn = website.openConnection();                
                String myCookie = "raptureContext=" + handler.getApi().getSerializedContext();
                conn.setRequestProperty("Cookie", myCookie);
                ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
                fos = new FileOutputStream(targetVal.asString());
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (Exception e) {
                throw new ReflexException(lineNumber, "Could not get blob", e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch(IOException e1) {
                        // Silently fail, what's a boy to do.
                    }
                }
            }
        } else {
            throwError("both must be stream based", source, target, sourceVal, targetVal);
        }
        debugger.stepEnd(this, retVal, scope);
        return retVal;
    }

    @Override
    public String toString() {
        return String.format("copy(%s,%s)", source, target);
    }
}
