package rapture.relationship.cassandra;

import java.util.HashMap;

import org.junit.After;

import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.relationship.RelationshipStore;
import rapture.relationship.RelationshipStoreContractTest;


public class CassandraRelationshipStoreTest extends RelationshipStoreContractTest {

    CassandraRelationshipStore store;
    
    public CassandraRelationshipStoreTest() {
        this.store = new CassandraRelationshipStore();
        store.setStoreURI(RaptureURI.builder(Scheme.RELATIONSHIP, "test").build());
        store.setConfig(new HashMap<String, String>());
    }
    
    @Override
    public RelationshipStore getRelationshipStore() {
        return store;
    }
    
    @After
    public void tearDown() {
        store.drop();
    }

}
