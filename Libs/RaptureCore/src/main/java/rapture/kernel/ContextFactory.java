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
package rapture.kernel;

import java.util.UUID;

import rapture.common.CallingContext;

public final class ContextFactory {
    public static final CallingContext ADMIN;
    static {
        ADMIN = new CallingContext();
        ADMIN.setUser("$admin");
        ADMIN.setContext(UUID.randomUUID().toString());
        ADMIN.setValid(true);
    }
    private static CallingContext KERNELUSER = null;
    private static final CallingContext ANON;
    static {
        ANON = new CallingContext();
        ANON.setContext(UUID.randomUUID().toString());
        ANON.setUser("$anonymous");
        ANON.setValid(true);
    }

    public static CallingContext getAnonymousUser() {
        return ANON;
    }

    public static CallingContext getKernelUser() {
        if (KERNELUSER == null) {
            return ADMIN;
        } else {
            return KERNELUSER;
        }
    }

    /**
     * This is called by the Rapture startup code, after it has validated that the config does actually correspond to an api user.
     * 
     * @param userId
     */
    public static void setKernelUser(String userId, String context) {
        KERNELUSER = new CallingContext();
        KERNELUSER.setUser(userId);
        KERNELUSER.setValid(true);
        KERNELUSER.setContext(context);
    }

    // TODO: Prevent anyone from creating the $admin user (or any user beginning
    // with $)

    private ContextFactory() {
    }
}
