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
package rapture.relationship.memory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureRelationship;
import rapture.common.RaptureURI;
import rapture.relationship.RelationshipStore;

import com.google.common.collect.ArrayListMultimap;

public class MemoryRelationshipStore implements RelationshipStore {

    private HashMap<String, RaptureRelationship> relationshipStore;
    private ArrayListMultimap<RaptureURI, RaptureRelationship> inboundMultiMap;
    private ArrayListMultimap<RaptureURI, RaptureRelationship> outboundMultiMap;
    private ArrayListMultimap<String, RaptureRelationship> labelMultiMap;

    public MemoryRelationshipStore() {
       dropStore();
    }

    @Override
    public RaptureURI createRelationship(RaptureRelationship relationship, String user) {

        relationshipStore.put(relationship.getURI().toString(), relationship);

        inboundMultiMap.put(relationship.getToURI(), relationship);
        outboundMultiMap.put(relationship.getFromURI(), relationship);
        labelMultiMap.put(relationship.getLabel(), relationship);

        return relationship.getURI();

    }

    @Override
    public RaptureRelationship getRelationship(RaptureURI relationshipURI) {
        return relationshipStore.get(relationshipURI.toString());
    }

    @Override
    public void deleteRelationship(RaptureURI relationshipURI, String user) {

        RaptureRelationship relationship = relationshipStore.get(relationshipURI.toString());

        inboundMultiMap.remove(relationship.getToURI(), relationship);
        outboundMultiMap.remove(relationship.getFromURI(), relationship);
        labelMultiMap.remove(relationship.getLabel(), relationship);

        relationshipStore.remove(relationshipURI.toString());
    }

    @Override
    public List<RaptureRelationship> getInboundRelationships(RaptureURI inboundRelationship) {
        return inboundMultiMap.get(inboundRelationship);
    }

    @Override
    public List<RaptureRelationship> getOutboundRelationships(RaptureURI outboundRelationship) {
        return outboundMultiMap.get(outboundRelationship);
    }

    @Override
    public List<RaptureRelationship> getLabeledRelationships(String label) {
        return labelMultiMap.get(label);
    }

    @Override
    public void setInstanceName(String instance) {
        // For Memory don't do anything
    }

    @Override
    public void setConfig(Map<String, String> config) {
        // For Memory don't do anything
    }

    @Override
    public void setStoreURI(RaptureURI storeURI) {
        // For Memory don't do anything
    }

    @Override
    public List<RaptureFolderInfo> getSubKeys(String prefix) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<RaptureFolderInfo> removeSubKeys(String prefix, Boolean force) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dropStore() {
        this.relationshipStore = new HashMap<String, RaptureRelationship>();
        inboundMultiMap = ArrayListMultimap.create();
        outboundMultiMap = ArrayListMultimap.create();
        labelMultiMap = ArrayListMultimap.create();
    }

}
