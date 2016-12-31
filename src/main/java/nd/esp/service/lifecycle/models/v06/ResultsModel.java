package nd.esp.service.lifecycle.models.v06;

public class ResultsModel {

	private String suiteTitle;
	private String sourceId;
	private String id;
	private String title;
	private String relationType;
	private int hashCode;

	public String getSuiteTitle() {
		return suiteTitle;
	}

	public void setSuiteTitle(String suiteTitle) {
		this.suiteTitle = suiteTitle;
	}

	public int getHashCode() {
		return hashCode;
	}

	public void setHashCode(int hashCode) {
		this.hashCode = hashCode;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}
}
