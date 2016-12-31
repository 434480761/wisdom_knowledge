package nd.esp.service.lifecycle.services.assets.v06.impls;

import nd.esp.service.lifecycle.controllers.CategoryControllerV06;
import nd.esp.service.lifecycle.daos.assets.v06.AssetDao;
import nd.esp.service.lifecycle.educommon.models.ResClassificationModel;
import nd.esp.service.lifecycle.educommon.services.NDResourceService;
import nd.esp.service.lifecycle.educommon.services.impl.CommonServiceHelper;
import nd.esp.service.lifecycle.educommon.support.RelationType;
import nd.esp.service.lifecycle.models.v06.AssetModel;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.*;
import nd.esp.service.lifecycle.repository.sdk.*;
import nd.esp.service.lifecycle.services.CategoryService;
import nd.esp.service.lifecycle.services.assets.v06.AssetServiceV06;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceV06;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.busi.CommonHelper;
import nd.esp.service.lifecycle.support.enums.LifecycleStatus;
import nd.esp.service.lifecycle.support.enums.ResourceNdCode;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.utils.gson.ObjectUtils;
import nd.esp.service.lifecycle.vos.CategoryDataApplyForNdCodeViewModel;
import nd.esp.service.lifecycle.vos.business.Suite4BusinessViewModel;
import nd.esp.service.lifecycle.vos.business.assets.KnowledgeCategoriesViewModel;
import nd.esp.service.lifecycle.vos.business.assets.ObjectiveTypeAndSuiteViewModel;
import nd.esp.service.lifecycle.vos.business.assets.ObjectiveTypeBussinesViewModel;
import nd.esp.service.lifecycle.vos.business.assets.ObjectiveTypeViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

/**
 * 业务实现类
 *
 * @author xuzy
 */
