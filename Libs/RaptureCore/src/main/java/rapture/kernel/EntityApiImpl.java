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

import java.util.List;
import java.util.Map;

import rapture.kernel.Kernel;
import rapture.common.CallingContext;
import rapture.common.RaptureEntity;
import rapture.common.RaptureEntityStorage;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.api.EntityApi;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.RaptureFolderInfo;

public class EntityApiImpl extends KernelBase implements EntityApi {

     public EntityApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }

 	@Override
 	public RaptureEntity getEntity(CallingContext context, String entityUri) {
 		RaptureURI internalURI = new RaptureURI(entityUri, Scheme.ENTITY);
 		return RaptureEntityStorage.readByAddress(internalURI);
 	}

 	@Override
 	public void putEntity(CallingContext context, String entityUri,
 			RaptureEntity entity) {
 		RaptureURI internalURI = new RaptureURI(entityUri, Scheme.ENTITY);
		entity.setName(internalURI.getShortPath());
		RaptureEntityStorage.add(entity, context.getUser(), "Added entity");
 	}

 	@Override
 	public void deleteEntity(CallingContext context, String entityUri) {
 		RaptureURI internalURI = new RaptureURI(entityUri, Scheme.ENTITY);
 		RaptureEntityStorage.deleteByAddress(internalURI, context.getUser(), "Remove entity");
 	}

 	@Override
 	public List<RaptureFolderInfo> getChildren(CallingContext context,
 			String uriPrefix) {
 		return RaptureEntityStorage.getChildren(uriPrefix);
 	}


	@Override
	public String putEntityDocument(CallingContext context, String entityUri,
			String content) {
		// 1. Load the entity definition
		RaptureEntity e = getEntity(context, entityUri);
		// 2. Validate document against the structure in that definition
		List<String> res = Kernel.getTransform().validateDocument(context, content, e.getStructureUri());
		if (res.isEmpty()) {
			// 3. Work out the URI based on the fields in the document
		       Map<String, Object> docMap = JacksonUtil.getMapFromJson(content);
			   String entityUriPart = getUriPart(docMap, e.getNamingFields());
			// 4. Do a put document on that	
			   String fullUri = e.getRepoUri() + e.getPrefixInRepo() + entityUriPart;
			   System.out.println("Document URI for entity is " + fullUri);
			   Kernel.getDoc().putDoc(context, fullUri, content);
			   return entityUriPart;
		} else {
			System.out.println(res);
		}
		return "";
	}

    private String getUriPart(Map<String, Object> docMap, List<String> fields) {
    	StringBuilder sb = new StringBuilder();
    	fields.forEach( s -> {
    		sb.append("/");
    		sb.append(getField(docMap, s.split("\\.")));
    		});
    	return sb.toString();
    }
    
    private String getField(Map<String, Object> docMap, String[] s) {
    	Map<String, Object> current = docMap;
    	for(int i=0; i< s.length; i++) {
    		if (i == (s.length-1)) {
    			return current.get(s[i]).toString();
    		} else {
    			current = (Map<String, Object>)current.get(s[i]);
    		}
    	}
    	return "";
    }
    
    private String fixContentUri(String contentUri) {
    	if (contentUri.startsWith("//")) {
			contentUri = contentUri.substring(1);
		} else if (!contentUri.startsWith("/")) {
			contentUri = "/" + contentUri;
		}
		return contentUri;	
    }
    
	@Override
	public String getEntityDocument(CallingContext context, String entityUri,
			String contentUri) {
		// 1. Load the entity definition
		// 2. Construct the real URI for this document
		// 3. Load it.
		RaptureEntity e = getEntity(context, entityUri);
		String fullUri = e.getRepoUri() + e.getPrefixInRepo() + fixContentUri(contentUri);	
		System.out.println("Full URI is " + fullUri);
		return Kernel.getDoc().getDoc(context, fullUri);
	}

	@Override
	public void deleteEntityDocument(CallingContext context, String entityUri,
			String contentUri) {
		// 1. Load the entity definition
		// 2. Construct the real URI for this document
		// 3. Delete it.	
		RaptureEntity e = getEntity(context, entityUri);
		String fullUri = e.getRepoUri() + e.getPrefixInRepo() + fixContentUri(contentUri);	
		System.out.println("Full URI is " + fullUri);
		Kernel.getDoc().deleteDoc(context, fullUri);
	}

	@Override
	public Map<String, RaptureFolderInfo> listDocsByUriPrefix(CallingContext context, String entityUri, String uriPrefix, int depth) {
		// 1. Load the entity definition
		// 2. Construct the prefix
		// 3. Call getChildren through the doc interface
		RaptureEntity e = getEntity(context, entityUri);
		String fullUri = e.getRepoUri() + e.getPrefixInRepo() + fixContentUri(uriPrefix);	
		System.out.println("Full URI is " + fullUri);
		return Kernel.getDoc().listDocsByUriPrefix(context, fullUri, depth);
	}

 
}
