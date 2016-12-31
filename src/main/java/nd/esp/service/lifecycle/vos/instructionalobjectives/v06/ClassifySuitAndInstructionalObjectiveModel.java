package nd.esp.service.lifecycle.vos.instructionalobjectives.v06;

import java.util.List;

import nd.esp.service.lifecycle.models.v06.SuitAndInstructionalObjectiveModel;

public class ClassifySuitAndInstructionalObjectiveModel {
	
	private String title;
	
	private List<SuitAndInstructionalObjectiveModel> group;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<SuitAndInstructionalObjectiveModel> getGroup() {
		return group;
	}

	public void setGroup(List<SuitAndInstructionalObjectiveModel> group) {
		this.group = group;
	}

}
