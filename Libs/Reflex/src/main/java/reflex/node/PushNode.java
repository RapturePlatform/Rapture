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
import reflex.ReflexException;
import reflex.Scope;
import reflex.debug.IReflexDebugger;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class PushNode extends BaseNode {
    private ReflexNode toSaveNode;
    private ReflexNode location;

    public PushNode(int lineNumber, IReflexHandler handler, Scope s, ReflexNode value, ReflexNode location) {
        super(lineNumber, handler, s);
        this.toSaveNode = value;
        this.location = location;
    }

    @Override
    public ReflexValue evaluate(IReflexDebugger debugger, Scope scope) {
        debugger.stepStart(this, scope);
        // In this particular case, we get the ReflexValue to save from the
        // scope
        ReflexValue toSave = toSaveNode.evaluate(debugger, scope);
        ReflexValue value = location.evaluate(debugger, scope);

        if (toSave.isList() && value.isList()) {
            // Multiple save
            List<ReflexValue> toSaveList = toSave.asList();
            List<ReflexValue> locationList = value.asList();
            if (toSaveList.size() != locationList.size()) {
                throw new ReflexException(lineNumber, "Size mismatch in array push");
            }
            List<String> displayNames = new ArrayList<String>();
            if (toSaveList.get(0).isMap()) {
                List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
                for (int i = 0; i < toSaveList.size(); i++) {
                    ReflexValue save = toSaveList.get(i);
                    ReflexValue loc = locationList.get(i);
                    displayNames.add(loc.toString());
                    datas.add(save.asMap());
                }
                handler.getDataHandler().batchPushData(displayNames, datas);
            } else {
                List<String> datas = new ArrayList<String>();
                for (int i = 0; i < toSaveList.size(); i++) {
                    ReflexValue save = toSaveList.get(i);
                    ReflexValue loc = locationList.get(i);
                    displayNames.add(loc.toString());
                    datas.add(save.toString());
                }
                handler.getDataHandler().batchPushRawData(displayNames, datas);
            }
        } else if (value.isFile()) {
            // Push the data to a file
            handler.getIOHandler().writeFile(value.asFile(), toSave.asObject().toString());
        } else if (value.isArchive()) {
            handler.getIOHandler().writeArchiveEntry(value.asArchive(), toSave);
        } else {
//        	System.out.println("Saving a map");
//        	String uri = value.toString();
//       	String content = JacksonUtil.jsonFromObject(KernelExecutor.convert(toSave.asMap()));
//        	System.out.println("Content is " + content);
//        	handler.getApi().getDoc().putDoc(uri, content);
            handler.getDataHandler().pushData(value.toString(), KernelExecutor.convert(toSave.asMap()));
            String comment = this.getScope().getAndUsePendingComment();
            writeAudit(debugger, (comment == null ? "Wrote document" : comment) + " " + value.toString());
        }
        debugger.stepEnd(this, new ReflexVoidValue(lineNumber), scope);
        return new ReflexVoidValue(lineNumber);
    }

}
