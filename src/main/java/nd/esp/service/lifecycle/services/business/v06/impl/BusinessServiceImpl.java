package nd.esp.service.lifecycle.services.business.v06.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nd.esp.service.lifecycle.models.business.v06.CommonReasonModel;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.CommonReason;
import nd.esp.service.lifecycle.repository.sdk.CommonReasonRepository;
import nd.esp.service.lifecycle.services.business.v06.BusinessService;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.utils.BeanMapperUtils;
import nd.esp.service.lifecycle.utils.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class BusinessServiceImpl implements BusinessService {
	@Autowired
	private CommonReasonRepository commonReasonRepository;
	
	@Override
	public CommonReasonModel saveCommonReason(CommonReasonModel crm) {
		CommonReason cr = BeanMapperUtils.beanMapper(crm, CommonReason.class);
		Timestamp date = new Timestamp(System.currentTimeMillis());
		try {
			cr.setIdentifier(UUID.randomUUID().toString());
			cr.setCreateTime(date);
			cr.setLastUpdate(date);
			cr = commonReasonRepository.add(cr);
			return BeanMapperUtils.beanMapper(cr, CommonReasonModel.class);
		} catch (EspStoreException e) {
			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
					e.getMessage());
		}
	}

	@Override
	public Map<String, Object> deleteCommonReason(String id) {
		try {
			CommonReason cr = commonReasonRepository.get(id);
			if(cr == null){
				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						"LC/RESOURCE_NOT_FOUND",
						"不存在该常见原因");
			}
			commonReasonRepository.del(id);
		} catch (EspStoreException e) {
			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
					e.getMessage());
		}
		Map<String,Object> returnMap = new HashMap<String, Object>();
		returnMap.put("code", "LC/DELETE_RESOURCE_SUCCESS");
		returnMap.put("message", "删除成功");
		return returnMap;
	}

	@Override
	public List<CommonReasonModel> queryAllCommonReason() {
		CommonReason entity = new CommonReason();
		List<CommonReason> list = null;
		List<CommonReasonModel> returnList = new ArrayList<CommonReasonModel>();
		try {
			list = commonReasonRepository.getAllByExample(entity);
		} catch (EspStoreException e) {
			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
					e.getMessage());
		}
		if(CollectionUtils.isNotEmpty(list)){
			for (CommonReason cr : list) {
				returnList.add(BeanMapperUtils.beanMapper(cr, CommonReasonModel.class));
			}
		}
		return returnList;
	}

}
