package nd.esp.service.lifecycle.daos.instructionalobjectives.v06;

import java.util.List;
import java.util.Map;

import nd.esp.service.lifecycle.models.v06.ResultsModel;
import nd.esp.service.lifecycle.repository.model.Chapter;
import nd.esp.service.lifecycle.repository.model.InstructionalObjective;

public interface InstructionalobjectiveDao {
	/**
	 * 根据套件id查找教学目标的总数
	 * @param suiteId
	 * @return
	 */
	public Integer queryCountBySuiteId(String suiteId);
	
	/**
	 * 根据套件id查找教学目标的总数(包含教学目标类型未挂教学目标的数量)
	 * @param suiteId
	 * @return
	 */
	public Integer queryCountBySuiteId4Blank(String suiteId);
	
	/**
	 * 根据套件id查找教学目标列表
	 * @param suiteId
	 * @param limit
	 * @return
	 */
	public List<Map<String,Object>> queryListBySuiteId(String suiteId,String limit);
	
	/**
	 * 根据title查找教学目标
	 * @param title
	 * @return
	 */
	public List<InstructionalObjective> queryListByTitle(String title);
	
	/**
	 * 根据identifier获取教材目标详情
	 * @param identifier
	 * @return
	 */
	public InstructionalObjective getInstructionalObjective(String identifier);
	
	/**
	 * 根据title查找知识点列表
	 * @param title
	 * @return
	 */
	public List<Map<String,Object>> queryKnowledgeListByTitles(List<String> titles);
	
	/**
	 * 根据知识点id查找知识点列表
	 * @param ids
	 * @return
	 */
	public List<Chapter> queryKnowledgeListByIds(List<String> ids);

	/**
	 * 根据知识点id查找出相关的教学目标类型、教学目标、子教学目标数据
	 * @param knId
	 * @return
	 */
	public List<Map<String,Object>> queryByKnId(String knId);
	
	/**
	 * 根据教学目标类型列表查找知识点名称列表
	 * @param otIds
	 * @return
	 */
	public List<Map<String,String>> queryKnTitleListByObjectiveTypeIds(List<String> otIds);
	
	/**
	 * 根据教学目标类型列表与指定知识点列表查找教学目标列表
	 * @param otIds
	 * @return
	 */
	public List<Map<String,String>> queryListByOtIdsAndKnIds(List<String> otIds,List<String> knIds);
	
	/**
	 * 根据套件id列表查找教学目标列表及套件id和title
	 * @param suitId
	 * @return
	 */
	public List<ResultsModel> getInstructionalObjectiveById(List<String> suitId);
}
