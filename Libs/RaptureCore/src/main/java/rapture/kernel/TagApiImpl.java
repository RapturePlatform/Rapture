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
import rapture.common.RaptureScriptStorage;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.TagValueType;
import rapture.common.api.TagApi;
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
		return Kernel.getDoc().getTrusted().applyTag(context, docUri, tagUri, value);
	}

	@Override
	public DocumentMetadata applyTags(CallingContext context, String docUri,
			Map<String, String> tagMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentMetadata removeTag(CallingContext context, String docUri,
			String tagUri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DocumentMetadata removeTags(CallingContext context, String docUri,
			List<String> tags) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<RaptureFolderInfo> getChildren(CallingContext context,
			String tagUri) {
		return TagDescriptionStorage.getChildren(tagUri);
	}

  
}
