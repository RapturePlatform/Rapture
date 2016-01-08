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
package rapture.common.hooks;

import java.net.HttpURLConnection;
import java.util.List;

import org.apache.log4j.Logger;

import rapture.api.hooks.impl.AbstractApiHook;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;

public enum HookFactory {

    INSTANCE;

    private Logger log = Logger.getLogger(getClass());

    /**
     * Creates a hook based on a hook config
     * @param hookConfig
     * @return
     * @throws RaptureException
     */
    public AbstractApiHook createHook(SingleHookConfig hookConfig) throws RaptureException {
        try {
            Class<?> hookClass = Class.forName(hookConfig.getClassName());
            AbstractApiHook hook = (AbstractApiHook) hookClass.newInstance();
            hook.setId(hookConfig.getId());
            return hook;
        } catch (Exception e) {
            log.error("Error", e);
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, "Error! Cannot create hook from class " + hookConfig.getClassName());
        }
    }

    /**
     * Creates a {@link SingleHookConfig} object based on a hook and also other required parameters
     * @param hook
     * @param id
     * @param hookTypes
     * @param includes
     * @param excludes
     * @return
     */
    public SingleHookConfig createConfig(AbstractApiHook hook, String id, List<HookType> hookTypes, List<String> includes, List<String> excludes) {
        SingleHookConfig hookConfig = new SingleHookConfig();
        hookConfig.setId(id);
        hookConfig.setClassName(hook.getClass().getCanonicalName());
        hookConfig.setHookTypes(hookTypes);
        hookConfig.setIncludes(includes);
        hookConfig.setExcludes(excludes);
        return hookConfig;
    }

}
