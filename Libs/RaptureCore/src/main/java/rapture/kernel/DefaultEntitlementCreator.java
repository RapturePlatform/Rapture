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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import rapture.common.EntitlementConst;
import rapture.common.Messages;
import rapture.common.api.EntitlementApi;
import rapture.common.model.RaptureEntitlement;

/**
 * This class looks at EntitlementConst through reflection and ensures that each
 * non parameterized entitlement in there has a corresponding Entitlement
 * defined.
 * 
 * This is called on startup by the Kernel.
 * 
 * @author amkimian
 * 
 */
public class DefaultEntitlementCreator {
    private static final Logger log = Logger.getLogger(DefaultEntitlementCreator.class);

    public static void ensureEntitlementSetup(EntitlementApi entitlement) {
        log.debug(Messages.getString("DefaultEntitlementCreator.EnsurePresent")); //$NON-NLS-1$
        Field[] fields = EntitlementConst.class.getDeclaredFields();
        Set<String> entitlementNames = new HashSet<String>();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers())) {
                try {
                    String value = f.get(null).toString();
                    if (value.indexOf('$') == -1) {
                        entitlementNames.add(value.substring(1));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (entitlement != null) {
            List<RaptureEntitlement> entsHere = entitlement.getEntitlements(ContextFactory.getKernelUser());
            for (RaptureEntitlement ent : entsHere) {
                entitlementNames.remove(ent.getName());
            }

            EntitlementUtilLockoutChecker.setRaptureInternalsMode(true);
            for (String checkEntitlement : entitlementNames) {
                log.info(Messages.getString("DefaultEntitlementCreator.Entitlement") + checkEntitlement //$NON-NLS-1$
                        + Messages.getString("DefaultEntitlementCreator.NotPresent")); //$NON-NLS-1$
                entitlement.addEntitlement(ContextFactory.getKernelUser(), checkEntitlement, null);
            }
            EntitlementUtilLockoutChecker.setRaptureInternalsMode(false);
        }
    }
}
