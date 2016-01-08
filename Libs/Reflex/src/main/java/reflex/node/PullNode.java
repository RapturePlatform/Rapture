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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import reflex.IReflexHandler;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.util.MapConverter;
import reflex.value.ReflexArchiveFileValue;
import reflex.value.ReflexProcessValue;
import reflex.value.ReflexStreamValue;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;
import reflex.value.internal.ReflexVoidValue;

public class PullNode extends BaseNode {

    private String identifier;
    private ReflexNode location;

    public PullNode(int lineNumber, IReflexHandler handler, Scope s, String identifier, ReflexNode location) {
        super(lineNumber, handler, s);
        this.identifier = identifier;
        this.location = location;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        ReflexValue value = location.evaluate(debugger, scope);
        ReflexValue rVal;
        if (value.isList()) {
            int size = value.asList().size();
            List<ReflexValue> ret = new ArrayList<ReflexValue>(size);
            List<String> displays = new ArrayList<String>(size);
            for (ReflexValue v : value.asList()) {
                displays.add(v.asString());
            }
            List<Map<String, Object>> vals = handler.getDataHandler().batchPullData(displays);
            for (Map<String, Object> retVal : vals) {
                ReflexValue innerVal = new ReflexNullValue(lineNumber);;
                if (retVal != null) {
                    innerVal = new ReflexValue(MapConverter.convertMap(retVal));
                }
                ret.add(innerVal);
            }
            rVal = new ReflexValue(ret);
        } else if (value.isStreamBased()) {
            // Read the contents of the file into rVal
            ReflexStreamValue fVal = value.asStream();
            rVal = fVal.getFileReadAdapter().readContent(fVal, handler.getIOHandler());
        } else if (value.isArchive()) {
            ReflexArchiveFileValue fVal = value.asArchive();
            rVal = handler.getIOHandler().getArchiveEntry(fVal);
        } else if (value.isProcess()) {
            ReflexProcessValue fProc = value.asProcess();
            rVal = fProc.getOutput();
        } else {
            Map<String, Object> retVal = handler.getDataHandler().pullData(value.asString());
            if (retVal == null) {
                rVal = new ReflexNullValue(lineNumber);;
            } else {
                rVal = new ReflexValue(MapConverter.convertMap(retVal));
            }
        }
        scope.assign(identifier, rVal);
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexVoidValue(lineNumber);
    }

}
