package rapture.common;

/**
 * Created by yanwang on 3/15/16.
 */
public class SearchUpdateObject implements RaptureTransferObject {

    public static enum ActionType {
        CREATE,
        UPDATE,
        DELETE,
    }

    private ActionType type;
    private RaptureURI uri;
    private String content;

    public RaptureURI getUri() {
        return uri;
    }

    public void setUri(RaptureURI uri) {
        this.uri = uri;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }
}
