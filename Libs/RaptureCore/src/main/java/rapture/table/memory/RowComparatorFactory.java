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
package rapture.table.memory;

import rapture.dsl.iqry.OrderDirection;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author bardhi
 * @since 4/28/15.
 */
public class RowComparatorFactory {
    private static final Logger log = Logger.getLogger(RowComparatorFactory.class);

    static Comparator<List<Object>> createComparator(final List<String> fieldList, List<String> columnNames, OrderDirection direction) {
        Map<String, Integer> fieldToIndex = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            fieldToIndex.put(columnName, i);
        }
        return createComparator(fieldList, fieldToIndex, direction);

    }

    private static Comparator<List<Object>> createComparator(final List<String> fieldList, final Map<String, Integer> fieldToIndex, OrderDirection direction) {
        return new Comparator<List<Object>>() {
            @Override
            public int compare(List<Object> o1, List<Object> o2) {
                for (String fieldName : fieldList) {
                    Integer index = fieldToIndex.get(fieldName);
                    int retVal;
                    if (o1.size() <= index && o2.size() <= index) {
                        return 0;
                    } else if (o1.size() <= index) {
                        return -1;
                    } else if (o2.size() <= index) {
                        return 1;
                    } else {
                        Object v1 = o1.get(index);
                        Object v2 = o2.get(index);
                        
                        if (v1 instanceof Number && v2 instanceof Number) {
                            retVal = ((Number) v1).intValue() - ((Number)v2).intValue();
                        } else if (v1 instanceof Comparable) {
                            try {
                                retVal = ((Comparable) v1).compareTo(v2);
                            } catch (ClassCastException e) {
                                log.warn(String.format("Sort by on field %s matched non-comparable classes [%s] and [%s]", fieldName, v1.getClass().getCanonicalName(), v2.getClass().getCanonicalName()));
                                return 0;
                            }
                        } else {
                            log.warn(String.format("Sort by on field %s matched non-comparable objects [%s] and [%s]", fieldName, v1, v2));
                            return 0;
                        }
                    }
                    if (retVal != 0) { //already found one greater than other. Otherwise, move on to next
                        return retVal;
                    }
                }
                return 0;
            }
        };
    }
}
