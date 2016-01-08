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
package rapture.relationship;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rapture.common.RaptureRelationship;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.kernel.Kernel;

public abstract class RelationshipStoreContractTest {

    private static String fromURI = "script://my.other/this/script/path";
    private static String toURI = "document://my.authority/aDocumentPath/aDoc";
    private static String authority = "relationship://relationship.authority/";
    private static String label = "created";

    private RelationshipStore store;

    public abstract RelationshipStore getRelationshipStore();

    @Before
    public void setUp() {
        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.initBootstrap();
        this.store = getRelationshipStore();
    }

    @Test
    public void testCreateAndGetRelationship() {

        RaptureRelationship relationship = new RaptureRelationship();

        relationship.setAuthorityURI(authority);
        relationship.setFromURI(new RaptureURI(fromURI));
        relationship.setToURI(new RaptureURI(toURI));
        relationship.setLabel(label);

        RaptureURI relationshipURI = store.createRelationship(relationship, "test");
        
        RaptureRelationship relationship2 = store.getRelationship(relationshipURI);
        
        assertEquals(relationship.getURI(),relationship2.getURI());
        assertEquals(relationship.getFromURI(),relationship2.getFromURI());
        assertEquals(relationship.getToURI(),relationship2.getToURI());
        
    }
    
    @Test
    public void testGetInboundRelations() {
        testCreateAndGetRelationship();
       List<RaptureRelationship> inboundRelationships = store.getInboundRelationships(new RaptureURI(toURI,Scheme.SCRIPT));
        assertEquals(1,inboundRelationships.size());
        assertEquals(new RaptureURI(fromURI),inboundRelationships.get(0).getFromURI());
        assertEquals(new RaptureURI(toURI),inboundRelationships.get(0).getToURI());
    }
    
    @Test
    public void testGetOutboundRelations() {
        testCreateAndGetRelationship();
        List<RaptureRelationship> outboundRelationships = store.getOutboundRelationships(new RaptureURI(fromURI,Scheme.DOCUMENT));
        assertEquals(1,outboundRelationships.size());
        assertEquals(new RaptureURI(fromURI),outboundRelationships.get(0).getFromURI());
        assertEquals(new RaptureURI(toURI),outboundRelationships.get(0).getToURI());
    }
    
    @Test
    public void testDeleteRelationship() {
        testCreateAndGetRelationship();
        List<RaptureRelationship> inboundRelationships = store.getInboundRelationships(new RaptureURI(toURI,Scheme.SCRIPT));
        
        store.deleteRelationship(inboundRelationships.get(0).getURI(),"");
        
        inboundRelationships = store.getInboundRelationships(new RaptureURI(fromURI,Scheme.SCRIPT));
        
        assertEquals(0,inboundRelationships.size());
    }
    
    @Test
    public void testGetLabels() {
        testCreateAndGetRelationship();
        List<RaptureRelationship> labeledRelationships = store.getLabeledRelationships(label);
        assertEquals(1,labeledRelationships.size());
        testCreateAndGetRelationship();
        labeledRelationships = store.getLabeledRelationships(label);
        assertEquals(2,labeledRelationships.size());
        labeledRelationships = store.getLabeledRelationships("unknown");
        assertEquals(0,labeledRelationships.size());
    }

}
