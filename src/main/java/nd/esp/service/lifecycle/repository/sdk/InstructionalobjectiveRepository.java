package nd.esp.service.lifecycle.repository.sdk;


/**
 * 类描述:InstructionalobjectiveRepository
 * 创建人:
 * 创建时间:2015-05-20 10:14:31
 * @version
 */
  
import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.model.InstructionalObjective;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InstructionalobjectiveRepository extends ResourceRepository<InstructionalObjective>,
JpaRepository<InstructionalObjective, String> {

    @Query("select i from  InstructionalObjective i where i.identifier in ?1 and i.enable = 1 order by i.dbcreateTime")
    public List<InstructionalObjective> getAllByIds(List<String> ids);
}