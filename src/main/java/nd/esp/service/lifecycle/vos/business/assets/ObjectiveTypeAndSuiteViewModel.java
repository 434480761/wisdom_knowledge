package nd.esp.service.lifecycle.vos.business.assets;

import nd.esp.service.lifecycle.vos.business.Suite4BusinessViewModel;
import nd.esp.service.lifecycle.vos.business.SuiteObjectivesViewModel;

import javax.validation.Valid;
import java.util.List;

/**
 * Created by Administrator on 2016/11/16 0016.
 */
public class ObjectiveTypeAndSuiteViewModel {
    private String rootSuite;
    private List<Suite4BusinessViewModel> suite;
    @Valid
    private List<ObjectiveTypeViewModel> objectiveTypes;

    public String getRootSuite() {
        return rootSuite;
    }

    public void setRootSuite(String rootSuite) {
        this.rootSuite = rootSuite;
    }

    public List<Suite4BusinessViewModel> getSuite() {
        return suite;
    }

    public void setSuite(List<Suite4BusinessViewModel> suite) {
        this.suite = suite;
    }

    public List<ObjectiveTypeViewModel> getObjectiveTypes() {
        return objectiveTypes;
    }

    public void setObjectiveTypes(List<ObjectiveTypeViewModel> objectiveTypes) {
        this.objectiveTypes = objectiveTypes;
    }
}
