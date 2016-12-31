package nd.esp.service.lifecycle.vos.business;

import java.util.List;
import java.util.Map;

public class MultiLevelSuiteData {
	private Map<String, List<MultiLevelSuiteViewModel>> datas;

	public Map<String, List<MultiLevelSuiteViewModel>> getDatas() {
		return datas;
	}

	public void setDatas(Map<String, List<MultiLevelSuiteViewModel>> datas) {
		this.datas = datas;
	}
	
}
