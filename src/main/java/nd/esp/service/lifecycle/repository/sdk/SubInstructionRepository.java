package nd.esp.service.lifecycle.repository.sdk;


/**
 * 类描述:InstructionalobjectiveRepository
 * 创建人:
 * 创建时间:2015-05-20 10:14:31
 * @version
 */

import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.model.InstructionalObjective;
import nd.esp.service.lifecycle.repository.model.SubInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubInstructionRepository extends ResourceRepository<SubInstruction>,
JpaRepository<SubInstruction, String> {

}