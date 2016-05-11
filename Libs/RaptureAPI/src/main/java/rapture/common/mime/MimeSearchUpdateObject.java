package rapture.common.mime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import rapture.common.AbstractUpdateObject;
import rapture.common.RaptureTransferObject;
import rapture.common.RaptureURI;
import rapture.common.model.DocumentWithMeta;

/**
 * Created by yanwang on 3/15/16. Updated by AM
 */
public class MimeSearchUpdateObject implements RaptureTransferObject {

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repo == null) ? 0 : repo.hashCode());
        result = prime * result + ((searchRepo == null) ? 0 : searchRepo.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((updateObject == null) ? 0 : updateObject.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MimeSearchUpdateObject other = (MimeSearchUpdateObject) obj;
        if (repo == null) {
            if (other.repo != null) return false;
        } else if (!repo.equals(other.repo)) return false;
        if (searchRepo == null) {
            if (other.searchRepo != null) return false;
        } else if (!searchRepo.equals(other.searchRepo)) return false;
        if (type != other.type) return false;
        if (updateObject == null) {
            if (other.updateObject != null) return false;
        } else if (!updateObject.equals(other.updateObject)) return false;
        if (uri == null) {
            if (other.uri != null) return false;
        } else if (!uri.equals(other.uri)) return false;
        return true;
    }

    public static enum ActionType {
        CREATE, UPDATE, DELETE, REBUILD, DROP,
    }

    private ActionType type;

    @Deprecated
    @JsonIgnore
    private DocumentWithMeta doc;

    @Deprecated
    public DocumentWithMeta getDoc() {
        return doc;
    }

    @Deprecated
    public void setDoc(DocumentWithMeta doc) {
        this.doc = doc;
    }


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
