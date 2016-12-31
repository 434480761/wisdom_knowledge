package nd.esp.service.lifecycle.repository.model;

import nd.esp.service.lifecycle.repository.EspEntity;
import nd.esp.service.lifecycle.repository.IndexMapper;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.sql.Timestamp;

/**
 * Created by Administrator on 2016/11/21 0021.
 */
@Entity
@Table(name = "samples")
public class Sample extends EspEntity {
    private String assetId;
    private String knowledgeId1;
    private String knowledgeId2;
    private String knowledgeId3;
    private String knowledgeId4;
    private String knowledgeId5;
    
    @Transient
    private String knowledgeTitle1;
    
    @Transient
    private String knowledgeTitle2;
    
    @Transient
    private String knowledgeTitle3;
    
    @Transient
    private String knowledgeTitle4;
    
    @Transient
    private String knowledgeTitle5;
    
    private Timestamp createTime;
    private Timestamp lastUpdate;
    private String creator;
    private String status;
    private boolean enable;
    
    @Column(name="custom_properties")
    private String customProperties;

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

    @Override
    public IndexSourceType getIndexType() {
        return null;
    }

	public String getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(String customProperties) {
		this.customProperties = customProperties;
	}

	public String getKnowledgeTitle1() {
		return knowledgeTitle1;
	}

	public void setKnowledgeTitle1(String knowledgeTitle1) {
		this.knowledgeTitle1 = knowledgeTitle1;
	}

	public String getKnowledgeTitle2() {
		return knowledgeTitle2;
	}

	public void setKnowledgeTitle2(String knowledgeTitle2) {
		this.knowledgeTitle2 = knowledgeTitle2;
	}

	public String getKnowledgeTitle3() {
		return knowledgeTitle3;
	}

	public void setKnowledgeTitle3(String knowledgeTitle3) {
		this.knowledgeTitle3 = knowledgeTitle3;
	}

	public String getKnowledgeTitle4() {
		return knowledgeTitle4;
	}

	public void setKnowledgeTitle4(String knowledgeTitle4) {
		this.knowledgeTitle4 = knowledgeTitle4;
	}

	public String getKnowledgeTitle5() {
		return knowledgeTitle5;
	}

	public void setKnowledgeTitle5(String knowledgeTitle5) {
		this.knowledgeTitle5 = knowledgeTitle5;
	}
	
	
}
