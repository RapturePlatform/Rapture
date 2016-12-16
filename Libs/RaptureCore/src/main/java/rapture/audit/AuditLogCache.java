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
package rapture.audit;

import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import rapture.common.AuditLogConfig;
import rapture.common.CallingContext;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.kernel.Kernel;

/**
 * An auditlog cache holds parsed audit log implementations
 * 
 * @author alan
 * 
 */
public class AuditLogCache {
    private Cache<RaptureURI, Optional<AuditLog>> auditLogs;

    // private Map<String, AuditLog> auditLogs = Collections.synchronizedMap(new
    // HashMap<String, AuditLog>());
    private static Logger log = Logger.getLogger(AuditLogCache.class);

    public AuditLogCache() {
        auditLogs = CacheBuilder.newBuilder()
                .maximumSize(40).
                expireAfterAccess(200, TimeUnit.SECONDS)
                .removalListener(new RemovalListener<RaptureURI, Optional<AuditLog>>() {

            @Override
            public void onRemoval(RemovalNotification<RaptureURI, Optional<AuditLog>> not) {
                //
            }}).build();
    }

    public AuditLog getAuditLog(final CallingContext ctx, final RaptureURI logURI) {
        try {
            Optional<AuditLog> o =  auditLogs.get(logURI, new Callable<Optional<AuditLog>>() {

                @Override
                public Optional<AuditLog> call() throws Exception {
                    try {
                        Optional<AuditLog> log =  Optional.of(createAuditLog(ctx, logURI));
                        
                        return log;
                    } catch(RaptureException e) {
                        log.error("Could not load audit log for " + logURI);
                        return Optional.absent();
                    }
                }
                
            });
            return o.orNull();
        } catch (ExecutionException e) {
           log.error("Could not create audit log " + e.getMessage());
           return null;
        }
    }

    private AuditLog createAuditLog(CallingContext ctx, RaptureURI internalURI) {
        AuditLogConfig aConfig = Kernel.getAudit().getTrusted().getAuditLog(ctx, internalURI.getAuthority());
        if (aConfig != null && !aConfig.getConfig().isEmpty()) {
            log.info("Creating audit log provider for " + internalURI.getAuthority() + " with config " + aConfig.getConfig());
            AuditLog l = AuditLogFactory.getLog(internalURI.getAuthority(), aConfig.getConfig());
            l.setContext(internalURI);
            return l;
        } else {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, String.format(Messages.getString("AuditLogCache.noConfig"), internalURI.getAuthority())); //$NON-NLS-1$
        }
    }

    public void reset(RaptureURI logURI) {
       auditLogs.invalidate(logURI);
    }
}
