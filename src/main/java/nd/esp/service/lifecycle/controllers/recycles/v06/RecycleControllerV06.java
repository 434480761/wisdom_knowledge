package nd.esp.service.lifecycle.controllers.recycles.v06;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nd.esp.service.lifecycle.educommon.models.ResourceModel;
import nd.esp.service.lifecycle.educommon.services.NDResourceService;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceV06;
import nd.esp.service.lifecycle.services.recycles.v06.RecycleServiceV06;
import nd.esp.service.lifecycle.services.recycles.v06.impl.RecycleServiceImplV06;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.busi.CommonHelper;
import nd.esp.service.lifecycle.support.enums.LifecycleStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回收站模块
 * @author xuzy
 *
 */
@RestController
@RequestMapping("/v0.6/recycles/{res_type}/{res_id}")
public class RecycleControllerV06 {
	@Autowired
	public NDResourceService ndResourceService;
	
	@Autowired
	public RecycleServiceV06 recycleService;
	
    @Autowired
    private EducationRelationServiceV06 educationRelationService;
    
	/**
	 * 资源加入回收站
	 * @param resType
	 * @param resId
	 * @return
	 */
	@RequestMapping(value = "", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public Map<String,Object> add(@PathVariable(value="res_type") String resType,@PathVariable(value="res_id") String resId, @RequestParam(required = false, defaultValue = "false", value = "refresh_suite") boolean refreshSuite){
		//1、回收站支持的资源类型（目前与业务耦合比较大）
		//目前仅支持套件、子套件、教学目标类型资源（都是assets）放入回收站
		if(!IndexSourceType.AssetType.getName().equals(resType)){
			throw new LifeCircleException("回收站暂不支持该类型");
		}
		
		//2、判断资源是否存在(必须是可用)
		ndResourceService.getDetail(resType, resId, new ArrayList<String>());
		
		//3、调用service方法
		recycleService.operateRecycleResource(resType, resId, RecycleServiceImplV06.OPERATE_ADD);
		
        //是否需要刷新套件缓存
        if (refreshSuite) {
            CommonHelper.getPrimaryExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    educationRelationService.queryFordevAndSaveForRedis();
                }
            });
        }
		return null;
	}
	
	/**
	 * 还原回收站资源
	 * @return
	 */
	@RequestMapping(value = "", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public Map<String,Object> restore(@PathVariable(value="res_type") String resType,@PathVariable(value="res_id") String resId, @RequestParam(required = false, defaultValue = "false", value = "refresh_suite") boolean refreshSuite){
		//1、判断资源是否在回收站中
		List<String> includeList = new ArrayList<String>();
		includeList.add("LC");
		ResourceModel rm = ndResourceService.getDetail(resType, resId, includeList,true);
		if(rm.getLifeCycle() != null){
			if(!rm.getLifeCycle().isEnable() && LifecycleStatus.RECYCLED.getCode().equals(rm.getLifeCycle().getStatus())){
				//2、调用service方法
				return recycleService.operateRecycleResource(resType, resId, RecycleServiceImplV06.OPERATE_RESTORE);
			}else{
				throw new LifeCircleException("回收站无此资源");
			}
		}
		
        //是否需要刷新套件缓存
        if (refreshSuite) {
            CommonHelper.getPrimaryExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    educationRelationService.queryFordevAndSaveForRedis();
                }
            });
        }
		return null;
	}
}
