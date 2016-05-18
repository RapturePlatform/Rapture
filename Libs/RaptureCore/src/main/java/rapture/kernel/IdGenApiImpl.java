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
package rapture.kernel;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.RaptureIdGenConfig;
import rapture.common.RaptureIdGenConfigStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.IdGenApi;
import rapture.common.exception.ExceptionToString;
import rapture.common.exception.RaptureException;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JsonContent;
import rapture.config.ConfigLoader;
import rapture.dsl.idgen.IdGenFactory;
import rapture.dsl.idgen.RaptureIdGen;
import rapture.idgen.SystemIdGens;
import rapture.repo.RepoVisitor;

public class IdGenApiImpl extends KernelBase implements IdGenApi {
    private static Logger log = Logger.getLogger(IdGenApiImpl.class);

    private Map<String, RaptureIdGen> idgenCache;

    public IdGenApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
        idgenCache = new HashMap<String, RaptureIdGen>();
    }

    @Override
    public RaptureIdGenConfig createIdGen(CallingContext context, String idGenUri, String config) {
        if (idGenExists(context, idGenUri)) {
            throw RaptureExceptionFactory.create("IdGen already exists: " + idGenUri);
        }
        RaptureURI internalURI = new RaptureURI(idGenUri, Scheme.IDGEN);
        RaptureIdGenConfig cfg = new RaptureIdGenConfig();
        cfg.setName(internalURI.getDocPath());
        cfg.setAuthority(internalURI.getAuthority());
        cfg.setConfig(config);
        RaptureIdGenConfigStorage.add(cfg, context.getUser(), "Created idgen config");
        makeIdGenValid(context, idGenUri);
        nextIds(context, idGenUri, 0L);
        return cfg;
    }

    @Override
    public void deleteIdGen(CallingContext context, String idGenUri) {
        RaptureURI uri = new RaptureURI(idGenUri, Scheme.IDGEN);
        String normalized = uri.toString();
        RaptureIdGen idgen = getIdGenConfig(uri);

        // RAP-2107 Can't delete the IdGen Store?
        idgen.getIdGenStore().resetIdGen(0L);

        idgen.invalidate();
        idgenCache.remove(normalized);
        RaptureIdGenConfigStorage.deleteByFields(uri.getAuthority(), uri.getDocPath(), context.getUser(), "deleted idgen");
    }

    @Override
    public Boolean idGenExists(CallingContext context, String idGenUri) {
        try {
            getIdGenConfig(idGenUri);
            return true;
        } catch (RaptureException e) {
            if (e.getMessage().startsWith("No such IdGen")) return false;
            log.error(ExceptionToString.format(e));
            throw e;
        }
    }

    private RaptureIdGen getIdGenConfig(String idGenUri) {
        RaptureURI internalUri = new RaptureURI(idGenUri, Scheme.IDGEN);
        return getIdGenConfig(internalUri);
    }

    private RaptureIdGen getIdGenConfig(RaptureURI internalUri) {
        String normalized = internalUri.toString();
        if (idgenCache.containsKey(normalized)) {
            return idgenCache.get(normalized);
        } else {
            RaptureIdGenConfig config = RaptureIdGenConfigStorage.readByAddress(internalUri);
            if (config != null) {
                RaptureIdGen f = IdGenFactory.getIdGen(config.getConfig());
                idgenCache.put(normalized, f);
                return f;
            } else {
                throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, "No such IdGen " + internalUri);
            }
        }
    }

    /**
     * Get all of the idgens, for all authorities
     */

    @Override
    public List<RaptureIdGenConfig> getIdGenConfigs(CallingContext context, String authority) {

        RaptureURI internalURI = new RaptureURI(authority, Scheme.IDGEN);
        final List<RaptureIdGenConfig> ret = new ArrayList<RaptureIdGenConfig>();
        String prefix = RaptureIdGenConfigStorage.addressToStorageLocation(internalURI).getDocPath();
        getConfigRepo().visitAll(prefix, null, new RepoVisitor() {
            @Override
            public boolean visit(String name, JsonContent content, boolean isFolder) {
                if (!isFolder) {
                    log.info("Visiting " + name);
                    RaptureIdGenConfig idgen;
                    try {
                        idgen = RaptureIdGenConfigStorage.readFromJson(content);
                        ret.add(idgen);
                    } catch (RaptureException e) {
                        log.error("Could not load document " + name + ", continuing anyway");
                    }
                }
                return true;
            }
        });
        return ret;
    }

    @Override
    public String nextIds(CallingContext context, String idGenUri, Long num) {
        log.debug("Incrementing idGen " + idGenUri + ", and value = " + num);
        RaptureIdGen f = getIdGenConfig(idGenUri);
        return f.incrementIdGen(num);
    }

    @Override
    public String next(CallingContext context, String idGenUri) {
        log.debug("Incrementing idGen " + idGenUri + ", and value = " + 1L);
        RaptureIdGen f = getIdGenConfig(idGenUri);
        return f.incrementIdGen(1L);
    }

    private void makeIdGenValid(CallingContext context, String idGenUri) {
        RaptureIdGen f = getIdGenConfig(idGenUri);
        f.makeValid();
    }

    @Override
    public void setIdGen(CallingContext context, String idGenUri, Long count) {
        RaptureIdGen f = getIdGenConfig(idGenUri);
        f.getIdGenStore().resetIdGen(count);
    }

    @Override
    public RaptureIdGenConfig getIdGenConfig(CallingContext context, String idGenUri) {
        RaptureURI addressUri = new RaptureURI(idGenUri, Scheme.IDGEN);
        return RaptureIdGenConfigStorage.readByAddress(addressUri);
    }

    @Override
    public void setupDefaultIdGens(CallingContext context, Boolean force) {
        boolean retVal = true;
        setupDefaultIdGen(context, SystemIdGens.EVENT_IDGEN_URI, ConfigLoader.getConf().EventIdGenConfig, force);
        setupDefaultIdGen(context, SystemIdGens.ACTIVITY_IDGEN_URI, ConfigLoader.getConf().ActivityIdGenConfig, force);
    }

    private Boolean setupDefaultIdGen(CallingContext context, String idgenUri, String config, Boolean force) {
        if (force || !Kernel.getIdGen().idGenExists(context, idgenUri)) {
            Kernel.getIdGen().createIdGen(context, idgenUri, config);
            return true;
        } else {
            return false;
        }

    }
}
