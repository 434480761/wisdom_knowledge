package nd.esp.service.lifecycle.repository.sdk;


import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.model.CommonReason;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonReasonRepository extends ResourceRepository<CommonReason>,JpaRepository<CommonReason, String>{
	
}
