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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.log4j.Logger;

import com.google.cloud.storage.Storage;

import rapture.common.Messages;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.dsl.idgen.IdGenStore;
import rapture.util.IDGenerator;

/**
 * A idgen implemented on GoogleDb
 */

public class IdGenGoogleStorage implements IdGenStore {
    private static Logger log = Logger.getLogger(IdGenGoogleStorage.class);

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
    private GoogleBlobStore blobStore;
    private String id;

    public static void setStorageForTesting(Storage storage) {
        GoogleBlobStore.setStorageForTesting(storage);
    }

    public IdGenGoogleStorage() {
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        if (blobStore != null) throw new RuntimeException("Already configured");
        blobStore = new GoogleBlobStore();
        blobStore.setConfig(config);
        this.config = config;
        id = IDGenerator.getUUID();
        try {
            resetIdGen(Long.parseLong(config.get(SEQ)));
        } catch (NumberFormatException e) {
            log.info("SEQ invalid. Initializing to zero");
            resetIdGen(0L);
        }
    }

    Map<String, Object> loadFromBlobStore(String key) {
        if (blobStore == null) throw new RuntimeException("IdGenGoogleStorage not configured - call setConfig first");
        try (InputStream blob = blobStore.getBlob(key)) {
            if (blob != null) {
                try (Scanner s = new Scanner(blob)) {
                    String entity = s.useDelimiter("\\A").hasNext() ? s.next() : null;
                    if (entity != null) return JacksonUtil.getMapFromJson(entity);
                }
            }
        } catch (IOException e) {
            // declared on close call
        }
        return null;
    }

    @Override
    public Long getNextIdGen(Long interval) {
        Map<String, Object> map = loadFromBlobStore(idGenName);
        if (!((Boolean) map.get(VALID))) throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_BAD_REQUEST,
                googleMsgCatalog.getMessage("GeneratorInvalid"));
        if (map != null) {
            Long next = interval;
            Object o = map.get(SEQ);
            if (o instanceof Number) next += ((Number) o).longValue();
            map.put(SEQ, next);
            try (InputStream isis = new ByteArrayInputStream(JacksonUtil.jsonFromObject(map).getBytes())) {
                blobStore.storeBlob(idGenName, false, isis);
            } catch (IOException e) {
                // declared on close call
            }
            return next;
        }
        return null;
    }

    @Override
    public void resetIdGen(Long number) {
        Map<String, Object> map = new HashMap<>();
        for (Entry<String, String> e : config.entrySet()) {
            map.put(e.getKey(), e.getValue());
        }
        map.put(SEQ, number);
        map.put(VALID, true);
        InputStream isis = new ByteArrayInputStream(JacksonUtil.jsonFromObject(map).getBytes());
        blobStore.storeBlob(idGenName, false, isis);
    }

    @Override
    public void invalidate() {
        Map<String, Object> map = loadFromBlobStore(idGenName);
        map.put(VALID, false);
        InputStream isis = new ByteArrayInputStream(JacksonUtil.jsonFromObject(map).getBytes());
        blobStore.storeBlob(idGenName, false, isis);
    }

    @Override
    public void makeValid() {
        Map<String, Object> map = loadFromBlobStore(idGenName);
        map.put(VALID, true);
        InputStream isis = new ByteArrayInputStream(JacksonUtil.jsonFromObject(map).getBytes());
        blobStore.storeBlob(idGenName, false, isis);
    }

    public boolean isValid() {
        Map<String, Object> map = loadFromBlobStore(idGenName);
        return (((Boolean) map.get(VALID)));
    }

    @Override
    public void init() {
    }
}
