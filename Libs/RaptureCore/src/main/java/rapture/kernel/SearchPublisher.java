package rapture.kernel;

import com.google.common.collect.ImmutableList;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.RaptureURI;
import rapture.common.SearchUpdateObject;
import rapture.common.model.DocumentWithMeta;

/**
 * Created by yanwang on 3/15/16.
 */
public class SearchPublisher {

    public static String CATEGORY = "search";

    public static void publishMessage(CallingContext context,
                                      SearchUpdateObject.ActionType type,
                                      DocumentWithMeta doc) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);

        SearchUpdateObject object = new SearchUpdateObject();
        object.setType(type);
        object.setDoc(doc);
        task.addMimeObject(object);

        Kernel.getPipeline().publishMessageToCategory(context, task);
    }
}
