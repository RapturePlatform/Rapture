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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rapture.common.RaptureURI;
import rapture.common.model.RelationshipRepoConfig;
import rapture.common.model.RelationshipRepoConfigPathBuilder;
import rapture.common.model.RelationshipRepoConfigStorage;
import rapture.relationship.RelationshipStore;
import rapture.relationship.RelationshipStoreFactory;

public class RelationshipRepoCache {

    private Map<String, RelationshipStore> relationshipRepoCache = new HashMap<String, RelationshipStore>();
    private static Logger log = Logger.getLogger(RelationshipRepoCache.class);

    public RelationshipStore getRelationshipStore(RaptureURI storeURI) {

        String authority = storeURI.getAuthority();
        if (relationshipRepoCache.containsKey(authority)) {
            return relationshipRepoCache.get(authority);
        } else {
            RaptureURI storageLocation = new RelationshipRepoConfigPathBuilder().authority(storeURI.getAuthority()).buildStorageLocation();
            RelationshipRepoConfig relationshipConfig = RelationshipRepoConfigStorage.readByStorageLocation(storageLocation);
            if (relationshipConfig != null) {
                RelationshipStore repNew = RelationshipStoreFactory.getStore(storeURI, relationshipConfig.getConfig());
                relationshipRepoCache.put(authority, repNew);
                log.info("Created repo");
                return repNew;
            } else {
                log.error("No relationship info for " + storageLocation.toString());
            }
        }
        return null;
    }

    public void removeEntry(RaptureURI authority) {
        relationshipRepoCache.remove(authority);
    }

}
