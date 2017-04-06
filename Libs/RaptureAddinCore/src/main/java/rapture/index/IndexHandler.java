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
package rapture.index;

import java.util.List;
import java.util.Map;

import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.model.DocumentMetadata;

public interface IndexHandler {
    String ROWID = "rowId";

    void deleteTable();

    void removeAll(String rowId);

    void setConfig(Map<String, String> config);

    List<TableRecord> queryTable(TableQuery query);

    void setInstanceName(String instanceName);

    // This is the new one, ultimately replacing queryTable above
    TableQueryResult query(String query);

    Long getLatestEpoch();

    void setIndexProducer(IndexProducer indexProducer);

    void initialize();

    void addedRecord(String key, String value, DocumentMetadata mdLatest);

    void updateRow(String key, Map<String, Object> recordValues);

    void ensureIndicesExist();
}
