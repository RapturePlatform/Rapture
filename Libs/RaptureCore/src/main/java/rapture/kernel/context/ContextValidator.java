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
package rapture.kernel.context;

import rapture.common.CallingContext;
import rapture.common.EntitlementSet;
import rapture.common.RaptureEntitlementsContext;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptNotLoggedInException;
import rapture.common.model.BasePayload;
import rapture.kernel.Kernel;

/**
 * This class should be used to check whether a context is valid. Context
 * validation methods from {@link Kernel} should eventually be moved here
 * 
 * @author bardhi
 * 
 */
public class ContextValidator {

    /**
     * Checks whether a context is valid and logged in
     * 
     * @param context
     *            The {@link CallingContext} object validate
     * @param entitlementSet
     *            The {@link EntitlementSet} that we need to check against
     * @param payload
     *            The {@link BasePayload} object that contains the data. This is
     *            also used to check entitlement at a deeper granularity
     * @throws RaptureException
     *             if the user is not entitled to this entitlement
     * @throws RaptNotLoggedInException
     *             In case the user is not logged in
     */
    public static void validateContext(CallingContext context, EntitlementSet entitlementSet, BasePayload payload) throws RaptureException, RaptNotLoggedInException {
        if (context == null) {
            throw new RaptNotLoggedInException("Context is null, therefore invalid.");
        } else {
            Kernel.getKernel().validateContext(context, entitlementSet.getPath(), getEntitlementsContext(payload));
        }
    }

    private static RaptureEntitlementsContext getEntitlementsContext(BasePayload payload) {
        return new RaptureEntitlementsContext(payload);
    }
}
