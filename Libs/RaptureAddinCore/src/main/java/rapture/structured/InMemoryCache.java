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
package rapture.structured;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * in-memory cache is sufficient if a user continues to make structured API requests to the same rapture kernel.
 * 
 * @author dukenguyen
 * 
 */
public class InMemoryCache implements Cache {

    private static final Logger log = Logger.getLogger(InMemoryCache.class);

    private com.google.common.cache.Cache<String, Map<String, Integer>> columnTypeCache = CacheBuilder.newBuilder().maximumSize(10000).build();
    private com.google.common.cache.Cache<String, ResultSet> cursorCache = CacheBuilder.newBuilder().expireAfterWrite(24, TimeUnit.HOURS)
            .removalListener(new RemovalListener<String, ResultSet>() {
                @Override
                public void onRemoval(RemovalNotification<String, ResultSet> notification) {
                    ResultSet rs = notification.getValue();
                    try {
                        if (rs != null && !rs.isClosed()) {
                            log.info(String.format("Closing ResultSet for cursorId [%s]", notification.getKey()));
                            rs.close();
                        }
                    } catch (SQLException e) {
                        log.error(String.format("Failed to close ResultSet: [%s]", e.getMessage()));
                    }
                }
            }).build();

    @Override
    public void putColumnTypes(String table, Map<String, Integer> columnTypes) {
        columnTypeCache.put(table, columnTypes);
    }

    @Override
    public Map<String, Integer> getColumnTypes(String table) {
        return columnTypeCache.getIfPresent(table);
    }

    @Override
    public void removeColumnTypes(String table) {
        columnTypeCache.invalidate(table);
    }

    @Override
    public void putCursor(String cursorId, ResultSet resultSet) {
        cursorCache.put(cursorId, resultSet);
    }

    @Override
    public ResultSet getCursor(String cursorId) {
        return cursorCache.getIfPresent(cursorId);
    }

    @Override
    public void removeCursor(String cursorId) {
        cursorCache.invalidate(cursorId);
    }

}
