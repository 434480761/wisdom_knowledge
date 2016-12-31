package nd.esp.service.lifecycle.repository.model;

import java.sql.Date;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import nd.esp.service.lifecycle.repository.EspEntity;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
/**
 * 记录常见原因
 * @author xuzy
 *
 */
@Entity
@Table(name="common_reasons")
public class CommonReason extends EspEntity {
	private static final long serialVersionUID = 1L;

	private String creator;
	
	@Column(name="create_time")
	private Timestamp createTime;
	
	@Column(name="last_update")
	private Timestamp lastUpdate;

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
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

	@Override
	public IndexSourceType getIndexType() {
		// TODO Auto-generated method stub
		return null;
	}

}
