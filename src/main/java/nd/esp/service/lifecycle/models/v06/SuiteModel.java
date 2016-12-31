package nd.esp.service.lifecycle.models.v06;

public class SuiteModel {

	private String identifier;

	private String description;

	public String getSuiteType() {
		return suiteType;
	}

	public void setSuiteType(String suiteType) {
		this.suiteType = suiteType;
	}

	private String suiteType;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
