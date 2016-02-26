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
package rapture.kernel.plugin;

import org.apache.commons.lang.StringUtils;

import rapture.common.CallingContext;
import rapture.common.PluginTransportItem;
import rapture.common.RaptureRelationship;
import rapture.common.RaptureURI;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RelationshipRepoConfig;
import rapture.kernel.Kernel;

public class RelationshipInstaller implements RaptureInstaller {
    @Override
    public void install(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        if (isRepository(uri)) {
            RelationshipRepoConfig data = JacksonUtil.objectFromJson(item.getContent(),RelationshipRepoConfig.class);
            Kernel.getRelationship().getTrusted().updateRelationshipRepo(context, data);
        } else {
            RaptureRelationship rel = JacksonUtil.objectFromJson(item.getContent(), RaptureRelationship.class);
            Kernel.getRelationship().createRelationship(context, uri.getAuthority(), rel.getFromURI().toString(), rel.getToURI().toString(), rel.getLabel(), rel.getProperties());
        }
    }
    
    private static final boolean isRepository(RaptureURI uri) {
        return StringUtils.isEmpty(uri.getDocPath());
    }

    @Override
    public void remove(CallingContext context, RaptureURI uri, PluginTransportItem item) {
        if (isRepository(uri)) {
            Kernel.getRelationship().deleteRelationshipRepo(context, uri.toString());
        } else {
            Kernel.getRelationship().deleteRelationship(context, uri.toString());
        }
    }
}
