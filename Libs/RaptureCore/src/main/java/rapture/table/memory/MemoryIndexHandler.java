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
package rapture.table.memory;

import rapture.common.TableQuery;
import rapture.common.TableQueryResult;
import rapture.common.TableRecord;
import rapture.common.model.DocumentMetadata;
import rapture.dsl.iqry.IndexQuery;
import rapture.dsl.iqry.IndexQueryFactory;
import rapture.dsl.iqry.OrderDirection;
import rapture.dsl.iqry.WhereClause;
import rapture.dsl.iqry.WhereExtension;
import rapture.dsl.iqry.WhereStatement;
import rapture.index.IndexHandler;
import rapture.index.IndexProducer;
import rapture.index.IndexRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.Predicate;

/*
 * An in memory table, primarily for testing
 * 
 */
public class MemoryIndexHandler implements IndexHandler {
    protected static Logger log = Logger.getLogger(MemoryIndexHandler.class);

    protected Map<String, Map<String, Object>> memoryView = null;
    private IndexProducer indexProducer;

    public MemoryIndexHandler() {
        reset();
    }

    @Override
    public void deleteTable() {
        log.info("Removing index content ");
        memoryView.clear();
    }

    @Override
    public void removeAll(String rowId) {
        memoryView.put(rowId, new HashMap<String, Object>());
    }

    private void reset() {
        memoryView = new HashMap<>();
    }

    @Override
    public void setConfig(Map<String, String> config) {
        log.info("Creating memory queue with config " + config.toString());
        reset();
    }

    @Override
    public void addedRecord(String key, String value, DocumentMetadata mdLatest) {
        List<IndexRecord> records = indexProducer.getIndexRecords(key, value, mdLatest);
        for (IndexRecord record : records) {
            memoryView.put(key, record.getValues());
            memoryView.get(key).put(ROWID, key);
        }
    }

    @Override
    public void updateRow(String key, Map<String, Object> recordValues) {
        memoryView.put(key, recordValues);
    }

    @Override
    public void ensureIndicesExist() {
        //noop
    }

    @Override
    public List<TableRecord> queryTable(TableQuery query) {
        return new ArrayList<TableRecord>();
    }

    @Override
    public void setInstanceName(String instanceName) {
        //unused -- ignore
    }

    @Override
    public TableQueryResult query(String query) {
        TableQueryResult result = new TableQueryResult();
        IndexQuery indexQuery = IndexQueryFactory.parseQuery(query);
        Map<String, Map<String, Object>> data = memoryView;
        List<Predicate<Map<String, Object>>> predicates = predicatesFromQuery(indexQuery);

        List<String> columnNames = indexQuery.getSelect().getFieldList();
        result.setColumnNames(columnNames);
        List<List<Object>> rows = new LinkedList<>();
        for (Map.Entry<String, Map<String, Object>> mainEntry : data.entrySet()) {
            boolean isGood = true;
            Map<String, Object> body = mainEntry.getValue();
            for (Predicate<Map<String, Object>> predicate : predicates) {
                isGood = predicate.apply(body);
                if (!isGood) {
                    break;
                }
            }
            if (isGood) {
                List<Object> row = new LinkedList<>();
                for (String columnName : columnNames) {
                    row.add(body.get(columnName));
                }
                rows.add(row);
            }
        }

        if (indexQuery.getOrderBy().getFieldList().size() > 0) {
            Collections.sort(rows, RowComparatorFactory.createComparator(indexQuery.getOrderBy().getFieldList(), columnNames, indexQuery.getDirection()));
            if (indexQuery.getDirection() == OrderDirection.DESC) {
                Collections.reverse(rows);
            }
        }

        if (indexQuery.getLimit() != 0) {
            rows = rows.subList(0, indexQuery.getLimit());
        }

        result.setRows(rows);

        return result;
    }

    private List<Predicate<Map<String, Object>>> predicatesFromQuery(IndexQuery parsedQuery) {
        List<Predicate<Map<String, Object>>> predicates = new LinkedList<>();
        WhereClause whereClause = parsedQuery.getWhere();
        if (whereClause.getPrimary() != null) {
            predicates.add(predicateFromClause(whereClause.getPrimary()));
        }
        if (!whereClause.getExtensions().isEmpty()) {
            for (WhereExtension whereExtension : whereClause.getExtensions()) {
                predicates.add(predicateFromClause(whereExtension.getClause()));
            }
        }
        return predicates;
    }

    private Predicate<Map<String, Object>> predicateFromClause(final WhereStatement statement) {
        return new Predicate<Map<String, Object>>() {
            @Override
            public boolean apply(Map<String, Object> input) {
                String fieldName = statement.getField();
                Object queryValue = statement.getValue().getValue();
                Object actualValue = input.get(fieldName);
                if (queryValue != null && actualValue != null && statement.getClass().equals(actualValue.getClass())) {
                    return compare((Comparable) actualValue, (Comparable) queryValue);
                } else {
                    String actualValueString;
                    if (actualValue != null) {
                        actualValueString = actualValue.toString();
                    } else {
                        actualValueString = null;
                    }

                    String queryValueString;
                    if (queryValue != null) {
                        queryValueString = queryValue.toString();
                    } else {
                        queryValueString = null;
                    }
                    return compare(actualValueString, queryValueString);
                }
            }

            protected <T extends Comparable<T>> boolean compare(T actualValue, T queryValue) {
                switch (statement.getOper()) {
                    case GT:
                        return actualValue != null && actualValue.compareTo(queryValue) > 0;
                    case LT:
                        return actualValue != null && actualValue.compareTo(queryValue) < 0;
                    case NOTEQUAL:
                        return actualValue != null && !queryValue.equals(actualValue);
                    case EQUAL:
                        return queryValue.equals(actualValue);
                    default:
                        return false;
                }
            }

        };
    }

    @Override
    public Long getLatestEpoch() {
        return 0L;
    }

    @Override
    public void setIndexProducer(IndexProducer indexProducer) {
        this.indexProducer = indexProducer;

    }

    @Override
    public void initialize() {

    }

}
