package rapture.index;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import rapture.dsl.iqry.IndexQuery;
import rapture.dsl.iqry.WhereClause;
import rapture.dsl.iqry.WhereExtension;
import rapture.dsl.iqry.WhereStatement;


public abstract class AbstractIndexHandler implements IndexHandler {

    protected List<Predicate<Map<String, Object>>> predicatesFromQuery(IndexQuery parsedQuery) {
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
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public boolean apply(Map<String, Object> input) {
                String fieldName = statement.getField();
                Object queryValue = statement.getValue().getValue();
                Object actualValue = input.get(fieldName);
                if (queryValue != null && actualValue != null) {
                    if (queryValue.getClass().equals(actualValue.getClass())) {
                        return compare((Comparable) actualValue, (Comparable) queryValue);
                    }
                    if (queryValue instanceof Number && actualValue instanceof Number) {
                        return compare((Number) actualValue, (Number) queryValue);
                    }
                    if (queryValue instanceof Number && actualValue instanceof String) {
                        try {
                            Number queryDouble = ((Number) queryValue).doubleValue();
                            Number actualDouble = Double.parseDouble(actualValue.toString());
                            return compare(actualDouble, queryDouble);
                        } catch (NumberFormatException e) {
                            // never mind
                        }
                    }

                }
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

            protected <T extends Number> boolean compare(T actualValue, T queryValue) {
                switch (statement.getOper()) {
                case GT:
                    return actualValue.doubleValue() > queryValue.doubleValue();
                case LT:
                    return actualValue.doubleValue() < queryValue.doubleValue();
                case NOTEQUAL:
                    return actualValue.doubleValue() != queryValue.doubleValue();
                case EQUAL:
                    return actualValue.doubleValue() == queryValue.doubleValue();
                default:
                    return false;
                }
            }

        };
    }


}