@Service("assetServiceV06")
@Transactional
public class AssetServiceImplV06 implements AssetServiceV06 {
    @Autowired
    private NDResourceService ndResourceService;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    @Qualifier("CategoryServiceImpl")
    private CategoryService categoryService;
    @Autowired
    private AssetDao assetDao;
    @Autowired
    private ResourceCategoryRepository resourceCategoryRepository;
    @Autowired
    private ResourceRelationRepository resourceRelationRepository;
    @Autowired
    private ResCoverageRepository resCoverageRepository;
    @Autowired
    private CategoryControllerV06 cc;
    @Autowired
    private ContributeRepository contributeRepository;
    @Autowired
    private CategoryDataRepository categoryDataRepository;
    @Autowired
    private HttpServletRequest request;
    private final static ExecutorService executorService = CommonHelper
            .getPrimaryExecutorService();
    @Autowired
    private CommonServiceHelper commonServiceHelper;
    @Autowired
    private EducationRelationServiceV06 educationRelationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public AssetModel createAsset(AssetModel am) {
        if ("auto_increment".equals(am.getTitle())) {
            Pattern suitePattern = Pattern.compile("^套件[0-9]*$");
            Pattern subSuitePattern = Pattern.compile("^套件[0-9.]*$");

            String cp = am.getCustomProperties();
            Map<String, Object> map = ObjectUtils.fromJson(cp, Map.class);
            String category = (String) map.get("category");
            String parent = (String) map.get("parent");
            if ("$RA0502".equals(category)) {
            	synchronized ("suite") {
                    //套件目录
                    //1、查找出所有的套件目录
                    List<Asset> assetList = assetDao.queryByCategory(category);
                    //2、判断是否存在重复
                    String description = am.getDescription();
                    if (description == null) {
                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "LC/CHECK_PARAM_VALID_FAIL",
                                "description不能为空");
                    }

                    //有变化，兄弟结点不允许重复，其它是可以重复  modify by xuzy 20161103
//    				if(CollectionUtils.isNotEmpty(assetList)){
//    					for (Asset asset : assetList) {
//    						String des = asset.getDescription();
//    						if(des != null && des.equals(description)){
//    							throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
//    									 "LC/CHECK_PARAM_VALID_FAIL",
//    		                             "description已存在");
//    						}
//    					}
//    				}

                    //3、根据parent获取所有的子节点
                    if (parent != null) {
                        Asset parentAsset = null;
                        //4、根据子节点算出新的编号
                        List<String> titles = new ArrayList<String>();
                        if (parent.toLowerCase().equals("root")) {
                            //自动创建维度数据
                            CategoryData cd = getCategoryDataByName(am.getDescription());
                            CategoryData cd2 = null;
                            if (cd == null) {
                                cd2 = createCategoryData(am.getDescription());
                                try {
                                    categoryDataRepository.add(cd2);
                                } catch (EspStoreException e) {
                                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                                            e.getMessage());
                                }
                            }
                            if (cd != null || cd2 != null) {
                                List<ResClassificationModel> cList = am.getCategoryList();
                                if (cList == null) {
                                    cList = new ArrayList<ResClassificationModel>();
                                }
                                ResClassificationModel tmp = new ResClassificationModel();
                                tmp.setResourceId(am.getIdentifier());
                                if (cd != null) {
                                    tmp.setTaxoncode(cd.getNdCode());
                                } else if (cd2 != null) {
                                    tmp.setTaxoncode(cd2.getNdCode());
                                }
                                cList.add(tmp);
                                am.setCategoryList(cList);
                            }

                            for (Asset asset : assetList) {
                                if (StringUtils.isNotEmpty(asset.getTitle()) && suitePattern.matcher(asset.getTitle()).find()) {
                                    titles.add(asset.getTitle());
                                }
                            }
                        } else {
                            //判断资源是否存在
                            try {
                                parentAsset = assetRepository.get(parent);
                                if (parentAsset == null || !parentAsset.getEnable()) {
                                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                            "LC/CHECK_PARAM_VALID_FAIL",
                                            "parent对应的资源不存在");
                                }
                            } catch (EspStoreException e) {
                                e.printStackTrace();
                            }

                            List<Asset> list2 = assetDao.queryBySourceId(parent, category);
                            if (CollectionUtils.isNotEmpty(list2)) {
                                for (Asset asset : list2) {
                                    //有变化，兄弟结点不允许重复，其它是可以重复  modify by xuzy 20161103
                                    String des = asset.getDescription();
                                    if (des != null && des.equals(description)) {
                                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "LC/CHECK_PARAM_VALID_FAIL",
                                                "description已存在");
                                    }

                                    if (StringUtils.isNotEmpty(asset.getTitle()) && subSuitePattern.matcher(asset.getTitle()).find()) {
                                        titles.add(asset.getTitle());
                                    }
                                }
                            }
                        }
                        String s = generateSuiteTitle(parentAsset, titles);
                        am.setTitle(s);
                    }
                    return (AssetModel) ndResourceService.create(ResourceNdCode.assets.toString(), am);
				}
            } else if ("$RA0503".equals(category)) {
                //教学目标类型（套件）
                //1、判断教学目标类型是否重复
                List<Asset> assetList = assetDao.queryByCategory(category);
                String description = am.getDescription();
                if (description == null) {
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "LC/CHECK_PARAM_VALID_FAIL",
                            "description不能为空");
                }
                if (CollectionUtils.isNotEmpty(assetList)) {
                    for (Asset asset : assetList) {
                        String des = asset.getDescription();
                        if (des != null && des.equals(description)) {
                            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "LC/CHECK_PARAM_VALID_FAIL",
                                    "description已存在");
                        }
                    }
                }
                //2、算code
                Asset parentAsset = null;
                //判断资源是否存在
                try {
                    parentAsset = assetRepository.get(parent);
                    if (parentAsset == null || !parentAsset.getEnable()) {
                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "LC/CHECK_PARAM_VALID_FAIL",
                                "parent对应的资源不存在");
                    }
                } catch (EspStoreException e) {
                    e.printStackTrace();
                }
                String title = null;
                if (parentAsset != null && parentAsset.getTitle() != null) {
                    title = parentAsset.getTitle().substring(2);
                }
                try {
                    int likeName = 0;
                    if (title != null && title.contains(".")) {
                        title = title.substring(0, title.indexOf("."));
                        likeName = Integer.valueOf(title);
                    }
                    List<Asset> list2 = assetDao.queryInsTypesByCategory(String.valueOf(likeName), category);
                    if (CollectionUtils.isNotEmpty(list2)) {
                        int maxNum = 0;
                        Pattern p = Pattern.compile("^[0-9]+$");
                        for (Asset asset : list2) {
                            if (p.matcher(asset.getTitle()).matches()) {
                                if (Integer.valueOf(asset.getTitle()) > maxNum) {
                                    maxNum = Integer.valueOf(asset.getTitle());
                                }
                            }
                        }
                        if (maxNum > 0) {
                            am.setTitle(String.valueOf(maxNum + 1));
                        } else {
                            am.setTitle(String.valueOf(likeName) + "01");
                        }
                    } else {
                        am.setTitle(String.valueOf(likeName) + "01");
                    }
                } catch (NumberFormatException e) {
                    am.setTitle(String.valueOf(System.currentTimeMillis()));
                }
            }
        }
        return (AssetModel) ndResourceService.create(ResourceNdCode.assets.toString(), am);
    }

    @Override
    public AssetModel updateAsset(AssetModel am) {
        return (AssetModel) ndResourceService.update(ResourceNdCode.assets.toString(), am);
    }

    private String generateSuiteTitle(Asset parentAsset, List<String> titleList) {
        if (CollectionUtils.isNotEmpty(titleList)) {
            String max = titleList.get(0);
            if (max.length() > 2) {
                if (max.contains(".")) {
                    String s = max.substring(max.lastIndexOf(".") + 1);
                    try {
                        int i = Integer.valueOf(s);
                        return max.substring(0, max.lastIndexOf(".") + 1) + (i + 1);
                    } catch (NumberFormatException e) {
                    }
                } else {
                    String s = max.substring(2);
                    try {
                        int i = Integer.valueOf(s);
                        return "套件" + (i + 1);
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } else {
            if (parentAsset != null) {
                String title = parentAsset.getTitle();
                if (title != null && title.length() > 2) {
                    return title + ".1";
                } else {
                    return "套件-1";
                }
            } else {
                return "套件1";
            }
        }
        return "套件-0";
    }

    private String dealTitleWhenParentNew(Asset parentAsset, int number) {
        if (parentAsset == null) {
            return null;
        }
        String title = parentAsset.getTitle();
        if (title != null && title.length() > 2) {
            return title + "." + number;
        }
        return null;
    }

    @Override
    public AssetModel patchAsset(AssetModel am) {
        return (AssetModel) ndResourceService.patch(ResourceNdCode.assets.toString(), am);
    }

    @Override
    public void create4Business(ObjectiveTypeBussinesViewModel otbViewModel) {
        //检查套件是否存在
//		checkBussinessResourceExist(otbViewModel.getSuitId(),true);
        //检查资源状态是否是有效值
        checkStatus(otbViewModel.getObjectiveTypes());

        dealObjectiveTypes(otbViewModel.getUserId(), otbViewModel.getObjectiveTypes());
    }

    @Override
    public List<ObjectiveTypeViewModel> batchCreate4Business(ObjectiveTypeAndSuiteViewModel objectiveTypeAndSuiteViewModel, String userId) {

        List<Suite4BusinessViewModel> suite4BusinessViewModels = objectiveTypeAndSuiteViewModel.getSuite();
        List<String> suiteList = new ArrayList<>();//校验用的suitelist
        if (CollectionUtils.isNotEmpty(suite4BusinessViewModels)) {
            for (Suite4BusinessViewModel model : suite4BusinessViewModels) {
                if (model.getOperate_type() == 1) {
                    suiteList.add(model.getIdentifier());
                }
            }
            if (CollectionUtils.isNotEmpty(suiteList)) {
                for (String id : suiteList) {
                    try {
                        Asset asset = assetRepository.get(id);
                        if (asset == null) {
                            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.AssetNotFound.getCode(), "修改的素材id不存在");
                        }
                    } catch (EspStoreException e) {
                        e.printStackTrace();
                    }
                }
            }
            dealSuiteBatch(suite4BusinessViewModels, objectiveTypeAndSuiteViewModel.getRootSuite(), userId);
        }
//        //数据库中已有的suitelist，根据这个判断是否自动创建学习目标，因为suite可能不传，需要去数据库找
//        List<String> suiteListDb = new ArrayList<>();
        List<ObjectiveTypeViewModel> objectiveTypeViewModels = objectiveTypeAndSuiteViewModel.getObjectiveTypes();
//        if (CollectionUtils.isNotEmpty(objectiveTypeViewModels)) {
//            for (ObjectiveTypeViewModel model : objectiveTypeViewModels) {
//                try {
//                    Asset asset = assetRepository.get(model.getSuiteId());
//                    if (asset != null) {
//                        suiteListDb.add(model.getSuiteId());
//                    }
//                } catch (EspStoreException e) {
//                    e.printStackTrace();
//                }
//            }
        return dealObjectiveTypeBatch(objectiveTypeViewModels, userId);
    }

    @Override
    public List<String> updateSortNum(List<String> rootSuites, float repairedNum) {
        //TODO 将所有套件下的学习目标类型的sort_num进行排序，从5000开始
        Map<String, Object> param = new HashMap<>();
        String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id from ndresource nd,resource_categories rc where nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' and rc.resource=nd.identifier and nd.enable = 1 and not exists (SELECT identifier from resource_relations where res_type='assets' and resource_target_type = 'assets' and target = nd.identifier) order by nd.create_time";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jdbcTemplate);
        List<Map<String, Object>> list = npjt.queryForList(sql, param);
        List<String> rootSuiteIds = new ArrayList<>();
        for (Map<String, Object> map : list) {
            rootSuiteIds.add((String) map.get("identifier"));
        }
        if (CollectionUtils.isNotEmpty(rootSuites)) {
            rootSuiteIds = rootSuites;
        }
        if (CollectionUtils.isEmpty(rootSuiteIds)) {
            return null;
        }
        List<String> repairedList = new ArrayList<>();
        //每个根套件都遍历找到所有子套件，再处理每个子套件的学习目标类型
        for (String rootSuiteId : rootSuiteIds) {
            List<ResourceRelation> addRelations = new ArrayList<>();
            List<Map<String, Object>> subList = new ArrayList<>();
            educationRelationService.recursiveSuiteDirectory(subList, rootSuiteId, 0);
            //把父套件也加入list里面
            Map<String, Object> rootmap = new HashMap<String, Object>();
            rootmap.put("identifier", rootSuiteId);
            subList.add(rootmap);
            //遍历list处理每个套件的学习目标类型
            for (Map<String, Object> map : subList) {
                String suiteId = (String) map.get("identifier");
                if (StringUtils.isNotEmpty(suiteId)) {
                    dealObjectiveTypeSortNum(suiteId, addRelations, repairedNum);
                }
                repairedList.add((String) map.get("identifier"));
            }
            if (CollectionUtils.isNotEmpty(addRelations)) {
                try {
                    resourceRelationRepository.batchAdd(addRelations);
                } catch (EspStoreException e) {
                    e.printStackTrace();
                }
            }
        }
        return repairedList;
    }

    private void dealObjectiveTypeSortNum(String suiteId, List<ResourceRelation> addRelations, float repairedNum) {
        ResourceRelation relation = new ResourceRelation();
        relation.setRelationType(RelationType.ASSOCIATE.getName());
        relation.setResType(IndexSourceType.AssetType.getName());
        relation.setResourceTargetType(IndexSourceType.AssetType.getName());
        relation.setSourceUuid(suiteId);
        List<ResourceRelation> relations = new ArrayList<>();
        try {
            relations = resourceRelationRepository.getAllByExample(relation);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        List<String> targetIds = new ArrayList<>();
        for (ResourceRelation resourceRelation : relations) {
            targetIds.add(resourceRelation.getTarget());
        }
        float max = 0;
        if (CollectionUtils.isNotEmpty(targetIds)) {
            List<Map<String, Object>> sortNumMap = findSortNumOrderByCreateTimeByTargetId(targetIds);
            max = findMaxSortNumInList(sortNumMap, max, repairedNum) + 10;

            int num = 0;
            for (Map<String, Object> map : sortNumMap) {
                if ((float) map.get("sort_num") == repairedNum) {
                    num++;
                }
            }

            for (Map<String, Object> map : sortNumMap) {
                if (((float) map.get("sort_num") == repairedNum && num > 1) || (float) map.get("sort_num") == 0.0) {
                    map.put("sort_num", max);
                    max = max + 10;
                }
                for (ResourceRelation relation1 : relations) {
                    if (relation1.getIdentifier().equals(map.get("identifier"))) {
                        relation1.setSortNum((Float) map.get("sort_num"));
                        addRelations.add(relation1);
                    }
                }
            }
        }
    }

    //查出sort_num并按照创建时间递增排序
    private List<Map<String, Object>> findSortNumOrderByCreateTimeByTargetId(List<String> targetIds) {
        String cSql = "select rr.sort_num,rr.identifier from resource_relations rr,ndresource nr where rr.target = nr.identifier and rr.enable = true and nr.enable = true and rr.target in (:ids) ORDER BY nr.create_time";
        List<Map<String, Object>> result = new ArrayList<>();

        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jdbcTemplate);
        Map<String, List<String>> args = new HashMap<>();
        args.put("ids", targetIds);
        result = npjt.query(cSql, args, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new LinkedHashMap<>();
                float sortNum = rs.getFloat("sort_num");
                String identifier = rs.getString("identifier");
                map.put("identifier", identifier);
                map.put("sort_num", sortNum);
                return map;
            }
        });
        return result;
    }

    private float findMaxSortNumInList(List<Map<String, Object>> list, float max, float repairedNum) {
        boolean maxFlag = false;
        for (Map<String, Object> map : list) {
            if ((float) map.get("sort_num") != repairedNum) {
                maxFlag = true;
            }
        }
        if (!maxFlag) {
            max = repairedNum - 10;
            return max;
        }
        for (Map<String, Object> map : list) {
            if ((float) map.get("sort_num") != repairedNum) {
                max = (float) map.get("sort_num") > max ? (float) map.get("sort_num") : max;
            }
        }
        return max;
    }

    private void dealSuiteBatch(List<Suite4BusinessViewModel> suiteList, String rootSuite, String userId) {
//        Pattern suitePattern = Pattern.compile("^套件[0-9]*$");
        Pattern subSuitePattern = Pattern.compile("^套件[0-9.]*$");
        List<Asset> assets = new ArrayList<>();
        List<ResourceCategory> categories = new ArrayList<>();
        List<ResourceRelation> relations = new ArrayList<>();
        List<ResCoverage> coverages = new ArrayList<>();
        List<String> createIds = new ArrayList<>();
        Map<String, List<Suite4BusinessViewModel>> parentMap = new HashMap<>();//保存父套件id和子套件列表
        Map<String, Float> sortNumMap = new HashMap<>();
        for (Suite4BusinessViewModel model : suiteList) {
            List<Suite4BusinessViewModel> sonList = new ArrayList<>();//保存子套件列表
            if (CollectionUtils.isEmpty(parentMap.get(model.getParent()))) {
                sonList.add(model);
                parentMap.put(model.getParent(), sonList);
            } else {
                sonList = parentMap.get(model.getParent());
                sonList.add(model);
            }
            createIds.add(model.getIdentifier());
        }
        Map<String, List<String>> titleMap = new HashMap<>();
        Map<String, String> titleMapSon = new HashMap<>();
        //创建子套件和孙套件
        for (Suite4BusinessViewModel sovm : suiteList) {
            Asset asset = new Asset();
            Asset parentAsset = null;
            //判断资源是否存在
            try {
                parentAsset = assetRepository.get(sovm.getParent());
                if ((parentAsset == null || !parentAsset.getEnable()) && (!createIds.contains(sovm.getParent()))) {
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "LC/CHECK_PARAM_VALID_FAIL",
                            "parent对应的资源不存在");
                }
            } catch (EspStoreException e) {
                e.printStackTrace();
            }
            List<String> titles = new ArrayList<String>();
            List<Asset> list2 = assetDao.queryBySourceId(sovm.getParent(), "$RA0502");
            if (CollectionUtils.isNotEmpty(list2)) {
                for (Asset asset1 : list2) {
                    //有变化，兄弟结点不允许重复，其它是可以重复  modify by xuzy 20161103
                    String des = asset1.getDescription();
                    if (des != null && des.equals(sovm.getDescription())) {
                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "LC/CHECK_PARAM_VALID_FAIL",
                                "description已存在: " + des);
                    }
                    if (subSuitePattern.matcher(asset1.getTitle()).find()) {
                        titles.add(asset1.getTitle());
                    }
                }
                if (CollectionUtils.isEmpty(titleMap.get(sovm.getParent()))) {
                    titleMap.put(sovm.getParent(), titles);
                }
                if (parentAsset != null) {
                    String s = generateSuiteTitle(parentAsset, titleMap.get(sovm.getParent()));
                    if (StringUtils.isNotEmpty(s)) {
                        asset.setTitle(s);
                        titleMap.get(sovm.getParent()).add(0, s);
                    }
                }
            } else {
                //确保新建子套件下的孙套件的description不重复
                List<Suite4BusinessViewModel> list3 = parentMap.get(sovm.getParent());

                if (CollectionUtils.isNotEmpty(list3)) {
                    for (Suite4BusinessViewModel model : list3) {
                        //有变化，兄弟结点不允许重复，其它是可以重复  modify by xuzy 20161103
                        String des = model.getDescription();
                        if (des != null && des.equals(sovm.getDescription()) && (!model.equals(sovm))) {
                            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "LC/CHECK_PARAM_VALID_FAIL",
                                    "description已存在: " + des);
                        }
                    }
                    int i = 1;
                    //将新增子套件的孙套件的title都设置好
                    for (Asset asset1 : assets) {
                        if (sovm.getParent().equals(asset1.getIdentifier())) {
                            parentAsset = asset1;
                        }
                    }
                    for (Suite4BusinessViewModel model : list3) {

                        if (StringUtils.isEmpty(model.getTitle()) || model.getTitle().equals("auto_increment")) {
                            model.setTitle(dealTitleWhenParentNew(parentAsset, i));
                            titleMapSon.put(model.getIdentifier(), model.getTitle());
                            i++;
                        }
                    }
                    asset.setTitle(titleMapSon.get(sovm.getIdentifier()));
                }
            }
