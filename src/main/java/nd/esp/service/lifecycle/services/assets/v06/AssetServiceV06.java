package nd.esp.service.lifecycle.services.assets.v06;

import nd.esp.service.lifecycle.models.v06.AssetModel;
import nd.esp.service.lifecycle.repository.model.ResourceRelation;
import nd.esp.service.lifecycle.vos.business.assets.ObjectiveTypeAndSuiteViewModel;
import nd.esp.service.lifecycle.vos.business.assets.ObjectiveTypeBussinesViewModel;
import nd.esp.service.lifecycle.vos.business.assets.ObjectiveTypeViewModel;

import java.util.List;
import java.util.Map;

/**
 * @author xuzy
 * @version 0.6
 * @created 2015-07-02
 */
public interface AssetServiceV06{
	/**
	 * 素材创建
	 * @param rm
	 * @return
	 */
	public AssetModel createAsset(AssetModel am);

	/**
	 * 素材修改
	 * @param rm
	 * @return
	 */
	public AssetModel updateAsset(AssetModel am);

	AssetModel patchAsset(AssetModel am);

	/**
	 * 入参
		{
		    "user_id": "644869",//创建者id
		    "objective_types": [
		        {
		            "identifier": "",//学习目标类型id，有值代表修改，没值代表新增
		            "title": "auto_increment",//后端默认生成，目前暂使用时间戳
		            "status": "CREATED",//状态
		            "suit_id": "8d04f39f-0358-4392-b459-f578498d241d",//套件id，用来建关系以及判断套件下是否有相同的学习目标类型
		            "description": "xxx",//学习目标类型的描述
		            "operate_type":0,//暂未使用
		            "custom_properties":{"XUZY":"291213"},//自定义属性
		            "knowledge_categories":[//学习目标要关联的知识点维度数据，如果不存在，使用name自动创建
		        {
		            "nd_code":"KC010",
		            "name":"xxx"
		        }
		        ]
		        }
		    ]
		}
	 * @param otbViewModel
	 */
	public void create4Business(ObjectiveTypeBussinesViewModel otbViewModel);

	/**
	 * {
        "root_suite":"",
        "suite": [
	                {
	                 "description": "",
                    "parent": "",
	                 "identifier": "",
                    "operate_type": "1",
	                "status":"",
	                "custom_properties": {
	                    "": ""
	                    }
	            }
	 ],
	    "objective_types": [
                     {
                     "description": "",
                     "suite_id": "",
                     "identifier": "",
                     "operate_type": "2",
                     "status":"",
                     "custom_properties": {
                     "": ""
                     }
                }
            ]
	 }

	 * @param objectiveTypeAndSuiteViewModel
	 */
	public List<ObjectiveTypeViewModel> batchCreate4Business(ObjectiveTypeAndSuiteViewModel objectiveTypeAndSuiteViewModel, String userId);
	public List<String> updateSortNum(List<String> rootSuites, float repairedNum);
	public float findMaxSortNumInBatchDeal(ResourceRelation example, Map<String, Float> sortNumMap);
}