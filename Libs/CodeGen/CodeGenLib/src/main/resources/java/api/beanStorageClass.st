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

/**
 * This file is autogenerated and any changes will be overwritten.
 */

package <package>;

<importFactory>

import rapture.common.RaptureFolderInfo;
import rapture.common.RaptureURI;
import rapture.common.TableQueryResult;
import rapture.object.storage.StorageLocationFactory;
import rapture.kernel.pipeline.ObjectStorageSearchPublisher;

import rapture.common.impl.jackson.JsonContent;
import rapture.common.model.DocumentMetadata;
import rapture.common.model.DocumentWithMeta;
import rapture.object.storage.ObjectFilter;
import rapture.object.storage.ObjectStorage;
import rapture.persistence.storable.mapper.impl.jackson.StorableObjectMapper;
import rapture.repo.RepoVisitor;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

<serDeserImports>

// FTS is : <fts>
@SuppressWarnings("all")
public class <name>Storage {
    private static final Logger log = Logger.getLogger(<name>Storage.class);
    private static final <name>StorableInfo storableInfo = new <name>StorableInfo();

    <methods; separator="\n">

    <objectMapper>
<if(serDeserCode)>
    <serDeserCode>
<endif>
}	
