/**
 * Copyright (C) 2011-2015 Incapture Technologies LLC
 *
 * This is an autogenerated license statement. When copyright notices appear below
 * this one that copyright supercedes this statement.
 *
 * Unless required by applicable law or agreed to in writing, software is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * Unless explicit permission obtained in writing this software cannot be distributed.
 */
package rapture.blob.mongodb;

import org.junit.Before;
import org.junit.experimental.categories.Category;

import rapture.blob.BlobStoreContractTest;
import rapture.blob.BlobStoreFactory;
import rapture.blob.BlobStore;
import rapture.common.MongoDbTests;

@Category(MongoDbTests.class) 
public class MongoDBMultipartBlobStoreTest extends BlobStoreContractTest {
    
    private BlobStore store;

    @Before
    public void setUp() {
        store = BlobStoreFactory.createBlobStore("BLOB {} USING MONGODB { grid=\"integrationTest\", multipart=\"true\" }");
    }

    @Override
    public BlobStore getBlobStore() {
        return store;
    }

}
