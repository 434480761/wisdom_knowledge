package nd.esp.service.lifecycle.repository.sdk;


/**
 * 类描述:CategoryPatternRepository
 * 创建人:
 * 创建时间:2015-05-20 10:14:30
 * @version
 */
  
import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.model.ResourceCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResourceCategoryRepository extends ResourceRepository<ResourceCategory>,
JpaRepository<ResourceCategory, String> {

    @Query("select r from ResourceCategory r where r.resource = ?1 and r.categoryCode = ?2")
    List<ResourceCategory> findTaxoncodeByResourceAndCategoryCode(String resource,String categoryCode);
}