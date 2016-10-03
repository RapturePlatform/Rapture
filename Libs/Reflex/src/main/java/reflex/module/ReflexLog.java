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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import rapture.common.api.ScriptingApi;
import reflex.ReflexException;
import reflex.value.ReflexValue;

/**
 * Module that attempts to log to the server-side log of the reflex container and automatically logs to the appropriate step if run within the context of a
 * workflow step
 * 
 * @author dukenguyen
 *
 */
public class ReflexLog extends AbstractModule {

    private static final Logger log = Logger.getLogger(ReflexLog.class);

    private String auditLogUri;

    private ScriptingApi api;

    @Override
    public void configure(List<ReflexValue> parameters) {
        if (parameters.size() == 1) {
            auditLogUri = parameters.get(0).asString();
            log.info("Reflex script will log to audit log at: " + auditLogUri);
        }
        api = handler.getApi();
    }

    public void debug(List<ReflexValue> params) {
        log(Level.DEBUG, params);
    }

    public void info(List<ReflexValue> params) {
        log(Level.INFO, params);
    }

    public void warn(List<ReflexValue> params) {
        log(Level.WARN, params);
    }

    public void error(List<ReflexValue> params) {
        log(Level.ERROR, params);
    }

    public void trace(List<ReflexValue> params) {
        log(Level.TRACE, params);
    }

    public void fatal(List<ReflexValue> params) {
        log(Level.FATAL, params);
    }

    private void log(Level level, List<ReflexValue> params) {
        if (params.size() == 1) {
            String msg = params.get(0).asObject().toString();
            log.log(level, msg);
            if (api != null && auditLogUri != null) {
                api.getAudit().writeAuditEntry(auditLogUri, "reflex", level.toInt(), msg);
            }
        } else {
            throw new ReflexException(-1, "Invalid number of arguments to log statement");
        }
    }

}
