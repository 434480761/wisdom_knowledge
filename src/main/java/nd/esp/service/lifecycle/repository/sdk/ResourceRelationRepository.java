package nd.esp.service.lifecycle.repository.sdk;


import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.model.ResourceRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author Rainy(yang.lin)
 * @version V1.0
 * @Description
 * @date 2015年5月25日 下午1:35:42
 */

public interface ResourceRelationRepository extends ResourceRepository<ResourceRelation>,
        JpaRepository<ResourceRelation, String> {

    @Query("SELECT DISTINCT(p.target) FROM ResourceRelation p where p.sourceUuid in (?1) AND p.resourceTargetType=?2 AND relationType = ?3 AND enable = true")
    List<String> findTargetIdsBySourceIdsAndTargetType(List<String> sourceIds, String targetType, String relationType);


    @Query("SELECT DISTINCT(p.sourceUuid) FROM ResourceRelation p where p.target in (?1) AND p.resType=?2 AND relationType = ?3 AND enable = true")
    List<String> findSourceIdsByTargetIdsAndResType(List<String> targetIds, String resType, String relationType);
    
    @Query("SELECT p FROM ResourceRelation p where p.target in (?1) AND p.resType=?2 AND relationType = ?3 AND enable = true")
    List<ResourceRelation> findSourcesByTargetIdsAndResType(List<String> targetIds, String resType, String relationType);

    @Query("SELECT t1 FROM ResourceRelation t1 where t1.resType=?1 and t1.resourceTargetType=?2 and t1.enable=1 and EXISTS(select 1 from Education t2 where t1.sourceUuid=t2.identifier and t2.enable=1) and EXISTS(select 1 from Education t2 where t1.target=t2.identifier and t2.enable=1) and t1.target=?3 ")
    List<ResourceRelation> findByResTypeAndTargetTypeAndTargetId(String resType, String targetType, String targetId);

    @Query("SELECT t1 FROM ResourceRelation t1 where t1.resType=?1 and t1.resourceTargetType=?2 and t1.enable=1 and EXISTS(select 1 from Education t2 where t1.sourceUuid=t2.identifier and t2.enable=1) and EXISTS(select 1 from Education t2 where t1.target=t2.identifier and t2.enable=1) and t1.sourceUuid=?3 ")
    List<ResourceRelation> findByResTypeAndTargetTypeAndSourceId(String resType, String targetType, String sourceId);

    @Query("select r.target from  ResourceRelation r where r.sourceUuid = ?1 and r.resType = ?2 and r.resourceTargetType = ?3 and r.enable = 1 and r.relationType = ?4 order by r.sortNum")
    List<String> findBySourceIdAndResTypeAndTargetType(String sourceId, String resType, String targetType, String relationType);

    @Query("select r.target from  ResourceRelation r where r.sourceUuid in ?1 and r.resType = 'assets' and r.resourceTargetType = 'instructionalobjectives' and r.enable = true")
    List<String> findObjectivesIdsByObjTypeIds(List<String> objTypeIds);

    @Query("select  r.target from ResourceRelation r where r.sourceUuid in ?1 and r.resType = 'knowledges' and r.resourceTargetType = 'instructionalobjectives' and r.enable = true")
    List<String> findObjectivesIdsByKnIds(List<String> knIds);

    @Query("select  r from ResourceRelation r where r.target in ?1 and r.resourceTargetType = 'instructionalobjectives' and r.enable = true")
    List<ResourceRelation> findAllObjectiveRelationsByTarget(List<String> targets);

    @Query("select r.target from ResourceRelation r where r.target in ?1 and r.resType = 'knowledges' and r.sourceUuid not in ?2 and r.enable = true")
    List<String> findObjectivesIdsOnlyInKnIds(List<String> ObjIds, List<String> knIds);

    @Query("select r from ResourceRelation r where r.sourceUuid = ?1 and r.resType ='assets' and r.resourceTargetType = 'assets' and r.relationType='ASSOCIATE' and r.enable = true order by r.sortNum")
    List<ResourceRelation> findRelationsBySourceOrderBySortNum(String source);

    @Query("select r.sourceUuid from ResourceRelation r where r.target = ?1 and r.resType = 'assets' and r.resourceTargetType = 'assets' and r.enable = true and r.relationType = 'COPY'")
    String findCopySuiteIdById(String identifier);

    @Query("select r from ResourceRelation r where  r.enable = true and r.relationType in ('COPY','PARENT') and r.sourceUuid in ?1")
    List<ResourceRelation> findAllRelationBySources(List<String> sources);

    @Query("select r from ResourceRelation r where  r.enable = true and r.relationType in ('COPY','PARENT') and r.target in ?1 ")
    List<ResourceRelation> findAllRelationByTargets(List<String> targets);

    @Query("select r from ResourceRelation r where r.sourceUuid = ?1 and r.resType ='assets' and r.resourceTargetType = 'assets' and r.relationType = 'COPY' and r.enable = true order by r.sortNum")
    List<ResourceRelation> findCopyRelationsBySourceOrderBySortNum(String source);

    @Query("select r from ResourceRelation r where r.sourceUuid = ?1 and r.resType ='assets' and r.resourceTargetType = 'assets' and r.relationType = 'COPY' and r.enable = true order by r.createTime")
    List<ResourceRelation> findCopyRelationsBySourceOrderByCreateTime(String source);
}