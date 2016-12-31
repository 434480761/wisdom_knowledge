package nd.esp.service.lifecycle.controllers.suites;

import nd.esp.service.lifecycle.services.suites.v06.ExtendSuiteService;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.vos.business.extendsuite.ExtendSuiteViewModel;
import org.apache.commons.collections.map.LinkedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Created by Administrator on 2016/12/8 0008.
 */
@RestController
@RequestMapping(value = "/v0.6/suites")
public class SuiteController {

    @Autowired
    private ExtendSuiteService suiteService;

    /**
     * 在某个样例下扩展接口
     *
     * @return vid : rid
     * @author gzp
     */
    @RequestMapping(value = "/{sample_id}/extends", method = RequestMethod.POST)
    public Map<String, String> extendSuites(@PathVariable(value = "sample_id") String sampleId,
                                            @RequestBody List<ExtendSuiteViewModel> extendSuiteViewModels) {
        Map<String, String> map = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(extendSuiteViewModels) && StringUtils.isNotEmpty(sampleId)) {
            map = suiteService.extendSuites(sampleId, extendSuiteViewModels);
        }
        return map;
    }

    @RequestMapping(value = "/{sample_id}/extends", method = RequestMethod.GET)
    public List<ExtendSuiteViewModel> queryExtendSuites(@PathVariable(value = "sample_id") String sampleId) {
        List<ExtendSuiteViewModel> list = new LinkedList<>();
        list = suiteService.queryExtendSuites(sampleId);
        return list;
    }

    @RequestMapping(value = "/{sample_id}/repair", method = RequestMethod.POST)
    public Map<String,String> repairOldData(@PathVariable(value = "sample_id") String sampleId,
                                            @RequestBody List<ExtendSuiteViewModel> extendSuiteViewModels){
        Map<String,String> map = new HashMap<>();
        map = suiteService.repairOldData(sampleId,extendSuiteViewModels);
        return map;
    }

    @RequestMapping(value = "/{suite_id}/extends",method = RequestMethod.DELETE)
    public Map<String,List<String>> deleteCopySuite(@PathVariable(value = "suite_id") String suiteId){
        return suiteService.deleteCopySuite(suiteId);
    }
}
