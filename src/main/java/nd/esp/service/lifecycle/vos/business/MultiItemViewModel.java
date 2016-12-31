package nd.esp.service.lifecycle.vos.business;

import java.util.List;

/**
 * Created by Administrator on 2016/12/21 0021.
 */
public class MultiItemViewModel {
    private String title;
    private List<MultiLevelSuiteViewModel> group;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<MultiLevelSuiteViewModel> getGroup() {
        return group;
    }

    public void setGroup(List<MultiLevelSuiteViewModel> group) {
        this.group = group;
    }
}
