package nd.esp.service.lifecycle.vos.business.extendsuite;

import nd.esp.service.lifecycle.repository.model.ResourceCategory;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/8 0008.
 */
public class ObjViewModel {
    private String identifier;
    private String creator;
    private String providerSource;
    private List<String> keywords;
    private List<String> versions;
    private String description;
    private String title;
    private String status;
    private List<KnViewModel> knowledges;
    private List<String> applicablePeriod;
    private Map<String,Object> customProperties;

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getProviderSource() {
        return providerSource;
    }

    public void setProviderSource(String providerSource) {
        this.providerSource = providerSource;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getApplicablePeriod() {
        return applicablePeriod;
    }

    public void setApplicablePeriod(List<String> applicablePeriod) {
        this.applicablePeriod = applicablePeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<KnViewModel> getKnowledges() {
        return knowledges;
    }

    public void setKnowledges(List<KnViewModel> knowledges) {
        this.knowledges = knowledges;
    }
}
