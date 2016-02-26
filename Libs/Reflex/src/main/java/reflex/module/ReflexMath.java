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

import reflex.IReflexHandler;
import reflex.ReflexException;
import reflex.debug.IReflexDebugger;
import reflex.importer.Module;
import reflex.value.ReflexValue;
import reflex.value.internal.ReflexVoidValue;

public class ReflexMath implements Module {

    private double getDP(List<ReflexValue> params) {
        if (params.size() == 1) {
            if (params.get(0).isNumber()) {
                return params.get(0).asDouble();
            }
        }
        throw new ReflexException(-1, "Cannot retrieve numeric first argument in math call");
    }

    private double getDP(List<ReflexValue> params, int position) {
        if (params.size() > position) {
            if (params.get(position).isNumber()) {
                return params.get(position).asDouble();
            }
        }
        throw new ReflexException(-1, "Cannot retrieve numeric first argument in math call");
    }

    private Integer getDPInt(List<ReflexValue> params, int position) {
        if (params.size() > position) {
            if (params.get(position).isNumber()) {
                return params.get(position).asInt();
            }
        }
        throw new ReflexException(-1, "Cannot retrieve numeric first argument in math call");
    }

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

    public ReflexValue pi(List<ReflexValue> params) {
        return new ReflexValue(Math.PI);
    }

    public ReflexValue e(List<ReflexValue> parms) {
        return new ReflexValue(Math.E);
    }

    public ReflexValue abs(List<ReflexValue> params) {
        return new ReflexValue(Math.abs(getDP(params)));
    }

    public ReflexValue acos(List<ReflexValue> params) {
        return new ReflexValue(Math.acos(getDP(params)));
    }

    public ReflexValue asin(List<ReflexValue> params) {
        return new ReflexValue(Math.asin(getDP(params)));
    }

    public ReflexValue atan(List<ReflexValue> params) {
        return new ReflexValue(Math.atan(getDP(params)));
    }

    public ReflexValue atan2(List<ReflexValue> params) {
        return new ReflexValue(Math.atan2(getDP(params, 0), getDP(params, 1)));
    }

    public ReflexValue cbrt(List<ReflexValue> params) {
        return new ReflexValue(Math.cbrt(getDP(params)));
    }

    public ReflexValue ceil(List<ReflexValue> params) {
        return new ReflexValue(Math.ceil(getDP(params)));
    }

    public ReflexValue cos(List<ReflexValue> params) {
        return new ReflexValue(Math.cos(getDP(params)));
    }

    public ReflexValue cosh(List<ReflexValue> params) {
        return new ReflexValue(Math.cosh(getDP(params)));
    }

    public ReflexValue exp(List<ReflexValue> params) {
        return new ReflexValue(Math.exp(getDP(params)));
    }

    public ReflexValue expm1(List<ReflexValue> params) {
        return new ReflexValue(Math.expm1(getDP(params)));
    }

    public ReflexValue floor(List<ReflexValue> params) {
        return new ReflexValue(Math.floor(getDP(params)));
    }

    public ReflexValue hypot(List<ReflexValue> params) {
        return new ReflexValue(Math.hypot(getDP(params, 0), getDP(params, 1)));
    }

    public ReflexValue log(List<ReflexValue> params) {
        return new ReflexValue(Math.log(getDP(params)));
    }

    public ReflexValue log10(List<ReflexValue> params) {
        return new ReflexValue(Math.log10(getDP(params)));
    }

    public ReflexValue log1p(List<ReflexValue> params) {
        return new ReflexValue(Math.log1p(getDP(params)));
    }

    public ReflexValue max(List<ReflexValue> params) {
        return new ReflexValue(Math.max(getDP(params, 0), getDP(params, 1)));
    }

    public ReflexValue min(List<ReflexValue> params) {
        return new ReflexValue(Math.min(getDP(params, 0), getDP(params, 1)));
    }

    public ReflexValue pow(List<ReflexValue> params) {
        return new ReflexValue(Math.pow(getDP(params, 0), getDP(params, 1)));
    }

    public ReflexValue sin(List<ReflexValue> params) {
        return new ReflexValue(Math.sin(getDP(params)));
    }

    public ReflexValue sinh(List<ReflexValue> params) {
        return new ReflexValue(Math.sinh(getDP(params)));
    }

    public ReflexValue sqrt(List<ReflexValue> params) {
        return new ReflexValue(Math.sqrt(getDP(params)));
    }

    public ReflexValue tan(List<ReflexValue> params) {
        return new ReflexValue(Math.tan(getDP(params)));
    }

    public ReflexValue tanh(List<ReflexValue> params) {
        return new ReflexValue(Math.tanh(getDP(params)));
    }

    public ReflexValue degrees(List<ReflexValue> params) {
        return new ReflexValue(Math.toDegrees(getDP(params)));
    }

    public ReflexValue radians(List<ReflexValue> params) {
        return new ReflexValue(Math.toRadians(getDP(params)));
    }

    // Round decimal place
    public ReflexValue rdp(List<ReflexValue> params) {
        double param = getDP(params, 0);
        int scale = getDPInt(params, 1);
        double scaleAmount = Math.pow(10, scale);
        param *= scaleAmount;
        double rounded = Math.round(param);
        return new ReflexValue(rounded / scaleAmount);
    }
}
