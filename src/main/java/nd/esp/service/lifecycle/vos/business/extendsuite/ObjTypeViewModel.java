package nd.esp.service.lifecycle.vos.business.extendsuite;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/8 0008.
 */
public class ObjTypeViewModel {
    private String identifier;
    private String title;
    private String description;
    private Map<String,Object> customProperties;
    private Map<String,Object> categories;

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

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }

    public Map<String, Object> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, Object> categories) {
        this.categories = categories;
    }

    private List<ObjViewModel> objective;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<ObjViewModel> getObjective() {
        return objective;
    }

    public void setObjective(List<ObjViewModel> objective) {
        this.objective = objective;
    }

}
