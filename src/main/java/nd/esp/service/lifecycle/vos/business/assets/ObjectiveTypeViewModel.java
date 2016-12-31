package nd.esp.service.lifecycle.vos.business.assets;

import java.util.List;
import java.util.Map;

import org.hibernate.validator.constraints.NotBlank;

public class ObjectiveTypeViewModel {
	private String identifier;
	private String title;
	@NotBlank(message = "{objectiveTypeViewModel.status.notBlank.validmsg}")
	private String status;
	@NotBlank(message = "{objectiveTypeViewModel.description.notBlank.validmsg}")
	private String description;
	private Integer operateType;
	@NotBlank(message = "{objectiveTypeViewModel.suiteId.notBlank.validmsg}")
	private String suiteId;
	private Map<String,Object> customProperties;
	private List<KnowledgeCategoriesViewModel> knowledgeCategories;

	public String getSuiteId() {
		return suiteId;
	}
	public void setSuiteId(String suiteId) {
		this.suiteId = suiteId;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
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
	public Integer getOperateType() {
		return operateType;
	}
	public void setOperateType(Integer operateType) {
		this.operateType = operateType;
	}
	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}
	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}
	public List<KnowledgeCategoriesViewModel> getKnowledgeCategories() {
		return knowledgeCategories;
	}
	public void setKnowledgeCategories(
			List<KnowledgeCategoriesViewModel> knowledgeCategories) {
		this.knowledgeCategories = knowledgeCategories;
	}
}
