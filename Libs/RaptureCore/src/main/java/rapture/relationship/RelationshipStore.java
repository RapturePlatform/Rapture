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
package rapture.relationship;

import java.util.List;
import java.util.Map;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureRelationship;
import rapture.common.RaptureURI;

public interface RelationshipStore {
    
    public RaptureURI createRelationship(RaptureRelationship relationship, String user);
    public RaptureRelationship getRelationship(RaptureURI relationshipURI);
    public void deleteRelationship(RaptureURI relationshipURI, String user);
    
    public List<RaptureRelationship> getInboundRelationships(RaptureURI inboundRelationship);
    public List<RaptureRelationship> getOutboundRelationships(RaptureURI outboundRelationship);
    public List<RaptureRelationship> getLabeledRelationships(String label);
    
    public void setInstanceName(String instance);
    public void setConfig(Map<String,String> config);
    public void setStoreURI(RaptureURI storeURI);
    List<RaptureFolderInfo> getSubKeys(String prefix);
    List<RaptureFolderInfo> removeSubKeys(String prefix, Boolean force);
    public void dropStore();
    

}
