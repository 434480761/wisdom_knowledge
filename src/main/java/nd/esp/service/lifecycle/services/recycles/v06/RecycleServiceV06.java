package nd.esp.service.lifecycle.services.recycles.v06;

import java.util.Map;

public interface RecycleServiceV06 {
	/**
	 * 操作回收站资源
	 * @param resType
	 * @param resId
	 * @param operateType
	 * @return
	 */
	public Map<String,Object> operateRecycleResource(String resType,String resId,String operateType);
}
