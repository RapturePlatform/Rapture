package rapture.kernel.pipeline;

import rapture.common.DocUpdateObject;
import rapture.common.RaptureURI;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.ContextFactory;
import rapture.object.storage.ObjectStorageSearchable;

/**
 * A helper class that understands the ins and outs of ObjectStorage objects, and has the correct defaults before sending to Search Publisher
 * 
 * @author alanmoore
 *
 */
public class ObjectStorageSearchPublisher {

    private static ObjectStorageSearchable searchable = new ObjectStorageSearchable();

    public static void publishCreateMessage(final DocumentWithMeta doc) {
        new Thread() {
            @Override
            public void run() {
                publishCreateMessage(new DocUpdateObject(doc));
            }
        }.start();
    }

    public static void publishCreateMessage(DocUpdateObject duo) {
        SearchPublisher.publishCreateMessage(ContextFactory.getAnonymousUser(), searchable, duo);
    }

    public static void publishDeleteMessage(RaptureURI uri) {
        SearchPublisher.publishDeleteMessage(ContextFactory.getAnonymousUser(), searchable, uri);
    }
}
