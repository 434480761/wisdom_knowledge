package nd.esp.service.lifecycle.services.suites.v06.impl;

import nd.esp.service.lifecycle.educommon.support.RelationType;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.*;
import nd.esp.service.lifecycle.repository.sdk.*;
import nd.esp.service.lifecycle.services.assets.v06.AssetServiceV06;
import nd.esp.service.lifecycle.services.instructionalobjectives.v06.InstructionalObjectiveService;
import nd.esp.service.lifecycle.services.suites.v06.ExtendSuiteService;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.enums.LifecycleStatus;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.EduRedisTemplate;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.utils.gson.ObjectUtils;
import nd.esp.service.lifecycle.vos.business.extendsuite.ExtendSuiteViewModel;
import nd.esp.service.lifecycle.vos.business.extendsuite.KnViewModel;
import nd.esp.service.lifecycle.vos.business.extendsuite.ObjTypeViewModel;
import nd.esp.service.lifecycle.vos.business.extendsuite.ObjViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2016/12/8 0008.
 */
@Service
public class ExtendSuiteServiceImpl implements ExtendSuiteService {

    @Autowired
    private ResourceRelationRepository resourceRelationRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private InstructionalobjectiveRepository ioRepository;

    @Autowired
    private ResourceCategoryRepository categoryRepository;

    @Autowired
    private ResCoverageRepository coverageRepository;

    @Autowired
    private SubInstructionRepository subInstructionRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private InstructionalObjectiveService instructionalObjectiveService;

    @Autowired
    private AssetServiceV06 assetService;

    @Autowired
    private ContributeRepository contributeRepository;

    @Autowired
    private CategoryDataRepository categoryDataRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private EduRedisTemplate<CategoryData> categoryDataEduRedisTemplate;

    @Autowired
    private JdbcTemplate jt;

