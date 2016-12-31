package nd.esp.service.lifecycle.daos.instructionalobjectives.v06.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nd.esp.service.lifecycle.daos.instructionalobjectives.v06.InstructionalobjectiveDao;
import nd.esp.service.lifecycle.models.v06.ResultsModel;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.Chapter;
import nd.esp.service.lifecycle.repository.model.InstructionalObjective;
import nd.esp.service.lifecycle.repository.sdk.ChapterRepository;
import nd.esp.service.lifecycle.repository.sdk.InstructionalobjectiveRepository;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.utils.gson.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class InstructionalobjectiveDaoImpl implements InstructionalobjectiveDao{
	
	private static final Logger LOG = LoggerFactory
			.getLogger(InstructionalobjectiveDaoImpl.class);
	
	@Autowired
	private JdbcTemplate jt;
	
	@Autowired
	private InstructionalobjectiveRepository repository;
	
	@Autowired
	private ChapterRepository chapterRepository;
	
	@Override
	public Integer queryCountBySuiteId(String suiteId) {
		String sql = getQueryCountSql(suiteId);
		return jt.queryForObject(sql, Integer.class);
	}
	
	@Override
	public Integer queryCountBySuiteId4Blank(String suiteId) {
		String sql = getQueryCountSql4Blank(suiteId);
		return jt.queryForObject(sql, Integer.class);
	}

	@Override
	public List<Map<String, Object>> queryListBySuiteId(String suiteId,
			String limit) {
		String sql = getQueryListSql(suiteId,limit);
		return jt.queryForList(sql);
	}
	
	@Override
	public List<InstructionalObjective> queryListByTitle(String title) {
		InstructionalObjective entity = new InstructionalObjective();
		entity.setTitle(title);
		entity.setEnable(true);
		try {
			return repository.getAllByExample(entity);
		} catch (EspStoreException e) {
			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getMessage());
		}
	}

	@Override
	public InstructionalObjective getInstructionalObjective(String identifier) {
		try {
			return repository.get(identifier);
		} catch (EspStoreException e) {
			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getMessage());
		}
	}

	@Override
	public List<Map<String,Object>> queryKnowledgeListByTitles(List<String> titles) {
		String sql = "select nd.identifier,nd.title from ndresource nd,chapters c where nd.primary_category = 'knowledges' and nd.identifier = c.identifier and nd.enable = 1 and nd.title in (:titles)";
		NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("titles", titles);
		List<Map<String,Object>> rrList = npjt.query(sql, params, new RowMapper<Map<String,Object>>(){
			@Override
			public Map<String, Object> mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("identifier", rs.getString("identifier"));
				map.put("title", rs.getString("title"));
				return map;
			}
    	});
		return rrList;
	}

	@Override
	public List<Chapter> queryKnowledgeListByIds(List<String> ids) {
		try {
			return chapterRepository.getAll(ids);
		} catch (EspStoreException e) {
			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getMessage());
		}
	}
	
	/**
	 * 根据套件计算教学目标的数量（包括教学目标类型下没有挂教学目标）
	 * @return
	 */
	private String getQueryCountSql4Blank(String suiteId){
		String sql = ""
				+ "SELECT count(1) "
				+ "		FROM resource_relations rr1, "
				+ "		     resource_categories rc1, "
				+ "		     ndresource nd1 "
				+ "		LEFT JOIN resource_relations rr2 ON rr2.res_type='assets' "
				+ "		AND rr2.resource_target_type='instructionalobjectives' "
				+ "		AND rr2.enable = 1 "
				+ "		AND rr2.source_uuid = nd1.identifier "
				+ "		LEFT JOIN ndresource nd2 ON nd2.primary_category = 'instructionalobjectives' "
				+ "		AND nd2.enable = 1 "
				+ "		AND rr2.target = nd2.identifier "
				+ "		WHERE rr1.res_type='assets' "
				+ "		  AND rr1.source_uuid = '"+suiteId+"' "
				+ "		  AND rr1.resource_target_type = 'assets' "
				+ "		  AND rr1.enable = 1 "
				+ "		  AND nd1.enable = 1 "
				+ "		  AND rr1.target = nd1.identifier "
				+ "		  AND rc1.primary_category = 'assets' "
				+ "		  AND rc1.resource = nd1.identifier "
				+ "		  AND rc1.taxOnCode = '$RA0503'";
		return sql;
	}
	
	private String getQueryCountSql(String suiteId){
		String sql = ""
				+ "SELECT count(1) "
				+ "FROM resource_relations rr1, "
				+ "     ndresource nd1, "
				+ "     resource_categories rc1, "
				+ "     resource_relations rr2, "
				+ "     ndresource nd2 "
				+ "WHERE rr1.res_type='assets' "
				+ "  AND rr1.source_uuid = '"+suiteId+"'"
				+ "  AND rr1.resource_target_type = 'assets' "
				+ "  AND rr1.enable = 1 "
				+ "  AND nd1.enable = 1 "
				+ "  AND rr1.target = nd1.identifier "
				+ "  AND rc1.primary_category = 'assets' "
				+ "  AND rc1.resource = nd1.identifier "
				+ "  AND rc1.taxOnCode = '$RA0503' "
				+ "  AND rr2.res_type='assets' "
				+ "  AND rr2.resource_target_type='instructionalobjectives' "
				+ "  AND rr2.enable = 1 "
				+ "  AND rr2.source_uuid = nd1.identifier "
				+ "  AND rr2.target = nd2.identifier "
				+ "  AND nd2.primary_category = 'instructionalobjectives' "
				+ "  AND nd2.enable = 1";
		return sql;
	}
	
	private String getQueryListSql(String suiteId,String limit){
		String sql = ""
				+ "SELECT nd2.identifier AS identifier, "
				+ "       nd2.title AS title, "
				+ "       nd2.description AS description, "
				+ "       nd2.version AS VERSION, "
				+ "       nd2.estatus AS status, "
				+ "       nd2.creator AS creator, "
				+ "       nd2.create_time, "
				+ "       nd2.last_update, "
				+ "       nd1.identifier AS objective_type_id, "
				+ "       nd1.description AS objective_type_title, "
				+ "       rc2.taxOnCode AS subject_code, "
				+ "       rc2.taxOnName AS subject "
				+ "FROM resource_relations rr1, "
				+ "     resource_categories rc1, "
				+ "     ndresource nd1 "
				+ "LEFT JOIN resource_relations rr2 ON rr2.res_type='assets' "
				+ "AND rr2.resource_target_type='instructionalobjectives' "
				+ "AND rr2.enable = 1 "
				+ "AND rr2.source_uuid = nd1.identifier "
				+ "LEFT JOIN ndresource nd2 ON nd2.primary_category = 'instructionalobjectives' "
				+ "AND nd2.enable = 1 "
				+ "AND rr2.target = nd2.identifier "
				+ "LEFT JOIN resource_categories rc2 ON rc2.primary_category='instructionalobjectives' "
				+ "AND rc2.resource = nd2.identifier "
				+ "AND rc2.taxOnCode LIKE '$SB%' "
				+ "WHERE rr1.res_type='assets' "
				+ "  AND rr1.source_uuid = '"+suiteId+"' "
				+ "  AND rr1.resource_target_type = 'assets' "
				+ "  AND rr1.enable = 1 "
				+ "  AND nd1.enable = 1 "
				+ "  AND rr1.target = nd1.identifier "
				+ "  AND rc1.primary_category = 'assets' "
				+ "  AND rc1.resource = nd1.identifier "
				+ "  AND rc1.taxOnCode = '$RA0503' "
				+ "ORDER BY nd1.title LIMIT "+limit;
		return sql;
	}

	@Override
	public List<Map<String, Object>> queryByKnId(String knId) {
		String sql = ""
				+ "SELECT nd2.identifier as ot_id, "
				+ "       nd2.title as ot_title, "
				+ "       nd2.description as ot_desc, "
				+ "       nd2.custom_properties as ot_cust, "
				+ "       nd1.identifier as obj_id, "
				+ "       nd1.title as obj_title, "
				+ "       nd1.description as obj_desc, "
				+ "       nd3.description as sub_obj_desc "
				+ "FROM resource_relations rr1, "
				+ "     resource_relations rr2, "
				+ "     ndresource nd2, "
				+ "     ndresource nd1 "
				+ "LEFT JOIN resource_relations rr3 ON rr3.res_type = 'instructionalobjectives' "
				+ "AND rr3.resource_target_type = 'subInstruction' "
				+ "AND rr3.enable = 1 "
				+ "AND rr3.source_uuid = nd1.identifier "
				+ "LEFT JOIN ndresource nd3 ON nd3.primary_category = 'subInstruction' "
				+ "AND nd3.enable = 1 "
				+ "AND nd3.identifier = rr3.target "
				+ "WHERE rr1.res_type = 'knowledges' "
				+ "  AND rr1.resource_target_type='instructionalobjectives' "
				+ "  AND rr1.enable = 1 "
				+ "  AND rr1.source_uuid = '"+knId+"' "
				+ "  AND rr1.target = nd1.identifier "
				+ "  AND nd1.primary_category='instructionalobjectives' "
				+ "  AND nd1.enable= 1 "
				+ "  AND rr2.res_type='assets' "
				+ "  AND rr2.resource_target_type='instructionalobjectives' "
				+ "  AND rr2.enable = 1 "
				+ "  AND rr2.target = nd1.identifier "
				+ "  AND rr2.source_uuid = nd2.identifier "
				+ "  AND nd2.primary_category='assets' "
				+ "  AND nd2.enable = 1";
		return jt.queryForList(sql);
	}
	
	public List<Map<String,String>> queryKnTitleListByObjectiveTypeIds(List<String> otIds){
		String sql = ""
				+ "SELECT nd2.identifier, "
				+ "       nd2.estatus as status, "
				+ "       nd2.title "
				+ "FROM resource_relations rr1, "
				+ "     ndresource nd1, "
				+ "     resource_relations rr2, "
				+ "     ndresource nd2 "
				+ "WHERE rr1.res_type='assets' "
				+ "  AND rr1.resource_target_type = 'instructionalobjectives' "
				+ "  AND rr1.enable = 1 "
				+ "  AND rr1.source_uuid IN (:otIds) "
				+ "  AND rr1.target = nd1.identifier "
				+ "  AND nd1.primary_category = 'instructionalobjectives' "
				+ "  AND nd1.enable = 1 "
				+ "  AND rr2.res_type='knowledges' "
				+ "  AND rr2.resource_target_type='instructionalobjectives' "
				+ "  AND rr2.enable = 1 "
				+ "  AND nd1.identifier = rr2.target "
				+ "  AND rr2.source_uuid = nd2.identifier "
				+ "  AND nd2.primary_category = 'knowledges' "
				+ "  AND nd2.enable = 1 "
				+ "GROUP BY nd2.title";
		NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("otIds", otIds);
		List<Map<String,String>> returnList = npjt.query(sql, params, new RowMapper<Map<String,String>>(){
			@Override
			public Map<String,String> mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Map<String,String> map = new HashMap<String, String>();
				map.put("identifier", rs.getString("identifier"));
				map.put("title", rs.getString("title"));
				map.put("status", rs.getString("status"));
				return map;
			}
    	});
		return returnList;
	}
	
	public List<Map<String,String>> queryListByOtIdsAndKnIds(List<String> otIds,List<String> knIds){
		String sql = ""
				+ "SELECT rr1.source_uuid, "
				+ "       rr1.target, "
				+ "       nd1.title, "
				+ "       nd1.description, "
				+ "       nd1.custom_properties, "	
				+ "       nd1.keywords, "	
				+ "       nd1.provider_source, "	
				+ "       nd1.creator, "	
				+ "       nd1.estatus, "	
				+ "       nd2.identifier "
				+ "FROM resource_relations rr1, "
				+ "     ndresource nd1, "
				+ "     resource_relations rr2, "
				+ "     ndresource nd2 "
				+ "WHERE rr1.res_type='assets' "
				+ "  AND rr1.resource_target_type = 'instructionalobjectives' "
				+ "  AND rr1.enable = 1 "
				+ "  AND rr1.source_uuid IN (:otIds) "
				+ "  AND rr1.target = nd1.identifier "
				+ "  AND nd1.primary_category = 'instructionalobjectives' "
				+ "  AND nd1.enable = 1 "
				+ "  AND rr2.res_type='knowledges' "
				+ "  AND rr2.resource_target_type='instructionalobjectives' "
				+ "  AND rr2.enable = 1 "
				+ "  AND nd1.identifier = rr2.target "
				+ "  AND rr2.source_uuid = nd2.identifier "
				+ "  AND nd2.primary_category = 'knowledges' "
				+ "  AND nd2.enable = 1 "
				+ "  AND nd2.identifier IN (:knIds) "
				+ "ORDER BY rr1.sort_num";
		NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("otIds", otIds);
		params.put("knIds", knIds);
		List<Map<String,String>> returnList = npjt.query(sql, params, new RowMapper<Map<String,String>>(){
			@Override
			public Map<String,String> mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Map<String,String> map = new HashMap<String, String>();
				map.put("sourceUuid", rs.getString("source_uuid"));
				map.put("identifier", rs.getString("target"));
				map.put("title", rs.getString("title"));
				map.put("description", rs.getString("description"));
				map.put("keywords", rs.getString("keywords"));
				map.put("providerSource", rs.getString("provider_source"));
				map.put("creator", rs.getString("creator"));
				map.put("customProperties", rs.getString("custom_properties"));
				map.put("knId", rs.getString("identifier"));
				map.put("status", rs.getString("estatus"));
				return map;
			}
    	});
		return returnList;
	}
	
	@Override
	public List<ResultsModel> getInstructionalObjectiveById(List<String> suitIdList) {

		final List<ResultsModel> resourceList = new ArrayList<ResultsModel>();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("suit0", suitIdList.get(0));
		String sql = "SELECT nd2.description as suite_title,rr.source_uuid as source_id,nd.identifier as id,nd.description as title,rr.relation_type as suiteType FROM resource_relations rr,ndresource nd,resource_categories rc,ndresource nd2 where rr.res_type='assets' and rr.resource_target_type = 'assets' and rr.enable = 1 and nd.primary_category='assets' and rc.primary_category='assets' and nd.enable = 1 and nd2.primary_category='assets' and nd2.enable = 1 and nd2.identifier = rr.source_uuid and rr.source_uuid in (:suit0";
		for (int i=1;i<suitIdList.size();i++) {
			sql +=","+":suit" + i;
			params.put("suit" + i, suitIdList.get(i));
		}
		sql += ") and rr.target = nd.identifier and nd.identifier = rc.resource and rc.taxOnCode='$RA0503'";
		LOG.info("查询的SQL语句：" + sql.toString());
		LOG.info("查询的SQL参数:" + ObjectUtils.toJson(params));

		NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
				jt);
		namedJdbcTemplate.query(sql, params, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				ResultsModel sm = new ResultsModel();
				sm.setSourceId(rs.getString("source_id"));
				sm.setId(rs.getString("id"));
				sm.setTitle(rs.getString("title"));
				sm.setSuiteTitle(rs.getString("suite_title"));
				sm.setRelationType(rs.getString("suiteType"));
				resourceList.add(sm);
				return null;
			}
		});
		return resourceList;
	}
}
