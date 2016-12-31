package nd.esp.service.lifecycle.vos.business.assets;

import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;

public class ObjectiveTypeBussinesViewModel {
	@NotBlank(message = "{objectiveTypeBussinesViewModel.userId.notBlank.validmsg}")
	private String userId;
	@Valid
	private List<ObjectiveTypeViewModel> objectiveTypes;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public List<ObjectiveTypeViewModel> getObjectiveTypes() {
		return objectiveTypes;
	}
	public void setObjectiveTypes(List<ObjectiveTypeViewModel> objectiveTypes) {
		this.objectiveTypes = objectiveTypes;
	}
}