//            asset.setTitle(generateSuiteTitle(parentAsset, titles));
            asset.setDescription(sovm.getDescription());
            asset.setIdentifier(sovm.getIdentifier());
            asset.setStatus(sovm.getStatus());
            asset.setCustomProperties(ObjectUtils.toJson(sovm.getCustom_properties()));
            Timestamp t = new Timestamp(System.currentTimeMillis());
            if (sovm.getOperate_type() == 2) {
                asset.setCreator(userId);
                asset.setCreateTime(t);
            }
            asset.setLastUpdate(t);
            assets.add(asset);
            if (sovm.getOperate_type() == 2) {
                ResourceRelation relation = new ResourceRelation();
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                relation.setSourceUuid(sovm.getParent());
                relation.setEnable(true);
                relation.setRelationType(RelationType.ASSOCIATE.toString());
                relation.setResType(IndexSourceType.AssetType.getName());
                relation.setResourceTargetType(IndexSourceType.AssetType.getName());
                relation.setSortNum(findMaxSortNumInBatchDeal(relation, sortNumMap));
                relation.setStatus(LifecycleStatus.CREATED.toString());
                relation.setOrderNum(0);
                relation.setIdentifier(UUID.randomUUID().toString());
                relation.setCreator(userId);
                relation.setTarget(sovm.getIdentifier());
                relation.setCreateTime(ts);
                relation.setLastUpdate(ts);
                relations.add(relation);

                ResourceCategory category = new ResourceCategory();
                category.setIdentifier(UUID.randomUUID().toString());
                category.setResource(sovm.getIdentifier());
                category.setTaxoncode("$RA0502");
                category.setTaxonname("套件模板素材");
                category.setTaxoncodeid("1c2f86ac-dd23-45c9-bfa8-a53e592f6ee6");
                category.setCategoryCode("$R");
                category.setCategoryName("resourcetype");
                category.setPrimaryCategory(IndexSourceType.AssetType.getName());
                category.setShortName("objectivesets");
                categories.add(category);

                ResCoverage rcv = new ResCoverage();
                rcv.setIdentifier(UUID.randomUUID().toString());
                rcv.setResource(sovm.getIdentifier());
                rcv.setResType(IndexSourceType.AssetType.getName());
                rcv.setTarget("nd");
                rcv.setTargetType("Org");
                rcv.setStrategy("OWNER");
                rcv.setTargetTitle("createAssets4Business");
                coverages.add(rcv);
            }
        }

        try {
            //记录生命周期状态
            List<Contribute> contributeList = new ArrayList<Contribute>();
            Timestamp ts = new Timestamp(System.currentTimeMillis());

            if (CollectionUtils.isNotEmpty(assets)) {
                assetRepository.batchAdd(assets);
                for (Asset a : assets) {
                    contributeList.add(addContribute(IndexSourceType.AssetType.getName(), a.getIdentifier(), a.getStatus(), a.getCreator(), ts));
                }
            }
            if (CollectionUtils.isNotEmpty(relations)) {
                resourceRelationRepository.batchAdd(relations);
            }
            if (CollectionUtils.isNotEmpty(categories)) {
                resourceCategoryRepository.batchAdd(categories);
            }
            if (CollectionUtils.isNotEmpty(contributeList)) {
                contributeRepository.batchAdd(contributeList);
            }
            if (CollectionUtils.isNotEmpty(coverages)) {
                resCoverageRepository.batchAdd(coverages);
            }
        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        //修改rootsuite状态为created
        try {
            Asset asset = assetRepository.get(rootSuite);
            asset.setStatus(LifecycleStatus.CREATED.toString());
            assetRepository.save(asset);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
    }

    private List<ObjectiveTypeViewModel> dealObjectiveTypeBatch(List<ObjectiveTypeViewModel> objectiveTypeViewModelList, String userId) {

        if (CollectionUtils.isEmpty(objectiveTypeViewModelList)) {
            return null;
        }
        List<Asset> assetList = new ArrayList<Asset>();
        List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();
        List<ResourceRelation> relations = new ArrayList<ResourceRelation>();
        List<ResCoverage> coverages = new ArrayList<ResCoverage>();
        List<ObjectiveTypeViewModel> autoCreateList = new ArrayList<ObjectiveTypeViewModel>();
        Map<String, Float> sortNumMap = new HashMap<>();

        for (ObjectiveTypeViewModel model : objectiveTypeViewModelList) {
            String suiteId = model.getSuiteId();
            Set<String> descriptionSet = new HashSet<>();
            descriptionSet.add(model.getDescription());
            Map<String, List<String>> map = getExistDescription(suiteId, descriptionSet);
            if (CollectionUtils.isNotEmpty(map) && StringUtils.isEmpty(model.getIdentifier())) {
                continue;
            }
            //修改
            if (model.getOperateType() == 1) {
                Asset existAsset = checkBussinessResourceExist(model.getIdentifier(), false);
                if (StringUtils.hasText(model.getTitle())) {
                    existAsset.setTitle(model.getTitle());
                }
                existAsset.setStatus(model.getStatus());
                existAsset.setDescription(model.getDescription());
                existAsset.setCreator(userId);
                existAsset.setCustomProperties(ObjectUtils.toJson(model.getCustomProperties()));
                existAsset.setLastUpdate(new Timestamp(System.currentTimeMillis()));
                assetList.add(existAsset);

                //维度
                if (CollectionUtils.isNotEmpty(model.getKnowledgeCategories())) {
                    for (KnowledgeCategoriesViewModel kcvm : model.getKnowledgeCategories()) {
                        if (StringUtils.hasText(kcvm.getNdCode())) {//ndCode优先
                            String ndCode = kcvm.getNdCode();
                            CategoryData categoryData = kcCodeIsExist(ndCode);
                            if (categoryData != null) {//不符合的不管
                                ResourceCategory temp = new ResourceCategory();
                                temp.setTaxoncode(ndCode);
                                temp.setResource(model.getIdentifier());
                                temp.setPrimaryCategory(IndexSourceType.AssetType.getName());

                                try {
                                    List<ResourceCategory> tempList = resourceCategoryRepository.getAllByExample(temp);
                                    if (CollectionUtils.isEmpty(tempList)) {//没有才加
                                        ResourceCategory rc = new ResourceCategory();
                                        rc.setIdentifier(UUID.randomUUID().toString());
                                        rc.setResource(model.getIdentifier());
                                        rc.setTaxoncode(ndCode);
                                        rc.setTaxonname(categoryData.getTitle());
                                        rc.setTaxoncodeid(categoryData.getIdentifier());
                                        rc.setShortName(categoryData.getShortName());
                                        rc.setCategoryCode("KC");
                                        rc.setCategoryName("KnowledgeCategory");
                                        rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                                        resourceCategories.add(rc);
                                    }
                                } catch (EspStoreException e) {
                                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                                            e.getMessage());
                                }
                            }
                        } else if (StringUtils.hasText(kcvm.getName())) {//次级name为准
                            CategoryData cdCategoryData = getCategoryDataByName(kcvm.getName());
                            if (cdCategoryData == null) {
                                cdCategoryData = createCategoryData(kcvm.getName());
                            }

                            ResourceCategory rc = new ResourceCategory();
                            rc.setIdentifier(UUID.randomUUID().toString());
                            rc.setResource(model.getIdentifier());
                            rc.setTaxoncode(cdCategoryData.getNdCode());
                            rc.setTaxonname(cdCategoryData.getTitle());
                            rc.setTaxoncodeid(cdCategoryData.getIdentifier());
                            rc.setShortName(cdCategoryData.getShortName());
                            rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                            rc.setCategoryCode("KC");
                            rc.setCategoryName("KnowledgeCategory");
                            resourceCategories.add(rc);
                        }
                    }
                }

                //关系
                ResourceRelation temp = new ResourceRelation();
                temp.setEnable(true);
                temp.setSourceUuid(suiteId);
                temp.setResType(IndexSourceType.AssetType.getName());
                temp.setResourceTargetType(IndexSourceType.AssetType.getName());
                temp.setTarget(model.getIdentifier());

                try {
                    List<ResourceRelation> tempList = resourceRelationRepository.getAllByExample(temp);
                    if (CollectionUtils.isEmpty(tempList)) {
                        ResourceRelation relation = new ResourceRelation();
                        Timestamp ts = new Timestamp(System.currentTimeMillis());
                        relation.setIdentifier(UUID.randomUUID().toString());
                        relation.setEnable(true);
                        relation.setRelationType(RelationType.ASSOCIATE.getName());
                        relation.setResType(IndexSourceType.AssetType.getName());
                        relation.setResourceTargetType(IndexSourceType.AssetType.getName());
                        relation.setSourceUuid(suiteId);
                        relation.setLastUpdate(ts);
                        relation.setTarget(model.getIdentifier());
                        relations.add(relation);
                    }
                } catch (EspStoreException e) {
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                            e.getMessage());
                }
            } else if (model.getOperateType() == 2) {
                Asset newAsset = new Asset();
                newAsset.setIdentifier(model.getIdentifier());
                newAsset.setTitle(Long.toString(System.currentTimeMillis()));
                newAsset.setDescription(model.getDescription());
                newAsset.setStatus(model.getStatus());
                newAsset.setCreator(userId);
                newAsset.setCustomProperties(ObjectUtils.toJson(model.getCustomProperties()));
                Timestamp t = new Timestamp(System.currentTimeMillis());
                newAsset.setCreateTime(t);
                newAsset.setLastUpdate(t);
                assetList.add(newAsset);

                model.setIdentifier(newAsset.getIdentifier());
                autoCreateList.add(model);
                //维度
                if (CollectionUtils.isNotEmpty(model.getKnowledgeCategories())) {
                    for (KnowledgeCategoriesViewModel kcvm : model.getKnowledgeCategories()) {
                        if (StringUtils.hasText(kcvm.getNdCode())) {//ndCode优先
                            String ndCode = kcvm.getNdCode();
                            CategoryData categoryData = kcCodeIsExist(ndCode);
                            if (categoryData != null) {//不符合的不管
                                ResourceCategory rc = new ResourceCategory();
                                rc.setIdentifier(UUID.randomUUID().toString());
                                rc.setResource(newAsset.getIdentifier());
                                rc.setTaxoncode(ndCode);
                                rc.setTaxonname(categoryData.getTitle());
                                rc.setTaxoncodeid(categoryData.getIdentifier());
                                rc.setShortName(categoryData.getShortName());
                                rc.setCategoryCode("KC");
                                rc.setCategoryName("KnowledgeCategory");
                                rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                                resourceCategories.add(rc);
                            }
                        } else if (StringUtils.hasText(kcvm.getName())) {//次级name为准
                            CategoryData cdCategoryData = getCategoryDataByName(kcvm.getName());
                            if (cdCategoryData == null) {
                                cdCategoryData = createCategoryData(kcvm.getName());
                            }

                            ResourceCategory rc = new ResourceCategory();
                            rc.setIdentifier(UUID.randomUUID().toString());
                            rc.setResource(newAsset.getIdentifier());
                            rc.setTaxoncode(cdCategoryData.getNdCode());
                            rc.setTaxonname(cdCategoryData.getTitle());
                            rc.setTaxoncodeid(cdCategoryData.getIdentifier());
                            rc.setShortName(cdCategoryData.getShortName());
                            rc.setCategoryCode("KC");
                            rc.setCategoryName("KnowledgeCategory");
                            rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                            resourceCategories.add(rc);
                        }
                    }
                }

                //$RA0503
                ResourceCategory rc = new ResourceCategory();
                rc.setIdentifier(UUID.randomUUID().toString());
                rc.setResource(newAsset.getIdentifier());
                rc.setTaxoncode("$RA0503");
                rc.setTaxonname("目标模板");
                rc.setTaxoncodeid("03f90f0a-cd9d-45c4-91bf-52c57618fdb2");
                rc.setShortName("objectivetemplate");
                rc.setCategoryCode("$R");
                rc.setCategoryName("resourcetype");
                rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                resourceCategories.add(rc);

                //关系
                ResourceRelation relation = new ResourceRelation();
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                relation.setEnable(true);
                relation.setRelationType(RelationType.ASSOCIATE.getName());
                relation.setResType(IndexSourceType.AssetType.getName());
                relation.setResourceTargetType(IndexSourceType.AssetType.getName());
                relation.setSourceUuid(suiteId);
                relation.setSortNum(findMaxSortNumInBatchDeal(relation, sortNumMap));
                relation.setIdentifier(UUID.randomUUID().toString());
                relation.setTarget(newAsset.getIdentifier());
                relation.setCreateTime(ts);
                relation.setLastUpdate(ts);
                relations.add(relation);

                //coverages
                ResCoverage rcv = new ResCoverage();
                rcv.setIdentifier(UUID.randomUUID().toString());
                rcv.setResource(newAsset.getIdentifier());
                rcv.setResType(IndexSourceType.AssetType.getName());
                rcv.setTarget("nd");
                rcv.setTargetType("Org");
                rcv.setStrategy("OWNER");
                rcv.setTargetTitle("createAssets4Business");
                coverages.add(rcv);
            }
        }
        try {
            //记录生命周期状态
            List<Contribute> contributeList = new ArrayList<Contribute>();
            Timestamp ts = new Timestamp(System.currentTimeMillis());

            if (CollectionUtils.isNotEmpty(assetList)) {
                assetRepository.batchAdd(assetList);
                for (Asset a : assetList) {
                    contributeList.add(addContribute(IndexSourceType.AssetType.getName(), a.getIdentifier(), a.getStatus(), a.getCreator(), ts));
                }
            }
            if (CollectionUtils.isNotEmpty(resourceCategories)) {
                resourceCategoryRepository.batchAdd(resourceCategories);
            }
            if (CollectionUtils.isNotEmpty(relations)) {
                resourceRelationRepository.batchAdd(relations);
            }
            if (CollectionUtils.isNotEmpty(coverages)) {
                resCoverageRepository.batchAdd(coverages);
            }
            if (CollectionUtils.isNotEmpty(contributeList)) {
                contributeRepository.batchAdd(contributeList);
            }
        } catch (EspStoreException e) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getMessage());
        }
