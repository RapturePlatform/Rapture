package rapture.common.mime;

import rapture.common.AbstractUpdateObject;
import rapture.common.RaptureTransferObject;
import rapture.common.RaptureURI;
import rapture.common.model.DocumentWithMeta;

/**
 * Created by yanwang on 3/15/16. Updated by AM
 */
public class MimeSearchUpdateObject implements RaptureTransferObject {

    public static enum ActionType {
        CREATE, UPDATE, DELETE, REBUILD, DROP,
    }

    private ActionType type;

    // TODO These should be one interface
    @Deprecated
    private DocumentWithMeta doc;
    private AbstractUpdateObject updateObject;

    private RaptureURI uri;
    private String repo;
    private String searchRepo;

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public static String getMimeType() {
        return "application/vnd.rapture.searchupdate";
    }

    public String getSearchRepo() {
        return searchRepo;
    }

    public void setSearchRepo(String searchRepo) {
        this.searchRepo = searchRepo;
    }

    public AbstractUpdateObject getUpdateObject() {
        return updateObject;
    }

    public void setUpdateObject(AbstractUpdateObject updateObject) {
        this.updateObject = updateObject;
    }

    public RaptureURI getUri() {
        return uri;
    }

    public void setUri(RaptureURI uri) {
        this.uri = uri;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }
}
