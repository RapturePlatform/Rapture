package rapture.kernel.pipeline;

import org.apache.log4j.Logger;

import rapture.common.RapturePipelineTask;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.mime.MimeSearchUpdateObject;
import rapture.exchange.QueueHandler;
import rapture.kernel.Kernel;

public class RaptureSearchUpdateHandler implements QueueHandler {

    private static final Logger log = Logger.getLogger(RaptureSearchUpdateHandler.class);

    private final PipelineTaskStatusManager statusManager;

    public RaptureSearchUpdateHandler() {
        statusManager = new PipelineTaskStatusManager();
    }

    @Override
    public boolean handleMessage(String tag, String routing, String contentType, RapturePipelineTask task) {
        String content = task.getContent();
        log.debug("Processing search update request"); //$NON-NLS-1$
        try {
            statusManager.startRunning(task);
            MimeSearchUpdateObject payload = JacksonUtil.objectFromJson(content, MimeSearchUpdateObject.class);

            switch (payload.getType()) {
            case CREATE:
                if (payload.getDoc() != null) {
                    Kernel.getSearch().getTrusted().writeSearchEntry(payload.getSearchRepo(), payload.getDoc());
                } else if (payload.getSeriesUpdateObject() != null) {
                    Kernel.getSearch().getTrusted().writeSearchEntry(payload.getSearchRepo(), payload.getSeriesUpdateObject());
                } else {
                    log.error("Empty payload.  Doing nothing.");
                }
                break;
            case DELETE:
                Kernel.getSearch().getTrusted().deleteSearchEntry(payload.getSearchRepo(), payload.getUri());
                break;
            case REBUILD:
                Kernel.getSearch().getTrusted().rebuild(payload.getRepo(), payload.getSearchRepo());
                break;
            case DROP:
                Kernel.getSearch().getTrusted().drop(payload.getRepo(), payload.getSearchRepo());
                break;
            default:
                log.error("Don't know how to process this search update");
            }
            // Call something in
            statusManager.finishRunningWithSuccess(task);

        } catch (Exception e) {
            log.error(String.format("Failed to process update %s", //$NON-NLS-1$
                    e.getMessage()));
            statusManager.finishRunningWithFailure(task);
        }
        return true;
    }

}