//				}
//			}

        return autoCreateList;
    }

    private void asynCreateInstructionalObjective4Business(final String userId, final List<ObjectiveTypeViewModel> objectiveTypeViewModels) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                for (ObjectiveTypeViewModel objectiveTypeViewModel : objectiveTypeViewModels) {
                    commonServiceHelper.autoCreateObjectives4Business(objectiveTypeViewModel, userId);
                }
            }
        });

    }

    private Asset checkBussinessResourceExist(String id, boolean isSuit) {
        try {
            Asset asset = assetRepository.get(id);

            if (asset != null && asset.getEnable() != null && asset.getEnable()) {
                ResourceCategory resourceCategory = new ResourceCategory();
                resourceCategory.setResource(id);
                if (isSuit) {
                    resourceCategory.setTaxoncode("$RA0502");
                } else {
                    resourceCategory.setTaxoncode("$RA0503");
                }

                resourceCategory.setPrimaryCategory(IndexSourceType.AssetType.getName());

                resourceCategory = resourceCategoryRepository.getByExample(resourceCategory);
                if (resourceCategory == null) {
                    String message = "suite_id对应的资源不存在";
                    if (!isSuit) {
                        message = "学习目标类型不存在";
                    }

                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                            LifeCircleErrorMessageMapper.ResourceNotFond.getCode(), message);
                }
            } else {
                String message = "suit_id不是套件类型资源";
                if (!isSuit) {
                    message = "资源存在但不是学习目标类型";
                }

                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.ResourceNotFond.getCode(), message);
            }

            return asset;
        } catch (EspStoreException e) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.StoreSdkFail.getCode(), e.getMessage());
        }
    }

    private void dealObjectiveTypes(String userId, List<ObjectiveTypeViewModel> objectiveTypes) {
        if (CollectionUtils.isNotEmpty(objectiveTypes)) {
//			Set<String> descriptionSet = new HashSet<String>();
//			for(ObjectiveTypeViewModel viewModel : objectiveTypes){
//				descriptionSet.add(viewModel.getDescription());
//			}

//			if(CollectionUtils.isNotEmpty(descriptionSet)){//descriptionSet一定有值
//				Map<String, List<String>> map = getExistDescription(suitId, descriptionSet);
//				
//				List<ObjectiveTypeViewModel> realNeedDealList = new ArrayList<ObjectiveTypeViewModel>();
//				if(CollectionUtils.isNotEmpty(map)){//说明有重复的
//					for(ObjectiveTypeViewModel viewModel : objectiveTypes){
//						if(map.containsKey(viewModel.getDescription())){
//							if(StringUtils.isNotEmpty(viewModel.getIdentifier()) &&
//									map.get(viewModel.getDescription()).contains(viewModel.getIdentifier())){//id为空的直接忽略
//								
//								realNeedDealList.add(viewModel);
//							}
//						}
//					}
//				}else{
//					realNeedDealList = objectiveTypes;
//				}


//				if(CollectionUtils.isNotEmpty(realNeedDealList)){
            List<Asset> assetList = new ArrayList<Asset>();
            List<ResourceCategory> resourceCategories = new ArrayList<ResourceCategory>();
            List<ResourceRelation> relations = new ArrayList<ResourceRelation>();
            List<ResCoverage> coverages = new ArrayList<ResCoverage>();
            List<ObjectiveTypeViewModel> autoCreateList = new ArrayList<ObjectiveTypeViewModel>();
            Map<String, Float> sortNumMap = new HashMap<>();

            for (ObjectiveTypeViewModel real : objectiveTypes) {
                String suitId = real.getSuiteId();
                Set<String> descriptionSet = new HashSet<String>();
                descriptionSet.add(real.getDescription());
                Map<String, List<String>> map = getExistDescription(suitId, descriptionSet);
                if (CollectionUtils.isNotEmpty(map) && StringUtils.isEmpty(real.getIdentifier())) {
                    continue;
                }

                if (StringUtils.isNotEmpty(real.getIdentifier())) {//修改
                    Asset existAsset = checkBussinessResourceExist(real.getIdentifier(), false);
                    if (StringUtils.hasText(real.getTitle())) {
                        existAsset.setTitle(real.getTitle());
                    }
                    existAsset.setStatus(real.getStatus());
                    existAsset.setDescription(real.getDescription());
                    existAsset.setCreator(userId);
                    existAsset.setCustomProperties(ObjectUtils.toJson(real.getCustomProperties()));
                    existAsset.setLastUpdate(new Timestamp(System.currentTimeMillis()));
                    assetList.add(existAsset);

                    //维度
                    if (CollectionUtils.isNotEmpty(real.getKnowledgeCategories())) {
                        for (KnowledgeCategoriesViewModel kcvm : real.getKnowledgeCategories()) {
                            if (StringUtils.hasText(kcvm.getNdCode())) {//ndCode优先
                                String ndCode = kcvm.getNdCode();
                                CategoryData categoryData = kcCodeIsExist(ndCode);
                                if (categoryData != null) {//不符合的不管
                                    ResourceCategory temp = new ResourceCategory();
                                    temp.setTaxoncode(ndCode);
                                    temp.setResource(real.getIdentifier());
                                    temp.setPrimaryCategory(IndexSourceType.AssetType.getName());

                                    try {
                                        List<ResourceCategory> tempList = resourceCategoryRepository.getAllByExample(temp);
                                        if (CollectionUtils.isEmpty(tempList)) {//没有才加
                                            ResourceCategory rc = new ResourceCategory();
                                            rc.setIdentifier(UUID.randomUUID().toString());
                                            rc.setResource(real.getIdentifier());
                                            rc.setTaxoncode(ndCode);
                                            rc.setTaxonname(categoryData.getTitle());
                                            rc.setTaxoncodeid(categoryData.getIdentifier());
                                            rc.setShortName(categoryData.getShortName());
                                            rc.setCategoryCode("KC");
                                            rc.setCategoryName("KnowledgeCategory");
                                            rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                                            resourceCategories.add(rc);
                                        }
                                    } catch (EspStoreException e) {
                                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                                                e.getMessage());
                                    }
                                }
                            } else if (StringUtils.hasText(kcvm.getName())) {//次级name为准
                                CategoryData cdCategoryData = getCategoryDataByName(kcvm.getName());
                                if (cdCategoryData == null) {
                                    cdCategoryData = createCategoryData(kcvm.getName());
                                }

                                ResourceCategory rc = new ResourceCategory();
                                rc.setIdentifier(UUID.randomUUID().toString());
                                rc.setResource(real.getIdentifier());
                                rc.setTaxoncode(cdCategoryData.getNdCode());
                                rc.setTaxonname(cdCategoryData.getTitle());
                                rc.setTaxoncodeid(cdCategoryData.getIdentifier());
                                rc.setShortName(cdCategoryData.getShortName());
                                rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                                rc.setCategoryCode("KC");
                                rc.setCategoryName("KnowledgeCategory");
                                resourceCategories.add(rc);
                            }
                        }
                    }

                    //关系
                    ResourceRelation temp = new ResourceRelation();
                    temp.setEnable(true);
                    temp.setSourceUuid(suitId);
                    temp.setResType(IndexSourceType.AssetType.getName());
                    temp.setResourceTargetType(IndexSourceType.AssetType.getName());
                    temp.setTarget(real.getIdentifier());

                    try {
                        List<ResourceRelation> tempList = resourceRelationRepository.getAllByExample(temp);
                        if (CollectionUtils.isEmpty(tempList)) {
                            ResourceRelation relation = new ResourceRelation();
                            Timestamp ts = new Timestamp(System.currentTimeMillis());
                            relation.setIdentifier(UUID.randomUUID().toString());
                            relation.setEnable(true);
                            relation.setRelationType(RelationType.ASSOCIATE.getName());
                            relation.setResType(IndexSourceType.AssetType.getName());
                            relation.setResourceTargetType(IndexSourceType.AssetType.getName());
                            relation.setSourceUuid(suitId);
                            relation.setTarget(real.getIdentifier());
                            relation.setLastUpdate(ts);
                            relations.add(relation);
                        }
                    } catch (EspStoreException e) {
                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                                e.getMessage());
                    }
                } else {//新增
                    Asset newAsset = new Asset();
                    newAsset.setIdentifier(UUID.randomUUID().toString());
                    newAsset.setTitle(Long.toString(System.currentTimeMillis()));
                    newAsset.setDescription(real.getDescription());
                    newAsset.setStatus(real.getStatus());
                    newAsset.setCreator(userId);
                    newAsset.setCustomProperties(ObjectUtils.toJson(real.getCustomProperties()));
                    Timestamp t = new Timestamp(System.currentTimeMillis());
                    newAsset.setCreateTime(t);
                    newAsset.setLastUpdate(t);
                    assetList.add(newAsset);

                    real.setIdentifier(newAsset.getIdentifier());
                    autoCreateList.add(real);

                    //维度
                    if (CollectionUtils.isNotEmpty(real.getKnowledgeCategories())) {
                        for (KnowledgeCategoriesViewModel kcvm : real.getKnowledgeCategories()) {
                            if (StringUtils.hasText(kcvm.getNdCode())) {//ndCode优先
                                String ndCode = kcvm.getNdCode();
                                CategoryData categoryData = kcCodeIsExist(ndCode);
                                if (categoryData != null) {//不符合的不管
                                    ResourceCategory rc = new ResourceCategory();
                                    rc.setIdentifier(UUID.randomUUID().toString());
                                    rc.setResource(newAsset.getIdentifier());
                                    rc.setTaxoncode(ndCode);
                                    rc.setTaxonname(categoryData.getTitle());
                                    rc.setTaxoncodeid(categoryData.getIdentifier());
                                    rc.setShortName(categoryData.getShortName());
                                    rc.setCategoryCode("KC");
                                    rc.setCategoryName("KnowledgeCategory");
                                    rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                                    resourceCategories.add(rc);
                                }
                            } else if (StringUtils.hasText(kcvm.getName())) {//次级name为准
                                CategoryData cdCategoryData = getCategoryDataByName(kcvm.getName());
                                if (cdCategoryData == null) {
                                    cdCategoryData = createCategoryData(kcvm.getName());
                                }

                                ResourceCategory rc = new ResourceCategory();
                                rc.setIdentifier(UUID.randomUUID().toString());
                                rc.setResource(newAsset.getIdentifier());
                                rc.setTaxoncode(cdCategoryData.getNdCode());
                                rc.setTaxonname(cdCategoryData.getTitle());
                                rc.setTaxoncodeid(cdCategoryData.getIdentifier());
                                rc.setShortName(cdCategoryData.getShortName());
                                rc.setCategoryCode("KC");
                                rc.setCategoryName("KnowledgeCategory");
                                rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                                resourceCategories.add(rc);
                            }
                        }
                    }

                    //$RA0503
                    ResourceCategory rc = new ResourceCategory();
                    rc.setIdentifier(UUID.randomUUID().toString());
                    rc.setResource(newAsset.getIdentifier());
                    rc.setTaxoncode("$RA0503");
                    rc.setTaxonname("目标模板");
                    rc.setTaxoncodeid("03f90f0a-cd9d-45c4-91bf-52c57618fdb2");
                    rc.setShortName("objectivetemplate");
                    rc.setCategoryCode("$R");
                    rc.setCategoryName("resourcetype");
                    rc.setPrimaryCategory(IndexSourceType.AssetType.getName());
                    resourceCategories.add(rc);

                    //关系
                    ResourceRelation relation = new ResourceRelation();
                    Timestamp ts = new Timestamp(System.currentTimeMillis());
                    relation.setEnable(true);
                    relation.setRelationType(RelationType.ASSOCIATE.getName());
                    relation.setResType(IndexSourceType.AssetType.getName());
                    relation.setResourceTargetType(IndexSourceType.AssetType.getName());
                    relation.setSourceUuid(suitId);
                    relation.setSortNum(findMaxSortNumInBatchDeal(relation, sortNumMap));
                    relation.setIdentifier(UUID.randomUUID().toString());
                    relation.setTarget(newAsset.getIdentifier());
                    relation.setCreateTime(ts);
                    relation.setLastUpdate(ts);
                    relations.add(relation);

                    //coverages
                    ResCoverage rcv = new ResCoverage();
                    rcv.setIdentifier(UUID.randomUUID().toString());
                    rcv.setResource(newAsset.getIdentifier());
                    rcv.setResType(IndexSourceType.AssetType.getName());
                    rcv.setTarget("nd");
                    rcv.setTargetType("Org");
                    rcv.setStrategy("OWNER");
                    rcv.setTargetTitle("createAssets4Business");
                    coverages.add(rcv);
                }
            }

            try {
                //记录生命周期状态
                List<Contribute> contributeList = new ArrayList<Contribute>();
                Timestamp ts = new Timestamp(System.currentTimeMillis());

                if (CollectionUtils.isNotEmpty(assetList)) {
                    assetRepository.batchAdd(assetList);
                    for (Asset a : assetList) {
                        contributeList.add(addContribute(IndexSourceType.AssetType.getName(), a.getIdentifier(), a.getStatus(), a.getCreator(), ts));
                    }
                }
                if (CollectionUtils.isNotEmpty(resourceCategories)) {
                    resourceCategoryRepository.batchAdd(resourceCategories);
                }
                if (CollectionUtils.isNotEmpty(relations)) {
                    resourceRelationRepository.batchAdd(relations);
                }
                if (CollectionUtils.isNotEmpty(coverages)) {
                    resCoverageRepository.batchAdd(coverages);
                }
                if (CollectionUtils.isNotEmpty(contributeList)) {
                    contributeRepository.batchAdd(contributeList);
                }
            } catch (EspStoreException e) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                        e.getMessage());
            }
