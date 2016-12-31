package nd.esp.service.lifecycle.controllers.business.v06;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nd.esp.service.lifecycle.models.business.v06.CommonReasonModel;
import nd.esp.service.lifecycle.services.business.v06.BusinessService;
import nd.esp.service.lifecycle.utils.BeanMapperUtils;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.vos.business.v06.CommonReasonViewModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务接口
 * @author xuzy
 *
 */
@RestController
@RequestMapping("/v0.6/businesses")
public class BusinessControllerV06 {
	@Autowired
	private BusinessService businessService;
	
	/**
	 * 新增常见原因
	 * @param crvm
	 * @return
	 */
	@RequestMapping(value="/reasons",method=RequestMethod.POST)
	public CommonReasonViewModel saveCommonReason(@RequestBody CommonReasonViewModel crvm){
		CommonReasonModel crm = BeanMapperUtils.beanMapper(crvm, CommonReasonModel.class);
		crm = businessService.saveCommonReason(crm);
		return BeanMapperUtils.beanMapper(crm, CommonReasonViewModel.class);
	}
	
	/**
	 * 删除常见原因
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/reasons/{id}",method=RequestMethod.DELETE)
	public Map<String,Object> deleteCommonReason(@PathVariable String id){
		return businessService.deleteCommonReason(id);
	}
	
	/**
	 * 获取所有的常见原因
	 * @return
	 */
	@RequestMapping(value="/reasons/query/all",method=RequestMethod.GET)
	public List<CommonReasonViewModel> queryAllCommonReason(){
		List<CommonReasonViewModel> returnList = new ArrayList<CommonReasonViewModel>();
		List<CommonReasonModel> crmList = businessService.queryAllCommonReason();
		if(CollectionUtils.isNotEmpty(crmList)){
			for (CommonReasonModel commonReasonModel : crmList) {
				returnList.add(BeanMapperUtils.beanMapper(commonReasonModel, CommonReasonViewModel.class));
			}
		}
		return returnList;
	}
}
