package nd.esp.service.lifecycle.vos.business.extendsuite;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/8 0008.
 */
public class ExtendSuiteViewModel {
    private String identifier;
    private String title;
    private String description;
    private String language;
    private Map<String,Object> categories;
    private Map<String,Object> lifeCycle;
    private List<ObjTypeViewModel> objectiveTypes;
    private List<ExtendSuiteViewModel> children;
    private String copySuiteId;
    private String parentId;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, Object> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Object> categories) {
        this.categories = categories;
    }

    public Map<String, Object> getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(Map<String, Object> lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public List<ObjTypeViewModel> getObjectiveTypes() {
        return objectiveTypes;
    }

    public void setObjectiveTypes(List<ObjTypeViewModel> objectiveTypes) {
        this.objectiveTypes = objectiveTypes;
    }

    public List<ExtendSuiteViewModel> getChildren() {
        return children;
    }

    public void setChildren(List<ExtendSuiteViewModel> children) {
        this.children = children;
    }

    public String getCopySuiteId() {
        return copySuiteId;
    }

    public void setCopySuiteId(String copySuiteId) {
        this.copySuiteId = copySuiteId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
