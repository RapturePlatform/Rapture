package rapture.common.mime;

import rapture.common.RaptureTransferObject;
import rapture.common.model.DocumentWithMeta;

/**
 * Created by yanwang on 3/15/16.
 * Updated by AM
 */
public class MimeSearchUpdateObject implements RaptureTransferObject {

    public static enum ActionType {
        CREATE,
        UPDATE,
        DELETE,
    }

    private ActionType type;
    private DocumentWithMeta doc;
    private String repo;


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
	
	public static String getMimeType() {
	    return "application/vnd.rapture.searchupdate";
	}

	public String getRepo() {
		return repo;
	}

	public void setRepo(String repo) {
		this.repo = repo;
	}
}
