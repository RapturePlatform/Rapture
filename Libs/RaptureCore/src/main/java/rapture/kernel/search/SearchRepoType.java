package rapture.kernel.search;

import java.util.ArrayList;
import java.util.List;

public enum SearchRepoType {
    DOC, META, URI, SERIES, BLOB;

    private static List<String> valList = null;

    public static List<String> valuesAsList() {
        if (valList == null) {
            valList = new ArrayList<String>();
            for (SearchRepoType t : values()) {
                valList.add(t.toString());
            }
        }
        return valList;
    }
}
