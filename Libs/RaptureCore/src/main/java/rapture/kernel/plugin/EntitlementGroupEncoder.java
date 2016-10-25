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
package rapture.kernel.plugin;

import java.net.HttpURLConnection;

import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.model.RaptureEntitlementGroup;
import rapture.kernel.Kernel;

public class EntitlementGroupEncoder extends ReflectionEncoder {

    @Override
    public Object getReflectionObject(CallingContext ctx, String uri) {
        // we dont use *Storage.readByAddress so that we preserve entitlement checking
        RaptureURI ruri = new RaptureURI(uri, Scheme.ENTITLEMENTGROUP);
        String groupname = ruri.hasDocPath() ? ruri.getShortPath() : ruri.getAuthority();
        RaptureEntitlementGroup group = Kernel.getEntitlement().getEntitlementGroup(ctx, groupname);
        if (group == null) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, String.format("Group [%s] not found", groupname));
        }
        return group;
    }
}