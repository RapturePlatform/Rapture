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
package reflex.module;

import java.util.List;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.debug.IReflexDebugger;
import reflex.importer.Module;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexNullValue;
import reflex.value.internal.ReflexVoidValue;

public class RaptureUriModule implements Module {

    @Override
    public ReflexValue keyholeCall(String name, List<ReflexValue> parameters) {
        return new ReflexVoidValue();
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

    @Override
    public void setReflexHandler(IReflexHandler handler) {
    }

    @Override
    public void setReflexDebugger(IReflexDebugger debugger) {
    }

    public ReflexValue about(List<ReflexValue> params) {
        return new ReflexValue("This module contains methods to manipulate Rapture URIs\n"
                + "raptureURI(STRING) returns a RaptureURI object or throws an exception if the argumant is invalid\n"
                + "getScheme(STRING | RAPTUREURI) returns the Scheme component of a URI \n"
                + "getAuthority(STRING | RAPTUREURI) returns the Authority component of a URI \n"
                + "getDocPath(STRING | RAPTUREURI) returns the DocPath component of a URI \n");
    }

    public ReflexValue raptureURI(List<ReflexValue> params) {
        if (params.size() != 1) {
            throw new ReflexException(-1, "expected one parameter");
        }
        return new ReflexValue(toRaptureURI(params.get(0)));
    }

    private RaptureURI toRaptureURI(ReflexValue val) {
        Object o = val.getValue();
        if (o instanceof RaptureURI) {
            return (RaptureURI) o;
        } else {
            return new RaptureURI(val.asString(), Scheme.DOCUMENT);
        }
    }

    public ReflexValue getScheme(List<ReflexValue> params) {
        if (params.size() != 1) throw new ReflexException(-1, "expected one parameter");
        RaptureURI uri = toRaptureURI(params.get(0));
        return new ReflexValue(uri.getScheme().toString());
    }

    public ReflexValue getAuthority(List<ReflexValue> params) {
        if (params.size() != 1) throw new ReflexException(-1, "expected one parameter");
        RaptureURI uri = toRaptureURI(params.get(0));
        return new ReflexValue(uri.getAuthority());
    }

    public ReflexValue getDocPath(List<ReflexValue> params) {
        if (params.size() != 1) throw new ReflexException(-1, "expected one parameter");
        RaptureURI uri = toRaptureURI(params.get(0));
        return new ReflexValue(uri.getDocPath());
    }

    public ReflexValue getVersion(List<ReflexValue> params) {
        if (params.size() != 1) throw new ReflexException(-1, "expected one parameter");
        RaptureURI uri = toRaptureURI(params.get(0));
        String version = uri.getVersion();
        return (version == null) ? new ReflexNullValue(-1) : new ReflexValue(version);
    }

    public ReflexValue getElement(List<ReflexValue> params) {
        if (params.size() != 1) throw new ReflexException(-1, "expected one parameter");
        RaptureURI uri = toRaptureURI(params.get(0));
        String element = uri.getElement();
        return (element == null) ? new ReflexNullValue(-1) : new ReflexValue(element);
    }

    public ReflexValue getAttribute(List<ReflexValue> params) {
        if (params.size() != 1) throw new ReflexException(-1, "expected one parameter");
        RaptureURI uri = toRaptureURI(params.get(0));
        String attribute = uri.getAttribute();
        return (attribute == null) ? new ReflexNullValue(-1) : new ReflexValue(attribute);
    }
}