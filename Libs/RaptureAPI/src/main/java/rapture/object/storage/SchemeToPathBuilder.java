package rapture.object.storage;

import rapture.common.RaptureScriptPathBuilder;
import rapture.common.RaptureURI;
import rapture.common.dp.WorkerPathBuilder;
import rapture.common.dp.WorkflowPathBuilder;

public class SchemeToPathBuilder {
	public static StoragePathBuilder getStoragePathBuilder(RaptureURI uri) {
		switch(uri.getScheme()) {
		case SCRIPT:
			RaptureScriptPathBuilder x = (new RaptureScriptPathBuilder()).authority(uri.getAuthority()).name(uri.getFullPath());
			return x;
		case WORKFLOW:
			return new WorkflowPathBuilder();
		case WORKORDER:
			return new WorkerPathBuilder();
		default:
			return null;
		}
	}
}
