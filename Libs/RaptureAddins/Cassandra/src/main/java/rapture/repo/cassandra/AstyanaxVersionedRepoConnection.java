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
package rapture.repo.cassandra;

import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.util.RangeBuilder;
import rapture.common.exception.RaptureExceptionFactory;
import rapture.repo.cassandra.key.NormalizedKey;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Map;

public class AstyanaxVersionedRepoConnection extends AstyanaxRepoConnection {

    public AstyanaxVersionedRepoConnection(String instance, Map<String, String> config) {
        super(instance, config);
    }

    @Override
    protected String getVersionColumnName() {
        return "" + System.currentTimeMillis();
    }

    @Override
    public String get(String key, String versionColumnName) {
        NormalizedKey normalizedKey = keyNormalizer.normalizeKey(key);
        ColumnList<String> result;
        try {
            result = keyspace.prepareQuery(columnFamily).getKey(normalizedKey.get())
                    .withColumnRange(new RangeBuilder().setStart(versionColumnName).setLimit(1).setReversed(true).build())
                    .execute().getResult();

        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }

        if (!result.isEmpty()) {
            return result.getColumnByIndex(0).getStringValue();
        }

        return null;
    }

    @Override
    public boolean deleteVersionsUpTo(String key, String versionColumnName) {
        // Astyanax doesn't support deleting columns by range. Might be able to by using Thrift directly,
        // but as I'm not yet familiar with Thrift, doing it this way for the nonce in the interest of time.
        NormalizedKey normalizedKey = keyNormalizer.normalizeKey(key);
        ColumnList<String> result;
        try {
            result = keyspace.prepareQuery(columnFamily).getKey(normalizedKey.get())
                    .withColumnRange(new RangeBuilder().setEnd(versionColumnName).build())
                    .execute().getResult();

        } catch (ConnectionException ce) {
            throw RaptureExceptionFactory.create(HttpURLConnection.HTTP_INTERNAL_ERROR, messageCatalog.getMessage("DbCommsError"), ce);
        }

        if (!result.isEmpty()) {
            MutationBatch mutationBatch = keyspace.prepareMutationBatch();
            ColumnListMutation<String> columnListMutation = mutationBatch.withRow(columnFamily, normalizedKey.get());

            Iterator<Column<String>> columnIterator = result.iterator();
            while (columnIterator.hasNext()) {
                columnListMutation.deleteColumn(columnIterator.next().getName());
            }

            try {
                mutationBatch.execute();
            } catch (ConnectionException ce) {
                log.error(ce);
                return false;
            }
        }

        return true;
    }
}
