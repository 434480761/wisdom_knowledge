package nd.esp.service.lifecycle.repository.sdk;

import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.model.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by Administrator on 2016/11/21 0021.
 */
public interface SampleRepository extends ResourceRepository<Sample>,
        JpaRepository<Sample, String> {
    @Query("select s from Sample s where s.assetId = ?1 and s.knowledgeId1 = ?2 and s.knowledgeId2 is null and s.knowledgeId3 is null and s.knowledgeId4 is null and s.knowledgeId5 is null and s.enable = true")
    public List<Sample> querySampleBySuiteIdAndKnId1(String suiteId,String knId1);

    @Query("select s from Sample s where s.assetId = ?1 and s.knowledgeId1 = ?2 and s.knowledgeId2 = ?3 and s.knowledgeId3 is null and s.knowledgeId4 is null and s.knowledgeId5 is null and s.enable = true")
    public List<Sample> querySampleBySuiteIdAndKnId2(String suiteId,String knId1,String knId2);

    @Query("select s from Sample s where s.assetId = ?1 and s.knowledgeId1 = ?2 and s.knowledgeId2 = ?3 and s.knowledgeId3 = ?4 and s.knowledgeId4 is null and s.knowledgeId5 is null and s.enable = true")
    public List<Sample> querySampleBySuiteIdAndKnId3(String suiteId,String knId1,String knId2,String knId3);

    @Query("select s from Sample s where s.assetId = ?1 and s.knowledgeId1 = ?2 and s.knowledgeId2 = ?3 and s.knowledgeId3 = ?4 and s.knowledgeId4 = ?5 and s.knowledgeId5 is null and s.enable = true")
    public List<Sample> querySampleBySuiteIdAndKnId4(String suiteId,String knId1,String knId2,String knId3,String knId4);

    @Query("select s from Sample s where s.assetId = ?1 and s.knowledgeId1 = ?2 and s.knowledgeId2 = ?3 and s.knowledgeId3 = ?4 and s.knowledgeId4 = ?5 and s.knowledgeId5 = ?6 and s.enable = true")
    public List<Sample> querySampleBySuiteIdAndKnId5(String suiteId,String knId1,String knId2,String knId3,String knId4,String knId5);

    @Query("select s from Sample s where s.assetId = ?1 and s.enable = true order by s.createTime")
    public List<Sample> querySampleBySuiteId(String suiteId);
}
