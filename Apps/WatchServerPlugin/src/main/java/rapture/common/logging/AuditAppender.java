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
package rapture.common.logging;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import rapture.common.CallingContext;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;

/**
 * Custom appender to also write to the Rapture Audit system
 */
public class AuditAppender extends AppenderSkeleton {

    private CallingContext ctx = ContextFactory.getKernelUser();
    private String auditLogUri;

    public AuditAppender(String auditLogUri) {
        this.auditLogUri = auditLogUri;
        setName(AuditAppender.class.getName() + ":" + auditLogUri);
    }

    @Override
    protected void append(LoggingEvent event) {
        Kernel.getAudit().writeAuditEntry(ctx, auditLogUri, "workflow", 1, (String) event.getMessage());
        ThrowableInformation ti = event.getThrowableInformation();
        if (ti != null) {
            Kernel.getAudit().writeAuditEntry(ctx, auditLogUri, "workflow", 1, ExceptionUtils.getStackTrace(ti.getThrowable()));
        }
    }

    public String getAuditLogUri() {
        return auditLogUri;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
