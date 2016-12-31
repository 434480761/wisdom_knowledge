package nd.esp.service.lifecycle.vos.business;

import java.util.List;
import java.util.Map;

public class SuiteObjectivesViewModel {
	private String identifier;
	private String title;
	private String status;
	private String description;
	private int operateType;
	private String providerSource;
	private List<String> keywords;
	private List<String> applicablePeriod;
	private Map<String,Object> customProperties;
	private String relationType;
	//教学目标多版本描述，后端用子教学目标资源来表示
	private List<String> versions;
	private String objectiveTypeTitle;
	private String objectiveTypeId;
	private String copySuiteId;
	public String getRelationType() {
		return relationType;
	}

	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getProviderSource() {
		return providerSource;
	}
	public void setProviderSource(String providerSource) {
		this.providerSource = providerSource;
	}
	public List<String> getKeywords() {
		return keywords;
	}
	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}
	public List<String> getApplicablePeriod() {
		return applicablePeriod;
	}
	public void setApplicablePeriod(List<String> applicablePeriod) {
		this.applicablePeriod = applicablePeriod;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getObjectiveTypeId() {
		return objectiveTypeId;
	}
	public void setObjectiveTypeId(String objectiveTypeId) {
		this.objectiveTypeId = objectiveTypeId;
	}
	public String getObjectiveTypeTitle() {
		return objectiveTypeTitle;
	}
	public void setObjectiveTypeTitle(String objectiveTypeTitle) {
		this.objectiveTypeTitle = objectiveTypeTitle;
	}
	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}
	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}
	public List<String> getVersions() {
		return versions;
	}
	public void setVersions(List<String> versions) {
		this.versions = versions;
	}
	public int getOperateType() {
		return operateType;
	}
	public void setOperateType(int operateType) {
		this.operateType = operateType;
	}

	public String getCopySuiteId() {
		return copySuiteId;
	}

	public void setCopySuiteId(String copySuiteId) {
		this.copySuiteId = copySuiteId;
	}
}
