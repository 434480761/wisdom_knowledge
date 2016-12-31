package nd.esp.service.lifecycle.vos.business;

import java.util.List;

/**
 * Created by Administrator on 2016/12/20 0020.
 */
public class MultiLevelChildViewModel {
    private String suiteTitle;
    private List<String> objectiveTypeTitle;
    private List<MultiLevelChildViewModel> children;

    public String getSuiteTitle() {
        return suiteTitle;
    }

    public void setSuiteTitle(String suiteTitle) {
        this.suiteTitle = suiteTitle;
    }

    public List<String> getObjectiveTypeTitle() {
        return objectiveTypeTitle;
    }

    public void setObjectiveTypeTitle(List<String> objectiveTypeTitle) {
        this.objectiveTypeTitle = objectiveTypeTitle;
    }

    public List<MultiLevelChildViewModel> getChildren() {
        return children;
    }

    public void setChildren(List<MultiLevelChildViewModel> children) {
        this.children = children;
    }
}
