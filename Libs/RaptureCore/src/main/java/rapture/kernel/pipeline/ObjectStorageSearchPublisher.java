package rapture.kernel.pipeline;

import org.apache.log4j.Logger;

import rapture.common.DocUpdateObject;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.ContextFactory;
import rapture.object.storage.ObjectStorageSearchable;
/**
 * A helper class that understands the ins and outs of ObjectStorage objects, and has the correct defaults before sending to
 * Search Publisher
 * @author alanmoore
 *
 */
public class ObjectStorageSearchPublisher {
	private static ObjectStorageSearchable searchable = new ObjectStorageSearchable();
    private static final Logger log = Logger.getLogger(ObjectStorageSearchable.class);

    public static void publishCreateMessage(final DocumentWithMeta doc) {
        log.info("ObjectStorageSearchPublisher - publishing message...");
        new Thread() {
            @Override
            public void run() {
                publishCreateMessage(new DocUpdateObject(doc));
            }
        }.start();
        log.info("... message published");
    }

    public static void publishCreateMessage(DocUpdateObject duo) {
        SearchPublisher.publishCreateMessage(ContextFactory.getAnonymousUser(), searchable, duo);
    }
}
