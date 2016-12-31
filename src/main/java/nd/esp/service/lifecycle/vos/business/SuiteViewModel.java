package nd.esp.service.lifecycle.vos.business;

import java.util.List;
import java.util.Map;

public class SuiteViewModel {
	private String suitId;
	private String userId;
	private List<SuiteKnowledgesViewModel> knowledges;
	private Map<String,Object> customProperties;
	private List<SuiteObjectivesViewModel> objectives;
	public String getSuitId() {
		return suitId;
	}
	public void setSuitId(String suitId) {
		this.suitId = suitId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public List<SuiteObjectivesViewModel> getObjectives() {
		return objectives;
	}
	public void setObjectives(List<SuiteObjectivesViewModel> objectives) {
		this.objectives = objectives;
	}
	public List<SuiteKnowledgesViewModel> getKnowledges() {
		return knowledges;
	}
	public void setKnowledges(List<SuiteKnowledgesViewModel> knowledges) {
		this.knowledges = knowledges;
	}
	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}
	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}
}
