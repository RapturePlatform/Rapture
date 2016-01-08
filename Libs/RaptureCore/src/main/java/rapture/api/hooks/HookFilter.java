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
package rapture.api.hooks;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.api.hooks.impl.AbstractApiHook;
import rapture.common.CallingContext;
import rapture.common.exception.RaptureException;
import rapture.common.hooks.CallName;
import rapture.common.hooks.HookFactory;
import rapture.common.hooks.HookType;
import rapture.common.hooks.SingleHookConfig;

public class HookFilter {

    private Logger log = Logger.getLogger(getClass());

    public List<AbstractApiHook> filter(Collection<SingleHookConfig> allHooksConfig, HookType hookType, CallingContext context, CallName callName) {
        List<AbstractApiHook> hooks = new LinkedList<AbstractApiHook>();
        for (SingleHookConfig hookConfig : allHooksConfig) {
            if (typeMatches(hookConfig, hookType) && contextMatches(hookConfig, context) && callNameMatches(hookConfig, callName)) {
                try {
                    AbstractApiHook hook = HookFactory.INSTANCE.createHook(hookConfig);
                    hooks.add(hook);
                } catch (RaptureException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return hooks;
    }

    private boolean callNameMatches(SingleHookConfig hookConfig, CallName callName) {
        int maxMatchLength = 0;

        boolean isIncluded = true;

        for (String include : hookConfig.getIncludes()) {
            if (callName.toString().matches(include) && include.length() > maxMatchLength) {
                maxMatchLength = include.length();
                isIncluded = true;
            }
        }
        for (String exclude : hookConfig.getExcludes()) {
            if (callName.toString().matches(exclude) && exclude.length() > maxMatchLength) {
                maxMatchLength = exclude.length();
                isIncluded = false;
            }
        }
        return isIncluded;
    }

    private boolean contextMatches(SingleHookConfig hookConfig, CallingContext context) {
        return true; // not implemented for now
    }

    private boolean typeMatches(SingleHookConfig hookConfig, HookType hookType) {
        for (HookType type : hookConfig.getHookTypes()) {
            if (type.equals(hookType)) {
                return true;
            }
        }
        return false;
    }

}
