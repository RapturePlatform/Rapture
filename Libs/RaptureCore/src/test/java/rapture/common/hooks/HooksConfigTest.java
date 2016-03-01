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
package rapture.common.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

import rapture.api.hooks.impl.AbstractApiHook;
import rapture.api.hooks.impl.AuditApiHook;
import rapture.api.hooks.impl.LogApiHook;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.storable.helpers.HooksConfigHelper;

import com.google.common.collect.Lists;

public class HooksConfigTest {

    @Test
    public void toJson() {
        HooksConfig hooksConfig = createHooksConfig();
        String json = JacksonUtil.jsonFromObject(hooksConfig);
        assertNotNull(json);

        SingleHookConfig hookConfig1 = HooksConfigHelper.getHooks(hooksConfig).iterator().next();
        HooksConfig hooksConfigNew = JacksonUtil.objectFromJson(json, HooksConfig.class);
        assertEquals(2, HooksConfigHelper.getHooks(hooksConfigNew).size());
        SingleHookConfig hookConfigNew = HooksConfigHelper.getHooks(hooksConfigNew).iterator().next();
        assertEquals(hookConfig1.getIncludes().size(), hookConfigNew.getIncludes().size());
        assertEquals(hookConfig1.getExcludes().size(), hookConfigNew.getExcludes().size());
        assertEquals(hookConfig1.getIncludes().get(0), hookConfigNew.getIncludes().get(0));
    }

    public static HooksConfig createHooksConfig() {
        HooksConfig hooksConfig = new HooksConfig();
        hooksConfig.setIdToHook(new HashMap<String, SingleHookConfig>());
        AbstractApiHook hook = new AuditApiHook();
        String id = AuditApiHook.getStandardId();
        ArrayList<HookType> hookTypes = Lists.newArrayList(HookType.PRE, HookType.POST);
        ArrayList<String> includes = Lists.newArrayList(CallName.Admin_addIPToWhiteList.toString(), CallName.Admin_addIPToWhiteList.getApiName().toString()
                + ".*");
        ArrayList<String> excludes = Lists.newArrayList(ApiName.Admin.toString() + ".*");
        SingleHookConfig hookConfig = HookFactory.INSTANCE.createConfig(hook, id, hookTypes, includes, excludes);
        HooksConfigHelper.registerHook(hooksConfig, hookConfig);

        hook = new LogApiHook();
        id = LogApiHook.getStandardId();
        hookTypes = Lists.newArrayList(HookType.PRE);
        includes = Lists.newArrayList(ApiName.User.toString() + ".*");
        excludes = Lists.newArrayList(CallName.User_getWhoAmI.toString(), ApiName.Admin.toString() + ".*");
        hookConfig = HookFactory.INSTANCE.createConfig(hook, id, hookTypes, includes, excludes);
        HooksConfigHelper.registerHook(hooksConfig, hookConfig);

        return hooksConfig;
    }

    @Test
    public void toObject() {
        String json = "{\"idToHook\":{\"LogHook\":{\"id\":\"LogHook\",\"className\":\"rapture.api.hooks.impl.LogApiHook\",\"hookTypes\":[\"PRE\"],\"includes\":[\"User.*\"],\"excludes\":[\"User_addCommentary\",\"Admin.*\"]},\"AuditHook\":{\"id\":\"AuditHook\",\"className\":\"rapture.api.hooks.impl.AuditApiHook\",\"hookTypes\":[\"PRE\",\"POST\"],\"includes\":[\"Admin_addIPToWhiteList\",\"Admin.*\"],\"excludes\":[\"Admin_addRemote.*\",\"Type.*\"]}}}";
        HooksConfig hooksConfig = JacksonUtil.objectFromJson(json, HooksConfig.class);
        assertEquals(2, HooksConfigHelper.getHooks(hooksConfig).size());
    }

}
