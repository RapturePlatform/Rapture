package rapture.kernel.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.mime.MimeSearchUpdateObject;
import rapture.common.model.DocumentWithMeta;
import rapture.common.series.SeriesUpdateObject;
import rapture.config.ConfigLoader;
import rapture.kernel.Kernel;
import rapture.object.Searchable;

/**
 * Created by yanwang on 3/15/16.
 */
public class SearchPublisher {

    private static final Logger log = Logger.getLogger(SearchPublisher.class);

    public static String CATEGORY = "alpha";

    public static void publishCreateMessage(CallingContext context, Searchable searchableRepo, DocumentWithMeta doc) {
        if (!shouldPublish(searchableRepo, doc.getDisplayName())) {
            return;
        }
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(searchableRepo.getFtsIndexRepo());
        object.setType(MimeSearchUpdateObject.ActionType.CREATE);
        object.setDoc(doc);
        task.addMimeObject(object);

        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    public static void publishCreateMessage(CallingContext context, Searchable searchableRepo, SeriesUpdateObject seriesUpdateObject) {
        String uri = seriesUpdateObject.getUri();
        if (!shouldPublish(searchableRepo, uri)) {
            return;
        }
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(searchableRepo.getFtsIndexRepo());
        object.setType(MimeSearchUpdateObject.ActionType.CREATE);
        object.setSeriesUpdateObject(seriesUpdateObject);
        task.addMimeObject(object);

        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    public static void publishDeleteMessage(CallingContext context, Searchable searchableRepo, RaptureURI uri) {
        if (!shouldPublish(searchableRepo, uri.toString())) {
            return;
        }
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(searchableRepo.getFtsIndexRepo());
        object.setType(MimeSearchUpdateObject.ActionType.DELETE);
        object.setUri(uri);
        task.addMimeObject(object);
        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    public static void publishRebuildMessage(CallingContext context, String repoUri) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(repoUri);
        object.setType(MimeSearchUpdateObject.ActionType.REBUILD);
        task.addMimeObject(object);
        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    public static void publishDropMessage(CallingContext context, String repoUri) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(repoUri);
        object.setType(MimeSearchUpdateObject.ActionType.DROP);
        task.addMimeObject(object);
        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    private static boolean shouldPublish(Searchable searchableRepo, String uri) {
        if (ConfigLoader.getConf().FullTextSearchOn && searchableRepo.getFtsIndex()) {
            if (StringUtils.isBlank(searchableRepo.getFtsIndexRepo())) {
                searchableRepo.setFtsIndexRepo(ConfigLoader.getConf().FullTextSearchDefaultRepo);
            }
            log.info(String.format("Publishing search update for uri [%s] to search repo [%s] ...", uri, searchableRepo.getFtsIndexRepo()));
            return true;
        }
        log.info(String.format("Will not publish search update for uri [%s]", uri));
        return false;
    }
}
