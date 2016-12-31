package nd.esp.service.lifecycle.vos.business;

import java.util.Map;

/**
 * Created by Administrator on 2016/11/17 0017.
 */
public class Suite4BusinessViewModel {
    private String identifier;
    private String parent;
    private String description;
    private int operate_type;
    private String status;
    private Map<String,Object> custom_properties;
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOperate_type() {
        return operate_type;
    }

    public void setOperate_type(int operate_type) {
        this.operate_type = operate_type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getCustom_properties() {
        return custom_properties;
    }

    public void setCustom_properties(Map<String, Object> custom_properties) {
        this.custom_properties = custom_properties;
    }
}
