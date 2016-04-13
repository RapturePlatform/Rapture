package rapture.elasticsearch;

import java.util.List;

public class SimpleURI {
	private List<String> parts;
	private String repo;

	public List<String> getParts() {
		return parts;
	}

	public void setParts(List<String> parts) {
		this.parts = parts;
	}

	public String getRepo() {
		return repo;
	}

	public void setRepo(String repo) {
		this.repo = repo;
	}
}
