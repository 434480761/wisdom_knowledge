package nd.esp.service.lifecycle.services.instructionalobjectives.v06;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nd.esp.service.lifecycle.educommon.models.ResourceModel;
import nd.esp.service.lifecycle.models.v06.InstructionalObjectiveModel;
import nd.esp.service.lifecycle.models.v06.SuitAndInstructionalObjectiveModel;
import nd.esp.service.lifecycle.repository.model.ResourceCategory;
import nd.esp.service.lifecycle.vos.ListViewModel;
import nd.esp.service.lifecycle.vos.ListViewModel4Suite;
import nd.esp.service.lifecycle.vos.business.MultiLevelSuiteViewModel;
import nd.esp.service.lifecycle.vos.business.SuiteViewModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.ClassifySuitAndInstructionalObjectiveModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.MultilayerClassifySubSuite;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.SampleViewModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.SuiteStatisticsModel;


/**
 * @author xuzy
 * @version 0.6
 * @created 2015-07-02
 */
public interface InstructionalObjectiveService{
	/**
	 * 素材创建
	 * @param rm
	 * @return
	 */
	public InstructionalObjectiveModel createInstructionalObjective(InstructionalObjectiveModel am);

	/**
	 * 素材修改
	 * @param rm
	 * @return
	 */
	public InstructionalObjectiveModel updateInstructionalObjective(InstructionalObjectiveModel am);

	/**
	 * 根据教学目标id查询出与之关联的章节信息id
	 * 分两种情况：
	 * 1.教学目标与章节直接关联
	 * 2.教学目标与课时关联，课时与章节关联
	 * @param id
	 * @return
     */
	List<Map<String, Object>> getChapterRelationById(String id);

	/***
	 * 根据教学目标Id查询它的Title
	 * @param idWithTitle 教学目标Id
	 */
	String getInstructionalObjectiveTitle(Map.Entry<String, String> idWithTitle);

	Map<String, String> getInstructionalObjectiveTitle(Collection<Map.Entry<String, String>> idWithTitles);

	/***
	 * 获取未关联到章节/课时的教学目标
	 * @param limit 分页
	 * @param unrelationCategory 未关联的category ""表示同时未关联章节和课时"chapters"/"lessons"/""
	 * @param knowledgeTypeCode 知识点类型维度编码
	 * @param instructionalObjectiveTypeId 教学目标类型Id
	 */
	ListViewModel<InstructionalObjectiveModel> getUnRelationInstructionalObjective(String knowledgeTypeCode, String instructionalObjectiveTypeId, String unrelationCategory, String limit);

	/**
	 * 根据套件id获取教学目标列表
	 * @param suiteId	套件id
	 * @param limit		分页参数
	 * @return
	 */
	public ListViewModel4Suite queryListBySuiteId(String suiteId,String limit);

	/**
	 * 获取章节下的有序的教学目标列表
	 * @param includesList
	 * @param relationsMap
	 * @param coveragesList
	 * @param limit
	 * @param reverseBoolean
     * @return
     */
	ListViewModel<ResourceModel> getResourcePageByChapterId(List<String> includesList, List<Map<String, String>> relationsMap, List<String> coveragesList, String limit, boolean reverseBoolean);

	InstructionalObjectiveModel patchInstructionalObjective(InstructionalObjectiveModel instructionalObjectiveModel);

	/**
	 * 业务创建接口
	 *
	 * 入参：
		{
		    "suit_id": "123456",
		    "user_id": "644869",
            "knowledges": [
                {
                    "knowledge_id": "",
                    "knowledge_title": "什么都不知道",
                    "position": 1
                }
            ],
            "custom_properties":{},
		    "objectives": [
		        {
		            "identifier": "",
		            "title": "记住我是谁",
		            "status": "ONLINE",
		            "description": "HEHEHE",
		            "operate_type":0,//0代表返回提示，1代表新增，2代表替换
		            "provider_source":"",
		            "keywords":[""],
		            "applicable_period":["SL000301"],
		            "versions":["多版本"],
		            "objective_type_id": "6837a5ef-1cca-4447-b828-b309b349dc9b",
		            "objective_type_title": "知道<span style=\"color: #ff0000\">【文章A】</span>的译者"
	 				"copy_suite_id":"" //在哪个拷贝套件下创建学习目标，不是拷贝套件不用传
		        }
		    ]
		}
		注意：
		编辑内容：  title、description、关联知识点;   当identifier为空则新增，否则为编辑；
		knowledges 是 数组，顺序表示知识点在学习目标的填充顺序；  前端判断是否有编辑title内的知识点？如果有修改，则传入知识点信息，如果没有修改，则不传~
	 *
	 *
	 * @param svm
	 * @return
	 */
	Object create4Business(SuiteViewModel svm);

