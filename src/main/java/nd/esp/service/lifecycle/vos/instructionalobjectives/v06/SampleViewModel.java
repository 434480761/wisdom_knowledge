package nd.esp.service.lifecycle.vos.instructionalobjectives.v06;

import java.sql.Timestamp;
import java.util.Map;
/**
 * 样例viewModel
 * @author xuzy
 *
 */
public class SampleViewModel {
    private String assetId;
    private String knowledgeId1;
    private String knowledgeId2;
    private String knowledgeId3;
    private String knowledgeId4;
    private String knowledgeId5;
    private Timestamp createTime;
    private Timestamp lastUpdate;
    private String creator;
    private String status;
    private boolean enable;
    private Map<String,Object> customProperties;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getKnowledgeId1() {
        return knowledgeId1;
    }

    public void setKnowledgeId1(String knowledgeId1) {
        this.knowledgeId1 = knowledgeId1;
    }

    public String getKnowledgeId2() {
        return knowledgeId2;
    }

    public void setKnowledgeId2(String knowledgeId2) {
        this.knowledgeId2 = knowledgeId2;
    }

    public String getKnowledgeId3() {
        return knowledgeId3;
    }

    public void setKnowledgeId3(String knowledgeId3) {
        this.knowledgeId3 = knowledgeId3;
    }

    public String getKnowledgeId4() {
        return knowledgeId4;
    }

    public void setKnowledgeId4(String knowledgeId4) {
        this.knowledgeId4 = knowledgeId4;
    }

    public String getKnowledgeId5() {
        return knowledgeId5;
    }

    public void setKnowledgeId5(String knowledgeId5) {
        this.knowledgeId5 = knowledgeId5;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Timestamp lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}
    
}
