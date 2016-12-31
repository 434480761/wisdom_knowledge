package nd.esp.service.lifecycle.vos.business;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/20 0020.
 */
public class MultiLevelSuiteViewModel {
    private List<String> objectiveTypeTitle;
    private List<Map<String,String>> suiteList;
    private List<MultiLevelChildViewModel> children;

    public List<String> getObjectiveTypeTitle() {
        return objectiveTypeTitle;
    }

    public void setObjectiveTypeTitle(List<String> objectiveTypeTitle) {
        this.objectiveTypeTitle = objectiveTypeTitle;
    }

    public List<Map<String, String>> getSuiteList() {
        return suiteList;
    }

    public void setSuiteList(List<Map<String, String>> suiteList) {
        this.suiteList = suiteList;
    }

    public List<MultiLevelChildViewModel> getChildren() {
        return children;
    }

    public void setChildren(List<MultiLevelChildViewModel> children) {
        this.children = children;
    }
}
