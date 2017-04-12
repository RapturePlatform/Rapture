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
package rapture.dsl.idef;

import java.util.ArrayList;
import java.util.List;

public class IndexDefinition {

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        result = prime * result + ((indexName == null) ? 0 : indexName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        IndexDefinition other = (IndexDefinition) obj;
        if (fields == null) {
            if (other.fields != null) return false;
        } else if (!fields.equals(other.fields)) return false;
        if (indexName == null) {
            if (other.indexName != null) return false;
        } else if (!indexName.equals(other.indexName)) return false;
        return true;
    }

    private String indexName;

	private List<FieldDefinition> fields = new ArrayList<>();
	public void add(FieldDefinition field) {
		fields.add(field);
	}
	
	public List<FieldDefinition> getFields() {
		return fields;
	}

	public boolean hasDocumentLocators() {
		for(FieldDefinition d : fields) {
			if (d.getLocator() instanceof DocLocator) {
				return true;
			}
		}
		return false;
	}
	
    @Override
    public String toString() {
        return "IndexDefinition [indexName=" + indexName + ", fields=" + fields + "]";
    }

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
}
