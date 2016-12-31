package nd.esp.service.lifecycle.vos.instructionalobjectives.v06;

import java.util.List;

public class SuiteStatisticsModel {

	
	private String rootSuiteCreatTime;
	
	private String rootSuiteName;
	
	private String rootSuiteDescription;
	
	private String rootSuiteAuthorName;
	
	private int rootSuiteInsObjTypeTotal;
	
	private String rootSuiteStatus;
		
	private List<SampleInfoModel> sampleInfo;

	public String getRootSuiteCreatTime() {
		return rootSuiteCreatTime;
	}

	public void setRootSuiteCreatTime(String rootSuiteCreatTime) {
		this.rootSuiteCreatTime = rootSuiteCreatTime;
	}

	public String getRootSuiteName() {
		return rootSuiteName;
	}

	public void setRootSuiteName(String rootSuiteName) {
		this.rootSuiteName = rootSuiteName;
	}

	public String getRootSuiteDescription() {
		return rootSuiteDescription;
	}

	public void setRootSuiteDescription(String rootSuiteDescription) {
		this.rootSuiteDescription = rootSuiteDescription;
	}

	public String getRootSuiteAuthorName() {
		return rootSuiteAuthorName;
	}

	public void setRootSuiteAuthorName(String rootSuiteAuthorName) {
		this.rootSuiteAuthorName = rootSuiteAuthorName;
	}

	public int getRootSuiteInsObjTypeTotal() {
		return rootSuiteInsObjTypeTotal;
	}

	public void setRootSuiteInsObjTypeTotal(int rootSuiteInsObjTypeTotal) {
		this.rootSuiteInsObjTypeTotal = rootSuiteInsObjTypeTotal;
	}

	public String getRootSuiteStatus() {
		return rootSuiteStatus;
	}

	public void setRootSuiteStatus(String rootSuiteStatus) {
		this.rootSuiteStatus = rootSuiteStatus;
	}

	public List<SampleInfoModel> getSampleInfo() {
		return sampleInfo;
	}

	public void setSampleInfo(List<SampleInfoModel> sampleInfo) {
		this.sampleInfo = sampleInfo;
	}
	
	
}
