package rapture.kernel.pipeline;

import rapture.common.CallingContext;
import rapture.common.RapturePipelineTask;
import rapture.common.mime.MimeSearchUpdateObject;
import rapture.common.model.DocumentWithMeta;
import rapture.kernel.Kernel;

import com.google.common.collect.ImmutableList;

/**
 * Created by yanwang on 3/15/16.
 */
public class SearchPublisher {

    public static String CATEGORY = "alpha";

    public static void publishMessage(CallingContext context,
                                      String publishRepo, MimeSearchUpdateObject.ActionType type,
                                      DocumentWithMeta doc) {
        RapturePipelineTask task = new RapturePipelineTask();
        task.setCategoryList(ImmutableList.of(CATEGORY));
        task.setPriority(2);
        task.setContentType(MimeSearchUpdateObject.getMimeType());

        MimeSearchUpdateObject object = new MimeSearchUpdateObject();
        object.setRepo(publishRepo);
        object.setType(type);
        object.setDoc(doc);
        task.addMimeObject(object);

        Kernel.getPipeline().publishMessageToCategory(context, task);
    }
}
