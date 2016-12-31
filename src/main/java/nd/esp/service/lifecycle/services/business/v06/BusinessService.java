package nd.esp.service.lifecycle.services.business.v06;

import java.util.List;
import java.util.Map;

import nd.esp.service.lifecycle.models.business.v06.CommonReasonModel;

public interface BusinessService {
	public CommonReasonModel saveCommonReason(CommonReasonModel crvm);
	public Map<String,Object> deleteCommonReason(String id);
	public List<CommonReasonModel> queryAllCommonReason();
}
