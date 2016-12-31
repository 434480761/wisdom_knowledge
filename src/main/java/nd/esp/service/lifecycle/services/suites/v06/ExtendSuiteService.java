package nd.esp.service.lifecycle.services.suites.v06;

import nd.esp.service.lifecycle.vos.business.extendsuite.ExtendSuiteViewModel;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/12/8 0008.
 */
public interface ExtendSuiteService {
    /**
     * @param sampleId
     * @param extendSuiteViewModels
     * @return
     */
    public Map<String, String> extendSuites(String sampleId, List<ExtendSuiteViewModel> extendSuiteViewModels);

    public List<ExtendSuiteViewModel> queryExtendSuites(String sampleId);

    public Map<String, String> repairOldData(String sampleId, List<ExtendSuiteViewModel> extendSuiteViewModels);

    public Map<String,List<String>> deleteCopySuite(String suiteId);
}
