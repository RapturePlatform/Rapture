package rapture.object.storage;

import rapture.config.ConfigLoader;
import rapture.object.Searchable;

public class ObjectStorageSearchable implements Searchable {

	@Override
	public Boolean getFtsIndex() {
		return ConfigLoader.getConf().FullTextSearchOn;
	}

	@Override
	public void setFtsIndex(boolean ftsIndex) {
	}

	@Override
	public String getFtsIndexRepo() {
		return ConfigLoader.getConf().FullTextSearchDefaultRepo;
	}

	@Override
	public void setFtsIndexRepo(String ftsIndexRepo) {
	}

}