    /**
     * @param sampleId
     * @param extendSuiteViewModels
     * @return
     */
    @Override
    public Map<String, String> extendSuites(String sampleId, List<ExtendSuiteViewModel> extendSuiteViewModels) {

        //创建套件list
        List<Asset> createAssetList = new ArrayList<>();

        //创建学习目标list
        List<InstructionalObjective> createObjectiveList = new ArrayList<>();

        //创建维度数据list
        List<ResourceCategory> createCategoryList = new ArrayList<>();

        //创建覆盖范围数据list
        List<ResCoverage> createCoverageList = new ArrayList<>();

        //创建关系list
        List<ResourceRelation> createRelationList = new ArrayList<>();

        //子教学目标list
        List<SubInstruction> createSubInstructionList = new ArrayList<>();

        //contribute list
        List<Contribute> createContributeList = new ArrayList<>();

        //保存parent的sort_num的map
        Map<String, Float> parentSortNumMap = new HashMap<>();

        //保存其他类型sort_num的map
        Map<String, Float> otherSortNumMap = new HashMap<>();

        Timestamp ts = new Timestamp(System.currentTimeMillis());

        Map<String, String> map = new LinkedHashMap<>();
        extendSuitesRecur(
                sampleId,
                extendSuiteViewModels,
                createAssetList,
                createObjectiveList,
                createCategoryList,
                createCoverageList,
                createRelationList,
                createSubInstructionList,
                createContributeList,
                parentSortNumMap,
                otherSortNumMap,
                map
        );

        //判断要不要与sample建关系,在关系表查target为parentId，关系类型为copy的
        for (ExtendSuiteViewModel model : extendSuiteViewModels) {

            String parentId = model.getParentId();
            ResourceRelation example = new ResourceRelation();
            example.setTarget(parentId);
            example.setEnable(true);
            example.setRelationType(RelationType.COPY.getName());
            List<ResourceRelation> judgeList = new ArrayList<>();
            try {
                judgeList = resourceRelationRepository.getAllByExample(example);
            } catch (EspStoreException e) {
                e.printStackTrace();
            }
            if (CollectionUtils.isEmpty(judgeList) && StringUtils.isEmpty(map.get(parentId))) {
                createRelationList.add(addResourceRelation(
                        "samples",
                        sampleId,
                        IndexSourceType.AssetType.getName(),
                        map.get(model.getIdentifier()),
                        RelationType.COPY.getName(),
                        5000f,
                        ts,
                        null));
            }
        }

        //最后处理数据
        try {
            if (CollectionUtils.isNotEmpty(createAssetList)) {
                assetRepository.batchAdd(createAssetList);
            }
            if (CollectionUtils.isNotEmpty(createCategoryList)) {
                categoryRepository.batchAdd(createCategoryList);
            }
            if (CollectionUtils.isNotEmpty(createCoverageList)) {
                coverageRepository.batchAdd(createCoverageList);
            }
            if (CollectionUtils.isNotEmpty(createObjectiveList)) {
                ioRepository.batchAdd(createObjectiveList);
            }
            if (CollectionUtils.isNotEmpty(createRelationList)) {
                resourceRelationRepository.batchAdd(createRelationList);
            }
            if (CollectionUtils.isNotEmpty(createSubInstructionList)) {
                subInstructionRepository.batchAdd(createSubInstructionList);
            }
            if (CollectionUtils.isNotEmpty(createContributeList)) {
                contributeRepository.batchAdd(createContributeList);
            }
        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        return map;
    }

    @Override
    public List<ExtendSuiteViewModel> queryExtendSuites(String sampleId) {
        List<ExtendSuiteViewModel> returnList = new LinkedList<>();
        //找到顶级套件ID
        Sample sample = null;
        try {
            sample = sampleRepository.get(sampleId);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        if (sample == null) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "SAMPLE NOT EXIST", "样例查询出错");
        }

        String knId1 = sample.getKnowledgeId1();
        String knId2 = sample.getKnowledgeId2();
        String knId3 = sample.getKnowledgeId3();
        String knId4 = sample.getKnowledgeId4();
        String knId5 = sample.getKnowledgeId5();

        List<String> copySuites = new ArrayList<>();
        findCopySuite(copySuites, sampleId, "samples", RelationType.COPY.getName());
        if (CollectionUtils.isEmpty(copySuites)) {
            return returnList;
        }
        returnList = dealCopySuiteData(copySuites);

        if (CollectionUtils.isNotEmpty(returnList)) {
            for (ExtendSuiteViewModel extendSuiteViewModel : returnList) {
                String identifier = extendSuiteViewModel.getIdentifier();

                //处理copy_suite_id
                String copySuiteId = resourceRelationRepository.findCopySuiteIdById(identifier);
                extendSuiteViewModel.setCopySuiteId(copySuiteId);

                //处理学习目标类型和学习目标
                List<Map<String, Object>> objTypeList = findObjectiveTypesBySuiteId(identifier);
                if (CollectionUtils.isEmpty(objTypeList)) {
                    continue;
                }
                List<ObjTypeViewModel> objTypeList1 = new ArrayList<>();
                for (Map<String, Object> map : objTypeList) {
                    ObjTypeViewModel model = new ObjTypeViewModel();
                    model.setIdentifier((String) map.get("identifier"));
                    model.setDescription((String) map.get("description"));
                    model.setTitle((String) map.get("title"));
                    model.setCustomProperties(ObjectUtils.fromJson((String) map.get("custom_properties"), Map.class));

                    List<KnViewModel> knowledges = new ArrayList<>();

                    List<String> objIds = new ArrayList<>();
                    if (StringUtils.isNotEmpty(knId1)) {
                        objIds = dealKnowledge(knId1, objIds, knowledges, 1);
                    }
                    if (StringUtils.isNotEmpty(knId2)) {
                        List<String> objId2 = new ArrayList<>();
                        objId2 = dealKnowledge(knId2, objId2, knowledges, 2);
                        objIds.retainAll(objId2);
                    }
                    if (StringUtils.isNotEmpty(knId3)) {
                        List<String> objId3 = new ArrayList<>();
                        objId3 = dealKnowledge(knId3, objId3, knowledges, 3);
                        objIds.retainAll(objId3);
                    }
                    if (StringUtils.isNotEmpty(knId4)) {
                        List<String> objId4 = new ArrayList<>();
                        objId4 = dealKnowledge(knId4, objId4, knowledges, 4);
                        objIds.retainAll(objId4);
                    }
                    if (StringUtils.isNotEmpty(knId5)) {
                        List<String> objId5 = new ArrayList<>();
                        objId5 = dealKnowledge(knId5, objId5, knowledges, 5);
                        objIds.retainAll(objId5);
                    }
                    List<String> objIdByType = new ArrayList<>();
                    objIdByType = resourceRelationRepository.findBySourceIdAndResTypeAndTargetType(model.getIdentifier(), IndexSourceType.AssetType.getName(), IndexSourceType.InstructionalObjectiveType.getName(), RelationType.COPY.getName());
                    objIds.retainAll(objIdByType);
                    List<String> objIdBySuite = new ArrayList<>();
                    objIdBySuite = resourceRelationRepository.findBySourceIdAndResTypeAndTargetType(identifier, IndexSourceType.AssetType.getName(), IndexSourceType.InstructionalObjectiveType.getName(), RelationType.COPY.getName());
                    objIds.retainAll(objIdBySuite);

                    List<InstructionalObjective> objectives = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(objIds)) {
                        objectives = ioRepository.getAllByIds(objIds);
                    }

                    if (CollectionUtils.isNotEmpty(objectives)) {
                        List<ObjViewModel> objList = new ArrayList<>();
                        for (InstructionalObjective objective : objectives) {
                            ObjViewModel objViewModel = new ObjViewModel();
                            List<String> keyWords = ObjectUtils.fromJson(objective.getDbkeywords(), List.class);

                            objViewModel.setIdentifier(objective.getIdentifier());
                            objViewModel.setCreator(objective.getCreator());
                            objViewModel.setProviderSource(objective.getProviderSource());
                            objViewModel.setDescription(objective.getDescription());
                            objViewModel.setTitle(objective.getTitle());
                            objViewModel.setStatus(objective.getStatus());
                            objViewModel.setKnowledges(knowledges);
                            objViewModel.setCustomProperties(ObjectUtils.fromJson(objective.getCustomProperties(), Map.class));
                            if (CollectionUtils.isNotEmpty(keyWords)) {
                                objViewModel.setKeywords(keyWords);
                            } else {
                                objViewModel.setKeywords(new ArrayList<String>());
                            }

                            //多版本
                            List<Map<String, Object>> versions = findVersions(objective.getIdentifier());
                            if (CollectionUtils.isNotEmpty(versions)) {
                                List<String> vList = new ArrayList<>();
                                for (Map<String, Object> version : versions) {
                                    vList.add((String) version.get("description"));
                                }
                                objViewModel.setVersions(vList);
                            } else {
                                objViewModel.setVersions(new ArrayList<String>());
                            }

                            //学段
                            List<String> applicablePeriod = findApplicablePeriod(objViewModel.getIdentifier());
                            objViewModel.setApplicablePeriod(applicablePeriod);

                            objList.add(objViewModel);
                        }

                        model.setObjective(objList);
                    }

                    //category
                    List<Map<String, Object>> knowledgeCategory = new ArrayList<>();
                    knowledgeCategory = findKnCategoryById(model.getIdentifier());
                    Map<String, Object> category = new HashMap<>();
                    category.put("KnowledgeCategory", knowledgeCategory);
                    model.setCategories(category);

                    objTypeList1.add(model);
                }
                extendSuiteViewModel.setObjectiveTypes(objTypeList1);

                //处理category
                Map<String, Object> categories = new HashMap<>();
                List<Map<String, String>> subject = new ArrayList<>();
                List<ResourceCategory> subjects = new ArrayList<>();
                subjects = categoryRepository.findTaxoncodeByResourceAndCategoryCode(identifier, "$S");
                for (ResourceCategory rc : subjects) {
                    Map<String, String> map = new HashMap<>();
                    map.put("taxoncode", rc.getTaxoncode());
                    subject.add(map);
                }
                categories.put("subject", subject);
            }

        }

