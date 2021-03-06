package nd.esp.service.lifecycle.daos.titan;

import nd.esp.service.lifecycle.daos.titan.inter.TitanCategoryRepository;
import nd.esp.service.lifecycle.daos.titan.inter.TitanCommonRepository;
import nd.esp.service.lifecycle.daos.titan.inter.TitanRepositoryUtils;
import nd.esp.service.lifecycle.repository.model.ResourceCategory;
import nd.esp.service.lifecycle.support.busi.titan.TitanCacheData;
import nd.esp.service.lifecycle.support.busi.titan.TitanKeyWords;
import nd.esp.service.lifecycle.support.busi.titan.TitanSyncType;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.utils.TitanScritpUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class TitanCategoryRepositoryImpl implements TitanCategoryRepository {

	@Autowired
	private TitanCommonRepository titanCommonRepository;

	private static final Logger LOG = LoggerFactory
			.getLogger(TitanCategoryRepositoryImpl.class);

	@Autowired
	private TitanRepositoryUtils titanRepositoryUtils;
	/**
	 * 1、添加维度数据；2、添加资源冗余数据
	 * */
	@Override
	public ResourceCategory add(ResourceCategory resourceCategory) {
		if(resourceCategory == null){
			return null;
		}
		ResourceCategory rc = addOrUpdateResourceCategory(resourceCategory);
		if(rc == null){
			titanRepositoryUtils.titanSync4MysqlAdd(TitanSyncType.SAVE_OR_UPDATE_ERROR,
					resourceCategory.getPrimaryCategory(),resourceCategory.getResource());
		}

		String path = resourceCategory.getTaxonpath();
		if(path !=null && !path.equals("")){
			String resultPath = addPath(resourceCategory.getResource(), resourceCategory.getPrimaryCategory(), path);
			if(resultPath == null){
				titanRepositoryUtils.titanSync4MysqlAdd(TitanSyncType.SAVE_OR_UPDATE_ERROR,
						resourceCategory.getPrimaryCategory(),resourceCategory.getResource());
			}
		}

		if(rc == null){
			return null;
		}
		//更新资源的冗余数据search_path\search_code
		Set<String> category = new HashSet<>();
		category.add(rc.getTaxoncode());
		Set<String> pathSet = new HashSet<>();
		pathSet.add(rc.getTaxonpath());

		updateResourceProperty(pathSet,category ,resourceCategory.getPrimaryCategory(), resourceCategory.getResource());
		return  rc;
	}

	/**
	 * 批添加维度数据：<br>
	 * 		1、只支持对同一个资源的维度数据进行批量增加，多个资源批量增加会出现异常<br>
	 *     	2、批量增加前会先删除历史数据<br>
	 *     	3、更新资源的冗余数据
	 * */
	@Override
	public List<ResourceCategory> batchAdd(
			List<ResourceCategory> resourceCategories) {
		if(CollectionUtils.isEmpty(resourceCategories)){
			return new ArrayList<>();
		}

		Map<String, List<ResourceCategory>> resourceCategoryMap = new HashMap<String, List<ResourceCategory>>();
		for(ResourceCategory resourceCategory:resourceCategories){
			List<ResourceCategory> valuesCategories = resourceCategoryMap.get(resourceCategory.getResource());
			if(valuesCategories==null){
				valuesCategories = new ArrayList<ResourceCategory>();
				resourceCategoryMap.put(resourceCategory.getResource(), valuesCategories);
			}
			valuesCategories.add(resourceCategory);
		}
		// FIXME
		List<ResourceCategory> list = new ArrayList<>();
		for(List<ResourceCategory> entryValueCategories:resourceCategoryMap.values()){
			ResourceCategory category = entryValueCategories.get(0);

			//批量保存PATH
			List<String> pathList = batchAddPath(entryValueCategories);
			Set<String> pathSet = new HashSet<>(pathList);
			//批量保存维度数据
			Set<String> categorySet = new HashSet<>();
			for (ResourceCategory resourceCategory : entryValueCategories) {
				ResourceCategory rc = addOrUpdateResourceCategory(resourceCategory);
				if(rc!=null){
					list.add(rc);
				} else {
					titanRepositoryUtils.titanSync4MysqlAdd(TitanSyncType.SAVE_OR_UPDATE_ERROR,
							resourceCategory.getPrimaryCategory(),resourceCategory.getResource());
				}
				categorySet.add(resourceCategory.getTaxoncode());
			}
			updateResourceProperty(pathSet, categorySet, category.getPrimaryCategory() ,category.getResource());
		}
		return list;
	}

	@Override
	public ResourceCategory update(ResourceCategory resourceCategory) {
		/**
		 * 待定
		 * */
		return null;
	}

	@Override
	public List<ResourceCategory> batchUpdate(List<ResourceCategory> entityList) {
		/**
		 * 待定
		 * */
		return null;
	}

	@Override
	public boolean delete(String id) {
		try {
			titanCommonRepository.deleteEdgeById(id);
		} catch (Exception e) {
			titanRepositoryUtils.titanSync4MysqlAdd(TitanSyncType.DELETE_CATEGORY_ERROR,
					TitanKeyWords.category_code.toString(),id);
			return false;
		}
		return true;
	}

	@Override
	public boolean batchDelete(List<String> ids) {
		if(CollectionUtils.isEmpty(ids)){
			return true;
		}
		for (String id : ids){
			delete(id);
		}
		return true;
	}

	@Override
	/**
	 * 删除维度数据和相关的冗余数据
	 * */
	public void deleteAll(String primaryCategory, String identifier) {
		String deleteScript = "g.V().has(primaryCategory,'identifier',identifier)" +
				".outE().or(hasLabel('has_categories_path'),hasLabel('has_category_code')).drop();";
		String deleteScript2 = "g.V().has(primaryCategory,'identifier',identifier).properties('search_code','search_path').drop()";

		Map<String, Object> param = new HashMap<>();
		param.put("primaryCategory", primaryCategory);
		param.put("identifier", identifier);
		try {
			titanCommonRepository.executeScript(deleteScript, param);
			titanCommonRepository.executeScript(deleteScript2, param);
		} catch (Exception e) {
			titanRepositoryUtils.titanSync4MysqlAdd(TitanSyncType.SAVE_OR_UPDATE_ERROR,
					primaryCategory, identifier);
		}
	}


	private List<String> batchAddPath(List<ResourceCategory> resourceCategories){
		if(resourceCategories==null||resourceCategories.size()==0){
			return null;
		}
		List<String> pathList = new ArrayList<>();
		String primaryCategory = resourceCategories.get(0).getPrimaryCategory();
		Map<String,List<String>> resourceCategoryMap = new HashMap<>();
		for (ResourceCategory resourceCategory : resourceCategories) {
			String path = resourceCategory.getTaxonpath();
			String resource = resourceCategory.getResource();
			List<String> resourceCategoryList = resourceCategoryMap.get(resource);
			if(resourceCategoryList==null){
				resourceCategoryList = new ArrayList<>();
				resourceCategoryMap.put(resource,resourceCategoryList);
			}

			if(!resourceCategoryList.contains(path)){
				resourceCategoryList.add(path);
			}
		}

		for(String key : resourceCategoryMap.keySet()){
			for(String path : resourceCategoryMap.get(key)){
				if(StringUtils.isNotEmpty(path)){
					String p = addPath(key,primaryCategory,path);
					if(p != null){
						pathList.add(p);
					} else {
						titanRepositoryUtils.titanSync4MysqlAdd(TitanSyncType.SAVE_OR_UPDATE_ERROR,
								primaryCategory,key);
					}
				}
			}
		}
		return pathList;
	}

	private ResourceCategory addOrUpdateResourceCategory(ResourceCategory resourceCategory){
		StringBuffer script;
		Map<String, Object> graphParams;
		//检查维度数据是否已经存在
		String checkExistCategory = "g.E().hasLabel('has_category_code').has('identifier',edgeIdentifier).inV().has('cg_taxoncode',taxoncode).id()";
		Map<String, Object> checkParam = new HashMap<>();
		checkParam.put("edgeIdentifier",resourceCategory.getIdentifier());
		checkParam.put("taxoncode", resourceCategory.getTaxoncode());
		String oldEdgeId = null;
		try {
			oldEdgeId = titanCommonRepository.executeScriptUniqueString(checkExistCategory,checkParam);
		} catch (Exception e) {
			LOG.error("titan_repository error:{};identifier:{}" ,e.getMessage(),resourceCategory.getResource());
			return null;
		}

		if(StringUtils.isNotEmpty(oldEdgeId)){
			return resourceCategory;
		} else {
			try {
				titanCommonRepository.deleteEdgeById(resourceCategory.getIdentifier());
			} catch (Exception e) {
				LOG.error("titan_repository error:{};identifier:{}" ,e.getMessage(),resourceCategory.getResource());
				return null;
			}
		}

		//检查code在数据库中是否已经存在
		Long categoryCodeNodeId = getCategoryCodeId(resourceCategory);
		String edgeId ;
		if (categoryCodeNodeId != null) {
			script = new StringBuffer("g.V().has(primaryCategory,'identifier',identifier).next()" +
					".addEdge('has_category_code',g.V(categoryCodeNodeId).next()");
			graphParams = TitanScritpUtils.getParamAndChangeScript(script,resourceCategory);
			script.append(",'identifier',edgeIdentifier");
			//增加对taxonpath的null判断
			if(resourceCategory.getTaxonpath() != null){
				script.append(",'cg_taxonpath',cgTaxonpath");
				graphParams.put("cgTaxonpath", resourceCategory.getTaxonpath());
			}
			script.append(").id()");
			graphParams.put("primaryCategory",
					resourceCategory.getPrimaryCategory());
			graphParams.put("identifier", resourceCategory.getResource());
			graphParams.put("categoryCodeNodeId", categoryCodeNodeId);
			graphParams.put("edgeIdentifier",resourceCategory.getIdentifier());

			try {
				edgeId = titanCommonRepository.executeScriptUniqueString(script.toString(), graphParams);
			} catch (Exception e) {
				LOG.error("titan_repository error:{};identifier:{}" ,e.getMessage(),resourceCategory.getResource());
				return null;
			}
		} else {
			script = new StringBuffer(
					"category_code = graph.addVertex(T.label,'category_code'");
			graphParams = TitanScritpUtils.getParamAndChangeScript(script, resourceCategory);
			script.append(");");
			script.append("g.V().hasLabel(primaryCategory).has('identifier',identifier).next()" +
					".addEdge('has_category_code',category_code");
			Map<String, Object> paramEdgeMap = TitanScritpUtils.getParamAndChangeScript(script, resourceCategory);
			script.append(",'identifier',edgeIdentifier");
			//增加对taxonpath的null判断
			if(resourceCategory.getTaxonpath() != null){
				script.append(",'cg_taxonpath',cgTaxonpath");
				graphParams.put("cgTaxonpath", resourceCategory.getTaxonpath());
			}
			script.append(").id()");

			graphParams.put("primaryCategory",
					resourceCategory.getPrimaryCategory());
			graphParams.putAll(paramEdgeMap);

			graphParams.put("identifier", resourceCategory.getResource());
			graphParams.put("edgeIdentifier",resourceCategory.getIdentifier());

			try {
				edgeId = titanCommonRepository.executeScriptUniqueString(script.toString(), graphParams);
			} catch (Exception e) {
				LOG.error("titan_repository error:{};identifier:{}" ,e.getMessage(),resourceCategory.getResource());
				return null;
			}
		}

		if(edgeId == null){
			return null;
		}

		return resourceCategory;
	}

	/**
	 * 添加path，如果path已经存在者不进行其它的操作
	 * */
	private String addPath(String resource ,String resourcePrimaryCategory,String path ){
		//检查path是否已经存在
		String checkPathExist = "g.V().hasLabel(resourcePrimaryCategory).has('identifier',resource)" +
				".outE().hasLabel('has_categories_path').inV().has('cg_taxonpath',path).id()";
		Map<String, Object> checkPathParam = new HashMap<>();
		checkPathParam.put("resourcePrimaryCategory",resourcePrimaryCategory);
		checkPathParam.put("resource",resource);
		checkPathParam.put("path",path);

		String oldPathId = null;
		try {
			oldPathId = titanCommonRepository.executeScriptUniqueString(checkPathExist, checkPathParam);
		} catch (Exception e) {
			LOG.error("titan_repository error:{}  identifier:{}" ,e.getMessage(),resource);
			return null;
		}
		if(StringUtils.isNotEmpty(oldPathId)){
			return path;
		}

		String queryPathScript = "g.V().has('categories_path','cg_taxonpath',taxonpath).id()";
		Map<String,Object> queryPathParams = new HashMap<>();
		queryPathParams.put("taxonpath",path);
		Long sourcePathId = null;
		try {
			sourcePathId = titanCommonRepository.executeScriptUniqueLong(queryPathScript,queryPathParams);
		} catch (Exception e) {
			LOG.error("titan_repository error:{}  identifier:{}" ,e.getMessage(),resource);
			return null;
		}

		Map<String,Object> addScriptParams = new HashMap<>();
		StringBuilder addPathScript;
		String edgeId;
		if(sourcePathId == null){
			addPathScript = new StringBuilder("categories_path = graph.addVertex(T.label,'categories_path','cg_taxonpath',taxonpath);");
			addPathScript.append("g.V().hasLabel(source_primaryCategory).has('identifier',source_identifier).next()" +
					".addEdge('has_categories_path',categories_path,'cg_taxonpath',taxonpath).id()");
			addScriptParams.put("taxonpath",path);
			addScriptParams.put("source_primaryCategory",resourcePrimaryCategory);
			addScriptParams.put("source_identifier",resource);

			try {
				edgeId = titanCommonRepository.executeScriptUniqueString(addPathScript.toString(), addScriptParams);
			} catch (Exception e) {
				LOG.error("titan_repository error:{}  identifier:{}" ,e.getMessage(),resource);
				return null;
			}
		} else {
			addPathScript = new StringBuilder("g.V().hasLabel(source_primaryCategory).has('identifier',source_identifier).next()" +
					".addEdge('has_categories_path',g.V(sourcePathId).next(),'cg_taxonpath',taxonpath).id()");
			addScriptParams.put("sourcePathId",sourcePathId);
			addScriptParams.put("source_primaryCategory",resourcePrimaryCategory);
			addScriptParams.put("source_identifier",resource);
			addScriptParams.put("taxonpath",path);
			try {
				edgeId = titanCommonRepository.executeScriptUniqueString(addPathScript.toString(), addScriptParams);
			} catch (Exception e) {
				LOG.error("titan_repository error:{}  identifier:{}" ,e.getMessage(),resource);
				return null;
			}
		}

		if(edgeId == null){
			return null;
		}


		return path;
	}

	private Long getCategoryCodeId(ResourceCategory resCoverage) {
		Long taxoncodeId =  TitanCacheData.taxoncode.getCacheMap().get(resCoverage.getTaxoncode());

		if(taxoncodeId == null) {
			String scriptString = "g.V().hasLabel('category_code').has('cg_taxoncode',taxoncode).id()";
			Map<String, Object> graphParams = new HashMap<String, Object>();
			graphParams.put("taxoncode",resCoverage.getTaxoncode());
			try {
				taxoncodeId = titanCommonRepository.executeScriptUniqueLong(scriptString, graphParams);
			} catch (Exception e) {
				LOG.error("titan_repository error:{}" ,e.getMessage());
			}

			if(TitanCacheData.taxoncode.getCacheMap().size() < 2000){
				TitanCacheData.taxoncode.getCacheMap().put(resCoverage.getTaxoncode(), taxoncodeId);
			}
		}
		return taxoncodeId;
	}

	/**
	 * 更新数据的冗余字段
	 * */
	private void updateResourceProperty(Set<String> pathSet , Set<String> codeSet , String primaryCategory, String identifier){
		StringBuffer script = new StringBuffer("g.V()has(primaryCategory,'identifier',identifier)");
		Map<String, Object> param = new HashMap<>();
		param.put("primaryCategory" ,primaryCategory);
		param.put("identifier" ,identifier);
		TitanScritpUtils.getSetScriptAndParam(script, param ,"search_code",codeSet);

		TitanScritpUtils.getSetScriptAndParam(script, param ,"search_path",pathSet);

		if(CollectionUtils.isNotEmpty(pathSet)){
			String searchPathString = StringUtils.join(pathSet, ",").toLowerCase();
			script.append(".property('search_path_string',searchPathString)");
			param.put("searchPathString", searchPathString);

		}
		if(CollectionUtils.isNotEmpty(codeSet)){
			String searchCodeString = StringUtils.join(codeSet, ",").toLowerCase();
			script.append(".property('search_code_string',searchCodeString)");
			param.put("searchCodeString", searchCodeString);
		}
		try {
			titanCommonRepository.executeScript(script.toString(), param);
		} catch (Exception e) {
			LOG.error("titan_repository error:{}  identifier:{}" ,e.getMessage(),identifier);
		}
	}

}
