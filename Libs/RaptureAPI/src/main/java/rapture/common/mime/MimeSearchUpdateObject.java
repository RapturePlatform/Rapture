package rapture.common.mime;

import rapture.common.RaptureTransferObject;
import rapture.common.RaptureURI;
import rapture.common.model.DocumentWithMeta;
import rapture.common.series.SeriesUpdateObject;

/**
 * Created by yanwang on 3/15/16. Updated by AM
 */
public class MimeSearchUpdateObject implements RaptureTransferObject {

    public static enum ActionType {
        CREATE, UPDATE, DELETE, REBUILD, DROP,
    }

    private ActionType type;
    private DocumentWithMeta doc;
    private SeriesUpdateObject seriesUpdateObject;
    private RaptureURI uri;
    private String repo;
    private String searchRepo;

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

    public String getSearchRepo() {
        return searchRepo;
    }

    public void setSearchRepo(String searchRepo) {
        this.searchRepo = searchRepo;
    }

    public SeriesUpdateObject getSeriesUpdateObject() {
        return seriesUpdateObject;
    }

    public void setSeriesUpdateObject(SeriesUpdateObject seriesUpdateObject) {
        this.seriesUpdateObject = seriesUpdateObject;
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
