package rapture.kernel.pipeline;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableList;

import rapture.common.AbstractUpdateObject;
import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.mime.MimeSearchUpdateObject;
import rapture.common.pipeline.PipelineConstants;
import rapture.config.ConfigLoader;
import rapture.kernel.Kernel;
import rapture.kernel.search.SearchRepoUtils;
import rapture.object.Searchable;

/**
 * Created by yanwang on 3/15/16.
 */
public class SearchPublisher {

    private static final Logger log = Logger.getLogger(SearchPublisher.class);

    public static String CATEGORY = PipelineConstants.CATEGORY_SEARCH;

    public static void publishCreateMessage(CallingContext context, Searchable searchableRepo, AbstractUpdateObject updateObject) {
        publishCreateMessage(context, searchableRepo, updateObject, true);
    }

    public static void publishCreateMessage(CallingContext context, Searchable searchableRepo, AbstractUpdateObject updateObject,
            boolean pipelineStatusEnabled) {
        if (!shouldPublish(searchableRepo, updateObject.getUri())) {
            return;
        }
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());
        task.setStatusEnabled(pipelineStatusEnabled);

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setSearchRepo(SearchRepoUtils.getSearchRepo(searchableRepo));
        object.setType(MimeSearchUpdateObject.ActionType.CREATE);
        object.setUpdateObject(updateObject);
        task.addMimeObject(object);

        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    public static void publishDeleteMessage(CallingContext context, Searchable searchableRepo, RaptureURI uri) {
        if (!shouldPublish(searchableRepo, uri)) {
            return;
        }
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setSearchRepo(SearchRepoUtils.getSearchRepo(searchableRepo));
        object.setType(MimeSearchUpdateObject.ActionType.DELETE);
        object.setUri(uri);
        task.addMimeObject(object);
        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    public static void publishRebuildMessage(CallingContext context, String repoUriStr) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(repoUriStr);
        object.setSearchRepo(SearchRepoUtils.getSearchRepo(context, repoUriStr));
        object.setType(MimeSearchUpdateObject.ActionType.REBUILD);
        task.addMimeObject(object);
        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    public static void publishDropMessage(CallingContext context, String repoUriStr) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(repoUriStr);
        object.setSearchRepo(SearchRepoUtils.getSearchRepo(context, repoUriStr));
        object.setType(MimeSearchUpdateObject.ActionType.DROP);
        task.addMimeObject(object);
        Kernel.getPipeline().publishMessageToCategory(context, task);
    }

    private static boolean shouldPublish(Searchable searchableRepo, RaptureURI uri) {
        if (ConfigLoader.getConf().FullTextSearchOn && searchableRepo.getFtsIndex()) {
            log.debug(String.format("Publishing search update for uri [%s] to search repo [%s] ...", uri.toString(),
                    SearchRepoUtils.getSearchRepo(searchableRepo)));
            return true;
        }
        return false;
    }
}
