package nd.esp.service.lifecycle.vos;

import java.io.Serializable;

/**
 * 定义查询列表对象,用于套件
 * 
 * @author xuzy
 *
 */
public class ListViewModel4Suite<T> extends ListViewModel<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long total4Limit;

	public Long getTotal4Limit() {
		return total4Limit;
	}

	public void setTotal4Limit(Long total4Limit) {
		this.total4Limit = total4Limit;
	}
	
}