	/**
	 * 根据知识点名称，套件id返回教学目标类型、教学目标、子教学目标数据
	 * {
		  "knowledge": {  //知识点模型
		    "identifier":"12sdfe2154",
		    "title":"中国"
		  },
		  "items": [
		    {
		      "objective_type": {//学习目标类型模型
		        "identifier":"",
		        "title":"",
		        "description":"",
		        "custom_properties":""
		      },
		      "objectives": [  //学习目标模型
		        {
		          "identifier":"",
		          "title":"",
		          "description":"",
		          "creator":"",
		          "versions":[]
		        }
		      ]
		    }
		  ]
		}
	 * @param title
	 * @return
	 */
	public Map<String,Object> queryListByKnTitle(String title,String suiteId);

	/**
	 * 根据套件id查询教学目标，并按知识点来进行分页
	 * @param suiteId
	 * @param limit
	 * @return
	 */
	public Map<String,Object> queryList4KnowledgeBySuiteId(String suiteId,String limit);

	/**
	 * 根据套件id查询教学目标，并按样例来进行分页
	 * 
	 * {
    "limit": "0,2",
    "total": 66,
    "items": [
        {
            "sample": {
                "sample_id": "35e6bbc9-953f-49b9-aa4c-780e3eb62706",
                "sample_status": "CREATED",
                "knowledge1": "801dd088-6873-4a77-8845-4e22782980e8",
                "sample_title": "太阳"
            },
            "datas": [
                {
                    "objective_type": {
                        "identifier": "fbc82708-ed00-438e-8874-68037d11d4da",
                        "description": "读出<span style=\"color:#ff0000\">【单位A】</span>的符号",
                        "custom_properties": {
                            "template": "读出$bracket0$的符号",
                            "varMap": {
                                "bracket0": "bracket0"
                            },
                            "knowledge_type_A": "KC011",
                            "regexTemplate": "读出([\\w\\W]+)的符号"
                        },
                        "title": "306",
                        "suite_id": "d28caf84-ec4f-4478-9092-efae88470168"
                    },
                    "objectives": [
                        {
                            "knowledge_list": [
                                {
                                    "identifier": "e1a5f262-8ebb-4aee-b943-53865a068f48",
                                    "title": "g"
                                }
                            ],
                            "identifier": "1b363f78-af76-4712-8f47-cd7480908899",
                            "versions": [
                                "TEST一元二次方程"
                            ],
                            "description": "",
                            "creator": "",
                            "custom_properties": {
                                "m": "m"
                            },
                            "providerSource":"",
                            "applicable_period":[],
                            "title": "读出g的符号",
                            "status":"CREATED"
                        }
                    ]
                }
            ]
        }
    ]
}
	 * 
	 * 
	 * 
	 * 
	 * @param suiteId
	 * @param limit
	 * @return
	 */
	public Map<String,Object> queryList4SampleBySuiteId(String suiteId,String limit,Boolean isSubSuite,int level,String relationType);

	/**
	 * 修改资源状态
	 * @param params
	 * 	[
			{
				"res_type":"",
				"user_id":"",
				"items":[
					{
						"identifier":"",
						"status":"",
						"message":"",
					}
				]
			}
		]
	 */
	public void changeStatus(List<Map<String,Object>> params);

	public List<Map<String, String>> querySample(String rootSuit,
			Set<String> knIDTemp);
    //deleteObjectiveTypes 用来判断是否需要删除学习目标类型
	public Map<String,Object> deleteSample(String sampleId, String userId);
	
