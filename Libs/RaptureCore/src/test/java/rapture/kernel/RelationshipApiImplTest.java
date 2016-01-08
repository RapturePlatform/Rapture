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
package rapture.kernel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.RaptureRelationship;
import rapture.common.RaptureURI;
import rapture.common.exception.RaptureException;
import rapture.common.model.RelationshipRepoConfig;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;


// - make this extent repoContractTest
public class RelationshipApiImplTest{
    
    private CallingContext callingContext;
    private RelationshipApiImpl relationshipAPI;

    @Before
    public void setUp() {
        
        RaptureConfig.setLoadYaml(false);
        System.setProperty("LOGSTASH-ISENABLED", "false");
        
        this.callingContext = ContextFactory.getKernelUser();
        Kernel.INSTANCE.clearRepoCache(false);
        relationshipAPI = new RelationshipApiImpl(Kernel.INSTANCE);
        Kernel.initBootstrap();
        relationshipAPI.createRelationshipRepo(callingContext, "relationship://my.authority/", "RREP {} USING MEMORY {}");
    }
    
    @Test
    public void testGetRelationshipStores() {
        List<RelationshipRepoConfig> relationshipRepositories = relationshipAPI.getAllRelationshipRepoConfigs(callingContext);
        assertEquals(1,relationshipRepositories.size());
    }

    // For now ignore as we have a global "don't do relationships" default set
    @Test
    @Ignore 
    public void testStoreRelationship() {
        String relationshipURI = relationshipAPI.createRelationship(callingContext, "relationship://my.authority/", "document://my.auth/whatever", "document://my.auth/thething", "test", null);
        RaptureRelationship relationship = relationshipAPI.getRelationship(callingContext, relationshipURI);
        assertEquals(new RaptureURI("document://my.auth/whatever"),relationship.getFromURI());
    }
    
//    @Test 
//    public void testDeleteRelationship() {
//        testStoreRelationship();
//        relationshipAPI.deleteRelationship(context, relationshipURI)
//    }
    
    @Test
    @Ignore 
    public void testGetRelationships() {
        testStoreRelationship();
        List<RaptureRelationship> outboundRelationships = relationshipAPI.getOutboundRelationships(callingContext, "relationship://my.authority/", "document://my.auth/whatever");
        
        assertEquals(1,outboundRelationships.size());
    }
    
    @Test
    public void testIllegalRepoPaths() {
        String repo = "relationship://";
        try {
            relationshipAPI.createRelationshipRepo(ContextFactory.getKernelUser(), repo, "RREP {} using MEMORY {}");
            fail(repo+ " is not a valid Repo URI");
        } catch (RaptureException e) {
            assertEquals("Cannot create a repository without an authority", e.getMessage());
        }
        try {
            relationshipAPI.createRelationshipRepo(ContextFactory.getKernelUser(), "", "RREP {} using MEMORY {}");
            fail(" empty is not a valid Repo URI");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            relationshipAPI.createRelationshipRepo(ContextFactory.getKernelUser(), null, "RREP {} using MEMORY {}");
            fail(" null is not a valid Repo URI");
        } catch (RaptureException e) {
            assertEquals("Argument Repository URI cannot be null or empty", e.getMessage());
        }
        try {
            relationshipAPI.createRelationshipRepo(ContextFactory.getKernelUser(), "document://x/x", "RREP {} using MEMORY {}");
            fail(repo+"x/x is not a valid URI");
        } catch (RaptureException e) {
            assertEquals("A Repository URI may not have a document path component", e.getMessage());
        }
    }
    

}
