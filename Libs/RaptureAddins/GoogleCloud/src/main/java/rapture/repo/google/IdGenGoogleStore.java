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
package rapture.repo.google;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Value;

import rapture.common.Messages;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.config.MultiValueConfigLoader;
import rapture.dsl.idgen.IdGenStore;

/**
 * A idgen implemented on GoogleDb
 */

public class IdGenGoogleStore implements IdGenStore {
    private static Logger log = Logger.getLogger(IdGenGoogleStore.class);

    private static final String SEQ = "seq";
    private static final String IDGEN = "idgen";
    private static final String IDGEN_NAME = "idgenName";
    private static final String TABLE_NAME = "prefix";
    private static final String VALID = "valid";
    private String idGenName = "idgen";
    private String instanceName = "default";
    private Messages googleMsgCatalog = new Messages("Google");
    private String kind;
    private Map<String, String> config = null;
    private Datastore datastore = null;

    public IdGenGoogleStore() {
    }

    // For unit testing.
    protected IdGenGoogleStore(Datastore testdatastore, String testkind) {
        datastore = testdatastore;
        kind = testkind;
        makeValid();
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        kind = StringUtils.stripToNull(config.get("prefix"));
        if (kind == null) throw new RuntimeException("Prefix not set in config " + JacksonUtil.formattedJsonFromObject(config));
        String projectId = StringUtils.trimToNull(config.get("projectid"));
        if (projectId == null) {
            projectId = MultiValueConfigLoader.getConfig("GOOGLE-projectid");
            if (projectId == null) {
                throw new RuntimeException("Project ID not set in RaptureGOOGLE.cfg or in config " + config);
            }
        }
        datastore = DatastoreOptions.newBuilder().setProjectId(projectId).build().getService();
        this.config = config;
        makeValid();
    }

    @Override
    public Long getNextIdGen(Long interval) {
        if (!isValid()) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, googleMsgCatalog.getMessage("GeneratorInvalid"));

        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(kind);
        Entity entity = datastore.get(entityKey);
        Long next = interval;

        if (entity != null) {
            Set<String> names = entity.getNames();
            if ((names == null) || (names.size() != 1)) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    googleMsgCatalog.getMessage("IdGenerator"));

            Value<?> value = entity.getValue(names.iterator().next());
            if (!(value instanceof LongValue)) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                    googleMsgCatalog.getMessage("IdGenerator"));
            next = ((LongValue) value).get() + interval;
        }
        try {
            Entity.Builder builder = Entity.newBuilder(entityKey);
            builder.set(kind, new LongValue(next));
            entity = builder.build();
            datastore.put(entity);
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialise idgen ", e);
        }
        return next;
    }

    @Override
    public void init() {

    }

    @Override
    public void resetIdGen(Long number) {
        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(kind);
        Entity entity = datastore.get(entityKey);
        try {
            Entity.Builder builder = Entity.newBuilder(entityKey);
            builder.set(kind, new LongValue(number));
            entity = builder.build();
            datastore.put(entity);
        } catch (Exception e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, googleMsgCatalog.getMessage("CannotReset"), e);
        }
    }


    @Override
    public void invalidate() {
        setValid(false);
    }

    @Override
    public void makeValid() {
        setValid(true);
    }

    public void setValid(Boolean flag) {
        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(VALID);
        Entity entity = datastore.get(entityKey);
        try {
            Entity.Builder builder = Entity.newBuilder(entityKey);
            builder.set(kind, new BooleanValue(flag));
            entity = builder.build();
            datastore.put(entity);
        } catch (Exception e) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, googleMsgCatalog.getMessage("CannotUpdate" + flag), e);
        }
    }

    public boolean isValid() {
        Key entityKey = datastore.newKeyFactory().setKind(kind).newKey(VALID);
        Entity entity = datastore.get(entityKey);
        if (entity != null) {
            Set<String> names = entity.getNames();
            if ((names != null) && (names.size() == 1)) {
                Value<?> value = entity.getValue(names.iterator().next());
                if (value instanceof BooleanValue) return ((BooleanValue) value).get();
            }
        }
        throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST, googleMsgCatalog.getMessage("isValid"));
    }
}