	/**
	 * 修改样例自定义属性
	 * @param sampleId
	 * @param userId
	 * @return
	 */
	public SampleViewModel updateSample(String sampleId,Map<String,Object> customPropertiesMap,String userId);

	public void transportData();

	public List<String> updateSortNum(List<String> objectiveTypeIds, float repairedNum);


	/**
	 * 查询最子套件
	 * @author xm
	 * @date 2016年11月29日 下午2:12:44
	 */
	public List<Map<String, String>> queryDownSubsuite();
	/**
	 * 按descript进行分类
	 * @author xm
	 * @date 2016年12月1日 下午9:21:39
	 */
	public Map<String, List<String>> classifyByDescription(List<Map<String, String>> list);
	/**
	 * 从缓存中取
	 * @author xm
	 * @date 2016年12月1日 下午9:22:56
	 */
	public Map<String, List<String>> getclassifyDownSubSuiteListFromRedis(String key);
	/**
	 * 存入缓存
	 * @author xm
	 * @date 2016年12月1日 下午9:23:33
	 */
	public void saveClassifyDownSubSuiteResultToRedis(final Map<String, List<String>> map,String key);

	/**
	 * 重载queryDownSubsuite()方法
	 * @author xm
	 * @date 2016年11月29日 下午2:12:44
	 */
	public List<Map<String, String>> queryDownSubsuite(String status,String category);


	/**查询最子套件接口，没有status
	 * @author xm
	 * @param words
	 * @param limit
	 */
	public ListViewModel<ClassifySuitAndInstructionalObjectiveModel> classfiDownSuiteQuery(String words,String limit,String status,String category);
	 /**
     * 根据套件id列表查询学习目标及套件的研究分析
     * @param suiteId
     * @return
     */
	List<SuitAndInstructionalObjectiveModel> getSuitAndInstructionalObjective(List<String> suitId);

	/**
	 * 查询根套件
	 * @author xm
	 * @date 2016年11月29日 下午8:27:21
	 */
	public List<Map<String, String>> queryRootParent();

	/**
	 * 根据套件的id查找对应的教学目标类型
	 * @author xm
	 * @date 2016年11月29日 下午8:37:49
	 */
	public List<Map<String, String>> queryObjectTypeBySuiteId(List<String> sourceId);

	/**
	 * 通过教学目标类型查询教学目标
	 * @author xm
	 * @date 2016年11月29日 下午8:56:52
	 */
	public List<Map<String, String>> queryInstructionObjectByObjectTypeId(List<String> sourceId);

	/**
	 * 通过知识点列表查询这些知识点关联的相同的教学目标并且这个学习目标和指定的教学目标类型关联
	 * @author xm
	 * @date 2016年11月30日 下午6:58:14
	 */
	public List<Map<String, String>> queryInstructionObjectByKnIdListAndInsObjType(List<String> knowledgeId,List<String> insObjType);
	/**
	 * 查询通过教学目标列表查子教学目标
	 * @author xm
	 * @date 2016年12月1日 下午9:26:05
	 */
	public List<Map<String, String>> querySubInstructionObjectByInstructionObjectId(List<String> insObjId);
	/**
	 * 通过知识点id查询知识点的enable
	 * @author xm
	 * @date 2016年12月1日 下午9:27:20
	 */
	public List<Map<String, String>> queryResourceByknowledgeId(String knId);

	/**
	 * 套件的相关信息统计
	 * @author xm
	 * @date 2016年12月1日 下午9:28:12
	 */
	public List<SuiteStatisticsModel> suiteStatistics();

	   /**
     * 修改返回样例数据为空时的样例状态为不可用
     *@author yzc
     *@date 2016年12月7日
     * @return
     */
	public void changeSampleStatus();

	public List<ResourceCategory> addApplicablePeriodCategoryList(
			String resource, List<String> applicablePeriod);

	public float dealObjectivesSortNum(String objectiveTypeId, Map<String, Float> sortNumMap, String relatioType);

	public void changeSampleStatus(String suiteId,String status);
	public Map<String, List<MultiLevelSuiteViewModel>> dealReturnMap(Map<String, Map<String, List<MultilayerClassifySubSuite>>> multiLevelMap, String level);
}