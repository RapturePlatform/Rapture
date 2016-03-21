package rapture.kernel;

import com.google.common.collect.ImmutableList;
import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.SearchUpdateObject;

/**
 * Created by yanwang on 3/15/16.
 */
public class SearchPublisher {

    public static String CATEGORY = "search";

    public static void publishMessage(CallingContext context,
                                      SearchUpdateObject.ActionType type,
                                      RaptureURI uri,
                                      String content) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);

        SearchUpdateObject object = new SearchUpdateObject();
        object.setType(type);
        object.setUri(uri);
        object.setContent(content);
        task.addMimeObject(object);

        Kernel.getPipeline().publishMessageToCategory(context, task);
    }
}
