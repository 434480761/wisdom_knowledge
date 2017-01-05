package nd.esp.service.lifecycle.controllers.instructionalobjectives.v06;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kristofa.brave.http.HttpResponse;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import nd.esp.service.lifecycle.educommon.models.ResourceModel;
import nd.esp.service.lifecycle.educommon.services.NDResourceService;
import nd.esp.service.lifecycle.educommon.services.impl.CommonServiceHelper;
import nd.esp.service.lifecycle.educommon.support.ParameterVerificationHelper;
import nd.esp.service.lifecycle.educommon.vos.ResourceViewModel;
import nd.esp.service.lifecycle.educommon.vos.constant.IncludesConstant;
import nd.esp.service.lifecycle.entity.elasticsearch.Resource;
import nd.esp.service.lifecycle.models.v06.InstructionalObjectiveModel;
import nd.esp.service.lifecycle.models.v06.SuitAndInstructionalObjectiveModel;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.model.Sample;
import nd.esp.service.lifecycle.repository.sdk.ResourceRelationRepository;
import nd.esp.service.lifecycle.repository.sdk.SampleRepository;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceV06;
import nd.esp.service.lifecycle.services.elasticsearch.AsynEsResourceService;
import nd.esp.service.lifecycle.services.instructionalobjectives.v06.InstructionalObjectiveService;
import nd.esp.service.lifecycle.services.notify.NotifyInstructionalobjectivesService;
import nd.esp.service.lifecycle.services.offlinemetadata.OfflineService;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.annotation.MarkAspect4Format2Category;
import nd.esp.service.lifecycle.support.annotation.MarkAspect4OfflineToES;
import nd.esp.service.lifecycle.support.busi.CommonHelper;
import nd.esp.service.lifecycle.support.busi.ValidResultHelper;
import nd.esp.service.lifecycle.support.busi.elasticsearch.ResourceTypeSupport;
import nd.esp.service.lifecycle.support.enums.LifecycleStatus;
import nd.esp.service.lifecycle.support.enums.OperationType;
import nd.esp.service.lifecycle.support.enums.ResourceNdCode;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.EduRedisTemplate;
import nd.esp.service.lifecycle.utils.ParamCheckUtil;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.utils.gson.ObjectUtils;
import nd.esp.service.lifecycle.vos.ListViewModel;
import nd.esp.service.lifecycle.vos.ListViewModel4Suite;
import nd.esp.service.lifecycle.vos.business.MultiItemViewModel;
import nd.esp.service.lifecycle.vos.business.MultiLevelSuiteData;
import nd.esp.service.lifecycle.vos.business.MultiLevelSuiteViewModel;
import nd.esp.service.lifecycle.vos.business.SuiteViewModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.*;
import nd.esp.service.lifecycle.vos.statics.ResourceType;
import nd.esp.service.lifecycle.vos.valid.ValidInstructionalObjectiveDefault4UpdateGroup;
import nd.esp.service.lifecycle.vos.valid.ValidInstructionalObjectiveDefaultGroup;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 教学目标V0.6API
 *
 * @author linsm
 */
@RestController
@RequestMapping("/v0.6/instructionalobjectives")
public class InstructionalObjectiveControllerV06 {

    private static final Logger LOG = LoggerFactory.getLogger(InstructionalObjectiveControllerV06.class);

    @Autowired
    @Qualifier("instructionalObjectiveServiceV06")
    private InstructionalObjectiveService instructionalObjectiveService;

    @Autowired
    private NotifyInstructionalobjectivesService notifyService;

    @Autowired
    private OfflineService offlineService;
    @Autowired
    private AsynEsResourceService esResourceOperation;

    // private static final Logger LOG = LoggerFactory.getLogger(InstructionalObjectiveControllerV06.class);

    @Autowired
    CommonServiceHelper commonServiceHelper;

    @Autowired
    private NDResourceService ndResourceService;

    @Autowired
    private EducationRelationServiceV06 educationRelationService;

    @Autowired
    private ResourceRelationRepository resourceRelationRepository;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private SampleRepository sampleRepository;

    @Qualifier(value = "defaultJdbcTemplate")

    @Autowired
    private JdbcTemplate defaultJdbcTemplate;

    @Autowired
    private EducationRelationServiceV06 educationRelationServer;

    @Autowired
    private EduRedisTemplate<MultiLevelSuiteData> resultRedis;

