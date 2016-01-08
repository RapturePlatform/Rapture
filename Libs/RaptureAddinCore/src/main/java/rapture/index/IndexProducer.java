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
package rapture.index;

import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.DocumentMetadata;
import rapture.dsl.idef.FieldDefinition;
import rapture.dsl.idef.IndexDefinition;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * //todo this needs to be updated to truly support multiple indices
 * <p/>
 * An index producer can create index records (things that need to be stored)
 * from a RaptureURI, MetaData and Content
 *
 * @author alanmoore
 */
public class IndexProducer {
    private List<IndexDefinition> indexDefinitions;

    public IndexProducer(List<IndexDefinition> indexDefinitions) {
        this.indexDefinitions = indexDefinitions; // IndexDefinitionFactory.getDefinition(config.getConfig());
    }

    public IndexProducer() {
        this.indexDefinitions = new ArrayList<>(); // IndexDefinitionFactory.getDefinition(config.getConfig());
    }

    public List<IndexRecord> getIndexRecords(String key, String content, DocumentMetadata meta) {
        List<IndexRecord> list = new LinkedList<>();
        for (IndexDefinition definition : indexDefinitions) {
            list.add(createIndexFor(key, content, meta, definition));
        }
        return list;
    }

    private IndexRecord createIndexFor(String key, String content, DocumentMetadata meta, IndexDefinition indexDefinition) {
        // If there are no DocumentLocator instances, don't parse the content
        Map<String, Object> mappedContent = null;
        boolean indexFromContent = indexDefinition.hasDocumentLocators();
        if (indexFromContent) {
            mappedContent = JacksonUtil.getMapFromJson(content);
        }
        IndexRecord record = new IndexRecord();
        for (FieldDefinition def : indexDefinition.getFields()) {
            Object value = def.getLocator().value(key, mappedContent, meta);
            record.putEntry(def.getName(), convert(value));
        }
        return record;
    }

    private Object convert(Object value) {
        return value;
    }

    @Override
    public String toString() {
        return "IndexProducer{" +
                "definitions=" + indexDefinitions +
                '}';
    }

    public void addIndexDefinition(IndexDefinition indexDefinition) {
        indexDefinitions.add(indexDefinition);
    }

    public void setIndexDefinitions(List<IndexDefinition> indexDefinitions) {
        this.indexDefinitions = indexDefinitions;
    }

    public List<IndexDefinition> getIndexDefinitions() {
        return indexDefinitions;
    }
}
