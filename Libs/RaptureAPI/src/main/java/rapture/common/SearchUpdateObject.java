package rapture.common;

import rapture.common.model.DocumentWithMeta;

/**
 * Created by yanwang on 3/15/16.
 * Updated by AM
 */
public class SearchUpdateObject implements RaptureTransferObject {

    public static enum ActionType {
        CREATE,
        UPDATE,
        DELETE,
    }

    private ActionType type;
    private DocumentWithMeta doc;


    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

	public DocumentWithMeta getDoc() {
		return doc;
	}

	public void setDoc(DocumentWithMeta doc) {
		this.doc = doc;
	}
}
