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

import java.util.ArrayList;
import java.util.List;

import rapture.common.RaptureSheetNote;
import rapture.kernel.script.KernelScript;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.debug.IReflexDebugger;
import reflex.importer.Module;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;
import reflex.value.internal.ReflexVoidValue;

/**
 * This module is intended to be called as part of the execution of a script associated with a sheet, and it provides methods that mirror some of the API sheet calls but
 * localized to the "current sheet" (which is set with a module call as well).
 * 
 * So a reflex script that runs with a sheet will have something like this:
 * 
 * import SheetManager as s;
 * $s.setContext([sheetAuthority],[sheetStore],[sheetKey]);
 * 
 * @author amkimian
 *
 */
public class SheetManager implements Module {

    @Override
    public ReflexValue keyholeCall(String name, List<ReflexValue> parameters) {
        throw new ReflexException(-1, "No keyhole allowed");
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

    private IReflexHandler handler;
    @SuppressWarnings("unused")
	private IReflexDebugger debugger;
    
    @Override
    public void setReflexHandler(IReflexHandler handler) {
       this.handler = handler;
    }

    @Override
    public void setReflexDebugger(IReflexDebugger debugger) {
       this.debugger = debugger;
    }

    private String sheetURI;
    
    public ReflexValue setContext(List<ReflexValue> params) {
        if (params.size() != 1) {
            throw new ReflexException(-1, "Bad call to setContext in SheetManager, don't call this manually");
        }
        sheetURI = params.get(0).asString();
        return new ReflexVoidValue();
    }
    
    public ReflexValue setCell(List<ReflexValue> params) {
        if (params.size() != 3) {
            throw new ReflexException(-1, "Bad call to setCell in SheetManager, don't call this manually");
        }
        int row = params.get(0).asInt();
        int col = params.get(1).asInt();
        String value = params.get(2).asString();
        
        // WARNING Will Robinson
        KernelScript script = (KernelScript) handler.getApi();
        script.getSheet().setSheetCell(sheetURI, row, col, value, 0);
        return new ReflexVoidValue();
    }
    
    public ReflexValue setStyle(List<ReflexValue> params) {
        if (params.size() != 3) {
            throw new ReflexException(-1, "Bad call to setStyle in SheetManager, don't call this manually");
        }
        int row = params.get(0).asInt();
        int col = params.get(1).asInt();
        String value = params.get(2).asString();
        
        // WARNING Will Robinson
        KernelScript script = (KernelScript) handler.getApi();
        script.getSheet().setSheetCell(sheetURI, row, col, value, 1);
        return new ReflexVoidValue();       
    }
    
    public ReflexValue getCell(List<ReflexValue> params) {
        if (params.size() != 2) {
            throw new ReflexException(-1, "Bad call to setCell in SheetManager, don't call this manually");
        }
        int row = params.get(0).asInt();
        int col = params.get(1).asInt();   
        // WARNING Will Robinson
        KernelScript script = (KernelScript) handler.getApi();
        String content = script.getSheet().getSheetCell(sheetURI, row, col, 0);       
        return content == null ? new ReflexNullValue() : new ReflexValue(content);
    }
    
    public ReflexValue setNote(List<ReflexValue> params) {
        if (params.size() != 2) {
            throw new ReflexException(-1, "Bad call to setNote");
        }
        String noteId = params.get(0).asString();
        String note = params.get(1).asString();
        KernelScript script = (KernelScript) handler.getApi();
        RaptureSheetNote theNote = new RaptureSheetNote();
        theNote.setNote(note);
        theNote.setId(noteId);
        script.getSheet().createSheetNote(sheetURI, theNote);
        return new ReflexValue(noteId);
    }
    
    
    public ReflexValue getContext(List<ReflexValue> params) {
        List<ReflexValue> ret = new ArrayList<ReflexValue>();
        ret.add(new ReflexValue(sheetURI));
        return new ReflexValue(ret);
    }
}