    /**
     * 创建教学目标对象
     *
     * @param rm 教学目标对象
     * @return
     */
    @MarkAspect4Format2Category
    @MarkAspect4OfflineToES
    @RequestMapping(method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public InstructionalObjectiveViewModel create(@Validated(ValidInstructionalObjectiveDefaultGroup.class) @RequestBody InstructionalObjectiveViewModel avm,
                                                  BindingResult validResult) {
        // 入参合法性校验
        ValidResultHelper.valid(validResult,
                "LC/CREATE_INSTRUCTIONALOBJECTIVE_PARAM_VALID_FAIL",
                "InstructionalObjectiveControllerV06",
                "create");
        avm.setIdentifier(UUID.randomUUID().toString());// 后续要使用，不得不在controller层生成uuid,categories
        CommonHelper.inputParamValid(avm, "10110", OperationType.CREATE);
        return operate(avm, OperationType.CREATE);

    }

    /**
     * 修改教学目标对象
     *
     * @param rm 教学目标对象
     * @return
     */
    @MarkAspect4Format2Category
    @MarkAspect4OfflineToES
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public InstructionalObjectiveViewModel update(@Validated(ValidInstructionalObjectiveDefault4UpdateGroup.class) @RequestBody InstructionalObjectiveViewModel avm,
                                                  BindingResult validResult,
                                                  @PathVariable String id) {

        // 入参合法性校验
        ValidResultHelper.valid(validResult,
                "LC/UPDATE_INSTRUCTIONALOBJECTIVE_PARAM_VALID_FAIL",
                "InstructionalObjectiveControllerV06",
                "update");
        avm.setIdentifier(id);
        CommonHelper.inputParamValid(avm, "10111", OperationType.UPDATE);
        return operate(avm, OperationType.UPDATE);
    }

    /**
     * 修改教学目标对象
     *
     * @param rm 教学目标对象
     * @return
     */
    @MarkAspect4Format2Category
    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public InstructionalObjectiveViewModel patch(@Validated(ValidInstructionalObjectiveDefault4UpdateGroup.class) @RequestBody InstructionalObjectiveViewModel avm,
                                                 BindingResult validResult, @PathVariable String id,
                                                 @RequestParam(value = "notice_file", required = false, defaultValue = "true") boolean notice) {

        // 入参合法性校验
//        ValidResultHelper.valid(validResult,
//                "LC/UPDATE_INSTRUCTIONALOBJECTIVE_PARAM_VALID_FAIL",
//                "InstructionalObjectiveControllerV06",
//                "update");
        avm.setIdentifier(id);
//        CommonHelper.inputParamValid(avm, "10111", OperationType.UPDATE);
        InstructionalObjectiveModel am = CommonHelper.convertViewModelIn(avm,
                InstructionalObjectiveModel.class,
                ResourceNdCode.instructionalobjectives, true);

        //add by xiezy - 2016.04.15
        //更新操作要先保存其原有状态
        String oldStatus = notifyService.getResourceStatus(avm.getIdentifier());

        am = instructionalObjectiveService.patchInstructionalObjective(am);

        //add by xiezy - 2016.04.15
        //异步通知智能出题
        notifyService.asynNotify4Resource(avm.getIdentifier(), avm.getLifeCycle().getStatus(), oldStatus, null, OperationType.UPDATE);

        avm = CommonHelper.convertViewModelOut(am, InstructionalObjectiveViewModel.class);
        avm.setTechInfo(null); // 没有这个属性

        if (notice) {
            offlineService.writeToCsAsync(ResourceNdCode.instructionalobjectives.toString(), id);
        }
        // offline metadata(coverage) to elasticsearch
        if (ResourceTypeSupport.isValidEsResourceType(ResourceNdCode.instructionalobjectives.toString())) {
            esResourceOperation.asynAdd(
                    new Resource(ResourceNdCode.instructionalobjectives.toString(), id));
        }
        return avm;
    }

    /**
     * @param avm
     * @param operationType
     */
    private InstructionalObjectiveViewModel operate(InstructionalObjectiveViewModel avm, OperationType operationType) {
        InstructionalObjectiveModel am = CommonHelper.convertViewModelIn(avm,
                InstructionalObjectiveModel.class,
                ResourceNdCode.instructionalobjectives);
        //add by xiezy - 2016.04.15
        String oldStatus = "";
        if (operationType == OperationType.UPDATE) {//如果是更新操作要先保存其原有状态
            oldStatus = notifyService.getResourceStatus(avm.getIdentifier());
        }

        if (operationType == OperationType.CREATE) {
            // 创建教学目标
            am = instructionalObjectiveService.createInstructionalObjective(am);
        } else {
            // 更新教学目标
            am = instructionalObjectiveService.updateInstructionalObjective(am);
        }

        //add by xiezy - 2016.04.15
        //异步通知智能出题
        notifyService.asynNotify4Resource(avm.getIdentifier(), avm.getLifeCycle().getStatus(), oldStatus, null, operationType);

        avm = CommonHelper.convertViewModelOut(am, InstructionalObjectiveViewModel.class);
        avm.setTechInfo(null); // 没有这个属性
        return avm;
    }


    /**
     * 根据教学目标id查询出与之相关联的教材章节。
     * 分两种情况：
     * 1.教学目标与章节直接关联
     * 2.教学目标与课时关联，课时与章节关联
     *
     * @param objectiveId
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/business/{objective_id}/chapters/paths", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Map<String, Object>> getPaths(@PathVariable("objective_id") String objectiveId) {

        List<Map<String, Object>> result = instructionalObjectiveService.getChapterRelationById(objectiveId);
        return result;
    }

    /***
     * 查询没有被关联到章节或课时的教学目标
     * @param limit 分页参数
     * @param unrelationCategory 没有被关联的category，chapters或lessons，不填默认为同时没有被关联到章节和课时
     * @param knowledgeTypeCode 知识点类型维度code
     * @param instructionalObjectiveTypeId 教学目标类型Id
     */
    @RequestMapping(value = "/unrelation2chapters", method = RequestMethod.GET)
    public Object getUnrelation2Chapters(
            @RequestParam(value = "limit", defaultValue = "(0,15)") String limit,
            @RequestParam(value = "unrelationCategory", defaultValue = "") String unrelationCategory,
            @RequestParam(value = "knowledgeTypeCode", defaultValue = "") String knowledgeTypeCode,
            @RequestParam(value = "instructionalObjectiveTypeId", defaultValue = "") String instructionalObjectiveTypeId) {

        if (!"".equals(unrelationCategory) && !IndexSourceType.ChapterType.getName().equals(unrelationCategory) && !IndexSourceType.LessonType.getName().equals(unrelationCategory)) {
            throw new LifeCircleException(HttpStatus.BAD_REQUEST,
                    LifeCircleErrorMessageMapper.InvalidArgumentsError);
        }

        limit = CommonHelper.checkLimitMaxSize(limit);

        ListViewModel<InstructionalObjectiveModel> listViewModel = instructionalObjectiveService.getUnRelationInstructionalObjective(knowledgeTypeCode, instructionalObjectiveTypeId, unrelationCategory, limit);

        Collection<Map.Entry<String, String>> idWithTitles = Collections2.transform(listViewModel.getItems(), new Function<InstructionalObjectiveModel, Map.Entry<String, String>>() {
            @Nullable
            @Override
            public Map.Entry<String, String> apply(InstructionalObjectiveModel model) {
                return new HashMap.SimpleEntry<>(model.getIdentifier(), model.getTitle());
            }
        });

        Map<String, String> titles = instructionalObjectiveService.getInstructionalObjectiveTitle(idWithTitles);

        for (InstructionalObjectiveModel model : listViewModel.getItems()) {
            String title = titles.get(model.getIdentifier());
            model.setTitle(null == title ? model.getTitle() : title);
        }

        return listViewModel;
    }

    /**
     * 根据套件id查找
     *
     * @param suiteId
     * @return
     */
    @RequestMapping(value = "/business/list/suite/{suite_id}", method = RequestMethod.GET)
    public ListViewModel4Suite queryListBySuiteId(@PathVariable(value = "suite_id") String suiteId, @RequestParam(value = "limit") String limit) {
        if (StringUtils.isEmpty(suiteId)) {
            return null;
        }
        //1、判断套件是否有效
        ResourceModel rm = ndResourceService.getDetail(IndexSourceType.AssetType.getName(), suiteId, new ArrayList<String>());
        if (rm == null) {
            return null;
        }

        //2、根据套件获取教学目标
        return instructionalObjectiveService.queryListBySuiteId(suiteId, limit);
    }

    /**
     * 业务创建教学目标接口
     *
     * @param svm
     * @return
     */
    @RequestMapping(value = "/business", method = RequestMethod.PATCH)
    public Object create4Business(@RequestBody SuiteViewModel svm, HttpServletResponse response) {
        Object returnValue = instructionalObjectiveService.create4Business(svm);
        if (returnValue != null && returnValue instanceof Map) {
            response.setStatus(500);
        }
        return returnValue;
    }

    /**
     * 根据教材章节id查询教学目标，并且按照课时，课时下的教学目标，章节下的教学目标三个顺序依次排序。
     *
     * @return
     */
    @RequestMapping(value = "/order_list", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE}, params = {"limit"})
    public ListViewModel<ResourceViewModel> getInstructionalObjectivesList(
            @RequestParam(required = false, value = "word") String words,
            @RequestParam String limit,
            @RequestParam(required = false, value = "coverage") Set<String> coverages,
            @RequestParam(required = false, value = "relation") Set<String> relations,
            @RequestParam(required = false, value = "include") String includes,
            @RequestParam(required = false, value = "reverse") String reverse) {
        ListViewModel<ResourceViewModel> resourceViewModelListViewModel = null;
        resourceViewModelListViewModel = requestQuering(includes, relations, coverages, words, limit, reverse);

        return resourceViewModelListViewModel;
    }

