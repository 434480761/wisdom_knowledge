package nd.esp.service.lifecycle.vos.instructionalobjectives.v06;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultilayerClassifySubSuite {

	
	//正则前  如【物理常数】的测量实验
	private String regexBeforeDescription;
	//正则后 如 【xx】的测量实验
	private String regexAfterDescrption;
	//状态
	private String status;
	//层级关系
	private boolean isRootSuite;
	
	private String suiteLevel;
	
	//自己套件信息
	private String identify;
	
	private String parent;
	
	private String title;

	private Set<String> beforeIdentifySet;

	
	
	
	

	public Set<String> getBeforeIdentifySet() {
		return beforeIdentifySet;
	}

	public void setBeforeIdentifySet(Set<String> beforeIdentifySet) {
		this.beforeIdentifySet = beforeIdentifySet;
	}

	public String getRegexBeforeDescription() {
		return regexBeforeDescription;
	}

	public void setRegexBeforeDescription(String regexBeforeDescription) {
		this.regexBeforeDescription = regexBeforeDescription;
	}

	public String getRegexAfterDescrption() {
		return regexAfterDescrption;
	}

	public void setRegexAfterDescrption(String regexAfterDescrption) {
		this.regexAfterDescrption = regexAfterDescrption;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isRootSuite() {
		return isRootSuite;
	}

	public void setRootSuite(boolean isRootSuite) {
		this.isRootSuite = isRootSuite;
	}

	public String getSuiteLevel() {
		return suiteLevel;
	}

	public void setSuiteLevel(String suiteLevel) {
		this.suiteLevel = suiteLevel;
	}

	public String getIdentify() {
		return identify;
	}

	public void setIdentify(String identify) {
		this.identify = identify;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}   

}
