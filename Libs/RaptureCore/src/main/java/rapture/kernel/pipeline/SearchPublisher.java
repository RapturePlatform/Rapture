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

	public static void publishCreateMessage(CallingContext context,
			String publishRepo, DocumentWithMeta doc) {
		RapturePipelineTask task = new RapturePipelineTask();
		task.setCategoryList(ImmutableList.of(CATEGORY));
		task.setPriority(2);
		task.setContentType(MimeSearchUpdateObject.getMimeType());

		MimeSearchUpdateObject object = new MimeSearchUpdateObject();
		object.setRepo(publishRepo);
		object.setType(MimeSearchUpdateObject.ActionType.CREATE);
		object.setDoc(doc);
		task.addMimeObject(object);

		Kernel.getPipeline().publishMessageToCategory(context, task);
	}

	public static void publishDeleteMessage(CallingContext context,
			String publishRepo, String uri) {
		RapturePipelineTask task = new RapturePipelineTask();
		task.setCategoryList(ImmutableList.of(CATEGORY));
		task.setPriority(2);
		task.setContentType(MimeSearchUpdateObject.getMimeType());

		MimeSearchUpdateObject object = new MimeSearchUpdateObject();
		object.setRepo(publishRepo);
		object.setType(MimeSearchUpdateObject.ActionType.DELETE);
		DocumentWithMeta doc = new DocumentWithMeta();
		doc.setDisplayName(uri);
		object.setDoc(doc);
		task.addMimeObject(object);

		Kernel.getPipeline().publishMessageToCategory(context, task);
	}

	public static void publishRebuildMessage(CallingContext context,
			String docRepoUri) {
		RapturePipelineTask task = new RapturePipelineTask();
		task.setCategoryList(ImmutableList.of(CATEGORY));
		task.setPriority(2);
		task.setContentType(MimeSearchUpdateObject.getMimeType());

		MimeSearchUpdateObject object = new MimeSearchUpdateObject();
		object.setRepo(docRepoUri);
		object.setType(MimeSearchUpdateObject.ActionType.REBUILD);
		task.addMimeObject(object);
		Kernel.getPipeline().publishMessageToCategory(context, task);
	}

	public static void publishDropRepoIndexMessage(CallingContext context,
			String docRepoUri) {
		RapturePipelineTask task = new RapturePipelineTask();
		task.setCategoryList(ImmutableList.of(CATEGORY));
		task.setPriority(2);
		task.setContentType(MimeSearchUpdateObject.getMimeType());

		MimeSearchUpdateObject object = new MimeSearchUpdateObject();
		object.setRepo(docRepoUri);
		object.setType(MimeSearchUpdateObject.ActionType.DROP);
		task.addMimeObject(object);
		Kernel.getPipeline().publishMessageToCategory(context, task);
	}
}