    @SuppressWarnings("unchecked")
    private ListViewModel<ResourceViewModel> requestQuering(String includes, Set<String> relations, Set<String> coverages, String words, String limit, String reverse) {

        //参数校验和处理
        Map<String, Object> paramMap =
                requestParamVerifyAndHandle(includes, relations, coverages, limit, reverse);

        // include
        List<String> includesList = (List<String>) paramMap.get("include");

        // relations,格式:stype/suuid/r_type
        List<Map<String, String>> relationsMap = (List<Map<String, String>>) paramMap.get("relation");

        // coverages,格式:Org/uuid/SHAREING
        List<String> coveragesList = (List<String>) paramMap.get("coverage");

        //limit
        limit = (String) paramMap.get("limit");

        //reverse,默认为false
        boolean reverseBoolean = (boolean) paramMap.get("reverse");

        String chapterId = "";

        //调用service,获取到业务模型的list
        ListViewModel<ResourceModel> rListViewModel = new ListViewModel<ResourceModel>();

        rListViewModel = instructionalObjectiveService.getResourcePageByChapterId(includesList, relationsMap, coveragesList, limit, reverseBoolean);

        //ListViewModel<ResourceModel> 转换为  ListViewModel<ResourceViewModel>
        ListViewModel<ResourceViewModel> result = new ListViewModel<ResourceViewModel>();
        result.setTotal(rListViewModel.getTotal());
        result.setLimit(rListViewModel.getLimit());
        //items处理
        List<ResourceViewModel> items = new ArrayList<ResourceViewModel>();
        for (ResourceModel resourceModel : rListViewModel.getItems()) {
            ResourceViewModel resourceViewModel = changeToView(resourceModel, "instructionalobjectives", includesList);
            items.add(resourceViewModel);
        }
        result.setItems(items);

        return result;
    }

    private Map<String, Object> requestParamVerifyAndHandle(String includes, Set<String> relations, Set<String> coverages, String limit, String reverse) {

        List<String> includesList = IncludesConstant.getValidIncludes(includes);

        relations = CollectionUtils.removeEmptyDeep(relations);

        // 3.relations,格式:stype/suuid/r_type
        List<Map<String, String>> relationsMap = new ArrayList<Map<String, String>>();
        if (CollectionUtils.isEmpty(relations)) {
            relationsMap = null;
        } else {
            for (String relation : relations) {
                Map<String, String> map = new HashMap<String, String>();
                //对于入参的coverage每个在最后追加一个空格，以保证elemnt的size为3
                relation = relation + " ";
                List<String> elements = Arrays.asList(relation.split("/"));
                //格式错误判断
                if (elements.size() != 3) {
                    LOG.error(relation + "--relation格式错误");
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                            LifeCircleErrorMessageMapper.CommonSearchParamError.getCode(),
                            relation + "--relation格式错误");
                }
                //判断源资源是否存在,stype + suuid
                if (!elements.get(1).trim().endsWith("$")) {//不为递归查询时才校验
                    CommonHelper.resourceExist(elements.get(0).trim(), elements.get(1).trim(), ResourceType.RESOURCE_SOURCE);
                }
                //r_type的特殊处理
                if (StringUtils.isEmpty(elements.get(2).trim())) {
                    elements.set(2, null);
                }
                map.put("stype", elements.get(0).trim());
                map.put("suuid", elements.get(1).trim());
                map.put("rtype", elements.get(2));
                relationsMap.add(map);
            }
        }

        // 4.coverages,格式:Org/uuid/SHAREING
        coverages = CollectionUtils.removeEmptyDeep(coverages);
        List<String> coveragesList = new ArrayList<String>();
        if (CollectionUtils.isEmpty(coverages)) {
            coveragesList = null;
        } else {
            for (String coverage : coverages) {
                String c = ParameterVerificationHelper.coverageVerification(coverage);
                coveragesList.add(c);
            }
        }

        //7. limit
        limit = CommonHelper.checkLimitMaxSize(limit);

        //reverse,默认为false
        boolean reverseBoolean = false;
        if (StringUtils.isNotEmpty(reverse) && reverse.equals("true")) {
            reverseBoolean = true;
        }

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("include", includesList);
        paramMap.put("relation", relationsMap);
        paramMap.put("coverage", coveragesList);
        paramMap.put("reverse", reverseBoolean);
        paramMap.put("limit", limit);

