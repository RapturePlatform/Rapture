package rapture.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumeration of the available 'type's that can be searched in our search repository. This file is auto-generated based on the @FTS annotation in ApiGen types.api
 * 
 * @author dukenguyen
 *
 */
public enum SearchRepoType {
    meta, uri, document<schemes:{ s |, <s>}>;

    private static List\<String\> valList = null;

    public static List\<String\> valuesAsList() {
        if (valList == null) {
            valList = new ArrayList\<String\>();
            for (SearchRepoType t : values()) {
                valList.add(t.toString());
            }
        }
        return valList;
    }
}