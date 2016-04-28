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

import org.apache.log4j.Logger;

import rapture.common.CallingContext;
import rapture.common.Messages;
import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureJobStorage;
import rapture.common.RaptureScriptStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TagValueType;
import rapture.common.api.TagApi;
import rapture.common.dp.WorkOrderStorage;
import rapture.common.dp.WorkflowStorage;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.TagDescription;
import rapture.common.model.TagDescriptionStorage;

public class TagApiImpl extends KernelBase implements TagApi {
    private static Logger logger = Logger.getLogger(TagApiImpl.class);

    public TagApiImpl(Kernel raptureKernel) {
        super(raptureKernel);
    }


	@Override
	public TagDescription createTagDescription(CallingContext context,
			String tagUri, String description, String valueType, String valueSet) {
        RaptureURI internalURI = new RaptureURI(tagUri, Scheme.TAG);

        TagDescription d = new TagDescription();
        d.setName(internalURI.getAuthority() + "/" + internalURI.getDocPath());
        d.setValueSet(valueSet);
        d.setValueType(TagValueType.valueOf(valueType));
        TagDescriptionStorage.add(d, context.getUser(), "Adding tag description");
        

		return d;
	}

	@Override
	public Boolean deleteTagDescription(CallingContext context, String tagUri) {
        if (tagUri.endsWith("/")) {
        	TagDescriptionStorage.removeFolder(tagUri);
        } else {
            RaptureURI internalURI = new RaptureURI(tagUri, Scheme.TAG);
            TagDescriptionStorage.deleteByAddress(internalURI, context.getUser(), "Removed tag description");
        }
        return true;
	}

	@Override
	public TagDescription getTagDescription(CallingContext context,
			String tagUri) {
		return TagDescriptionStorage.readByAddress(new RaptureURI(tagUri, Scheme.TAG));
	}

	@Override
	public DocumentMetadata applyTag(CallingContext context, String docUri,
			String tagUri, String value) {
		// apply tag is basically a trusted call on the doc interface (in this case)
		RaptureURI uri = new RaptureURI(docUri, Scheme.DOCUMENT);
		switch(uri.getScheme()) {
		case DOCUMENT:
			return Kernel.getDoc().getTrusted().applyTag(context, docUri, tagUri, value);
		case SCRIPT:
			return RaptureScriptStorage.applyTag(uri, context.getUser(), tagUri, value);
		case WORKFLOW:
			return WorkflowStorage.applyTag(uri, context.getUser(), tagUri, value);
		case WORKORDER:
			return WorkOrderStorage.applyTag(uri, context.getUser(), tagUri, value);
		case JOB:
			return RaptureJobStorage.applyTag(uri, context.getUser(), tagUri, value);
		default:
			log.error("Do not know how to work with " + uri);
			return null;
		}
			
	}

	@Override
	public DocumentMetadata applyTags(CallingContext context, String docUri,
			Map<String, String> tagMap) {
		RaptureURI uri = new RaptureURI(docUri, Scheme.DOCUMENT);
		switch(uri.getScheme()) {
		case DOCUMENT:
			return Kernel.getDoc().getTrusted().applyTags(context, docUri, tagMap);
		case SCRIPT:
			return RaptureScriptStorage.applyTags(uri, context.getUser(), tagMap);
		case WORKFLOW:
			return WorkflowStorage.applyTags(uri, context.getUser(), tagMap);
		case WORKORDER:
			return WorkOrderStorage.applyTags(uri, context.getUser(), tagMap);
		case JOB:
			return RaptureJobStorage.applyTags(uri, context.getUser(), tagMap);
		default:
			log.error("Do not know how to work with " + uri);
			return null;
		}
	}

	@Override
	public DocumentMetadata removeTag(CallingContext context, String docUri,
			String tagUri) {
		RaptureURI uri = new RaptureURI(docUri, Scheme.DOCUMENT);
		switch(uri.getScheme()) {
		case DOCUMENT:
			return Kernel.getDoc().getTrusted().removeTag(context, docUri, tagUri);
		case SCRIPT:
			return RaptureScriptStorage.removeTag(uri, context.getUser(), tagUri);
		case WORKFLOW:
			return WorkflowStorage.removeTag(uri, context.getUser(), tagUri);
		case WORKORDER:
			return WorkOrderStorage.removeTag(uri, context.getUser(), tagUri);
		case JOB:
			return RaptureJobStorage.removeTag(uri, context.getUser(), tagUri);
		default:
			log.error("Do not know how to work with " + uri);
			return null;
		}
	}

	@Override
	public DocumentMetadata removeTags(CallingContext context, String docUri,
			List<String> tags) {
		RaptureURI uri = new RaptureURI(docUri, Scheme.DOCUMENT);
		switch(uri.getScheme()) {
		case DOCUMENT:
			return Kernel.getDoc().getTrusted().removeTags(context, docUri, tags);
		case SCRIPT:
			return RaptureScriptStorage.removeTags(uri, context.getUser(), tags);
		case WORKFLOW:
			return WorkflowStorage.removeTags(uri, context.getUser(), tags);
		case WORKORDER:
			return WorkOrderStorage.removeTags(uri, context.getUser(), tags);
		case JOB:
			return RaptureJobStorage.removeTags(uri, context.getUser(), tags);
		default:
			log.error("Do not know how to work with " + uri);
			return null;
		}
	}


	@Override
	public List<RaptureFolderInfo> getChildren(CallingContext context,
			String tagUri) {
		return TagDescriptionStorage.getChildren(tagUri);
	}

  
}
