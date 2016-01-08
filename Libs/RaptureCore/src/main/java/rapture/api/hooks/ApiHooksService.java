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

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.api.hooks.impl.AbstractApiHook;
import rapture.common.CallingContext;
import rapture.common.hooks.CallName;
import rapture.common.hooks.HookType;
import rapture.common.hooks.HooksConfig;
import rapture.common.hooks.SingleHookConfig;

/**
 * This is a singleton used to track calls to the Rapture API and perform
 * operations
 * 
 * @author bardhi
 * 
 */
public class ApiHooksService {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());

    private HookFilter hookFilter;
    private HooksConfig hooksConfig;

    public ApiHooksService() {
        hooksConfig = new HooksConfig();
        hooksConfig.setIdToHook(new HashMap<String, SingleHookConfig>());
        hookFilter = new HookFilter();
    }

    /**
     * Call any pre-hooks
     * 
     * @param context
     * @param callName
     */
    public void pre(CallingContext context, CallName callName) {
        List<AbstractApiHook> hooks = hookFilter.filter(hooksConfig.getIdToHook().values(), HookType.PRE, context, callName);
        for (AbstractApiHook hook : hooks) {
            hook.execute(HookType.PRE, context, callName);
        }
    }

    /**
     * Call any post hooks
     * 
     * @param context
     * @param callName
     */
    public void post(CallingContext context, CallName callName) {
        List<AbstractApiHook> hooks = hookFilter.filter(hooksConfig.getIdToHook().values(), HookType.POST, context, callName);
        for (AbstractApiHook hook : hooks) {
            hook.execute(HookType.PRE, context, callName);
        }

    }

    public void configure(HooksConfig hooksConfig) {
        this.hooksConfig = hooksConfig;
    }

    public void addSingleHookConfig(SingleHookConfig singleHookConfig) {
        this.hooksConfig.getIdToHook().put(singleHookConfig.getId(), singleHookConfig);
    }

    public SingleHookConfig removeSingleHookConfig(String id) {
        return this.hooksConfig.getIdToHook().remove(id);
    }
}
