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
package rapture.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import rapture.common.model.BasePayload;

/**
 * This class is used by the entitlements system and it provides a dynamic way
 * of working out key parts of a payload that an entitlement may depend on.
 * These are
 * 
 * displayname $d authority $p typeName $t
 * 
 * We construct this with a reference to the BasePayload and use reflection on
 * demand to get those values out of the payload.
 * 
 * @author alan
 * 
 */
public class RaptureEntitlementsContext implements IEntitlementsContext {
    private BasePayload payload;

    public RaptureEntitlementsContext(BasePayload payload) {
        this.payload = payload;
    }

    private Method getBeanMethod(String memberName) throws SecurityException, NoSuchMethodException {
        String methodName = "get" + memberName;
        Method m = payload.getClass().getMethod(methodName);
        return m;
    }

    private String getBeanValue(String memberName) {
        try {
            Method m = getBeanMethod(memberName);
            return m.invoke(payload).toString();
        } catch (SecurityException e) {

        } catch (NoSuchMethodException e) {

        } catch (IllegalArgumentException e) {

        } catch (IllegalAccessException e) {

        } catch (InvocationTargetException e) {

        }
        return "";
    }

    @Override
    public String getDocPath() {
        return getBeanValue("DocPath");
    }

    @Override
    public String getAuthority() {
        return getBeanValue("Authority");
    }

    @Override
    public String getFullPath() {
        return getBeanValue("FullPath");
    }

}
