package nd.esp.service.lifecycle.repository.sdk;


/**
 * 类描述:CategoryPatternRepository
 * 创建人:
 * 创建时间:2015-05-20 10:14:30
 * @version
 */
  
import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.model.Contribute;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface Contribute4QuestionDBRepository extends ResourceRepository<Contribute>,
JpaRepository<Contribute, String> {

    @Query("select c from Contribute c where c.resource in ?1 and c.resType = ?2")
    public List<Contribute> getAllbySource(List<String> resources,String resType);
}