//				}
//			}

            if (CollectionUtils.isNotEmpty(autoCreateList)) {
                asynCreateInstructionalObjective4Business(userId, autoCreateList);
            }

        }
    }

    private Contribute addContribute(String resType, String resource, String status, String userId, Timestamp ts) {
        Contribute contribute = new Contribute();
        contribute.setIdentifier(UUID.randomUUID().toString());
        contribute.setLifeStatus(status);
        contribute.setResource(resource);
        contribute.setResType(resType);
        if (StringUtils.isNotEmpty(request.getParameter("user_id"))) {
            contribute.setTargetId(request.getParameter("user_id"));
        } else {
            contribute.setTargetId(userId);
        }

        contribute.setTargetName(request.getParameter("user_name"));
        contribute.setTargetType("User");
        contribute.setContributeTime(ts);
        return contribute;
    }

    private CategoryData kcCodeIsExist(String code) {
        if (code.startsWith("KC")) {
            CategoryData cd = new CategoryData();
            cd.setNdCode(code);

            try {
                cd = categoryDataRepository.getByExample(cd);
            } catch (EspStoreException e) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                        e.getMessage());
            }

            return cd;
        }

        return null;
    }

    private Map<String, List<String>> getExistDescription(String suitId, Set<String> descriptionSet) {
        String sql = "select ndr.identifier as id,ndr.description as des from ndresource ndr inner join resource_relations rr on ndr.identifier=rr.target"
                + " inner join resource_categories rc on ndr.identifier=rc.resource inner join res_coverages rv"
                + " on ndr.identifier=rv.resource where ndr.enable=1 and ndr.primary_category='assets' and"
                + " rc.primary_category='assets' and rc.taxOnCode='$RA0503'"
                + " and rv.target_type='Org' and rv.target='nd' and rv.strategy='OWNER' and rv.res_type='assets'"
                + " and rr.enable=1 and rr.res_type='assets' and rr.source_uuid='" + suitId + "' and rr.resource_target_type='assets'"
                + " and ndr.description in (:desSet)";

        final Map<String, List<String>> map = new HashMap<String, List<String>>();
        NamedParameterJdbcTemplate npjp = new NamedParameterJdbcTemplate(assetRepository.getJdbcTemple());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("desSet", descriptionSet);

        npjp.query(sql, params, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                String id = rs.getString("id");
                String description = rs.getString("des");

                if (map.containsKey(description)) {
                    map.get(description).add(id);
                } else {
                    List<String> ids = new ArrayList<String>();
                    ids.add(id);
                    map.put(description, ids);
                }
                return null;
            }
        });

        return map;
    }

    private void checkStatus(List<ObjectiveTypeViewModel> objectiveTypes) {
        if (CollectionUtils.isNotEmpty(objectiveTypes)) {
            for (ObjectiveTypeViewModel viewModel : objectiveTypes) {
                if (StringUtils.hasText(viewModel.getStatus())) {
                    if (!LifecycleStatus.isLegalStatus(viewModel.getStatus())) {
                        throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                LifeCircleErrorMessageMapper.StatusIsNotExist.getCode(),
                                "学习目标类型：" + viewModel.getIdentifier() + "--status必须为有效值");
                    }
                }
            }
        }
    }

    private CategoryData getCategoryDataByName(String name) {
        CategoryData cd = new CategoryData();
        cd.setTitle(name);
        try {
            List<CategoryData> cdList = categoryDataRepository.getAllByExample(cd);
            if (CollectionUtils.isNotEmpty(cdList)) {
                for (CategoryData categoryData : cdList) {
                    if (categoryData.getNdCode().startsWith("KC")) {
                        return categoryData;
                    }
                }
            } else {
                return null;
            }
        } catch (EspStoreException e) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getMessage());
        }
        return null;
    }

    private CategoryData createCategoryData(String name) {
        CategoryDataApplyForNdCodeViewModel tmp = applyNdCode(name);
        CategoryData cd = new CategoryData();
        cd.setIdentifier(UUID.randomUUID().toString());
        cd.setDescription(name);
        cd.setGbCode(tmp.getGbCode());
        cd.setNdCode(tmp.getNdCode());
        cd.setOrderNum(tmp.getOrderNum());
        cd.setParent(tmp.getParent());
        cd.setCategory(tmp.getCategory());
        cd.setTitle(name);
        return cd;
    }

    private CategoryDataApplyForNdCodeViewModel applyNdCode(String name) {
        CategoryDataApplyForNdCodeViewModel tmp = cc.extendCategoryData("KC");
        return tmp;
    }

    //找到最大的sort_num
    private float findMaxSortNum(ResourceRelation example) {
        List<ResourceRelation> relations = null;
        try {
            relations = resourceRelationRepository.getAllByExample(example);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        //第一个 暂时写死为5000 FIXME
        if (CollectionUtils.isEmpty(relations)) {
            return 4990;
        }
        float max = 0;
        for (ResourceRelation relation : relations) {
            if (relation.getSortNum() != null && relation.getSortNum() > max) {
                max = relation.getSortNum();
            }
        }
        return max;
    }

    public float findMaxSortNumInBatchDeal(ResourceRelation example, Map<String, Float> sortNumMap) {
        List<ResourceRelation> relations = null;
        try {
            relations = resourceRelationRepository.getAllByExample(example);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        //第一个 暂时写死为5000 FIXME
        if (CollectionUtils.isEmpty(relations) && CollectionUtils.isNotEmpty(sortNumMap) && sortNumMap.get(example.getSourceUuid()) == null) {
            sortNumMap.put(example.getSourceUuid(),5000f);
            return 5000;
        }
        if (CollectionUtils.isEmpty(relations) && CollectionUtils.isEmpty(sortNumMap)) {
            sortNumMap.put(example.getSourceUuid(),5000f);
            return 5000;
        }
        float max = 0;
        //从数据库取的sort_num
        for (ResourceRelation relation : relations) {
            if (relation.getSortNum() != null && relation.getSortNum() > max) {
                max = relation.getSortNum();
            }
        }
        //跟在内存中的对比取最大的，把最大的放入内存
        if (CollectionUtils.isNotEmpty(sortNumMap)) {
            if (sortNumMap.get(example.getSourceUuid()) != null && (Float) sortNumMap.get(example.getSourceUuid()) > max) {
                max = (Float) sortNumMap.get(example.getSourceUuid());
                sortNumMap.put(example.getSourceUuid(), max + 10);
            } else {
                sortNumMap.put(example.getSourceUuid(), max + 10);
            }
        } else {
            sortNumMap.put(example.getSourceUuid(), max + 10);
        }
        return max + 10;
    }
}
