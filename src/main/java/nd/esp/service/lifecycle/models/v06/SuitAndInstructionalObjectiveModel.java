package nd.esp.service.lifecycle.models.v06;

import java.util.List;

public class SuitAndInstructionalObjectiveModel {

	private List<String> objectiveTypeTitle;

	private List<SuiteModel> suiteList;

	public List<SuiteModel> getSuiteList() {
		return suiteList;
	}

	public void setSuiteList(List<SuiteModel> suiteList) {
		this.suiteList = suiteList;
	}

	public List<String> getObjectiveTypeTitle() {
		return objectiveTypeTitle;
	}

	public void setObjectiveTypeTitle(List<String> objectiveTypeTitle) {
		this.objectiveTypeTitle = objectiveTypeTitle;
	}
}