        return returnList;
    }

    @Override
    public Map<String, String> repairOldData(String sampleId, List<ExtendSuiteViewModel> extendSuiteViewModels) {
        //创建关系list
        List<ResourceRelation> createRelationList = new ArrayList<>();
        List<Asset> createAssetList = new ArrayList<>();
        List<ResourceCategory> createCategoryList = new ArrayList<>();
        List<ResCoverage> createCoverageList = new ArrayList<>();

        Map<String, Float> parentSortMap = new HashMap<>();
        Map<String, Float> otherSortMap = new HashMap<>();

        Map<String, String> map = new LinkedHashMap<>();
        repairDataRecur(
                sampleId,
                extendSuiteViewModels,
                createRelationList,
                createAssetList,
                createCategoryList,
                createCoverageList,
                map,
                parentSortMap,
                otherSortMap
        );

        //判断要不要与sample建关系,在关系表查target为parentId，关系类型为copy的
        for (ExtendSuiteViewModel model : extendSuiteViewModels) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            createRelationList.add(addResourceRelation(
                    "samples",
                    sampleId,
                    IndexSourceType.AssetType.getName(),
                    map.get(model.getIdentifier()),
                    RelationType.COPY.getName(),
                    5000f,
                    ts,
                    null));
        }
        try {

            if (CollectionUtils.isNotEmpty(createAssetList)) {
                assetRepository.batchAdd(createAssetList);
            }
            if (CollectionUtils.isNotEmpty(createCategoryList)) {
                categoryRepository.batchAdd(createCategoryList);
            }
            if (CollectionUtils.isNotEmpty(createCoverageList)) {
                coverageRepository.batchAdd(createCoverageList);
            }
            if (CollectionUtils.isNotEmpty(createRelationList)) {
                resourceRelationRepository.batchAdd(createRelationList);
            }

        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public Map<String, List<String>> deleteCopySuite(String suiteId) {
        Map<String, List<String>> returnMap = new HashMap<>();
        List<ResourceRelation> deleteRelationList = new ArrayList<>();
        List<Asset> deleteAssetList = new ArrayList<>();

        //删除跟样例的关系（如果有的话）
        ResourceRelation example = new ResourceRelation();
        example.setTarget(suiteId);
        example.setResType("samples");
        example.setResourceTargetType(IndexSourceType.AssetType.getName());
        example.setRelationType(RelationType.COPY.getName());
        try {
            ResourceRelation sampleRelation = resourceRelationRepository.getByExample(example);
            if (sampleRelation != null) {
                sampleRelation.setEnable(false);
                deleteRelationList.add(sampleRelation);
            }
        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        //找子套件
        List<Map<String, Object>> suiteIdList = new ArrayList<>();
        recursiveSuiteDirectory(suiteIdList, suiteId, 0);
        List<String> allSuites = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(suiteIdList)) {
            for (Map<String, Object> map : suiteIdList) {
                allSuites.add((String) map.get("identifier"));
            }
        }
        allSuites.add(suiteId);

        //删除套件本身
        try {
            List<Asset> assets = assetRepository.getAll(allSuites);
            if (CollectionUtils.isNotEmpty(assets)) {
                for (Asset asset : assets) {
                    asset.setEnable(false);
                }
                deleteAssetList.addAll(assets);
            }

        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        //删除套件的关系
        List<ResourceRelation> suiteRelation1 = resourceRelationRepository.findAllRelationBySources(allSuites);
        List<ResourceRelation> suiteRelation2 = resourceRelationRepository.findAllRelationByTargets(allSuites);
        suiteRelation1.addAll(suiteRelation2);
        if (CollectionUtils.isNotEmpty(suiteRelation1)) {
            for (ResourceRelation relation : suiteRelation1) {
                relation.setEnable(false);
            }
            deleteRelationList.addAll(suiteRelation1);
        }
        if (suiteRelation1.size() == 1) {
            return null;
        }

        //查出套件下的学习目标
        String sql = "select rr.target from resource_relations rr where rr.source_uuid in (:suiteId) and rr.relation_type = 'COPY' and rr.resource_target_type = 'instructionalobjectives'";
        Map<String, Object> params = new HashMap<>();
        params.put("suiteId", allSuites);
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        List<Map<String, Object>> list = npjt.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("identifier", rs.getString("target"));
                return map;
            }
        });
        List<String> objIds = new ArrayList<>();
        for (Map<String, Object> map : list) {
            objIds.add((String) map.get("identifier"));
        }

        List<InstructionalObjective> objectives = new ArrayList<>();
        //删除学习目标
        try {
            objectives = ioRepository.getAll(objIds);
            if (CollectionUtils.isNotEmpty(objectives)) {
                for (InstructionalObjective objective : objectives) {
                    objective.setEnable(false);
                }
            }
        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        //统一处理数据
        try {
            if (CollectionUtils.isNotEmpty(deleteAssetList)) {
                assetRepository.batchAdd(deleteAssetList);
            }
            if (CollectionUtils.isNotEmpty(deleteRelationList)) {
                resourceRelationRepository.batchAdd(deleteRelationList);
            }
            if (CollectionUtils.isNotEmpty(objectives)) {
                ioRepository.batchAdd(objectives);
            }
        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        returnMap.put("deleted suite : ", allSuites);
        returnMap.put("deleted objectives : ", objIds);
        returnMap.put("deleted relations : ", null);

        return returnMap;
    }

    private Map<String, String> extendSuitesRecur(String sampleId,
                                                  List<ExtendSuiteViewModel> extendSuiteViewModels,
                                                  List<Asset> createAssetList,
                                                  List<InstructionalObjective> createObjectiveList,
                                                  List<ResourceCategory> createCategoryList,
                                                  List<ResCoverage> createCoverageList,
                                                  List<ResourceRelation> createRelationList,
                                                  List<SubInstruction> createSubInstructionList,
                                                  List<Contribute> createContributeList,
                                                  Map<String, Float> parentSortNumMap,
                                                  Map<String, Float> otherSortNumMap,
                                                  Map<String, String> returnMap
    ) {
        if (CollectionUtils.isEmpty(extendSuiteViewModels)) {
            return returnMap;
        }

        Timestamp ts2 = new Timestamp(System.currentTimeMillis());
        for (ExtendSuiteViewModel extendSuiteViewModel : extendSuiteViewModels) {

            Timestamp ts = new Timestamp(System.currentTimeMillis());
            int versionNum = 0;

            //将虚拟id：实际id放入返回值
            String realId = UUID.randomUUID().toString();
            returnMap.put(extendSuiteViewModel.getIdentifier(), realId);

            //取出传入的各个属性的值
            String title = extendSuiteViewModel.getTitle();
            String description = extendSuiteViewModel.getDescription();
            String language = extendSuiteViewModel.getLanguage();
            Map<String, Object> catetories = extendSuiteViewModel.getCategories();
            Map<String, Object> lifeCycle = extendSuiteViewModel.getLifeCycle();
            List<ObjTypeViewModel> objTypeViewModels = extendSuiteViewModel.getObjectiveTypes();
            List<ExtendSuiteViewModel> children = extendSuiteViewModel.getChildren();
            String copySuiteId = extendSuiteViewModel.getCopySuiteId();
            String parentId = extendSuiteViewModel.getParentId();

            List<Map<String, String>> subject = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(catetories)) {
                subject = (List<Map<String, String>>) catetories.get("subject");
            }
            String creator = null;
            String version = null;
            if (CollectionUtils.isNotEmpty(lifeCycle)) {
                version = (String) lifeCycle.get("version");
                creator = (String) lifeCycle.get("creator");
            }

            //children中parent虚拟id处理
            if (StringUtils.isNotEmpty(returnMap.get(parentId))) {
                parentId = returnMap.get(parentId);
            }
            //children中copy虚拟id处理
            if (StringUtils.isNotEmpty(returnMap.get(copySuiteId))) {
                copySuiteId = returnMap.get(copySuiteId);
            }

            //处理拷贝的套件
            Asset asset = new Asset();
            asset.setIdentifier(realId);
            asset.setTitle(title);
            asset.setDescription(description);
            asset.setCreateTime(ts);
            asset.setLastUpdate(ts);
            asset.setEnable(true);
            asset.setStatus(LifecycleStatus.CREATED.getCode());
            asset.setVersion(version);
            asset.setLanguage(language);
            asset.setCreator(creator);
            asset.setPrimaryCategory(IndexSourceType.AssetType.getName());
            createAssetList.add(asset);

            ResourceCategory category1 = new ResourceCategory();
            category1.setIdentifier(UUID.randomUUID().toString());
            category1.setResource(realId);
            category1.setTaxoncode("$RA0502");
            category1.setTaxonname("套件模板素材");
            category1.setTaxoncodeid("1c2f86ac-dd23-45c9-bfa8-a53e592f6ee6");
            category1.setCategoryCode("$R");
            category1.setCategoryName("resourcetype");
            category1.setPrimaryCategory(IndexSourceType.AssetType.getName());
            category1.setShortName("objectivesets");
            createCategoryList.add(category1);

            if (CollectionUtils.isNotEmpty(subject)) {
                for (Map<String, String> map : subject) {
                    String taxoncode = map.get("taxoncode");
                    CategoryData data = new CategoryData();
                    if (categoryDataEduRedisTemplate.get(taxoncode, CategoryData.class) != null) {
                        data = categoryDataEduRedisTemplate.get(taxoncode, CategoryData.class);
                    } else {
                        data.setNdCode(taxoncode);
                        try {
                            data = categoryDataRepository.getByExample(data);
                            categoryDataEduRedisTemplate.set(taxoncode, data, 1l, TimeUnit.DAYS);
                        } catch (EspStoreException e) {
                            e.printStackTrace();
                        }
                    }

                    ResourceCategory category2 = new ResourceCategory();
                    category2.setIdentifier(UUID.randomUUID().toString());
                    category2.setResource(realId);
                    category2.setTaxoncode(data.getNdCode());
                    category2.setTaxoncodeid(data.getIdentifier());
                    category2.setPrimaryCategory(IndexSourceType.AssetType.getName());
                    category2.setCategoryCode("$S");
                    category2.setCategoryName("subject");
                    category2.setShortName(data.getShortName());
                    category2.setTaxonname(data.getTitle());
                    createCategoryList.add(category2);
                }
            }

            ResCoverage rcv = new ResCoverage();
            rcv.setIdentifier(UUID.randomUUID().toString());
            rcv.setResource(realId);
            rcv.setResType(IndexSourceType.AssetType.getName());
            rcv.setTarget("nd");
            rcv.setTargetType("Org");
            rcv.setStrategy("OWNER");
            rcv.setTargetTitle("copyAssets4Business");
            createCoverageList.add(rcv);

            Contribute contribute = new Contribute();
            contribute.setIdentifier(UUID.randomUUID().toString());
            contribute.setLifeStatus(LifecycleStatus.CREATED.getCode());
            contribute.setResType(IndexSourceType.AssetType.getName());
            contribute.setTargetType("USER");
            contribute.setTargetId(creator);
            contribute.setContributeTime(ts);
            contribute.setResource(realId);
            contribute.setMessage("扩展套件");
            createContributeList.add(contribute);

            //跟parent的关系
            ResourceRelation parentRelation = addResourceRelation(IndexSourceType.AssetType.getName(),
                    parentId,
                    IndexSourceType.AssetType.getName(),
                    realId,
                    RelationType.PARENT.getName(),
                    dealCopySortNum(parentId, copySuiteId, parentSortNumMap),
                    ts,
                    creator);
            createRelationList.add(parentRelation);

            //跟拷贝对象的关系
            ResourceRelation copyRelation = addResourceRelation(IndexSourceType.AssetType.getName(),
                    copySuiteId,
                    IndexSourceType.AssetType.getName(),
                    realId,
                    RelationType.COPY.getName(),
                    5000f,
                    ts,
                    creator);
            createRelationList.add(copyRelation);

            //处理学习目标类型和学习目标
            if (CollectionUtils.isNotEmpty(objTypeViewModels)) {
                for (ObjTypeViewModel objTypeViewModel : objTypeViewModels) {
                    String objTypeId = objTypeViewModel.getIdentifier();

                    List<ObjViewModel> objectives = objTypeViewModel.getObjective();
                    for (ObjViewModel objective : objectives) {
                        String realObjId = UUID.randomUUID().toString();
                        returnMap.put(objective.getIdentifier(), realObjId);

                        InstructionalObjective newObjective = new InstructionalObjective();
                        newObjective.setIdentifier(realObjId);
                        newObjective.setCreator(creator);
                        newObjective.setProviderSource(objective.getProviderSource());
                        newObjective.setKeywords(objective.getKeywords());
                        newObjective.setDescription(objective.getDescription());
                        newObjective.setVersion(version);
                        newObjective.setTitle(objective.getTitle());
                        newObjective.setStatus(LifecycleStatus.CREATED.getCode());
                        newObjective.setCreateTime(ts);
                        newObjective.setCustomProperties(ObjectUtils.toJson(objective.getCustomProperties()));
                        createObjectiveList.add(newObjective);

                        //处理多版本
                        if (CollectionUtils.isNotEmpty(objective.getVersions())) {
                            for (String v : objective.getVersions()) {
                                SubInstruction si = new SubInstruction();
                                String siId = UUID.randomUUID().toString();
                                si.setIdentifier(siId);
                                si.setmIdentifier(siId);
                                si.setTitle(objective.getTitle());
                                si.setDescription(v);
                                si.setCreator(creator);
                                si.setVersion("v0.6");
                                si.setStatus(LifecycleStatus.CREATED.getCode());
                                si.setCreateTime(ts2);
                                si.setLastUpdate(ts2);
                                si.setPrimaryCategory(IndexSourceType.SubInstructionType
                                        .getName());
                                // 创建子教学目标
                                createSubInstructionList.add(si);
                                // 教学目标与子教学目标的关系
                                createRelationList.add(addResourceRelation(
                                        IndexSourceType.InstructionalObjectiveType
                                                .getName(),
                                        realObjId,
                                        IndexSourceType.SubInstructionType
                                                .getName(),
                                        siId,
                                        RelationType.COPY.getName(),
                                        5001f,
                                        ts,
                                        creator));
                                ts2 = new Timestamp(System.currentTimeMillis() + versionNum * 10);
                                versionNum++;
                            }
                        }

                        //处理学习目标的applicable_period
                        List<String> applicablePeriod = objective.getApplicablePeriod();
                        if (CollectionUtils.isNotEmpty(applicablePeriod)) {
                            createCategoryList.addAll(instructionalObjectiveService.addApplicablePeriodCategoryList(realObjId, applicablePeriod));
                        }

                        //学习目标类型和学习目标的关系
                        ResourceRelation objRelation = new ResourceRelation();
                        objRelation.setSourceUuid(objTypeId);
                        objRelation.setResType(IndexSourceType.AssetType.getName());
                        objRelation.setResourceTargetType(IndexSourceType.InstructionalObjectiveType.getName());
                        objRelation.setEnable(true);
                        objRelation.setSortNum(instructionalObjectiveService.dealObjectivesSortNum(objTypeId, otherSortNumMap, RelationType.COPY.getName()));
                        objRelation.setIdentifier(UUID.randomUUID().toString());
                        objRelation.setTarget(realObjId);
                        objRelation.setCreator(creator);
                        objRelation.setCreateTime(ts);
                        objRelation.setRelationType(RelationType.COPY.getName());
                        createRelationList.add(objRelation);

                        //学习目标与套件要建关系
                        ResourceRelation suiteRelation = new ResourceRelation();
                        suiteRelation.setSourceUuid(realId);
                        suiteRelation.setResType(IndexSourceType.AssetType.getName());
                        suiteRelation.setResourceTargetType(IndexSourceType.InstructionalObjectiveType.getName());
                        suiteRelation.setEnable(true);
                        suiteRelation.setSortNum(instructionalObjectiveService.dealObjectivesSortNum(realId, otherSortNumMap, RelationType.COPY.getName()));
                        suiteRelation.setIdentifier(UUID.randomUUID().toString());
                        suiteRelation.setTarget(realObjId);
                        suiteRelation.setCreator(creator);
                        suiteRelation.setCreateTime(ts);
                        suiteRelation.setRelationType(RelationType.COPY.getName());
                        createRelationList.add(suiteRelation);

                        //学习目标和知识点的关系
                        Sample sample = null;
                        try {
                            sample = sampleRepository.get(sampleId);

                        } catch (EspStoreException e) {
                            e.printStackTrace();
                        }
                        if (sample == null) {
                            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "SAMPLE NOT EXIST", "样例未找到");
                        }
                        if (StringUtils.isNotEmpty(sample.getKnowledgeId1())) {
                            createRelationList.add(addResourceRelation(IndexSourceType.KnowledgeType.getName(),
                                    sample.getKnowledgeId1(),
                                    IndexSourceType.InstructionalObjectiveType.getName(),
                                    realObjId,
                                    RelationType.COPY.getName(),
                                    5001f,
                                    ts,
                                    creator));
                        }
                        if (StringUtils.isNotEmpty(sample.getKnowledgeId2())) {
                            createRelationList.add(addResourceRelation(IndexSourceType.KnowledgeType.getName(),
                                    sample.getKnowledgeId2(),
                                    IndexSourceType.InstructionalObjectiveType.getName(),
                                    realObjId,
                                    RelationType.COPY.getName(),
                                    5001f,
                                    ts,
                                    creator));
                        }
                        if (StringUtils.isNotEmpty(sample.getKnowledgeId3())) {
                            createRelationList.add(addResourceRelation(IndexSourceType.KnowledgeType.getName(),
                                    sample.getKnowledgeId3(),
                                    IndexSourceType.InstructionalObjectiveType.getName(),
                                    realObjId,
                                    RelationType.COPY.getName(),
                                    5001f,
                                    ts,
                                    creator));
                        }
                        if (StringUtils.isNotEmpty(sample.getKnowledgeId4())) {
                            createRelationList.add(addResourceRelation(IndexSourceType.KnowledgeType.getName(),
                                    sample.getKnowledgeId4(),
                                    IndexSourceType.InstructionalObjectiveType.getName(),
                                    realObjId,
                                    RelationType.COPY.getName(),
                                    5001f,
                                    ts,
                                    creator));
                        }
                        if (StringUtils.isNotEmpty(sample.getKnowledgeId5())) {
                            createRelationList.add(addResourceRelation(IndexSourceType.KnowledgeType.getName(),
                                    sample.getKnowledgeId5(),
                                    IndexSourceType.InstructionalObjectiveType.getName(),
                                    realObjId,
                                    RelationType.COPY.getName(),
                                    5001f,
                                    ts,
                                    creator));
                        }
                    }
                    //套件和学习目标类型的关系
                    ResourceRelation objTypeRelation = new ResourceRelation();
                    objTypeRelation.setSourceUuid(realId);
                    objTypeRelation.setResType(IndexSourceType.AssetType.getName());
                    objTypeRelation.setResourceTargetType(IndexSourceType.AssetType.getName());
                    objTypeRelation.setEnable(true);
                    objTypeRelation.setSortNum(assetService.findMaxSortNumInBatchDeal(objTypeRelation, otherSortNumMap));
                    objTypeRelation.setIdentifier(UUID.randomUUID().toString());
                    objTypeRelation.setRelationType(RelationType.COPY.getName());
                    objTypeRelation.setTarget(objTypeId);
                    objTypeRelation.setCreator(creator);
                    objTypeRelation.setCreateTime(ts);
                    createRelationList.add(objTypeRelation);

                }
            }

            //递归处理children
            if (CollectionUtils.isNotEmpty(children)) {
                returnMap.putAll(extendSuitesRecur(sampleId,
                        children,
                        createAssetList,
                        createObjectiveList,
                        createCategoryList,
                        createCoverageList,
                        createRelationList,
                        createSubInstructionList,
                        createContributeList,
                        parentSortNumMap,
                        otherSortNumMap,
                        returnMap));
            }
        }
        return returnMap;
    }

    private float dealCopySortNum(String parentId, String copySuiteId, Map<String, Float> sortNumMap) {
        float sortNum = 0;
        //按sort_num增序查出relation
        List<ResourceRelation> relationsInDB = new ArrayList<>();
        relationsInDB = resourceRelationRepository.findRelationsBySourceOrderBySortNum(parentId);
        if (CollectionUtils.isEmpty(relationsInDB) && sortNumMap.get(parentId) == null) {
            sortNumMap.put(parentId, 5000f);
            return 5000f;
        }

        //copy对象的sort_num
        float sortNumOfCopy = 0;

        //第一个比copy对象大的sort_num
        float bigSortNum = 0;
        int temp = 0;

        if (CollectionUtils.isEmpty(relationsInDB)) {
            sortNum = sortNumMap.get(parentId);
            if (sortNumMap.get(parentId) == null) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "", "sortNum出错");
            }
            sortNum = sortNum + 10;
            sortNumMap.put(parentId, sortNum);
            return sortNum;
        }

        for (ResourceRelation relation : relationsInDB) {
            if (relation.getTarget().equals(copySuiteId)) {
                sortNumOfCopy = relation.getSortNum();
                temp = relationsInDB.indexOf(relation);
            }
        }
        //防止越界
        if (temp + 1 >= relationsInDB.size()) {
            bigSortNum = sortNumOfCopy + 10;
        } else {
            bigSortNum = relationsInDB.get(temp + 1).getSortNum();
        }
        //map里有值，不是第一个拷贝
        if (CollectionUtils.isNotEmpty(sortNumMap) && sortNumMap.get(copySuiteId) != null) {
            sortNum = (sortNumMap.get(copySuiteId) + bigSortNum) / 2;
            sortNumMap.put(copySuiteId, sortNum);
        } else {
            //map里没值，是第一个拷贝
            List<ResourceRelation> copyRelations = new ArrayList<>();
            copyRelations = resourceRelationRepository.findCopyRelationsBySourceOrderByCreateTime(copySuiteId);
            if (CollectionUtils.isNotEmpty(copyRelations)) {
                ResourceRelation relation = copyRelations.get(copyRelations.size() - 1);
                ResourceRelation example = new ResourceRelation();
                example.setSourceUuid(parentId);
                example.setTarget(relation.getTarget());
                example.setEnable(true);
                example.setRelationType(RelationType.PARENT.getName());
                ResourceRelation relation1 = null;
                try {
                    relation1 = resourceRelationRepository.getByExample(example);
                } catch (EspStoreException e) {
                    e.printStackTrace();
                }
                if (relation1 != null) {
                    sortNumOfCopy = relation1.getSortNum();
                }
            }
            sortNum = (sortNumOfCopy + bigSortNum) / 2;
            sortNumMap.put(copySuiteId, sortNum);
        }
        return sortNum;
    }

    private ResourceRelation addResourceRelation(String resType,
                                                 String sourceUuid, String targetType, String target,
                                                 String relationType, float sortNum, Timestamp ts, String creator) {
        ResourceRelation rr = new ResourceRelation();
        rr.setIdentifier(UUID.randomUUID().toString());
        rr.setResType(resType);
        rr.setSourceUuid(sourceUuid);
        rr.setResourceTargetType(targetType);
        rr.setTarget(target);
        rr.setEnable(true);
        rr.setOrderNum(1);
        rr.setSortNum(sortNum);
        rr.setCreateTime(ts);
        rr.setLastUpdate(ts);
        rr.setStatus(LifecycleStatus.CREATED.getCode());
        rr.setRelationType(relationType);
        rr.setCreator(creator);
        return rr;
    }

    private void findCopySuite(List<String> list, String sampleId, String resType, String relationType) {

        String sql = "SELECT rr.target FROM resource_relations rr ,resource_categories rc WHERE rr.source_uuid = :sId and rr.enable = true and rr.relation_type = :relationType and rr.res_type = :resType and rc.resource = rr.target and rc.taxOnCode = '$RA0502' order by rr.sort_num";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<>();
        params.put("sId", sampleId);
        params.put("resType", resType);
        params.put("relationType", relationType);
        List<Map<String, Object>> list1 = new ArrayList<>();
        list1 = npjt.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("target", rs.getString("target"));
                return map;
            }
        });
        List<String> pids = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list1)) {
            for (Map<String, Object> map : list1) {
                pids.add((String) map.get("target"));
                list.add((String) map.get("target"));
            }
            for (String pid : pids) {
                findCopySuite(list, pid, IndexSourceType.AssetType.getName(), RelationType.PARENT.getName());
            }
        }
    }

    private List<ExtendSuiteViewModel> dealCopySuiteData(List<String> copySuites) {
        List<ExtendSuiteViewModel> list = new ArrayList<>();
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        String sql = "select rr.source_uuid from resource_relations rr where rr.target in (:suiteId) and rr.relation_type = 'PARENT' and rr.enable = true ";
        Map<String, Object> params = new HashMap<>();
        params.put("suiteId", copySuites);
        List<Map<String, Object>> list1 = new ArrayList<>();
        list1 = npjt.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("parent", rs.getString("source_uuid"));
                return map;
            }
        });

        Set<String> parentIds = new HashSet<>();
        for (Map<String, Object> map : list1) {
            parentIds.add((String) map.get("parent"));
        }

        List<Map<String, Object>> list2 = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(parentIds)) {
            String sql2 = "select nd.identifier,nd.title,nd.description,rr.source_uuid,nd.creator,nd.estatus,nd.version from resource_relations rr ,ndresource nd where rr.source_uuid in (:parentId) and nd.identifier = rr.target and rr.enable = true and nd.enable =true and rr.relation_type='PARENT' order by nd.create_time";
            Map<String, Object> params2 = new HashMap<>();
            params2.put("parentId", parentIds);
            list2 = npjt.query(sql2, params2, new RowMapper<Map<String, Object>>() {
                @Override
                public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Map<String, Object> map = new HashMap<>();
                    map.put("identifier", rs.getString("identifier"));
                    map.put("title", rs.getString("title"));
                    map.put("description", rs.getString("description"));
                    map.put("parent", rs.getString("source_uuid"));
                    map.put("creator", rs.getString("creator"));
                    map.put("status", rs.getString("estatus"));
                    map.put("version", rs.getString("version"));
                    return map;
                }
            });
        }

        for (Map<String, Object> map : list2) {
            if (copySuites.contains((String) map.get("identifier"))) {
                ExtendSuiteViewModel model = new ExtendSuiteViewModel();
                model.setIdentifier((String) map.get("identifier"));
                model.setTitle((String) map.get("title"));
                model.setDescription((String) map.get("description"));
                model.setParentId((String) map.get("parent"));
                Map<String, Object> lifeCycle = new HashMap<>();
                lifeCycle.put("creator", (String) map.get("creator"));
                lifeCycle.put("status", (String) map.get("status"));
                lifeCycle.put("version", (String) map.get("version"));
                lifeCycle.put("enable", true);
                model.setLifeCycle(lifeCycle);

                list.add(model);
            }
        }
        return list;
    }

    private List<Map<String, Object>> findObjectiveTypesBySuiteId(String suiteId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "select nd.identifier,nd.title,nd.description,nd.custom_properties from (select rc.resource,r.sort_num from resource_categories rc, resource_relations r  where rc.resource = r.target and r.source_uuid = :suiteId and rc.taxoncode='$RA0503' and r.enable = true) as t1, ndresource nd where nd.identifier = t1.resource and nd.enable =true order by t1.sort_num ";
        Map<String, Object> params = new HashMap<>();
        params.put("suiteId", suiteId);
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        list = npjt.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("identifier", rs.getString("identifier"));
                map.put("title", rs.getString("title"));
                map.put("description", rs.getString("description"));
                map.put("custom_properties", rs.getString("custom_properties"));
                return map;
            }
        });
        return list;
    }

    private List<Map<String, Object>> findKnCategoryById(String objTypeId) {
        String sql = "SELECT taxOnCode,taxOnName FROM resource_categories WHERE resource = :objTypeId and category_code='KC'";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<>();
        params.put("objTypeId", objTypeId);
        List<Map<String, Object>> list = new ArrayList<>();
        list = npjt.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("taxOnCode", rs.getString("taxOnCode"));
                map.put("taxOnName", rs.getString("taxOnName"));
                return map;
            }
        });
        return list;
    }

    private List<String> dealKnowledge(String knId, List<String> objIds, List<KnViewModel> knowledges, int position) {
        Chapter knowledge = null;
        try {
            knowledge = chapterRepository.get(knId);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        if (knowledge == null) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "KNOWLEDGE NOT EXIST", "知识点查询失败");
        }
        objIds = resourceRelationRepository.findBySourceIdAndResTypeAndTargetType(knId, IndexSourceType.KnowledgeType.getName(), IndexSourceType.InstructionalObjectiveType.getName(), RelationType.COPY.getName());

        KnViewModel model = new KnViewModel();
        model.setIdentifier(knowledge.getIdentifier());
        model.setTitle(knowledge.getTitle());
        model.setPosition(position);
        knowledges.add(model);

        return objIds;
    }

    private List<Map<String, Object>> findVersions(String objId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT nd.identifier,nd.description FROM ndresource nd ,resource_relations rr where rr.source_uuid = :objId and nd.identifier = rr.target and rr.enable = true and nd.enable = true and rr.resource_target_type = 'subInstruction' order by nd.create_time";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<>();
        params.put("objId", objId);
        list = npjt.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("identifier", rs.getString("identifier"));
                map.put("description", rs.getString("description"));
                return map;
            }
        });
        return list;
    }

    private List<String> findApplicablePeriod(String objId) {
        List<String> applicablePeriod = new ArrayList<>();
        ResourceCategory example = new ResourceCategory();
        example.setResource(objId);
        example.setPrimaryCategory(IndexSourceType.InstructionalObjectiveType.getName());
        example.setCategoryCode("SL");
        try {
            List<ResourceCategory> list = categoryRepository.getAllByExample(example);
            if (CollectionUtils.isNotEmpty(list)) {
                for (ResourceCategory category : list) {
                    applicablePeriod.add(category.getTaxoncode());
                }
            }
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        return applicablePeriod;
    }

    private void repairDataRecur(String sampleId,
                                 List<ExtendSuiteViewModel> extendSuiteViewModels,
                                 List<ResourceRelation> createRelationList,
                                 List<Asset> createAssetList,
                                 List<ResourceCategory> createCategoryList,
                                 List<ResCoverage> createCoverageList,
                                 Map<String, String> returnMap,
                                 Map<String, Float> parentSortNumMap,
                                 Map<String, Float> otherSortNumMap
    ) {
        Sample sample = null;
        try {
            sample = sampleRepository.get(sampleId);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        String kn1 = sample.getKnowledgeId1();
        String kn2 = sample.getKnowledgeId2();
        String kn3 = sample.getKnowledgeId3();
        String kn4 = sample.getKnowledgeId4();
        String kn5 = sample.getKnowledgeId5();

        for (ExtendSuiteViewModel model : extendSuiteViewModels) {

            Timestamp ts = new Timestamp(System.currentTimeMillis());

            //将虚拟id：实际id放入返回值
            String realId = UUID.randomUUID().toString();
            returnMap.put(model.getIdentifier(), realId);

            //取出传入的各个属性的值
            String title = model.getTitle();
            String description = model.getDescription();
            String language = model.getLanguage();
            String identifier = realId;
            List<ObjTypeViewModel> objTypeViewModels = model.getObjectiveTypes();
            List<ExtendSuiteViewModel> children = model.getChildren();
            String copySuiteId = model.getCopySuiteId();
            String parentId = model.getParentId();

            //children中parent虚拟id处理
            if (StringUtils.isNotEmpty(returnMap.get(parentId))) {
                parentId = returnMap.get(parentId);
            }
            //children中copy虚拟id处理
            if (StringUtils.isNotEmpty(returnMap.get(copySuiteId))) {
                copySuiteId = returnMap.get(copySuiteId);
            }

            //处理拷贝的套件
            Asset asset = new Asset();
            asset.setIdentifier(realId);
            asset.setTitle(title);
            asset.setDescription(description);
            asset.setCreateTime(ts);
            asset.setLastUpdate(ts);
            asset.setEnable(true);
            asset.setStatus(LifecycleStatus.CREATED.getCode());
            asset.setVersion("v0.1");
            asset.setLanguage(language);
            asset.setCreator(null);
            createAssetList.add(asset);

            ResourceCategory category1 = new ResourceCategory();
            category1.setIdentifier(UUID.randomUUID().toString());
            category1.setResource(realId);
            category1.setTaxoncode("$RA0502");
            category1.setTaxonname("套件模板素材");
            category1.setTaxoncodeid("1c2f86ac-dd23-45c9-bfa8-a53e592f6ee6");
            category1.setCategoryCode("$R");
            category1.setCategoryName("resourcetype");
            category1.setPrimaryCategory(IndexSourceType.AssetType.getName());
            category1.setShortName("objectivesets");
            createCategoryList.add(category1);

            ResCoverage rcv = new ResCoverage();
            rcv.setIdentifier(UUID.randomUUID().toString());
            rcv.setResource(realId);
            rcv.setResType(IndexSourceType.AssetType.getName());
            rcv.setTarget("nd");
            rcv.setTargetType("Org");
            rcv.setStrategy("OWNER");
            rcv.setTargetTitle("copyAssets4Business");
            createCoverageList.add(rcv);

            //修复与parent的关系类型
            ResourceRelation parentRelation = new ResourceRelation();
            parentRelation.setIdentifier(UUID.randomUUID().toString());
            parentRelation.setSourceUuid(parentId);
            parentRelation.setTarget(identifier);
            parentRelation.setRelationType(RelationType.PARENT.getName());
            parentRelation.setResType(IndexSourceType.AssetType.getName());
            parentRelation.setResourceTargetType(IndexSourceType.AssetType.getName());
            parentRelation.setSortNum(dealCopySortNum(parentId, copySuiteId, parentSortNumMap));
            parentRelation.setCreateTime(ts);
            parentRelation.setEnable(true);
            createRelationList.add(parentRelation);

            //copySuite的关系
            ResourceRelation copySuiteRelation = new ResourceRelation();
            copySuiteRelation.setIdentifier(UUID.randomUUID().toString());
            copySuiteRelation.setSourceUuid(copySuiteId);
            copySuiteRelation.setTarget(identifier);
            copySuiteRelation.setRelationType(RelationType.COPY.getName());
            copySuiteRelation.setResType(IndexSourceType.AssetType.getName());
            copySuiteRelation.setResourceTargetType(IndexSourceType.AssetType.getName());
            copySuiteRelation.setSortNum(5000f);
            copySuiteRelation.setCreateTime(ts);
            copySuiteRelation.setEnable(true);
            createRelationList.add(copySuiteRelation);

            //修复与学习目标类型的关系类型
            if (CollectionUtils.isNotEmpty(objTypeViewModels)) {
                for (ObjTypeViewModel objTypeViewModel : objTypeViewModels) {
                    String objTypeId = objTypeViewModel.getIdentifier();
                    ResourceRelation typeRelation = new ResourceRelation();
                    typeRelation.setSourceUuid(identifier);
                    typeRelation.setResType(IndexSourceType.AssetType.getName());
                    typeRelation.setResourceTargetType(IndexSourceType.AssetType.getName());
                    typeRelation.setSortNum(assetService.findMaxSortNumInBatchDeal(typeRelation, otherSortNumMap));
                    typeRelation.setRelationType(RelationType.COPY.getName());
                    typeRelation.setTarget(objTypeId);
                    typeRelation.setIdentifier(UUID.randomUUID().toString());
                    typeRelation.setEnable(true);
                    createRelationList.add(typeRelation);

                    List<ObjViewModel> objViewModels = objTypeViewModel.getObjective();
                    if (CollectionUtils.isNotEmpty(objViewModels)) {
                        for (ObjViewModel objViewModel : objViewModels) {
                            ResourceRelation example3 = new ResourceRelation();
                            example3.setRelationType(RelationType.ASSOCIATE.getName());
                            example3.setSourceUuid(objTypeId);
                            example3.setTarget(objViewModel.getIdentifier());
                            try {
                                ResourceRelation objRelation = resourceRelationRepository.getByExample(example3);
                                if (objRelation != null) {
                                    objRelation.setRelationType(RelationType.COPY.getName());
                                    createRelationList.add(objRelation);
                                    //学习目标和套件建关系
                                    createRelationList.add(addResourceRelation(IndexSourceType.AssetType.getName(),
                                            identifier,
                                            IndexSourceType.InstructionalObjectiveType.getName(),
                                            objViewModel.getIdentifier(),
                                            RelationType.COPY.getName(),
                                            5001f,
                                            ts,
                                            null));
                                }
                                //学习目标与知识点关系更新
                                if (StringUtils.isNotEmpty(kn1)) {
                                    ResourceRelation example = new ResourceRelation();
                                    example.setSourceUuid(kn1);
                                    example.setTarget(objViewModel.getIdentifier());
                                    example.setResType(IndexSourceType.KnowledgeType.getName());
                                    example.setEnable(true);
                                    ResourceRelation kn1Relation = resourceRelationRepository.getByExample(example);
                                    if (kn1Relation != null) {
                                        kn1Relation.setRelationType(RelationType.COPY.getName());
                                        createRelationList.add(kn1Relation);
                                    }
                                }
                                if (StringUtils.isNotEmpty(kn2)) {
                                    ResourceRelation example = new ResourceRelation();
                                    example.setSourceUuid(kn2);
                                    example.setTarget(objViewModel.getIdentifier());
                                    example.setResType(IndexSourceType.KnowledgeType.getName());
                                    example.setEnable(true);
                                    ResourceRelation kn2Relation = resourceRelationRepository.getByExample(example);
                                    if (kn2Relation != null) {
                                        kn2Relation.setRelationType(RelationType.COPY.getName());
                                        createRelationList.add(kn2Relation);
                                    }
                                }
                                if (StringUtils.isNotEmpty(kn3)) {
                                    ResourceRelation example = new ResourceRelation();
                                    example.setSourceUuid(kn3);
                                    example.setTarget(objViewModel.getIdentifier());
                                    example.setResType(IndexSourceType.KnowledgeType.getName());
                                    example.setEnable(true);
                                    ResourceRelation kn3Relation = resourceRelationRepository.getByExample(example);
                                    if (kn3Relation != null) {
                                        kn3Relation.setRelationType(RelationType.COPY.getName());
                                        createRelationList.add(kn3Relation);
                                    }
                                }
                                if (StringUtils.isNotEmpty(kn4)) {
                                    ResourceRelation example = new ResourceRelation();
                                    example.setSourceUuid(kn4);
                                    example.setTarget(objViewModel.getIdentifier());
                                    example.setResType(IndexSourceType.KnowledgeType.getName());
                                    example.setEnable(true);
                                    ResourceRelation kn4Relation = resourceRelationRepository.getByExample(example);
                                    if (kn4Relation != null) {
                                        kn4Relation.setRelationType(RelationType.COPY.getName());
                                        createRelationList.add(kn4Relation);
                                    }
                                }
                                if (StringUtils.isNotEmpty(kn5)) {
                                    ResourceRelation example = new ResourceRelation();
                                    example.setSourceUuid(kn5);
                                    example.setTarget(objViewModel.getIdentifier());
                                    example.setResType(IndexSourceType.KnowledgeType.getName());
                                    example.setEnable(true);
                                    ResourceRelation kn5Relation = resourceRelationRepository.getByExample(example);
                                    if (kn5Relation != null) {
                                        kn5Relation.setRelationType(RelationType.COPY.getName());
                                        createRelationList.add(kn5Relation);
                                    }
                                }
                            } catch (EspStoreException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(children)) {
                repairDataRecur(sampleId, children, createRelationList, createAssetList, createCategoryList, createCoverageList, returnMap, parentSortNumMap, otherSortNumMap);
            }
        }
    }

    public void recursiveSuiteDirectory(List<Map<String, Object>> list,
                                        String assetId, int num) {
        if (num > 10) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "LC/RECURSE_OUT", "递归层数过多，已超过10层");
        }
        num++;

        List<String> pids = new ArrayList<String>();
        pids.add(assetId);
        List<Map<String, Object>> tmpList = querySuiteChildKnByParentId(pids);
        if (CollectionUtils.isNotEmpty(tmpList)) {
            list.addAll(tmpList);
            for (Map<String, Object> map : tmpList) {
                String identifier = (String) map.get("identifier");
                recursiveSuiteDirectory(list, identifier, num);
            }
        }
    }

    // 提高效率
    private void recursiveSuiteDirectory(List<Map<String, Object>> list,
                                         List<String> pids, int num) {
        if (num > 10) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "LC/RECURSE_OUT", "递归层数过多，已超过10层");
        }
        num++;

        List<Map<String, Object>> tmpList = querySuiteChildKnByParentId(pids);
        if (CollectionUtils.isNotEmpty(tmpList)) {
            list.addAll(tmpList);
            List<String> tmpIds = new ArrayList<String>();
            for (Map<String, Object> map : tmpList) {
                String identifier = (String) map.get("identifier");
                tmpIds.add(identifier);
            }
            recursiveSuiteDirectory(list, tmpIds, num);
        }
    }

    private List<Map<String, Object>> querySuiteChildKnByParentId(
            List<String> pids) {
        String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,rr.source_uuid as parent,rr.identifier as relation_id from resource_relations rr,ndresource nd,resource_categories rc where rr.res_type='assets' and rr.resource_target_type='assets' and rr.relation_type = 'PARENT' and rr.enable = 1 and nd.enable = 1 and rr.source_uuid in (:pids) and rr.target = nd.identifier and  nd.identifier = rc.resource and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' order by rr.sort_num";

        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("pids", pids);

        List<Map<String, Object>> list = npjt.query(sql, params,
                new RowMapper<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        Map<String, Object> m = new HashMap<String, Object>();
                        m.put("identifier", rs.getString("identifier"));
                        m.put("title", rs.getString("title"));
                        m.put("description", rs.getString("description"));
                        m.put("status", rs.getString("status"));
                        m.put("parent", rs.getString("parent"));
                        m.put("relation_id", rs.getString("relation_id"));
                        return m;
                    }
                });
        return list;
    }
}