        return paramMap;
    }

    private ResourceViewModel changeToView(ResourceModel model, String resourceType, List<String> includes) {
        return CommonHelper.changeToView(model, resourceType, includes, commonServiceHelper);
    }

    /**
     * 根据知识点名称与套件ID，查询学习目标
     *
     * @return
     */
    @RequestMapping(value = "/list/business", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Map<String, Object> queryListByKnTitle(@RequestParam(value = "title") String title, @RequestParam(required = false, value = "suite_id") String suiteId) {
        return instructionalObjectiveService.queryListByKnTitle(title, suiteId);
    }

    /**
     * 根据套件id查询教学目标，并按知识点来进行分页
     *
     * @param suiteId
     * @param limit
     * @return
     */
    @RequestMapping(value = "/list/suite/{suite_id}", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public Map<String, Object> queryList4KnowledgeBySuiteId(
			@PathVariable(value = "suite_id") String suiteId,
			@RequestParam(value = "limit") String limit,
			@RequestParam(value = "root_suite", required = false) String rootSuit) {

		Map<String, Object> returnTempMap = instructionalObjectiveService
				.queryList4SampleBySuiteId(suiteId, limit, false, 0,
						"ASSOCIATE");

		List<String> suiteIdList = new ArrayList<>();
		suiteIdList.add(suiteId);
		// 检验入参，但入参符合条件，则在源items上做更改
		if (StringUtils.isNotEmpty(rootSuit)
				&& CommonHelper.checkUuidPattern(rootSuit)) {
			dealReturnMap(returnTempMap, suiteIdList);
		}
		return returnTempMap;
	}

    private void dealKnids(Sample sample, List<String> targetList) {
        if (StringUtils.isNotEmpty(sample.getKnowledgeId1())) {
            targetList.add(sample.getKnowledgeId1());
        }
        if (StringUtils.isNotEmpty(sample.getKnowledgeId2())) {
            targetList.add(sample.getKnowledgeId2());
        }
        if (StringUtils.isNotEmpty(sample.getKnowledgeId3())) {
            targetList.add(sample.getKnowledgeId3());
        }
        if (StringUtils.isNotEmpty(sample.getKnowledgeId4())) {
            targetList.add(sample.getKnowledgeId4());
        }
        if (StringUtils.isNotEmpty(sample.getKnowledgeId5())) {
            targetList.add(sample.getKnowledgeId5());
        }
    }

    /**
     * 修改资源的状态（暂不支持习题库资源）
     */
    @RequestMapping(value = "/status/change", method = RequestMethod.PUT, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public void changeStatusByKnId(@RequestBody List<Map<String, Object>> params, @RequestParam(required = false, defaultValue = "false", value = "refresh_suite") boolean refreshSuite) {
        IndexSourceType[] sourceTypes = IndexSourceType.values();
        LifecycleStatus[] ls = LifecycleStatus.values();
        //校验参数合法性
        if (CollectionUtils.isNotEmpty(params)) {
            for (Map<String, Object> map : params) {
                String resType = (String) map.get("res_type");
                boolean flag = false;
                for (IndexSourceType indexSourceType : sourceTypes) {
                    if (indexSourceType.getName().equals(resType)) {
                        flag = true;
                        break;
                    }
                }
                if (resType.equals("samples")) {
                    flag = true;
                }
                if (!flag) {
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "LC/CHECK_PARAM_VALID_FAIL", "资源类型值不对！res_type:" + resType);
                }
                if (CommonServiceHelper.isQuestionDb(resType)) {
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "LC/CHECK_PARAM_VALID_FAIL", "暂不支持资源类型！res_type:" + resType);
                }
                List<Map<String, String>> items = (List) map.get("items");
                for (Map<String, String> map2 : items) {
                    String status = map2.get("status");
                    boolean flag2 = false;
                    for (LifecycleStatus lifecycleStatus : ls) {
                        if (lifecycleStatus.getCode().equals(status)) {
                            flag2 = true;
                            break;
                        }
                    }
                    if (!flag2) {
                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "LC/CHECK_PARAM_VALID_FAIL", "状态不能为空！status:" + status);
                    }
                }
            }
            instructionalObjectiveService.changeStatus(params);

            //是否需要刷新套件缓存
            if (refreshSuite) {
                CommonHelper.getPrimaryExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        educationRelationService.queryFordevAndSaveForRedis();
                    }
                });
            }
        }
    }

    @RequestMapping(value = "/deleteSample/{id}", method = RequestMethod.DELETE)
    public Map<String, Object> deleteSample(@PathVariable(value = "id") String sampleId) {
        Map<String, Object> returnMap = instructionalObjectiveService.deleteSample(sampleId, request.getParameter("user_id"));
        return returnMap;
    }
    
    @RequestMapping(value = "/sample/{id}", method = RequestMethod.PUT,consumes = {MediaType.APPLICATION_JSON_VALUE},produces= {MediaType.APPLICATION_JSON_VALUE})
    public SampleViewModel updateSample(@PathVariable(value = "id") String sampleId,@RequestBody Map<String,Object> customPropertiesMap){
        return instructionalObjectiveService.updateSample(sampleId, customPropertiesMap,request.getParameter("user_id"));
    }

    @RequestMapping(value = "/transport", method = RequestMethod.GET)
    public void transportData() {
        instructionalObjectiveService.transportData();
    }

    @RequestMapping(value = "/list/subsuite/{suite_id}", method = RequestMethod.GET)
	public Map<String, Object> queryList4KnowledgeBySubSuiteId(
			@PathVariable(value = "suite_id") String subSuiteId,
			@RequestParam String limit,
			@RequestParam(value = "relation_type", defaultValue = "ASSOCIATE", required = false) String relationType,
			@RequestParam int level) {

		Map<String, Object> returnTempMap = instructionalObjectiveService
				.queryList4SampleBySuiteId(subSuiteId, limit, true, level,
						relationType);
		List<String> rootSuiteId = new ArrayList<>();
		educationRelationService.recursiveRootSuite(subSuiteId, rootSuiteId,
				null,relationType);
		if (CollectionUtils.isNotEmpty(rootSuiteId)) {
			dealReturnMap(returnTempMap, rootSuiteId);
		}
		return returnTempMap;
	}

    private void dealReturnMap(Map<String, Object> returnTempMap, List<String> suiteList) {
        String suiteId = suiteList.get(0);
        //获取knowledge的id
        List<Map<String, Object>> itemList = (List<Map<String, Object>>) returnTempMap.get("items");
        if (CollectionUtils.isEmpty(itemList)) {
            return;
        }
        List<Sample> queryResult = new ArrayList<>();
        for (Map<String, Object> map : itemList) {
            List<String> knIDTemp = new ArrayList<>();
            Sample knMap = (Sample) map.get("sample");
            if (knMap != null) {
                dealKnids(knMap, knIDTemp);
            }

            //查询
            List<Sample> list = new ArrayList<>();

            switch (knIDTemp.size()) {
                case 1: {
                    list = sampleRepository.querySampleBySuiteIdAndKnId1(suiteId,
                            knIDTemp.get(0));
                    queryResult.addAll(list);
                    break;
                }
                case 2: {
                    list = sampleRepository.querySampleBySuiteIdAndKnId2(suiteId,
                            knIDTemp.get(0), knIDTemp.get(1));
                    queryResult.addAll(list);
                    break;
                }
                case 3: {
                    list = sampleRepository.querySampleBySuiteIdAndKnId3(suiteId,
                            knIDTemp.get(0), knIDTemp.get(1), knIDTemp.get(2));
                    queryResult.addAll(list);
                    break;
                }
                case 4: {
                    list = sampleRepository.querySampleBySuiteIdAndKnId4(suiteId,
                            knIDTemp.get(0), knIDTemp.get(1), knIDTemp.get(2), knIDTemp.get(3));
                    queryResult.addAll(list);
                    break;
                }
                case 5: {
                    list = sampleRepository.querySampleBySuiteIdAndKnId5(suiteId,
                            knIDTemp.get(0), knIDTemp.get(1), knIDTemp.get(2), knIDTemp.get(3), knIDTemp.get(4));
                    queryResult.addAll(list);
                    break;
                }
                default:
                    break;
            }
        }
        String sampleId = "";
        String sampleStatus = "";
        String sampleTitle = "";
        String sampleCustomProperties = "";
        if (CollectionUtils.isNotEmpty(queryResult)) {
            for (Sample sample : queryResult) {
                List<String> targetID = new ArrayList<>();

                sampleId = sample.getIdentifier();
                sampleStatus = sample.getStatus();
                sampleTitle = sample.getTitle();
                sampleCustomProperties = sample.getCustomProperties();

                dealKnids(sample, targetID);

                //将结果放回指定items的sample处
                int itemListsize = itemList.size();
                for (int j = 0; j < itemListsize; j++) {
                    if (itemList.get(j).get("sample") instanceof Sample) {
                        Sample sample1 = (Sample) itemList.get(j).get("sample");
                        Map<String, Object> knMap = new HashMap<>();
                        List<String> knIds = new ArrayList<>();
                        dealKnids(sample1, knIds);
                        List<Map<String,String>> knowledgeList = new ArrayList<Map<String,String>>();
                        if (CollectionUtils.isNotEmpty(targetID) && CommonServiceHelper.listEqual(targetID, knIds)) {
                            knMap.put("sample_id", sampleId);
                            knMap.put("sample_status", sampleStatus);
                            knMap.put("sample_title", sampleTitle);
                            if(StringUtils.isNotEmpty(sampleCustomProperties)){
                            	knMap.put("custom_properties", ObjectUtils.fromJson(sampleCustomProperties, Map.class));
                            }else{
                            	knMap.put("custom_properties", new HashMap<String,String>());
                            }
                            
                            if (StringUtils.isNotEmpty(sample1.getKnowledgeId1())) {
                                knMap.put("knowledge1", sample1.getKnowledgeId1());
                                Map<String,String> knowledgeMap = new HashMap<String, String>();
                                knowledgeMap.put("identifier", sample1.getKnowledgeId1());
                                knowledgeMap.put("title", sample1.getKnowledgeTitle1());
                                knowledgeList.add(knowledgeMap);
                            }
                            if (StringUtils.isNotEmpty(sample1.getKnowledgeId2())) {
                                knMap.put("knowledge2", sample1.getKnowledgeId2());
                                Map<String,String> knowledgeMap = new HashMap<String, String>();
                                knowledgeMap.put("identifier", sample1.getKnowledgeId2());
                                knowledgeMap.put("title", sample1.getKnowledgeTitle2());
                                knowledgeList.add(knowledgeMap);
                            }
                            if (StringUtils.isNotEmpty(sample1.getKnowledgeId3())) {
                                knMap.put("knowledge3", sample1.getKnowledgeId3());
                                Map<String,String> knowledgeMap = new HashMap<String, String>();
                                knowledgeMap.put("identifier", sample1.getKnowledgeId3());
                                knowledgeMap.put("title", sample1.getKnowledgeTitle3());
                                knowledgeList.add(knowledgeMap);
                            }
                            if (StringUtils.isNotEmpty(sample1.getKnowledgeId4())) {
                                knMap.put("knowledge4", sample1.getKnowledgeId4());
                                Map<String,String> knowledgeMap = new HashMap<String, String>();
                                knowledgeMap.put("identifier", sample1.getKnowledgeId4());
                                knowledgeMap.put("title", sample1.getKnowledgeTitle4());
                                knowledgeList.add(knowledgeMap);
                            }
                            if (StringUtils.isNotEmpty(sample1.getKnowledgeId5())) {
                                knMap.put("knowledge5", sample1.getKnowledgeId5());
                                Map<String,String> knowledgeMap = new HashMap<String, String>();
                                knowledgeMap.put("identifier", sample1.getKnowledgeId5());
                                knowledgeMap.put("title", sample1.getKnowledgeTitle5());
                                knowledgeList.add(knowledgeMap);
                            }
                            itemList.get(j).put("sample", knMap);
                            itemList.get(j).put("knowledge_list", knowledgeList);
                        }
                    }
                }
            }
        } else {
            int i = 0;
            for (Map<String, Object> map : itemList) {
                if (itemList.get(i).get("sample") instanceof Sample) {
                    Sample sample = (Sample) itemList.get(i).get("sample");
                    Map<String, String> knMap = new HashMap<>();
                    knMap.put("sample_id", sample.getIdentifier());
                    knMap.put("sample_status", sample.getStatus());
                    knMap.put("sample_title", sample.getTitle());
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId1())) {
                        knMap.put("knowledge1", sample.getKnowledgeId1());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId2())) {
                        knMap.put("knowledge2", sample.getKnowledgeId2());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId3())) {
                        knMap.put("knowledge3", sample.getKnowledgeId3());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId4())) {
                        knMap.put("knowledge4", sample.getKnowledgeId4());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId5())) {
                        knMap.put("knowledge5", sample.getKnowledgeId5());
                    }
                    itemList.get(i).put("sample", knMap);
                    i++;
                }
            }
        }
        returnTempMap.put("items", itemList);
    }
    /**
     * 查询最子套件，及相关信息
     *
     * @author xm
     * @date 2016年11月24日 上午10:29:43
     * @method queryDownSubSuite
     */
    @RequestMapping(value = "/querySuite/downSubSuite", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE}, params = {"limit"})
    public ListViewModel<ClassifySuitAndInstructionalObjectiveModel> queryDownSubSuite(@RequestParam(required = false, value = "words") String words,
                                                                                       @RequestParam String limit, @RequestParam(required = false) String status,
                                                                                       @RequestParam(required = false) String subject) {
        //对status做判断
        if (StringUtils.hasText(status)) {
            if (!LifecycleStatus.isLegalStatus(status)) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.StatusIsNotExist.getCode(),
                        "root套件的" + status + "--status必须为有效值");
            }
        }
        return instructionalObjectiveService.classfiDownSuiteQuery(words, limit, status, subject);
    }


    /**
     * 根据套件id列表查询学习目标及套件的研究分析
     *
     * @param suiteIdList
     * @return
     */

    @RequestMapping(value = "/sort/suite", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<SuitAndInstructionalObjectiveModel> getSuitAndInstructionalObjective(
            @RequestParam(value = "suite_id", required = true) List<String> suiteIdList) {

        return instructionalObjectiveService.getSuitAndInstructionalObjective(suiteIdList);
    }


    /**
     * 统计根节点下的相关信息
     *
     * @author xm
     * @date 2016年12月1日 下午4:55:56
     */
    @RequestMapping(value = "/rootSuite/statistics", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public void rootSuiteStatistics(HttpServletResponse response) {
        List<SuiteStatisticsModel> resultSuiteStatisticsModelsList = instructionalObjectiveService.suiteStatistics();
        try {
            suiteStatisticsWriteToExcel(resultSuiteStatisticsModelsList,response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    /**
     * 将查询的套件统计信息写到excel表中
     *
     * @author xm
     * @date 2016年12月1日 下午12:28:02
     */
    public void suiteStatisticsWriteToExcel(List<SuiteStatisticsModel> SuiteStatisticsModelsList,HttpServletResponse response) throws IOException {

    	Workbook wb = new HSSFWorkbook();   	
    	response.setCharacterEncoding("UTF-8");
    	response.setContentType("application/vnd.ms-excel");
    	String filedisplay = "rootStatistic";
    	filedisplay = URLEncoder.encode(filedisplay, "UTF-8");
    	response.addHeader("Content-Disposition", "attachment;filename="+ filedisplay+".xls");

    	
        // 创建一个SHEET
        Sheet sheet1 = wb.createSheet("套件统计");
        String[] title = {"创建时间", "套件ID", "名称", "样例名称", "制作者", "学习目标类型数量", "目标数量", "目标描述数量", "状态"};
        // 创建一行
        int i = 0;
        Row row = sheet1.createRow((short) 0);
        // 填充标题
        for (String s : title) {
            Cell cell = row.createCell(i);
            cell.setCellValue(s);
            i++;
        }
        //填充数据
        //注意等下改下suiteExcelIndex和sampleExcelIndex的值
        int excelTotalIndex = 0;
        for (int j = 0; j < SuiteStatisticsModelsList.size(); j++) {
            int suiteExcelIndex = excelTotalIndex + 1;
            SuiteStatisticsModel temp = SuiteStatisticsModelsList.get(j);
            Row rowTemp = sheet1.createRow((short) suiteExcelIndex);
            //下面是填充他的大套件情况
            String rootSuiteCreatTime = temp.getRootSuiteCreatTime();
            String rootSuiteName = temp.getRootSuiteName();
            String rootSuiteDescription = temp.getRootSuiteDescription();
            String rootSuiteAuthorName = temp.getRootSuiteAuthorName();
            int rootSuiteInsObjTypeTotal = temp.getRootSuiteInsObjTypeTotal();
            String rootSuiteStatus = temp.getRootSuiteStatus();
            //将大套件情况对应写入excel中
            rowTemp.createCell(0).setCellValue(rootSuiteCreatTime);
            rowTemp.createCell(1).setCellValue(rootSuiteName);
            rowTemp.createCell(2).setCellValue(rootSuiteDescription);
            rowTemp.createCell(3).setCellValue("");
            rowTemp.createCell(4).setCellValue(rootSuiteAuthorName);
            rowTemp.createCell(5).setCellValue(rootSuiteInsObjTypeTotal);
            rowTemp.createCell(6).setCellValue("");
            rowTemp.createCell(7).setCellValue("");
            rowTemp.createCell(8).setCellValue(rootSuiteStatus);

            excelTotalIndex = excelTotalIndex + 1;

            //写套件对应的sample到excel表中
            List<SampleInfoModel> tempSampleList = temp.getSampleInfo();
            int sampleExcelIndex = excelTotalIndex + 1;

            for (int k = 0; k < tempSampleList.size(); k++) {
                SampleInfoModel tempSampleInfoModel = new SampleInfoModel();
                tempSampleInfoModel = tempSampleList.get(k);
                Row rowSampleTemp = sheet1.createRow((short) (sampleExcelIndex + k));

                String sampleCreateTime = tempSampleInfoModel.getSampleCreateTime();
                String sampleName = tempSampleInfoModel.getSampleName();
                String sampleAuthorName = tempSampleInfoModel.getSampleAuthorName();
                int sampleInsObjTotal = tempSampleInfoModel.getSampleInsObjTotal();
                int sampleInsObjAndSubInsObjTotal = tempSampleInfoModel.getSampleInsObjAndSubInsObjTotal();
                String sampleStatu = tempSampleInfoModel.getSampleStatu();
                rowSampleTemp.createCell(0).setCellValue(sampleCreateTime);
                rowSampleTemp.createCell(1).setCellValue("");
                rowSampleTemp.createCell(2).setCellValue("");
                rowSampleTemp.createCell(3).setCellValue(sampleName);
                rowSampleTemp.createCell(4).setCellValue(sampleAuthorName);
                rowSampleTemp.createCell(5).setCellValue("");
                rowSampleTemp.createCell(6).setCellValue(sampleInsObjTotal);
                rowSampleTemp.createCell(7).setCellValue(sampleInsObjAndSubInsObjTotal);
                rowSampleTemp.createCell(8).setCellValue(sampleStatu);

                excelTotalIndex = excelTotalIndex + 1;
            }

        }
         OutputStream out=null;
        try {
        	out = response.getOutputStream();
        	wb.write(out);
        	out.flush();
        	
        	}
        	catch (Exception e) {
        	e.printStackTrace();
        	
        	}finally{
        		if (out!=null) {
        			out.close();	
				}
        		
        	}
    }


    @RequestMapping(value = "/repair", method = RequestMethod.GET)
    public Map<String, List<String>> repair(@RequestParam(required = false) List<String> objectiveTypeIds,
                                            @RequestParam float repairedNum) {
        Map<String, List<String>> returnMap = new HashMap<>();
        List<String> repairedId = instructionalObjectiveService.updateSortNum(objectiveTypeIds, repairedNum);
        returnMap.put("repairedId", repairedId);
        return returnMap;
    }

    /**
     * 修改返回样例数据为空时的样例状态为不可用
     *
     * @return
     * @author yzc
     * @date 2016年12月7日
     */
    @RequestMapping(value = "/sample/status", method = RequestMethod.GET)
    public Map<String, String> changeSampleStatus() {

        instructionalObjectiveService.changeSampleStatus();
        Map<String, String> map = new HashMap<String, String>();
        map.put("process_state", "样例状态修改成功");
        map.put("process_code", "ChangeSampleStatusSuccess");
        return map;
    }

    @RequestMapping(value = "/suite/{suite_id}/change/sample", method = RequestMethod.PUT)
    public void changeSampleStatusAfterChangeSuite(@PathVariable(value = "suite_id") String suiteId, @RequestParam(required = false) String status) {
        instructionalObjectiveService.changeSampleStatus(suiteId, status);
    }

    /**
     * 多级套件分类，按学科，层级数，
     *
     * @return
     * @author xm
     */
    @RequestMapping(value = "/multiSubSuite/subSuite", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ListViewModel<MultiItemViewModel> queryMultiSubSuite(@RequestParam(required = false, value = "subject") String courseCode,
                                                                @RequestParam(required = true, value = "limit") String limit,
                                                                @RequestParam(required = false, value = "words") String words,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = true, value = "level") String levelNum,
                                                                @RequestParam(value = "relation_type", required = false, defaultValue = "ASSOCIATE") String relationType) {

        ListViewModel<MultiItemViewModel> result = new ListViewModel<>();

        //limit限制
        Integer limitNum[] = ParamCheckUtil.checkLimit(limit);

        //最子套件
        List<Map<String, String>> allDownSuiteList = instructionalObjectiveService.queryDownSubsuite();

        //取出缓存的套件树信息
        List<Map<String, Object>> redisResult = educationRelationService.querySuiteList(courseCode, status);

        //缓存树中元素结构存储
        List<MultilayerClassifySubSuite> levelSuiteList = new ArrayList<MultilayerClassifySubSuite>();

        Map<String, List<MultiLevelSuiteViewModel>> resultMap = new HashMap<>();

        //按层级来分配
        Map<String, Map<String, List<MultilayerClassifySubSuite>>> mulclassifyByDescriptionAndLeavelNum = new HashMap<String, Map<String, List<MultilayerClassifySubSuite>>>();

        MultiLevelSuiteData md = resultRedis.get("resultMap" + status + courseCode + levelNum, MultiLevelSuiteData.class);

        if (md != null && CollectionUtils.isNotEmpty(md.getDatas())) {
            resultMap = md.getDatas();
        } else {
            //遍历套件树，找出套件关系
            int maxLeavelNum = 1;   //树的最多的层数
            for (Map<String, String> downSuite : allDownSuiteList) {
                boolean isRootSuite = false;//判断是否为rootSuite
                String sourceId = downSuite.get("identifier");
                String behindId = "";  //由下往上   当soureId为最子id的时候，behindId为""，以此类推
                int leavelNum = 1;    //层级为  1--2（1+1）---3（1+1+1）--4（1+1+1+1）--parent
                int redisResultSize = redisResult.size();

                if (maxLeavelNum <= leavelNum) {
                    maxLeavelNum = leavelNum;
                }

                while (!isRootSuite) {
                    int changeBoolean = 0;
                    if (CollectionUtils.isNotEmpty(redisResult)) {
                        for (Map<String, Object> redisList : redisResult) {
                            String destId = (String) redisList.get("identifier");
                            String parentId = (String) redisList.get("parent");

                            changeBoolean++;

                            if (sourceId.equals(destId)) {
                                if (parentId.equals("root")) {
                                    //找到这个根一级的
                                    fillResultByLeavelNum(levelSuiteList,
                                            ((String) redisList.get("description")).trim(),
                                            (String) redisList.get("status"),
                                            isRootSuite,
                                            "0",
                                            sourceId,
                                            (String) redisList.get("parent"),
                                            (String) redisList.get("title"),
                                            behindId);
                                    isRootSuite = true;
                                    break;
                                } else {

                                    fillResultByLeavelNum(levelSuiteList,
                                            ((String) redisList.get("description")).trim(),
                                            (String) redisList.get("status"),
                                            isRootSuite,
                                            String.valueOf(leavelNum),
                                            sourceId,
                                            (String) redisList.get("parent"),
                                            (String) redisList.get("title"),
                                            behindId);
                                    behindId = sourceId;
                                    sourceId = parentId;
                                    leavelNum++;
                                    if (maxLeavelNum <= leavelNum) {
                                        maxLeavelNum = leavelNum;
                                    }
                                    break;
                                }
                            }
                            //判断循环结束是否是遍历完了redisResult还没找到数据
                            if (changeBoolean >= redisResultSize) {
                                isRootSuite = true;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }


            //按套件的层级划分
            for (int level = maxLeavelNum - 1; level >= 1; level--) {
                List<MultilayerClassifySubSuite> classifyList = new ArrayList<MultilayerClassifySubSuite>();
                for (MultilayerClassifySubSuite tempMultilayerClassifySubSuite : levelSuiteList) {
                    if (!tempMultilayerClassifySubSuite.getSuiteLevel().equals(String.valueOf(level))) {
                        continue;
                    }
                    classifyList.add(tempMultilayerClassifySubSuite);
                }
                Map<String, List<MultilayerClassifySubSuite>> classifyByDesctiptionMap = MulclassifyByDescription(classifyList);
                mulclassifyByDescriptionAndLeavelNum.put(String.valueOf(level), classifyByDesctiptionMap);
            }
            if (CollectionUtils.isEmpty(mulclassifyByDescriptionAndLeavelNum) || CollectionUtils.isEmpty(mulclassifyByDescriptionAndLeavelNum.get(levelNum))) {
                result.setLimit(limit);
                result.setTotal(0l);
                result.setItems(null);
                return result;
            }

            resultMap = instructionalObjectiveService.dealReturnMap(mulclassifyByDescriptionAndLeavelNum, levelNum);
            MultiLevelSuiteData md1 = new MultiLevelSuiteData();
            md1.setDatas(resultMap);
//            加缓存
            resultRedis.set("resultMap" + status + courseCode + levelNum, md1);
            resultRedis.expire("resultMap", 20l, TimeUnit.MINUTES);
        }
        if (CollectionUtils.isEmpty(resultMap)) {
            result.setLimit(limit);
            result.setTotal(0l);
            List<MultiItemViewModel> itemList1 = new ArrayList<>();
            result.setItems(itemList1);
            return result;
        }

        List<MultiItemViewModel> itemList = new ArrayList<>();

        //模糊查询
        if (StringUtils.isNotEmpty(words)) {
            Set<String> keySet = resultMap.keySet();
            for (String key : keySet) {
                Pattern pattern = Pattern.compile("\\【.+?\\】");
                String matchKey = pattern.matcher(key).replaceAll("");
                if (matchKey.contains(words)) {
                    MultiItemViewModel item = new MultiItemViewModel();
                    item.setTitle(key);
                    item.setGroup(resultMap.get(key));
                    itemList.add(item);
                }
            }
        } else {
            for (Entry<String, List<MultiLevelSuiteViewModel>> entry : resultMap.entrySet()) {
                MultiItemViewModel item = new MultiItemViewModel();
                item.setTitle(entry.getKey());
                item.setGroup(entry.getValue());
                itemList.add(item);
            }
        }

        //limit
        if (itemList.size() < limitNum[0]) {
            result.setLimit(limit);
            result.setTotal(0l);
            List<MultiItemViewModel> itemList1 = new ArrayList<>();
            result.setItems(itemList1);
            return result;
        }

        if (itemList.size() > limitNum[0]) {
            if (itemList.size() > (limitNum[0] + limitNum[1])) {
                itemList = itemList.subList(limitNum[0], limitNum[0] + limitNum[1]);
            } else {
                itemList = itemList.subList(limitNum[0], itemList.size());
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        List<MultiItemViewModel> itemListResult = new ArrayList<>();
        for (MultiItemViewModel model : itemList) {
            itemListResult.add(model);
        }
        Long total = Long.valueOf(resultMap.keySet().size());
        result.setLimit(limit);
        result.setTotal(total);

        result.setItems(itemListResult);


        return result;
    }

    /**
     * 填充赋值
     *
     * @author xm
     */
    public void fillResultByLeavelNum(List<MultilayerClassifySubSuite> levelSuiteList,
                                      String regexBeforeDescription,
                                      String status,
                                      boolean isRootSuite,
                                      String suiteLevel,
                                      String identify,
                                      String parent,
                                      String title,
                                      String beforeIdentify) {

        if (querylistMultilayerClassifySubSuite(levelSuiteList, identify) == null) {//还是遍历确定这个没某个identify，这个List不包含这个
            MultilayerClassifySubSuite tempMultilayerClassifySubSuite = new MultilayerClassifySubSuite();
            tempMultilayerClassifySubSuite.setIdentify(identify);
            tempMultilayerClassifySubSuite.setSuiteLevel(suiteLevel);
            tempMultilayerClassifySubSuite.setStatus(status);
            tempMultilayerClassifySubSuite.setRootSuite(isRootSuite);
            tempMultilayerClassifySubSuite.setParent(parent);
            tempMultilayerClassifySubSuite.setTitle(title);
            Set<String> beforeIdentifySet = new HashSet<String>();
            beforeIdentifySet.add(beforeIdentify);
            tempMultilayerClassifySubSuite.setBeforeIdentifySet(beforeIdentifySet);

            tempMultilayerClassifySubSuite.setRegexBeforeDescription(regexBeforeDescription);
            if (!regexBeforeDescription.isEmpty()) {
                String regexAfterDescrption = Pattern.compile("\\【.+?\\】").matcher(regexBeforeDescription).replaceAll("【XX】");
                tempMultilayerClassifySubSuite.setRegexAfterDescrption(regexAfterDescrption);
            }
            levelSuiteList.add(tempMultilayerClassifySubSuite);
        } else {
            MultilayerClassifySubSuite tempMultilayerClassifySubSuite = querylistMultilayerClassifySubSuite(levelSuiteList, identify);

            Set<String> beforeIdentifyList = tempMultilayerClassifySubSuite.getBeforeIdentifySet();
            if (Integer.parseInt(tempMultilayerClassifySubSuite.getSuiteLevel()) < Integer.parseInt(suiteLevel)) {
                tempMultilayerClassifySubSuite.setSuiteLevel(String.valueOf(suiteLevel));
            }

            beforeIdentifyList.add(beforeIdentify);
            tempMultilayerClassifySubSuite.setBeforeIdentifySet(beforeIdentifyList);
            //这个地方是否有错，等下试一下
        }
    }

    /**
     * 归类,形成 key（ String"description"）--value（List<String> "identifier"）
     *
     * @author xm
     */
    public Map<String, List<MultilayerClassifySubSuite>> MulclassifyByDescription(List<MultilayerClassifySubSuite> levelList) {

        Map<String, List<MultilayerClassifySubSuite>> resultMulclassifyByDescription = new TreeMap<String, List<MultilayerClassifySubSuite>>();

        for (MultilayerClassifySubSuite modelMultilayerClassifySubSuite : levelList) {
            String RegexAfterDescription = modelMultilayerClassifySubSuite.getRegexAfterDescrption();//后面用来做模糊匹配的description

            // 形成 key（ String"description"）--value（List<String> "identifier"）
            if (resultMulclassifyByDescription.containsKey(RegexAfterDescription)) {
                resultMulclassifyByDescription.get(RegexAfterDescription).add(modelMultilayerClassifySubSuite);
            } else {
                List<MultilayerClassifySubSuite> listTemp = new ArrayList<MultilayerClassifySubSuite>();
                listTemp.add(modelMultilayerClassifySubSuite);
                resultMulclassifyByDescription.put(RegexAfterDescription, listTemp);
            }
        }
        return resultMulclassifyByDescription;
    }

   /*
    * List中是否有这个id的model
    */
    public MultilayerClassifySubSuite querylistMultilayerClassifySubSuite(List<MultilayerClassifySubSuite> list, String id) {

        for (MultilayerClassifySubSuite multilayerClassifySubSuite : list) {
            if (id.equals(multilayerClassifySubSuite.getIdentify())) {
                return multilayerClassifySubSuite;
            }
        }
        return null;
    }
}
