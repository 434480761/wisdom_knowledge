package nd.esp.service.lifecycle.services.instructionalobjectives.v06.impls;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;

import nd.esp.service.lifecycle.daos.instructionalobjectives.v06.InstructionalobjectiveDao;
import nd.esp.service.lifecycle.educommon.models.ResClassificationModel;
import nd.esp.service.lifecycle.educommon.models.ResCoverageModel;
import nd.esp.service.lifecycle.educommon.models.ResLifeCycleModel;
import nd.esp.service.lifecycle.educommon.models.ResourceModel;
import nd.esp.service.lifecycle.educommon.services.NDResourceService;
import nd.esp.service.lifecycle.educommon.services.impl.CommonServiceHelper;
import nd.esp.service.lifecycle.educommon.support.RelationType;
import nd.esp.service.lifecycle.entity.elasticsearch.Resource;
import nd.esp.service.lifecycle.models.chapter.v06.ChapterModel;
import nd.esp.service.lifecycle.models.v06.EducationRelationLifeCycleModel;
import nd.esp.service.lifecycle.models.v06.EducationRelationModel;
import nd.esp.service.lifecycle.models.v06.InstructionalObjectiveModel;
import nd.esp.service.lifecycle.models.v06.ResultsModel;
import nd.esp.service.lifecycle.models.v06.SuitAndInstructionalObjectiveModel;
import nd.esp.service.lifecycle.models.v06.SuiteModel;
import nd.esp.service.lifecycle.repository.Education;
import nd.esp.service.lifecycle.repository.EspEntity;
import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.Asset;
import nd.esp.service.lifecycle.repository.model.Category;
import nd.esp.service.lifecycle.repository.model.CategoryData;
import nd.esp.service.lifecycle.repository.model.Chapter;
import nd.esp.service.lifecycle.repository.model.Contribute;
import nd.esp.service.lifecycle.repository.model.FullModel;
import nd.esp.service.lifecycle.repository.model.InstructionalObjective;
import nd.esp.service.lifecycle.repository.model.ResCoverage;
import nd.esp.service.lifecycle.repository.model.ResourceCategory;
import nd.esp.service.lifecycle.repository.model.ResourceRelation;
import nd.esp.service.lifecycle.repository.model.Sample;
import nd.esp.service.lifecycle.repository.model.SubInstruction;
import nd.esp.service.lifecycle.repository.model.TeachingMaterial;
import nd.esp.service.lifecycle.repository.sdk.AssetRepository;
import nd.esp.service.lifecycle.repository.sdk.CategoryDataRepository;
import nd.esp.service.lifecycle.repository.sdk.CategoryRepository;
import nd.esp.service.lifecycle.repository.sdk.ChapterRepository;
import nd.esp.service.lifecycle.repository.sdk.ContributeRepository;
import nd.esp.service.lifecycle.repository.sdk.InstructionalobjectiveRepository;
import nd.esp.service.lifecycle.repository.sdk.KnowledgeRepository;
import nd.esp.service.lifecycle.repository.sdk.ResCoverageRepository;
import nd.esp.service.lifecycle.repository.sdk.ResourceCategoryRepository;
import nd.esp.service.lifecycle.repository.sdk.ResourceRelationRepository;
import nd.esp.service.lifecycle.repository.sdk.SampleRepository;
import nd.esp.service.lifecycle.repository.sdk.SubInstructionRepository;
import nd.esp.service.lifecycle.repository.v02.ResourceRelationApiService;
import nd.esp.service.lifecycle.services.coverages.v06.CoverageService;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceV06;
import nd.esp.service.lifecycle.services.elasticsearch.AsynEsResourceService;
import nd.esp.service.lifecycle.services.instructionalobjectives.v06.InstructionalObjectiveService;
import nd.esp.service.lifecycle.services.suites.v06.ExtendSuiteService;
import nd.esp.service.lifecycle.services.teachingmaterial.v06.ChapterService;
import nd.esp.service.lifecycle.services.teachingmaterial.v06.TeachingMaterialServiceV06;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.busi.CommonHelper;
import nd.esp.service.lifecycle.support.enums.LifecycleStatus;
import nd.esp.service.lifecycle.support.enums.ResourceNdCode;
import nd.esp.service.lifecycle.utils.BeanMapperUtils;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.EduRedisTemplate;
import nd.esp.service.lifecycle.utils.ParamCheckUtil;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.utils.gson.ObjectUtils;
import nd.esp.service.lifecycle.vos.ListViewModel;
import nd.esp.service.lifecycle.vos.ListViewModel4Suite;
import nd.esp.service.lifecycle.vos.business.MultiLevelChildViewModel;
import nd.esp.service.lifecycle.vos.business.MultiLevelSuiteViewModel;
import nd.esp.service.lifecycle.vos.business.SuiteKnowledgesViewModel;
import nd.esp.service.lifecycle.vos.business.SuiteObjectivesViewModel;
import nd.esp.service.lifecycle.vos.business.SuiteViewModel;
import nd.esp.service.lifecycle.vos.coverage.v06.CoverageViewModel;
import nd.esp.service.lifecycle.vos.educationrelation.v06.RelationForQueryViewModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.ClassifyDownSuite;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.ClassifySuitAndInstructionalObjectiveModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.MultilayerClassifySubSuite;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.SampleInfoModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.SampleViewModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.SuiteStatisticsModel;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.gson.reflect.TypeToken;
import com.nd.gaea.client.http.WafSecurityHttpClient;
import com.nd.gaea.rest.o2o.JacksonCustomObjectMapper;

/**
 * 业务实现类
 *
 * @author linsm
 */
@Service("instructionalObjectiveServiceV06")
@Transactional
public class InstructionalObjectiveServiceImpl implements
        InstructionalObjectiveService {

    private static final Logger LOG = LoggerFactory
            .getLogger(InstructionalObjectiveServiceImpl.class);

    @Autowired
    private NDResourceService ndResourceService;

    @Autowired
    @Qualifier("educationRelationServiceV06")
    private EducationRelationServiceV06 educationRelationService;

    @Autowired
    @Qualifier(value = "coverageServiceImpl")
    private CoverageService coverageService;

    @Autowired
    private JdbcTemplate jt;

    @Autowired
    private ResourceRelationApiService resourceRelationApiService;

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private TeachingMaterialServiceV06 teachingMaterialService;

    @PersistenceContext(unitName = "entityManagerFactory")
    EntityManager defaultEm;

    @Autowired
    private InstructionalobjectiveDao instructionalobjectiveDao;

    @Autowired
    private InstructionalobjectiveRepository ioRepository;

    @Autowired
    private SubInstructionRepository siRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private ResCoverageRepository covRepository;

    @Autowired
    private ResourceCategoryRepository rcRepository;

    @Autowired
    private ResourceRelationRepository rrRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryDataRepository categoryDataRepository;

    @Autowired
    private EducationRelationServiceV06 educationRelationServer;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ExtendSuiteService suiteService;

    @Override
    public InstructionalObjectiveModel createInstructionalObjective(
            InstructionalObjectiveModel instructionalObjectiveModel) {
        // 调用通用创建接口
        instructionalObjectiveModel.setTechInfoList(null);
        instructionalObjectiveModel = (InstructionalObjectiveModel) ndResourceService
                .create(ResourceNdCode.instructionalobjectives.toString(),
                        instructionalObjectiveModel);
        instructionalObjectiveModel.setPreview(null);
        instructionalObjectiveModel.setEducationInfo(null);

        return instructionalObjectiveModel;
    }

    @Override
    public InstructionalObjectiveModel updateInstructionalObjective(
            InstructionalObjectiveModel instructionalObjectiveModel) {
        /**
         * 由于教学目标的演示demo需要有版本的信息 目前采用的做法
         * 修改教学目标时，如果状态不是为ONLINE，则新增一条教学目标，并与之前的教学目标（有可能需要通过关系查询最原始的教学目标）建立关系
         * 修改教学目标时，如果状态为ONLINE，则需要找到最原始的教学目标，并将新的内容赋值给原始的教学目标
         */

        // 1、找原始的教学目标数据
        String initalId = null;
        String initalSql = "SELECT nd.identifier from resource_relations rr,ndresource nd "
                + "where nd.primary_category='instructionalobjectives' and rr.res_type='instructionalobjectives' "
                + "and rr.resource_target_type='instructionalobjectives' and rr.target = '"
                + instructionalObjectiveModel.getIdentifier()
                + "' "
                + "and rr.source_uuid = nd.identifier and nd.enable = 1";

        List<Map<String, Object>> list = jt.queryForList(initalSql);
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, Object> map = list.get(0);
            initalId = (String) map.get("identifier");
        }

        String status = instructionalObjectiveModel.getLifeCycle().getStatus();
        if ("ONLINE".equals(status)) {
            if (initalId != null) {
                instructionalObjectiveModel.setIdentifier(initalId);
            }
            instructionalObjectiveModel.setTechInfoList(null);
            instructionalObjectiveModel = (InstructionalObjectiveModel) ndResourceService
                    .update(ResourceNdCode.instructionalobjectives.toString(),
                            instructionalObjectiveModel);
            instructionalObjectiveModel.setPreview(null);
            instructionalObjectiveModel.setEducationInfo(null);

            return instructionalObjectiveModel;
        } else if (instructionalObjectiveModel.getCustomProperties() != null
                && instructionalObjectiveModel.getCustomProperties().contains(
                "onlystatus")) {
            instructionalObjectiveModel = (InstructionalObjectiveModel) ndResourceService
                    .update(ResourceNdCode.instructionalobjectives.toString(),
                            instructionalObjectiveModel);
            instructionalObjectiveModel.setPreview(null);
            instructionalObjectiveModel.setEducationInfo(null);

            return instructionalObjectiveModel;
        } else {
            String oldId = instructionalObjectiveModel.getIdentifier();
            if (initalId != null) {
                oldId = initalId;
            }

            String newId = UUID.randomUUID().toString();
            instructionalObjectiveModel.setIdentifier(newId);
            instructionalObjectiveModel.setRelations(null);

            // 查找oldId的覆盖范围
            List<CoverageViewModel> cvList = coverageService
                    .getCoveragesByResource("instructionalobjectives", oldId,
                            null, null, null);
            if (CollectionUtils.isNotEmpty(cvList)) {
                List<ResCoverageModel> coverages = new ArrayList<ResCoverageModel>();
                for (CoverageViewModel cvm : cvList) {
                    ResCoverageModel c = BeanMapperUtils.beanMapper(cvm,
                            ResCoverageModel.class);
                    coverages.add(c);
                }
                instructionalObjectiveModel.setCoverages(coverages);
            }

            instructionalObjectiveModel = (InstructionalObjectiveModel) ndResourceService
                    .create(ResourceNdCode.instructionalobjectives.toString(),
                            instructionalObjectiveModel);
            instructionalObjectiveModel.setPreview(null);
            instructionalObjectiveModel.setEducationInfo(null);

            List<EducationRelationModel> educationRelationModels = new ArrayList<EducationRelationModel>();
            EducationRelationModel erm = new EducationRelationModel();
            erm.setIdentifier(UUID.randomUUID().toString());
            // erm.setOrderNum(5000);
            erm.setResType("instructionalobjectives");
            erm.setResourceTargetType("instructionalobjectives");
            erm.setSource(oldId);
            erm.setTarget(newId);
            erm.setLabel("版本迭代");
            erm.setRelationType("ASSOCIATE");
            EducationRelationLifeCycleModel lc = new EducationRelationLifeCycleModel();
            lc.setEnable(true);
            lc.setStatus("CREATED");
            erm.setLifeCycle(lc);
            educationRelationModels.add(erm);

            // 将target为oldId所对应的关系复制一份到newId中
            String relationSql = "select res_type,source_uuid from resource_relations where resource_target_type='instructionalobjectives' and target = '"
                    + oldId + "'";
            List<Map<String, Object>> l = jt.queryForList(relationSql);
            if (CollectionUtils.isNotEmpty(l)) {
                for (Map<String, Object> map : l) {
                    String resType = (String) map.get("res_type");
                    String source = (String) map.get("source_uuid");
                    EducationRelationModel tmp = new EducationRelationModel();
                    tmp.setIdentifier(UUID.randomUUID().toString());
                    // erm.setOrderNum(5000);
                    tmp.setResType(resType);
                    tmp.setResourceTargetType("instructionalobjectives");
                    tmp.setSource(source);
                    tmp.setTarget(newId);
                    tmp.setLabel("版本迭代");
                    tmp.setRelationType("ASSOCIATE");
                    EducationRelationLifeCycleModel tmpLc = new EducationRelationLifeCycleModel();
                    tmpLc.setEnable(true);
                    tmpLc.setStatus("CREATED");
                    tmp.setLifeCycle(tmpLc);
                    educationRelationModels.add(tmp);
                }
            }
            educationRelationService.createRelation(educationRelationModels,
                    false);
            return instructionalObjectiveModel;

        }
    }

    /**
     * 根据教学目标id查询出与之关联的章节信息id 分两种情况： 1.教学目标与章节直接关联 2.教学目标与课时关联，课时与章节关联
     *
     * @param id
     * @return
     */
    @Override
    public List<Map<String, Object>> getChapterRelationById(String id) {
        try {
            LinkedList<Map<String, Object>> pathList = new LinkedList<>();// 教材章节路径list
            Set<String> chapterIdSet = new HashSet<>();// 与教学目标相关联的chapterId集合，包含直接与chapter关联的和与lesson关联然后再与chapter关联。去重。
            // 查询与教学目标直接关联的chapter的关系，并且把关系当中的chapterId放到set里面。begin
            List<ResourceRelation> fromChapterToInstructionalObjectiveRelationList = resourceRelationApiService
                    .getByResTypeAndTargetTypeAndTargetId(
                            IndexSourceType.ChapterType.getName(),
                            IndexSourceType.InstructionalObjectiveType
                                    .getName(), id);
            if (fromChapterToInstructionalObjectiveRelationList != null
                    && fromChapterToInstructionalObjectiveRelationList.size() > 0) {
                for (ResourceRelation rr : fromChapterToInstructionalObjectiveRelationList) {
                    chapterIdSet.add(rr.getSourceUuid());
                }
            }
            // 查询与教学目标直接关联的chapter的关系，并且把关系当中的chapterId放到set里面。end

            // 查询与教学目标间接关联（通过lesson关联到chapter）的chapter的关系，并把关系当中的chapterId放到set里面。begin
            List<ResourceRelation> lessonRelationList = resourceRelationApiService
                    .getByResTypeAndTargetTypeAndTargetId(
                            IndexSourceType.LessonType.getName(),
                            IndexSourceType.InstructionalObjectiveType
                                    .getName(), id);
            for (ResourceRelation rr : lessonRelationList) {
                String lessonId = rr.getSourceUuid();
                List<ResourceRelation> fromChaptersToLessonsRelationList = resourceRelationApiService
                        .getByResTypeAndTargetTypeAndTargetId(
                                IndexSourceType.ChapterType.getName(),
                                IndexSourceType.LessonType.getName(), lessonId);
                if (fromChaptersToLessonsRelationList != null
                        && fromChaptersToLessonsRelationList.size() > 0) {
                    for (ResourceRelation rr2 : fromChaptersToLessonsRelationList) {
                        chapterIdSet.add(rr2.getSourceUuid());
                    }
                }
            }
            // 查询与教学目标间接关联（通过lesson关联到chapter）的chapter的关系，并把关系当中的chapterId放到set里面。end

            if (chapterIdSet != null && chapterIdSet.size() > 0) {
                for (String chapterId : chapterIdSet) {
                    if (chapterId != null && !"".equals(chapterId)) {
                        // 循环获取章节，以及该节的父章节的信息。begin
                        LinkedList<ChapterModel> chapterList = new LinkedList<>();// 存储每个章节及其父章节一级级往上查找的信息。
                        ChapterModel cm;
                        do {
                            cm = chapterService.getChapterDetail(chapterId);
                            if (cm != null) {
                                chapterList.add(cm);
                                chapterId = cm.getParent();
                            }
                        } while (cm != null
                                && !cm.getParent().equals(
                                cm.getTeachingMaterial()));
                        // 循环获取章节，以及该节的父章节的信息。end
                        // 代码这样写是为了阻断。。。
                        if (cm != null && cm.getTeachingMaterial() != null) {
                            TeachingMaterial teachingMaterial = teachingMaterialService
                                    .getById(cm.getTeachingMaterial());// 获取教材信息

                            // 拼接返回的字符串。begin
                            String pathStr = teachingMaterial.getIdentifier();
                            String textStr = teachingMaterial.getTitle();
                            String chapterIdStr = "";
                            if (chapterList != null && chapterList.size() > 0) {
                                for (int i = chapterList.size() - 1; i >= 0; i--) {
                                    ChapterModel chapterModel = new ChapterModel();
                                    chapterModel = chapterList.get(i);
                                    if (i == 0) {
                                        chapterIdStr = chapterModel
                                                .getIdentifier();
                                    }
                                    pathStr += "/"
                                            + chapterModel.getIdentifier();
                                    textStr += "/" + chapterModel.getTitle();
                                }
                            }
                            LinkedHashMap<String, Object> pathItem = new LinkedHashMap<>();
                            pathItem.put("path", pathStr);
                            pathItem.put("text", textStr);
                            pathItem.put("chapter_id", chapterIdStr);
                            pathList.add(pathItem);
                            // 拼接返回的字符串。end
                        }
                    }
                }
            }
            return pathList;
        } catch (EspStoreException e) {
            LOG.error("根据教学目标获取章节关联关系出错！", e);
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public String getInstructionalObjectiveTitle(
            Map.Entry<String, String> idWithTitle) {
        List<Map.Entry<String, String>> idWithTitles = new ArrayList<>();
        idWithTitles.add(idWithTitle);
        return getInstructionalObjectiveTitle(idWithTitles).get(
                idWithTitle.getKey());
    }

    @Override
    public Map<String, String> getInstructionalObjectiveTitle(
            Collection<Map.Entry<String, String>> idWithTitles) {

        if (0 == idWithTitles.size()) {
            return Collections.emptyMap();
        }

        // 教学目标id集合
        List<String> ids = new ArrayList<String>();
        for (Map.Entry<String, String> entry : idWithTitles) {
            ids.add(entry.getKey());
        }

        // 根据教学目标id获取custom_properties
        String cSql = "select identifier,custom_properties from ndresource where primary_category = 'instructionalobjectives' and (enable = 1 or (enable = 0 and estatus='RECYCLED'))  and identifier in (:ids)";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ids", ids);

        List<Map<String, Object>> iList = npjt.query(cSql, params,
                new RowMapper<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        Map<String, Object> m = new HashMap<String, Object>();
                        String identifier = rs.getString("identifier");
                        String customProperties = rs
                                .getString("custom_properties");

                        m.put("identifier", identifier);
                        if (StringUtils.isNotEmpty(customProperties)) {
                            m.put("customProperties", ObjectUtils.fromJson(
                                    customProperties, Map.class));
                        } else {
                            m.put("customProperties", null);
                        }

                        return m;
                    }
                });

        try {
            Collection<String> idString = Collections2.transform(idWithTitles,
                    new Function<Map.Entry<String, String>, String>() {
                        @Nullable
                        @Override
                        public String apply(
                                @Nullable Map.Entry<String, String> idWithTitle) {
                            if (idWithTitle != null) {
                                return String.format("\"%s\"",
                                        idWithTitle.getKey());
                            }
                            return null;
                        }
                    });

            // 查找教学目标关联的教学目标类型
            String SQLQueryInstructionalObjectiveType = String
                    .format("SELECT ndr.identifier AS id,ndr.title AS title,ndr.description AS description,rr.target AS target from `ndresource` AS ndr"
                                    + " inner join `resource_relations` AS rr on ndr.identifier=rr.source_uuid and rr.res_type=\"assets\" and rr.enable = 1 and rr.resource_target_type=\"instructionalobjectives\""
                                    + " WHERE (ndr.enable = 1 or (ndr.enable = 0 and ndr.estatus='RECYCLED')) and ndr.identifier in ( select resource from `resource_categories` where taxOnCode='$RA0503') AND rr.target in(%s)",
                            StringUtils.join(idString, ","));
            List<Map<String, Object>> instructionalObjectiveTypeList = jt
                    .queryForList(SQLQueryInstructionalObjectiveType);
            // 以教学目标Id为key的查询结果
            Map<String, Map<String, Object>> instructionalObjective2TypeMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(instructionalObjectiveTypeList)) {
                for (Map<String, Object> map : instructionalObjectiveTypeList) {
                    String target = (String) map.get("target");
                    instructionalObjective2TypeMap.put(target, map);
                }
            }

            // 查找教学目标关联的知识点
            String SQLQueryKnowledges = String
                    .format("SELECT ndr.identifier AS id,ndr.title AS title,ndr.description as description,rr.target AS target,rr.order_num as orderNum from `ndresource` AS ndr"
                                    + " inner join `resource_relations` AS rr on ndr.identifier=rr.source_uuid and rr.res_type=\"knowledges\" and rr.resource_target_type=\"instructionalobjectives\""
                                    + " WHERE (ndr.enable = 1 or (ndr.enable = 0 and ndr.estatus='RECYCLED')) and rr.enable = 1 and rr.target in (%s)",
                            StringUtils.join(idString, ","));
            List<Map<String, Object>> knowledgesList = jt
                    .queryForList(SQLQueryKnowledges);
            // 以教学目标Id为key的查询结果
            Map<String, List<Map<String, Object>>> knowledgesMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(knowledgesList)) {
                for (Map<String, Object> map : knowledgesList) {
                    String target = (String) map.get("target");

                    // 用来知识点的排序
                    List<String> orderList = null;
                    for (Map<String, Object> mm : iList) {
                        if (target.equals((String) mm.get("identifier"))) {
                            Map customProperties = (Map) mm
                                    .get("customProperties");
                            if (customProperties != null
                                    && customProperties
                                    .containsKey("knowledges_order")) {
                                orderList = (List) customProperties
                                        .get("knowledges_order");
                                break;
                            }
                        }
                    }
                    final List<String> orders = orderList;

                    List<Map<String, Object>> list = knowledgesMap.get(target);
                    if (null == list) {
                        list = new ArrayList<>();
                        knowledgesMap.put(target, list);
                    }
                    list.add(map);
                    // 多个知识点有排序(使用custom_properties里面的knowledges_order指定顺序来进行排序)
                    Collections.sort(list,
                            new Comparator<Map<String, Object>>() {
                                @Override
                                public int compare(Map<String, Object> o1,
                                                   Map<String, Object> o2) {
                                    Integer order1 = 0;
                                    Integer order2 = 0;
                                    if (CollectionUtils.isNotEmpty(orders)) {
                                        String id1 = (String) o1.get("id");
                                        String id2 = (String) o2.get("id");
                                        order1 = orders.indexOf(id1);
                                        order2 = orders.indexOf(id2);
                                        if (order1 == -1) {
                                            order1 = 100;
                                        }
                                        if (order2 == -1) {
                                            order2 = 100;
                                        }
                                    }
                                    return order1 - order2;
                                }
                            });
                }
            }

            Map<String, String> results = new HashMap<>();

            for (Map.Entry<String, String> idWithTitle : idWithTitles) {
                String id = idWithTitle.getKey();

                Map<String, Object> instructionalObjective2Type = instructionalObjective2TypeMap
                        .get(id);
                List<Map<String, Object>> knowledges = knowledgesMap.get(id);
                if (CollectionUtils.isEmpty(knowledges)
                        || CollectionUtils.isEmpty(instructionalObjective2Type)) {
                    results.put(id, idWithTitle.getValue());
                    continue;
                }

                // 获取多个知识点的title
                Collection<String> knowledgesTitle = Collections2.transform(
                        knowledges,
                        new Function<Map<String, Object>, String>() {
                            @Nullable
                            @Override
                            public String apply(
                                    Map<String, Object> stringObjectMap) {
                                return (String) stringObjectMap.get("title");
                            }
                        });

                String typeString = (String) instructionalObjective2Type
                        .get("description");
                Map<String, Object> customPropertiesMap = null;
                if (typeString.contains(SPECIAL_SPLIT)) {
                    for (Map<String, Object> mm : iList) {
                        if (id.equals(mm.get("identifier"))) {
                            customPropertiesMap = (Map) mm
                                    .get("customProperties");
                        }
                    }
                }

                results.put(
                        id,
                        toInstructionalObjectiveTitle(typeString,
                                knowledgesTitle, idWithTitle.getValue(),
                                customPropertiesMap));
            }

            return results;
        } catch (DataAccessException e) {
            LOG.error("根据教学目标获取Title出错！", e);
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public ListViewModel<InstructionalObjectiveModel> getUnRelationInstructionalObjective(
            String knowledgeTypeCode, String instructionalObjectiveTypeId,
            String unrelationCategory, String limit) {

        try {

            IndexSourceType[] unrelationType = new IndexSourceType[]{
                    IndexSourceType.ChapterType, IndexSourceType.LessonType};
            Collection<String> unrelationCategoryString = new ArrayList<>();
            for (IndexSourceType indexSourceType : unrelationType) {
                if (!indexSourceType.getName().equals(unrelationCategory)) {
                    unrelationCategoryString.add(String.format(
                            "res_type=\"%s\"", indexSourceType.getName()));
                }
            }

            String SQLFmt = "SELECT %s from `ndresource` as ndr LEFT join  `resource_relations` as rr ON ndr.identifier=rr.target"
                    + " AND rr.enable=1 AND rr.resource_target_type=\"instructionalobjectives\" AND ("
                    + StringUtils.join(unrelationCategoryString, " or ")
                    + ")"
                    + " WHERE ndr.enable=1 AND rr.res_type is null\n";
            if (org.apache.commons.lang3.StringUtils
                    .isNotBlank(knowledgeTypeCode)) {
                SQLFmt += String
                        .format(" and ndr.identifier in (SELECT target FROM `resource_relations` WHERE ENABLE=1 and res_type=\"knowledges\""
                                        + " AND resource_target_type=\"instructionalobjectives\" AND source_uuid in (select resource from `resource_categories` WHERE taxonCode=\"%s\"))",
                                knowledgeTypeCode);
            }
            if (org.apache.commons.lang3.StringUtils
                    .isNotBlank(instructionalObjectiveTypeId)) {
                SQLFmt += String
                        .format("and ndr.identifier in (SELECT target from `resource_relations` WHERE ENABLE=1 AND resource_target_type=\"instructionalobjectives\""
                                        + " and source_uuid=\"%s\")",
                                instructionalObjectiveTypeId);
            }

            Integer[] limits = ParamCheckUtil.checkLimit(limit);

            // result
            final List<InstructionalObjectiveModel> results = new ArrayList<>();
            jt.query(
                    String.format(
                            SQLFmt
                                    + String.format(" LIMIT %d,%d", limits[0],
                                    limits[1]), "ndr.*"),
                    new RowCallbackHandler() {
                        @Override
                        public void processRow(ResultSet resultSet)
                                throws SQLException {
                            Collection<String> array = new ArrayList<>();

                            ResultSetMetaData rsmd = resultSet.getMetaData();
                            int columnCount = rsmd.getColumnCount();

                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = JdbcUtils.lookupColumnName(
                                        rsmd, i);
                                Field field = ReflectionUtils.findField(
                                        InstructionalObjectiveModel.class,
                                        StringUtils.toCamelCase(columnName));
                                if (null != field) {
                                    if (String.class == field.getType()) {
                                        String content = resultSet.getString(i);
                                        if (StringUtils.isNotEmpty(content)
                                                && content.contains("\"")) {
                                            content = StringUtils.replace(
                                                    content, "\"", "\\\"");
                                        }
                                        StringUtils.hasText("\"");
                                        array.add(String.format(
                                                "\"%s\":\"%s\"", columnName,
                                                content));
                                    } else {
                                        array.add(String.format("\"%s\":%s",
                                                columnName, JdbcUtils
                                                        .getResultSetValue(
                                                                resultSet, i)));
                                    }
                                }
                            }

                            String jsonString = String.format("{%s}",
                                    StringUtils.join(array, ","));

                            try {
                                JacksonCustomObjectMapper mapper = new JacksonCustomObjectMapper();
                                InstructionalObjectiveModel instructionalObjectiveModel = mapper
                                        .readValue(
                                                jsonString,
                                                InstructionalObjectiveModel.class);

                                results.add(instructionalObjectiveModel);
                            } catch (IOException e) {
                                LOG.error(
                                        "查询未关联教学目标转换InstructionalObjectiveModel出错！",
                                        e);
                            }
                        }
                    });
            // count
            Map<String, Object> count = jt.queryForMap(String.format(SQLFmt,
                    "count(*) as count"));

            ListViewModel<InstructionalObjectiveModel> listViewModel = new ListViewModel<>();
            listViewModel.setLimit(limit);
            listViewModel.setItems(results);
            listViewModel.setTotal((Long) count.get("count"));

            return listViewModel;
        } catch (Exception e) {
            LOG.error("查询未关联教学目标出错！", e);
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getLocalizedMessage());
        }
    }

    @Override
    public InstructionalObjectiveModel patchInstructionalObjective(
            InstructionalObjectiveModel instructionalObjectiveModel) {
        // 调用通用创建接口
        instructionalObjectiveModel.setTechInfoList(null);
        instructionalObjectiveModel = (InstructionalObjectiveModel) ndResourceService
                .patch(ResourceNdCode.instructionalobjectives.toString(),
                        instructionalObjectiveModel);
        instructionalObjectiveModel.setPreview(null);
        instructionalObjectiveModel.setEducationInfo(null);

        return instructionalObjectiveModel;
    }

    @Override
    public ListViewModel4Suite queryListBySuiteId(String suiteId, String limit) {
        Integer result[] = ParamCheckUtil.checkLimit(limit);
        ListViewModel4Suite<Map<String, Object>> lvm = new ListViewModel4Suite<Map<String, Object>>();
        // 1、获取total值
        Integer total = instructionalobjectiveDao.queryCountBySuiteId(suiteId);

        // 2、获取total值（包含未挂教学目标的教学目标类型数量，用于真正的分页）
        Integer total4Limit = instructionalobjectiveDao
                .queryCountBySuiteId4Blank(suiteId);

        // 3、获取列表
        List<Map<String, Object>> list = instructionalobjectiveDao
                .queryListBySuiteId(suiteId, result[0] + "," + result[1]);

        // 4、处理教学目标的title值
        Collection<Map.Entry<String, String>> idWithTitles = Collections2
                .transform(list,
                        new Function<Map, Map.Entry<String, String>>() {

                            @Override
                            public Entry<String, String> apply(Map input) {
                                return new HashMap.SimpleEntry<>((String) input
                                        .get("identifier"), (String) input
                                        .get("title"));
                            }
                        });
        Map<String, String> titlesMap = getInstructionalObjectiveTitle(idWithTitles);

        for (Map<String, Object> map : list) {
            // 处理lifeCycle值
            Map<String, Object> m = new HashMap<String, Object>();
            String version = (String) map.get("version");
            String status = (String) map.get("status");
            String creator = (String) map.get("creator");
            Long createTime = (Long) map.get("create_time");
            Long lastUpdate = (Long) map.get("last_update");
            m.put("version", version);
            m.put("status", status);
            m.put("creator", creator);
            m.put("enable", true);
            m.put("create_time", createTime == null ? null : new Date(
                    createTime.longValue()));
            m.put("last_update", lastUpdate == null ? null : new Date(
                    lastUpdate.longValue()));
            map.remove("version");
            map.remove("status");
            map.remove("creator");
            map.remove("create_time");
            map.remove("last_update");
            if ((String) map.get("identifier") != null) {
                map.put("life_cycle", m);
            }
            map.put("title", titlesMap.get((String) map.get("identifier")));
        }

        lvm.setItems(list);
        lvm.setTotal((long) total);
        lvm.setLimit(limit);
        lvm.setTotal4Limit((long) total4Limit);
        return lvm;
    }

    /**
     * 查询样例
     *
     * @param sourceId
     * @param targetIds
     * @return
     */
    public List<Map<String, String>> querySample(String sourceId,
                                                 Set<String> targetIds) {
        String sql = "SELECT identifier,source_uuid,target,estatus from resource_relations where res_type = 'assets' and resource_target_type = 'knowledges' and enable = 1 and relation_type = 'SAMPLE' and source_uuid = :sourceId and target in (:targetIds)";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("sourceId", sourceId);
        params.put("targetIds", targetIds);

        List<Map<String, String>> list = npjt.query(sql, params,
                new RowMapper<Map<String, String>>() {
                    @Override
                    public Map<String, String> mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        Map<String, String> m = new HashMap<String, String>();
                        String identifier = rs.getString("identifier");
                        String sourceId = rs.getString("source_uuid");
                        String targetId = rs.getString("target");
                        m.put("identifier", identifier);
                        m.put("sourceId", sourceId);
                        m.put("targetId", targetId);
                        m.put("estatus", rs.getString("estatus"));
                        return m;
                    }
                });
        return list;
    }


    @Autowired
    private EduRedisTemplate<Category> etCategory;

    @Autowired
    private EduRedisTemplate<CategoryData> etCategoryData;

    @Autowired
    private ContributeRepository contributeRepository;

    @Autowired
    private AsynEsResourceService esResourceOperation;

    @Autowired
    private CommonServiceHelper commonServiceHelper;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ResourceRelationRepository resourceRelationRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    @Autowired
    private InstructionalobjectiveRepository objectiveRepository;

    @Qualifier(value = "defaultJdbcTemplate")
    @Autowired
    private JdbcTemplate defaultJdbcTemplate;

    // redis缓存
    @Autowired
    private EduRedisTemplate<Map<String, List<String>>> ert;

    // 使用线程池
    private final static ExecutorService executorService = CommonHelper
            .getPrimaryExecutorService();

    private final String SPECIAL_SPLIT = "（X）";


    /**
     * @param typeString          教学目标类型描述
     * @param knowledgeTitle      知识点title
     * @param originTitle
     * @param customPropertiesMap customProperties集合
     * @return 拼接后的字符串
     */
    private String toInstructionalObjectiveTitle(String typeString,
                                                 Collection<String> knowledgeTitle, String originTitle,
                                                 Map<String, Object> customPropertiesMap) {
        Pattern pattern = Pattern.compile("<span.*?>.*?</span>");
        String[] split = pattern.split(typeString);
        String[] knowledges = knowledgeTitle.toArray(new String[knowledgeTitle
                .size()]);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            sb.append(split[i]);
            sb.append(knowledges.length > i ? knowledges[i] : "");
        }
        // 替换（X）
        String xn = sb.toString();
        if (xn.contains(SPECIAL_SPLIT)) {
            if (customPropertiesMap != null) {
                StringBuilder sbTmp = new StringBuilder();
                String[] xs = xn.split(SPECIAL_SPLIT);
                int length = xs.length;
                if (xn.endsWith(SPECIAL_SPLIT)) {
                    length++;
                }
                for (int i = 0; i < xs.length; i++) {
                    sbTmp.append(xs[i]);
                    String sin = "input_" + i;
                    if (customPropertiesMap.containsKey(sin)
                            && (String) customPropertiesMap.get(sin) != null
                            && i < length - 1) {
                        sbTmp.append((String) customPropertiesMap.get(sin));
                    }
                }
                return sbTmp.toString();
            } else {
                return sb.toString().replaceAll(SPECIAL_SPLIT, "");
            }
        }
        return xn;
    }

    @Override
    public ListViewModel<ResourceModel> getResourcePageByChapterId(
            List<String> includes, List<Map<String, String>> relationsMap,
            List<String> coveragesList, String limit, boolean reverseBoolean) {

        ListViewModel<ResourceModel> rListViewModel = new ListViewModel<ResourceModel>();
        rListViewModel.setLimit(limit);

        String stype = relationsMap.get(0).get("stype");
        String suuid = relationsMap.get(0).get("suuid");
        String rtype = relationsMap.get(0).get("rtype");

        String sqlSelect = "select ";

        String sqlCount = " count(distinct(ndr.identifier)) ";

        String sqlCommonColumn = " distinct(ndr.identifier) AS identifier,ndr.title AS title,ndr.description AS description,ndr.elanguage AS language, "
                + " null AS preview, ndr.tags AS tags,ndr.keywords AS keywords,ndr.custom_properties as customProperties, ndr.code as code,null AS relationId ";
        // LC
        String sqlLifeCycleColumn = " ndr.version AS lifeCycle_version,ndr.estatus AS lifeCycle_status,ndr.enable AS lifeCycle_enable,ndr.creator AS lifeCycle_creator,ndr.publisher AS lifeCycle_publisher,ndr.provider AS lifeCycle_provider,ndr.provider_source AS lifeCycle_providerSource,ndr.create_time AS lifeCycle_createTime,ndr.last_update AS lifeCycle_lastUpdate ";
        String sqlLifeCycleColumn4Null = " null AS lifeCycle_version,null AS lifeCycle_status,null AS lifeCycle_enable,null AS lifeCycle_creator,null AS lifeCycle_publisher,null AS lifeCycle_provider,null AS lifeCycle_providerSource,null AS lifeCycle_createTime,null AS lifeCycle_lastUpdate ";
        // EDU
        String sqlEducationInfoColumn = " ndr.interactivity AS educationInfo_interactivity,ndr.interactivity_level AS educationInfo_interactivityLevel,ndr.end_user_type AS educationInfo_endUserType,ndr.semantic_density AS educationInfo_semanticDensity,ndr.context AS educationInfo_context,ndr.age_range AS educationInfo_ageRange,ndr.difficulty AS educationInfo_difficulty,ndr.learning_time AS educationInfo_learningTime,ndr.edu_description AS educationInfo_description,ndr.edu_language AS educationInfo_language ";
        String sqlEducationInfoColumn4Null = " null AS educationInfo_interactivity,null AS educationInfo_interactivityLevel,null AS educationInfo_endUserType,null AS educationInfo_semanticDensity,null AS educationInfo_context,null AS educationInfo_ageRange,null AS educationInfo_difficulty,null AS educationInfo_learningTime,null AS educationInfo_description,null AS educationInfo_language ";
        // CR
        String sqlCopyRightColumn = " ndr.cr_right AS copyRight_right,ndr.cr_description AS copyRight_crDescription,ndr.author AS copyRight_author ";
        String sqlCopyRightColumn4Null = " null AS copyRight_right,null AS copyRight_crDescription,null AS copyRight_author ";

        String sqlFrom = " FROM `ndresource` ndr "
                + "INNER JOIN ( "
                + " SELECT * "
                + " FROM `resource_relations` t_chapters_instructionalobjectives "
                + " LEFT JOIN ( "
                + "  SELECT t_chapters_lessons.order_num AS order_num1 "
                + "   ,t_lessons_instructionalobjectives.target AS target2 "
                + "   ,t_lessons_instructionalobjectives.sort_num AS sort_num2 "
                + "  FROM `resource_relations` t_lessons_instructionalobjectives "
                + "  INNER JOIN ( " + "   SELECT t1.* "
                + "   FROM `resource_relations` t1 "
                + "   WHERE t1.source_uuid = '"
                + suuid
                + "' "
                + "    AND t1.res_type = 'chapters' "
                + "    AND t1.resource_target_type = 'lessons' "
                + "    AND t1.relation_type = 'ASSOCIATE' "
                + "    AND t1.enable = 1 "
                + "   ) AS t_chapters_lessons ON t_lessons_instructionalobjectives.source_uuid = t_chapters_lessons.target "
                + "  WHERE t_lessons_instructionalobjectives.res_type = 'lessons' "
                + "   AND t_lessons_instructionalobjectives.resource_target_type = 'instructionalobjectives' "
                + "   AND t_lessons_instructionalobjectives.relation_type = 'ASSOCIATE' "
                + "   AND t_lessons_instructionalobjectives.enable = 1 "
                + "  ) AS t_chapters_lessons_instructionalobjectives ON t_chapters_instructionalobjectives.target = t_chapters_lessons_instructionalobjectives.target2 "
                + " WHERE t_chapters_instructionalobjectives.source_uuid = '"
                + suuid
                + "' "
                + "  AND t_chapters_instructionalobjectives.res_type = 'chapters' "
                + "  AND t_chapters_instructionalobjectives.resource_target_type = 'instructionalobjectives' "
                + "  AND t_chapters_instructionalobjectives.relation_type = 'ASSOCIATE' "
                + "  AND t_chapters_instructionalobjectives.enable = 1 "
                + " ORDER BY - t_chapters_lessons_instructionalobjectives.order_num1 DESC "
                + "  ,- t_chapters_lessons_instructionalobjectives.sort_num2 DESC "
                + "  ,- t_chapters_instructionalobjectives.sort_num DESC "
                + " ) AS t_chapters_instructionalobjectives_sequential ON ndr.identifier = t_chapters_instructionalobjectives_sequential.target "
                + "WHERE ndr.primary_category = 'instructionalobjectives' "
                + " AND ndr.enable = 1 "
                + " AND EXISTS ("
                + " SELECT 1 "
                + " FROM `res_coverages` t_rc "
                + " WHERE ndr.identifier=t_rc.resource "
                + "   AND t_rc.res_type='instructionalobjectives' "
                + "   AND t_rc.target_type='Org' " + "   AND t_rc.target='nd')";

        Integer result[] = ParamCheckUtil.checkLimit(limit);
        String sqlLimit = " LIMIT " + result[0] + "," + result[1];

        // 查询记录
        String sqlItemsQuery = sqlSelect + sqlCommonColumn + ","
                + sqlLifeCycleColumn + "," + sqlEducationInfoColumn4Null + ","
                + sqlCopyRightColumn4Null + sqlFrom + sqlLimit;
        Query itemsQuery = defaultEm.createNativeQuery(sqlItemsQuery,
                FullModel.class);
        List<FullModel> queryResult = itemsQuery.getResultList();

        // 查询数量
        String sqlCountQuery = sqlSelect + sqlCount + sqlFrom;
        Query countQuery = defaultEm.createNativeQuery(sqlCountQuery);
        BigInteger intTotal = (BigInteger) countQuery.getSingleResult();
        Long total = new Long(intTotal.intValue());

        // 结果集
        final Map<String, ResourceModel> resultMap = new LinkedHashMap<String, ResourceModel>();

        // ****************************结果集处理--Start****************************//
        for (FullModel fullModel : queryResult) {
            ResourceModel resourceModel = new ResourceModel();

            // 通用属性
            resourceModel.setIdentifier(fullModel.getIdentifier());
            resourceModel.setTitle(fullModel.getTitle());
            resourceModel.setDescription(fullModel.getDescription());
            resourceModel.setLanguage(fullModel.getLanguage());
            resourceModel.setKeywords(ObjectUtils.fromJson(
                    fullModel.getKeywords(), new TypeToken<List<String>>() {
                    }));
            resourceModel.setTags(ObjectUtils.fromJson(fullModel.getTags(),
                    new TypeToken<List<String>>() {
                    }));

            resourceModel.setCustomProperties(fullModel.getCustomProperties());
            resourceModel.setNdresCode(fullModel.getCode());
            resourceModel.setRelationId(fullModel.getRelationId());

            // LC

            ResLifeCycleModel rlc = new ResLifeCycleModel();
            rlc.setVersion(fullModel.getLifeCycle_version());
            rlc.setStatus(fullModel.getLifeCycle_status());

            int enableInt = StringUtils.isNotEmpty(fullModel
                    .getLifeCycle_enable()) ? Integer.parseInt(fullModel
                    .getLifeCycle_enable()) : 0;
            rlc.setEnable(enableInt == 1 ? true : false);
            rlc.setCreator(fullModel.getLifeCycle_creator());
            rlc.setPublisher(fullModel.getLifeCycle_publisher());
            rlc.setProvider(fullModel.getLifeCycle_provider());
            rlc.setProviderSource(fullModel.getLifeCycle_providerSource());

            long createTimeLong = StringUtils.isNotEmpty(fullModel
                    .getLifeCycle_createTime()) ? Long.parseLong(fullModel
                    .getLifeCycle_createTime()) : 0L;
            rlc.setCreateTime(new Date(createTimeLong));

            long lastUpdateLong = StringUtils.isNotEmpty(fullModel
                    .getLifeCycle_lastUpdate()) ? Long.parseLong(fullModel
                    .getLifeCycle_lastUpdate()) : 0L;
            rlc.setLastUpdate(new Date(lastUpdateLong));

            resourceModel.setLifeCycle(rlc);

            resultMap.put(fullModel.getIdentifier(), resourceModel);
        }
        // ****************************结果集处理--End****************************//

        // 返回的list
        List<ResourceModel> resultList = new ArrayList<ResourceModel>();
        for (String key : resultMap.keySet()) {
            resultList.add(resultMap.get(key));
        }

        rListViewModel.setItems(resultList);
        rListViewModel.setTotal(total);
        return rListViewModel;
    }


    @Override
    public Object create4Business(SuiteViewModel svm) {
        List<Map<String, Object>> errorList = new ArrayList<Map<String, Object>>();
        // 套件id
        String suiteId = svm.getSuitId();

        // 创建者
        String creator = svm.getUserId();

        // 新增或修改的教学目标列表
        List<InstructionalObjective> createInstructionList = new ArrayList<InstructionalObjective>();

        // 新增子教学教学目标列表
        List<SubInstruction> createSubInstructionList = new ArrayList<SubInstruction>();

        // 新增的知识点列表
        List<Chapter> createKnowledgeList = new ArrayList<Chapter>();


        // 删除关系列表
        List<String> deleteResourceRelationIds = new ArrayList<String>();

        // 创建关系列表
        List<ResourceRelation> createResourceRelationList = new ArrayList<ResourceRelation>();
        // 创建样例列表
        List<Sample> createSampleList = new ArrayList<>();

        // 创建资源维度
        List<ResourceCategory> createResourceCategoryList = new ArrayList<ResourceCategory>();

        // 删除资源维度数据
        List<String> deleteResourceCategoryIds = new ArrayList<String>();

        // 创建资源覆盖范围
        List<ResCoverage> createCoverageList = new ArrayList<ResCoverage>();

        // 记录生命周期状态
        List<Contribute> contributeList = new ArrayList<Contribute>();

        // 保存知识点title
        Map<String, String> knTitles = new HashMap<>();

        // 保存学习目标类型id和对应的最大的sort_num
        Map<String, Float> sortNumMap = new HashMap<>();

        //返回的套件id
        List<Map<String, String>> sampleIdList = new ArrayList<Map<String, String>>();

        //知识点列表
        List<SuiteKnowledgesViewModel> knowledges = svm.getKnowledges();

        //自定义属性
        Map<String, Object> sampleCustomPropertiesMap = svm.getCustomProperties();

        // 判断知识点
        if (CollectionUtils.isEmpty(knowledges)) {
            return getReturnInfo("", "", "", null, "新增教学目标必须包含知识点数据");
        }

        List<SuiteObjectivesViewModel> objectives = svm.getObjectives();
        if (CollectionUtils.isNotEmpty(objectives)) {
            Timestamp ts2 = new Timestamp(System.currentTimeMillis());
            Timestamp ts3 = new Timestamp(System.currentTimeMillis());
            int versionNum = 0;
            for (SuiteObjectivesViewModel suiteObjectivesViewModel : objectives) {
                boolean flag = false;
                //判断是否为copy的学习目标，不传默认为associate
                String relationType = suiteObjectivesViewModel.getRelationType();

                String copySuiteId = suiteObjectivesViewModel.getCopySuiteId();

                if (StringUtils.isEmpty(relationType)) {
                    relationType = RelationType.ASSOCIATE.getName();
                }

                // 使用到的知识点id列表
                Set<String> knowledgeIds = new LinkedHashSet<>();
                // 教学目标id
                String identifier = suiteObjectivesViewModel.getIdentifier();

                // 教学目标title
                String title = suiteObjectivesViewModel.getTitle();

                // 教学目标的状态
                String status = suiteObjectivesViewModel.getStatus();

                // 教学目标描述信息
                String description = suiteObjectivesViewModel.getDescription();

                // 教学目标类型id
                String objectiveTypeId = suiteObjectivesViewModel
                        .getObjectiveTypeId();

                // 来源
                String providerSource = suiteObjectivesViewModel
                        .getProviderSource();

                // 关键字，存放备注信息
                List<String> keywords = suiteObjectivesViewModel.getKeywords();

                // 适用学段
                List<String> applicablePeriod = suiteObjectivesViewModel
                        .getApplicablePeriod();

                // 操作类型
                int operateType = suiteObjectivesViewModel.getOperateType();// 0代表返回提示，1代表新增，2代表替换

                // 教学目标自定义属性，可能存放教学目标类型（X）对应的文本，key使用input_0,input_1...来表示
                Map<String, Object> customProperties = suiteObjectivesViewModel
                        .getCustomProperties();

                // 教学目标多个版本描述
                List<String> versions = suiteObjectivesViewModel.getVersions();

                // 获取教学目标类型详情，主要是要获取知识点类型维度数据
                List<String> includeList = new ArrayList<String>();
                includeList.add("CG");
                ResourceModel rm = ndResourceService.getDetail(
                        IndexSourceType.AssetType.getName(), objectiveTypeId,
                        includeList);
                List<ResClassificationModel> categoryList = rm
                        .getCategoryList();

                // 根据title判断教学目标是否已存在
                List<InstructionalObjective> ioList = instructionalobjectiveDao
                        .queryListByTitle(title);

                if (StringUtils.isEmpty(identifier)) {
                    // 教学目标的新id
                    identifier = UUID.randomUUID().toString();

                    // 新增教学目标逻辑
                    if (CollectionUtils.isNotEmpty(ioList)) {
                        // 覆盖只是随机覆盖一条，所以不遍历ioList
                        InstructionalObjective instructionalObjective = ioList
                                .get(0);
                        // for (InstructionalObjective instructionalObjective :
                        // ioList) {
                        if (instructionalObjective.getEnable()) {
                            if (operateType == 0) {
                                flag = true;
                                boolean f = false;
                                for (Map<String, Object> m : errorList) {
                                    if (title.equals(m.get("title"))) {
                                        f = true;
                                        break;
                                    }
                                }
                                if (!f) {
                                    errorList.add(getReturnInfo(
                                            instructionalObjective
                                                    .getIdentifier(), title,
                                            instructionalObjective
                                                    .getDescription(),
                                            instructionalObjective
                                                    .getCustomProperties(),
                                            "教学目标已存在"));
                                }
                            } else if (operateType == 2) {
                                identifier = instructionalObjective
                                        .getIdentifier();
                                // 删除原教学目标以及对应的关系
                                // deleteInstructionIds.add(instructionalObjective.getIdentifier());

                                ResourceRelation rr = new ResourceRelation();
                                rr.setResType(IndexSourceType.InstructionalObjectiveType
                                        .getName());
                                rr.setSourceUuid(instructionalObjective
                                        .getIdentifier());
                                rr.setEnable(true);

                                ResourceRelation rr2 = new ResourceRelation();
                                rr2.setResourceTargetType(IndexSourceType.InstructionalObjectiveType
                                        .getName());
                                rr2.setTarget(instructionalObjective
                                        .getIdentifier());
                                rr2.setEnable(true);
                                try {
                                    List<ResourceRelation> ll = rrRepository
                                            .getAllByExample(rr);
                                    List<ResourceRelation> ll2 = rrRepository
                                            .getAllByExample(rr2);
                                    if (CollectionUtils.isNotEmpty(ll)) {
                                        for (ResourceRelation resourceRelation : ll) {
                                            deleteResourceRelationIds
                                                    .add(resourceRelation
                                                            .getIdentifier());
                                        }
                                    }

                                    if (CollectionUtils.isNotEmpty(ll2)) {
                                        for (ResourceRelation resourceRelation : ll2) {
                                            deleteResourceRelationIds
                                                    .add(resourceRelation
                                                            .getIdentifier());
                                        }
                                    }
                                } catch (EspStoreException e) {
                                    throw new LifeCircleException(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            LifeCircleErrorMessageMapper.StoreSdkFail
                                                    .getCode(), e.getMessage());
                                }

                            }
                        }
                        // 回收站资源暂不考虑 20161103
                        // else if(!instructionalObjective.getEnable() &&
                        // LifecycleStatus.RECYCLED.equals(instructionalObjective.getStatus())){
                        // if(operateType == 0){
                        // flag = true;
                        // errorList.add(getReturnInfo(identifier,title,"教学目标在回收站中已存在"));
                        // }
                        // }
                        // }

                        if (flag) {
                            continue;
                        }

                    }


                    // customProperties里面存放知识点的顺序{"knowledges_order":["",...]}
                    Map<String, Object> customPropertiesMap = new HashMap<String, Object>();
                    List<String> knowledgeOrderList = new LinkedList<String>();

                    Map<String, Object> tmpMap = dealKnowledge(identifier,
                            creator, knowledges, createKnowledgeList,
                            createResourceRelationList,
                            createResourceCategoryList, createCoverageList,
                            categoryList, knowledgeOrderList, knTitles, relationType);

                    if (CollectionUtils.isNotEmpty(tmpMap)) {
                        return tmpMap;
                    }

                    // 创建新的教学目标
                    customPropertiesMap.put("knowledges_order",
                            knowledgeOrderList);

                    // 建立大套件与主知识的关系
                    if (CollectionUtils.isNotEmpty(knowledgeOrderList)) {
                        for (String id : knowledgeOrderList) {
                            knowledgeIds.add(id);
                        }
                    }

                    if (CollectionUtils.isNotEmpty(customProperties)) {
                        customPropertiesMap.putAll(customProperties);
                    }
                    InstructionalObjective io = new InstructionalObjective();
                    Timestamp ts = new Timestamp(System.currentTimeMillis());
                    io.setIdentifier(identifier);
                    io.setmIdentifier(identifier);
                    io.setTitle(title);
                    io.setDescription(description);
                    io.setCreator(creator);
                    io.setVersion("v0.6");
                    io.setStatus(status);
                    io.setCreateTime(ts);
                    io.setLastUpdate(ts);
                    io.setKeywords(keywords);
                    io.setProviderSource(providerSource);
                    io.setCustomProperties(ObjectUtils
                            .toJson(customPropertiesMap));
                    io.setPrimaryCategory(IndexSourceType.InstructionalObjectiveType
                            .getName());
                    createInstructionList.add(io);
                    createCoverageList.add(addResCoverage(
                            IndexSourceType.InstructionalObjectiveType
                                    .getName(), identifier));
                    // 如果为覆盖动作就不需要添加
                    if (operateType != 2) {
                        createResourceCategoryList
                                .addAll(addResourceCategoryList(
                                        IndexSourceType.InstructionalObjectiveType
                                                .getName(), identifier, null));
                    } else {
                        deleteResourceCategoryIds
                                .addAll(queryResourceCategory4SL(identifier));
                    }
                    createResourceCategoryList
                            .addAll(addApplicablePeriodCategoryList(identifier,
                                    applicablePeriod));
                    // 教学目标类型与教学目标的关系

                    createResourceRelationList.add(addResourceRelation(
                            IndexSourceType.AssetType.getName(),
                            objectiveTypeId,
                            IndexSourceType.InstructionalObjectiveType.getName(),
                            identifier,
                            relationType,
                            dealObjectivesSortNum(objectiveTypeId, sortNumMap, RelationType.ASSOCIATE.getName())));

                    //在拷贝套件下增加学习目标要与拷贝套件建关系
                    if ("COPY".equals(relationType) && StringUtils.isNotEmpty(copySuiteId)) {
                        createResourceRelationList.add(addResourceRelation(
                                IndexSourceType.AssetType.getName(),
                                copySuiteId,
                                IndexSourceType.InstructionalObjectiveType.getName(),
                                identifier,
                                relationType,
                                5001f));
                    }

                    // 处理多版本描述
                    if (CollectionUtils.isNotEmpty(versions)) {
                        for (String v : versions) {
                            SubInstruction si = new SubInstruction();
                            String siId = UUID.randomUUID().toString();
                            si.setIdentifier(siId);
                            si.setmIdentifier(siId);
                            si.setTitle(title);
                            si.setDescription(v);
                            si.setCreator(creator);
                            si.setVersion("v0.6");
                            si.setStatus(status);
                            si.setCreateTime(ts3);
                            si.setLastUpdate(ts3);
                            si.setPrimaryCategory(IndexSourceType.SubInstructionType
                                    .getName());
                            // 创建子教学目标
                            createSubInstructionList.add(si);
                            // 教学目标与子教学目标的关系
                            createResourceRelationList.add(addResourceRelation(
                                    IndexSourceType.InstructionalObjectiveType
                                            .getName(), identifier,
                                    IndexSourceType.SubInstructionType
                                            .getName(), siId));
                            ts3 = new Timestamp(System.currentTimeMillis() + versionNum * 10);
                            versionNum++;
                        }
                    }
                } else {
                    // 修改教学目标逻辑
                    if (CollectionUtils.isNotEmpty(ioList)) {
                        InstructionalObjective instructionalObjective = ioList
                                .get(0);
                        // for (InstructionalObjective instructionalObjective :
                        // ioList) {
                        if (!instructionalObjective.getIdentifier().equals(
                                identifier)
                                && instructionalObjective.getEnable()) {
                            if (operateType == 0) {
                                flag = true;
                                boolean f = false;
                                for (Map<String, Object> m : errorList) {
                                    if (title.equals(m.get("title"))) {
                                        f = true;
                                        break;
                                    }
                                }
                                if (!f) {
                                    errorList.add(getReturnInfo(
                                            instructionalObjective
                                                    .getIdentifier(), title,
                                            instructionalObjective
                                                    .getDescription(),
                                            instructionalObjective
                                                    .getCustomProperties(),
                                            "教学目标已存在"));
                                }
                            } else if (operateType == 2) {
                                identifier = instructionalObjective
                                        .getIdentifier();
                                // 删除原教学目标以及对应的关系
                                // deleteInstructionIds.add(instructionalObjective.getIdentifier());

                                ResourceRelation rr = new ResourceRelation();
                                rr.setResType(IndexSourceType.InstructionalObjectiveType
                                        .getName());
                                rr.setSourceUuid(instructionalObjective
                                        .getIdentifier());
                                rr.setEnable(true);

                                ResourceRelation rr2 = new ResourceRelation();
                                rr2.setResourceTargetType(IndexSourceType.InstructionalObjectiveType
                                        .getName());
                                rr2.setTarget(instructionalObjective
                                        .getIdentifier());
                                rr2.setEnable(true);
                                try {
                                    List<ResourceRelation> ll = rrRepository
                                            .getAllByExample(rr);
                                    List<ResourceRelation> ll2 = rrRepository
                                            .getAllByExample(rr2);
                                    if (CollectionUtils.isNotEmpty(ll)) {
                                        for (ResourceRelation resourceRelation : ll) {
                                            deleteResourceRelationIds
                                                    .add(resourceRelation
                                                            .getIdentifier());
                                        }
                                    }

                                    if (CollectionUtils.isNotEmpty(ll2)) {
                                        for (ResourceRelation resourceRelation : ll2) {
                                            deleteResourceRelationIds
                                                    .add(resourceRelation
                                                            .getIdentifier());
                                        }
                                    }
                                } catch (EspStoreException e) {
                                    throw new LifeCircleException(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            LifeCircleErrorMessageMapper.StoreSdkFail
                                                    .getCode(), e.getMessage());
                                }
                            }
                        }
                        // 回收站资源暂不考虑 20161103
                        // else if (!instructionalObjective.getIdentifier()
                        // .equals(identifier)
                        // && !instructionalObjective.getEnable()
                        // && LifecycleStatus.RECYCLED
                        // .equals(instructionalObjective
                        // .getStatus())) {
                        // if(operateType == 0){
                        // flag = true;
                        // errorList.add(getReturnInfo(identifier,title,"教学目标在回收站中已存在"));
                        // }
                        // }
                        // }
                    }

                    // 获取教学目标详情
                    InstructionalObjective oldEntity = instructionalobjectiveDao
                            .getInstructionalObjective(identifier);
                    if (oldEntity == null || !oldEntity.getEnable()) {
                        return getReturnInfo(identifier, title, description,
                                null, "教学目标不存在");
                    }

                    // customProperties里面存放知识点的顺序{"knowledges_order":["",...]}
                    Map<String, Object> customPropertiesMap = new HashMap<String, Object>();
                    List<String> knowledgeOrderList = new LinkedList<String>();

                    // 有传入知识点数据
                    if (CollectionUtils.isNotEmpty(knowledges)) {
                        Map<String, Object> tmpMap = dealKnowledge(identifier,
                                creator, knowledges, createKnowledgeList,
                                createResourceRelationList,
                                createResourceCategoryList, createCoverageList,
                                categoryList, knowledgeOrderList, knTitles, relationType);
                        if (CollectionUtils.isNotEmpty(tmpMap)) {
                            return tmpMap;
                        }

                        // 删除教学目标原先与知识点的关系
                        ResourceRelation rr = new ResourceRelation();
                        rr.setResType(IndexSourceType.KnowledgeType.getName());
                        rr.setResourceTargetType(IndexSourceType.InstructionalObjectiveType
                                .getName());
                        rr.setTarget(identifier);
                        try {
                            List<ResourceRelation> ll = rrRepository
                                    .getAllByExample(rr);
                            if (CollectionUtils.isNotEmpty(ll)) {
                                for (ResourceRelation resourceRelation : ll) {
                                    deleteResourceRelationIds
                                            .add(resourceRelation
                                                    .getIdentifier());
                                }
                            }
                        } catch (EspStoreException e) {
                            throw new LifeCircleException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    LifeCircleErrorMessageMapper.StoreSdkFail
                                            .getCode(), e.getMessage());
                        }

                        customPropertiesMap.put("knowledges_order",
                                knowledgeOrderList);

                        // 建立大套件与主知识的关系
                        if (CollectionUtils.isNotEmpty(knowledgeOrderList)) {
                            for (String id : knowledgeOrderList) {
                                knowledgeIds.add(id);
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(customProperties)) {
                        customPropertiesMap.putAll(customProperties);
                    }

                    // 修改教学目标数据
                    oldEntity.setTitle(title);
                    oldEntity.setDescription(description);
                    oldEntity.setStatus(status);
                    oldEntity.setProviderSource(providerSource);
                    oldEntity.setDbkeywords(ObjectUtils.toJson(keywords));

                    if (CollectionUtils.isNotEmpty(customPropertiesMap)) {
                        if (StringUtils
                                .isEmpty(oldEntity.getCustomProperties())) {
                            oldEntity.setCustomProperties(ObjectUtils
                                    .toJson(customPropertiesMap));
                        } else {
                            String s = oldEntity.getCustomProperties();
                            Map mm = ObjectUtils.fromJson(s, Map.class);
                            if (mm != null) {
                                mm.putAll(customPropertiesMap);
                                oldEntity.setCustomProperties(ObjectUtils
                                        .toJson(customPropertiesMap));
                            } else {
                                oldEntity.setCustomProperties(ObjectUtils
                                        .toJson(customPropertiesMap));
                            }
                        }
                    }
                    createInstructionList.add(oldEntity);
                    // 先删除教学目标的SL的维度数据再新增
                    deleteResourceCategoryIds
                            .addAll(queryResourceCategory4SL(identifier));
                    createResourceCategoryList
                            .addAll(addApplicablePeriodCategoryList(identifier,
                                    applicablePeriod));

                    // 删除教学目标与子教学目标旧的关系数据
                    ResourceRelation tmp = new ResourceRelation();
                    tmp.setResType(IndexSourceType.InstructionalObjectiveType
                            .getName());
                    tmp.setSourceUuid(identifier);
                    tmp.setResourceTargetType(IndexSourceType.SubInstructionType
                            .getName());
                    tmp.setEnable(true);
                    try {
                        List<ResourceRelation> rrl = rrRepository
                                .getAllByExample(tmp);
                        if (CollectionUtils.isNotEmpty(rrl)) {
                            for (ResourceRelation resourceRelation : rrl) {
                                deleteResourceRelationIds.add(resourceRelation
                                        .getIdentifier());
                            }
                        }
                    } catch (EspStoreException e) {
                        throw new LifeCircleException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                LifeCircleErrorMessageMapper.StoreSdkFail
                                        .getCode(), e.getMessage());
                    }

                    // 处理多版本描述
                    if (CollectionUtils.isNotEmpty(versions)) {
                        for (String v : versions) {

                            SubInstruction si = new SubInstruction();
                            String siId = UUID.randomUUID().toString();
                            si.setIdentifier(siId);
                            si.setmIdentifier(siId);
                            si.setTitle(title);
                            si.setDescription(v);
                            si.setCreator(creator);
                            si.setVersion("v0.6");
                            si.setStatus(status);
                            si.setCreateTime(ts3);
                            si.setLastUpdate(ts3);
                            si.setPrimaryCategory(IndexSourceType.SubInstructionType
                                    .getName());
                            // 创建子教学目标
                            createSubInstructionList.add(si);
                            // 教学目标与子教学目标的关系
                            createResourceRelationList.add(addResourceRelation(
                                    IndexSourceType.InstructionalObjectiveType
                                            .getName(), identifier,
                                    IndexSourceType.SubInstructionType
                                            .getName(), siId,
                                    relationType));
                            ts3 = new Timestamp(System.currentTimeMillis() + versionNum * 10);
                            versionNum++;
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(knowledgeIds)) {
                    // 查看样例是否存在
                    // TODO 修改查询方法，查询sample表
                    List<String> knowledgeIdList = new ArrayList<>();
                    List<String> oldKnId = new ArrayList<>();
                    for (String id : knowledgeIds) {
                        knowledgeIdList.add(id);
                        oldKnId.add(id);
                    }

                    String temp = "";
                    for (int i = 0; i < knowledgeIdList.size(); i++) {
                        for (int j = i; j < knowledgeIdList.size(); j++) {
                            if (knowledgeIdList.get(i).hashCode() > knowledgeIdList.get(j).hashCode()) {
                                temp = knowledgeIdList.get(i);
                                knowledgeIdList.set(i, knowledgeIdList.get(j));
                                knowledgeIdList.set(j, temp);
                            }
                        }
                    }

                    List<Sample> sampleList = new ArrayList<>();
                    switch (knowledgeIdList.size()) {
                        case 1: {
                            sampleList = sampleRepository.querySampleBySuiteIdAndKnId1(
                                    suiteId, knowledgeIdList.get(0));
                            break;
                        }
                        case 2: {
                            sampleList = sampleRepository.querySampleBySuiteIdAndKnId2(
                                    suiteId, knowledgeIdList.get(0),
                                    knowledgeIdList.get(1));
                            break;
                        }
                        case 3: {
                            sampleList = sampleRepository.querySampleBySuiteIdAndKnId3(
                                    suiteId, knowledgeIdList.get(0),
                                    knowledgeIdList.get(1), knowledgeIdList.get(2));
                            break;
                        }
                        case 4: {
                            sampleList = sampleRepository.querySampleBySuiteIdAndKnId4(
                                    suiteId, knowledgeIdList.get(0),
                                    knowledgeIdList.get(1), knowledgeIdList.get(2),
                                    knowledgeIdList.get(3));
                            break;
                        }
                        case 5: {
                            sampleList = sampleRepository.querySampleBySuiteIdAndKnId5(
                                    suiteId, knowledgeIdList.get(0),
                                    knowledgeIdList.get(1), knowledgeIdList.get(2),
                                    knowledgeIdList.get(3), knowledgeIdList.get(4));
                            break;
                        }
                        default:
                            break;
                    }

                    if (CollectionUtils.isEmpty(sampleList)) {
                        Sample sample = addSample(suiteId, oldKnId, knowledgeIdList,
                                creator, knTitles);

                        //样例增加自定义属性
                        if (CollectionUtils.isNotEmpty(sampleCustomPropertiesMap)) {
                            sample.setCustomProperties(ObjectUtils.toJson(sampleCustomPropertiesMap));
                        }

                        if (checkSampleList(createSampleList, sample, knowledgeIdList.size())) {
                            createSampleList.add(sample);
                            Map<String, String> m = new HashMap<String, String>();
                            m.put("sample_id", sample.getIdentifier());
                            m.put("title", sample.getTitle());
                            sampleIdList.add(m);
                        }
                        contributeList
                                .add(addContribute("samples",
                                        sample.getIdentifier(),
                                        LifecycleStatus.CREATED.getCode(), "", ts2,
                                        "创建样例"));
                    } else {
                        //修改样例的自定义属性
                        Sample sample = sampleList.get(0);
                        if (CollectionUtils.isNotEmpty(sampleCustomPropertiesMap)) {
                            sample.setCustomProperties(ObjectUtils.toJson(sampleCustomPropertiesMap));
                        }
                        createSampleList.add(sample);
                    }
                }

            }

            if (CollectionUtils.isNotEmpty(errorList)) {
                return errorList;
            }

            if (StringUtils.isNotEmpty(suiteId)) {

                // 统一处理数据
                try {

                    Timestamp ts = new Timestamp(System.currentTimeMillis());

                    if (CollectionUtils.isNotEmpty(deleteResourceCategoryIds)) {
                        rcRepository.batchDel(deleteResourceCategoryIds);
                    }
                    if (CollectionUtils.isNotEmpty(createInstructionList)) {
                        ioRepository.batchAdd(createInstructionList);
                        for (InstructionalObjective io : createInstructionList) {
                            contributeList.add(addContribute(
                                    IndexSourceType.InstructionalObjectiveType
                                            .getName(), io.getIdentifier(), io
                                            .getStatus(), io.getCreator(), ts,
                                    "创建资源"));
                        }
                    }
                    if (CollectionUtils.isNotEmpty(createKnowledgeList)) {
                        chapterRepository.batchAdd(createKnowledgeList);
                        for (Chapter c : createKnowledgeList) {
                            contributeList.add(addContribute(
                                    IndexSourceType.KnowledgeType.getName(),
                                    c.getIdentifier(), c.getStatus(),
                                    c.getCreator(), ts, "创建资源"));
                        }
                    }
                    if (CollectionUtils.isNotEmpty(createSubInstructionList)) {
                        siRepository.batchAdd(createSubInstructionList);
                        for (SubInstruction c : createSubInstructionList) {
                            contributeList.add(addContribute(
                                    IndexSourceType.SubInstructionType
                                            .getName(), c.getIdentifier(), c
                                            .getStatus(), c.getCreator(), ts,
                                    "创建资源"));
                        }
                    }
                    if (CollectionUtils.isNotEmpty(createCoverageList)) {
                        covRepository.batchAdd(createCoverageList);
                    }
                    if (CollectionUtils.isNotEmpty(createResourceCategoryList)) {
                        rcRepository.batchAdd(createResourceCategoryList);
                    }
                    if (CollectionUtils.isNotEmpty(createResourceRelationList)) {
                        rrRepository.batchAdd(createResourceRelationList);
                    }
                    if (CollectionUtils.isNotEmpty(deleteResourceRelationIds)) {
                        rrRepository.batchDel(deleteResourceRelationIds);
                    }
                    if (CollectionUtils.isNotEmpty(contributeList)) {
                        contributeRepository.batchAdd(contributeList);
                    }
                    if (CollectionUtils.isNotEmpty(createSampleList)) {
                        sampleRepository.batchAdd(createSampleList);
                    }

                } catch (EspStoreException e) {
                    throw new LifeCircleException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                            e.getMessage());
                }
            }
        }
        return sampleIdList;
    }

    //删除样例
    //1.删除样例表里的数据
    //2.删除知识点与学习目标的关系，删除学习目标类型和学习目标的关系
    //3.删除ndresource表里学习目标的数据
    @Override
    public Map<String, Object> deleteSample(String sampleId, String userId) {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Map<String, Object> returnMap = new LinkedHashMap<>();
        List<ResourceRelation> relationsToDelete = new ArrayList<>();
        List<Asset> assetsToDelete = new ArrayList<>();
        try {
            Sample sample = sampleRepository.get(sampleId);
            sample.setEnable(false);

            //找出copy套件
            ResourceRelation example = new ResourceRelation();
            example.setSourceUuid(sampleId);
            example.setResType("samples");
            example.setEnable(true);
            example.setResourceTargetType(IndexSourceType.AssetType.getName());

            List<ResourceRelation> copyRelations = resourceRelationRepository.getAllByExample(example);
            List<String> copySuiteIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(copyRelations)) {
                for (ResourceRelation relation : copyRelations) {
                    copySuiteIds.add(relation.getTarget());
                }
            }

            //删除拷贝套件
            for (String id : copySuiteIds) {
                suiteService.deleteCopySuite(id);
            }

            //找出套件下所有学习目标类型
            List<Map<String, String>> objTypeIds = new ArrayList<>();
            List<Map<String, Object>> suiteIds = new ArrayList<>();
            List<String> suiteId = new ArrayList<>();
            if (sample != null) {
                suiteId.add(sample.getAssetId());
                educationRelationService.recursiveSuiteDirectory(suiteIds, sample.getAssetId(), 0);
            }
            if (CollectionUtils.isNotEmpty(suiteIds)) {
                for (Map<String, Object> map : suiteIds) {
                    suiteId.add((String) map.get("identifier"));
                }
            }
            if (CollectionUtils.isNotEmpty(suiteId)) {
                objTypeIds = queryObjectTypeBySuiteId(suiteId);
            }
            List<String> objTypeIdList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(objTypeIds)) {
                for (Map<String, String> map : objTypeIds) {
                    objTypeIdList.add(map.get("identifier"));
                }
            }
            //处理知识点id,通过知识点找到学习目标
            List<String> knIds = new ArrayList<>();
            List<String> knIdList = new ArrayList<>();
            String knId1 = sample.getKnowledgeId1();
            String knId2 = sample.getKnowledgeId2();
            String knId3 = sample.getKnowledgeId3();
            String knId4 = sample.getKnowledgeId4();
            String knId5 = sample.getKnowledgeId5();

            List<String> knRelations = new ArrayList<>();
            if (StringUtils.isNotEmpty(knId1)) {
                knIds.add(knId1);
                knIdList.add(knId1);
                List<String> knId1Relation = resourceRelationRepository.findObjectivesIdsByKnIds(knIds);
                knRelations = knId1Relation;
                knIds.remove(knId1);
            }
            if (StringUtils.isNotEmpty(knId2)) {
                dealMultiKnIdObjective(knId2, knIds, knRelations);
                knIdList.add(knId2);
            }
            if (StringUtils.isNotEmpty(knId3)) {
                dealMultiKnIdObjective(knId3, knIds, knRelations);
                knIdList.add(knId3);
            }
            if (StringUtils.isNotEmpty(knId4)) {
                dealMultiKnIdObjective(knId4, knIds, knRelations);
                knIdList.add(knId4);
            }
            if (StringUtils.isNotEmpty(knId5)) {
                dealMultiKnIdObjective(knId5, knIds, knRelations);
                knIdList.add(knId5);
            }
            //通过学习目标类型，知识点找到学习目标
            List<String> objTypeRelations = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(objTypeIdList)) {
                objTypeRelations = resourceRelationRepository.findObjectivesIdsByObjTypeIds(objTypeIdList);
            }
            List<String> objectiveIds = new ArrayList<>();
            objTypeRelations.retainAll(knRelations);
            objectiveIds = objTypeRelations;
            List<String> notDeleteObjectiveIds = resourceRelationRepository.findObjectivesIdsOnlyInKnIds(objectiveIds, knIdList);
            objectiveIds.removeAll(notDeleteObjectiveIds);

            //学习目标类型与学习目标的关系,学习目标与知识点的关系

            if (CollectionUtils.isNotEmpty(objectiveIds)) {
                relationsToDelete = resourceRelationRepository.findAllObjectiveRelationsByTarget(objectiveIds);
            }
            //从ndresource表里删除数据
            List<InstructionalObjective> objectives = new ArrayList<>();
            objectives = ioRepository.getAllByIds(objectiveIds);
            //统一处理数据
            if (CollectionUtils.isNotEmpty(objectives)) {
                for (InstructionalObjective instructionalObjective : objectives) {
                    instructionalObjective.setEnable(false);
                }
                ioRepository.batchAdd(objectives);
            }

            if (CollectionUtils.isNotEmpty(relationsToDelete)) {
                for (ResourceRelation relation : relationsToDelete) {
                    relation.setEnable(false);
                }
                resourceRelationRepository.batchAdd(relationsToDelete);
            }

            if (sample != null) {
                sampleRepository.update(sample);
            }
            Contribute contribute = addContribute("samples", sampleId,
                    LifecycleStatus.REMOVED.toString(), userId, ts, "删除样例");
            contributeRepository.add(contribute);

            List<String> relationIds = new ArrayList<>();
            for (ResourceRelation relation : relationsToDelete) {
                relationIds.add(relation.getIdentifier());
            }
            returnMap.put("删除的样例id： ", sampleId);
            returnMap.put("删除的学习目标id： ", objectiveIds);
            returnMap.put("删除的关系id： ", relationIds);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        return returnMap;
    }

    public SampleViewModel updateSample(String sampleId, Map<String, Object> customPropertiesMap, String userId) {
        try {
            Sample sample = sampleRepository.get(sampleId);
            if (sample == null || !sample.isEnable()) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "",
                        "样例不存在");
            }
            if (customPropertiesMap != null) {
                Timestamp ts = new Timestamp(System.currentTimeMillis());
                sample.setCustomProperties(ObjectUtils.toJson(customPropertiesMap));
                sample.setLastUpdate(ts);
                Contribute contribute = addContribute("samples", sampleId,
                        sample.getStatus(), userId, ts, "修改样例");
                contributeRepository.add(contribute);
                sample = sampleRepository.update(sample);
                if (sample != null) {
                    SampleViewModel svm = BeanMapperUtils.beanMapper(sample, SampleViewModel.class);
                    svm.setCustomProperties(ObjectUtils.fromJson(sample.getCustomProperties(), Map.class));
                    return svm;
                }
            }
        } catch (Exception e) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                    e.getMessage());
        }
        return null;

    }

    private void dealMultiKnIdObjective(String knId, List<String> knIds, List<String> knRelations) {
        knIds.add(knId);
        List<String> knIdRelation = resourceRelationRepository.findObjectivesIdsByKnIds(knIds);
        knRelations.retainAll(knIdRelation);
        knIds.remove(knId);
    }

    private ResourceRelation createExample(String sourceUuid, String resType, String targetType, String relationType) {
        ResourceRelation example = new ResourceRelation();
        example.setSourceUuid(sourceUuid);
        example.setResType(resType);
        example.setResourceTargetType(targetType);
        example.setRelationType(relationType);
        return example;
    }

    @Override
    public void transportData() {

        // 从relation表里取出所有sample
        ResourceRelation example = new ResourceRelation();
        example.setRelationType("SAMPLE");
        List<ResourceRelation> relations = new ArrayList<>();
        try {
            relations = resourceRelationRepository.getAllByExample(example);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
        if (CollectionUtils.isEmpty(relations)) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "",
                    "relation为空");
        }

        // 取得所有知识点id
        List<String> knIds = new ArrayList<>();
        for (ResourceRelation relation : relations) {
            knIds.add(relation.getTarget());
        }

        // 取到所有知识点title
        List<Chapter> knowledges = new ArrayList<>();
        Map<String, String> titleMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(knIds)) {
            try {
                knowledges = chapterRepository.getAll(knIds);
            } catch (EspStoreException e) {
                e.printStackTrace();
            }
        }
        if (CollectionUtils.isEmpty(knowledges)) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "",
                    "knowledge为空");
        }
        for (Chapter knowledge : knowledges) {
            titleMap.put(knowledge.getIdentifier(), knowledge.getTitle());
        }

        // 将所有sample存入sample表，id为relation表里的id
        List<Sample> samples = new ArrayList<>();
        for (ResourceRelation relation : relations) {
            Sample sample = new Sample();
            sample.setIdentifier(relation.getIdentifier());
            sample.setTitle(titleMap.get(relation.getTarget()));
            sample.setAssetId(relation.getSourceUuid());
            sample.setKnowledgeId1(relation.getTarget());
            sample.setStatus(relation.getStatus());
            sample.setCreator(relation.getCreator());
            sample.setCreateTime(relation.getCreateTime());
            sample.setLastUpdate(relation.getLastUpdate());
            sample.setEnable(relation.getEnable());

            samples.add(sample);
        }
        if (CollectionUtils.isNotEmpty(samples)) {
            try {
                sampleRepository.batchAdd(samples);
            } catch (EspStoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理知识点逻辑
     *
     * @param identifier
     * @param creator
     * @param knowledges
     * @param createKnowledgeList
     * @param createResourceRelationList
     * @param createResourceCategoryList
     * @param createCoverageList
     * @param categoryList
     * @param knowledgeOrderList
     * @return
     */
    private Map<String, Object> dealKnowledge(String identifier,
                                              String creator, List<SuiteKnowledgesViewModel> knowledges,
                                              List<Chapter> createKnowledgeList,
                                              List<ResourceRelation> createResourceRelationList,
                                              List<ResourceCategory> createResourceCategoryList,
                                              List<ResCoverage> createCoverageList,
                                              List<ResClassificationModel> categoryList,
                                              List<String> knowledgeOrderList, Map<String, String> titleMap, String relationType) {
        boolean addKnFlag = false;
        List<String> knTitles = new ArrayList<String>();
        List<String> knIds = new ArrayList<String>();
        for (SuiteKnowledgesViewModel knowledge : knowledges) {
            if (StringUtils.isEmpty(knowledge.getKnowledgeId())) {
                // 根据title查找知识点
                knTitles.add(knowledge.getKnowledgeTitle());
                addKnFlag = true;
            } else {
                // 判断知识点是否存在
                knIds.add(knowledge.getKnowledgeId());
            }
        }
        List<Map<String, Object>> knTitleList = null;
        if (CollectionUtils.isNotEmpty(knTitles)) {
            knTitleList = instructionalobjectiveDao
                    .queryKnowledgeListByTitles(knTitles);
        }

        if (CollectionUtils.isNotEmpty(knIds)) {
            List<Chapter> knList = instructionalobjectiveDao
                    .queryKnowledgeListByIds(knIds);
            if (CollectionUtils.isEmpty(knList)) {
                return getReturnInfo(knIds.get(0), "", "", null, "知识点不存在");
            }
            if (knIds.size() != knList.size()) {
                for (String knId : knIds) {
                    boolean flag = false;
                    for (Chapter knowledge : knList) {
                        if (knId.equals(knowledge.getIdentifier())) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        return getReturnInfo(knId, "", "", null, "知识点不存在");
                    }
                }
            }
        }

        if (addKnFlag) {
            for (SuiteKnowledgesViewModel skvm : knowledges) {
                if (StringUtils.isEmpty(skvm.getKnowledgeId())) {
                    boolean flag = false;
                    if (knTitleList != null) {
                        for (Map<String, Object> map : knTitleList) {
                            if (skvm.getKnowledgeTitle().equals(
                                    (String) map.get("title"))) {
                                flag = true;
                                knowledgeOrderList.add((String) map
                                        .get("identifier"));
                                titleMap.put((String) map.get("identifier"),
                                        (String) map.get("title"));
                                // 需要同时新增知识点与教学目标的关系数据
                                createResourceRelationList
                                        .add(addResourceRelation(
                                                IndexSourceType.KnowledgeType
                                                        .getName(),
                                                (String) map.get("identifier"),
                                                IndexSourceType.InstructionalObjectiveType
                                                        .getName(), identifier,
                                                relationType));
                            }
                        }
                    }
                    if (!flag) {
                        // 从内存中查找知识点
                        if (CollectionUtils.isNotEmpty(createKnowledgeList)) {
                            for (Chapter c : createKnowledgeList) {
                                if (c.getTitle().equals(
                                        skvm.getKnowledgeTitle())) {
                                    flag = true;
                                    knowledgeOrderList.add(c.getIdentifier());
                                    titleMap.put(c.getIdentifier(),
                                            skvm.getKnowledgeTitle());
                                    // 需要同时新增知识点与教学目标的关系数据
                                    createResourceRelationList
                                            .add(addResourceRelation(
                                                    IndexSourceType.KnowledgeType
                                                            .getName(),
                                                    c.getIdentifier(),
                                                    IndexSourceType.InstructionalObjectiveType
                                                            .getName(),
                                                    identifier, relationType));
                                }
                            }
                        }
                    }
                    if (!flag) {
                        // 说明没找到知识点数据，该知识点需要新增
                        Chapter kn = addKnowledge(skvm.getKnowledgeTitle(),
                                creator);
                        createKnowledgeList.add(kn);
                        knowledgeOrderList.add(kn.getIdentifier());
                        titleMap.put(kn.getIdentifier(), kn.getTitle());
                        createCoverageList.add(addResCoverage(
                                IndexSourceType.KnowledgeType.getName(),
                                kn.getIdentifier()));
                        createResourceCategoryList
                                .addAll(addResourceCategoryList(
                                        IndexSourceType.KnowledgeType.getName(),
                                        kn.getIdentifier(), categoryList));
                        createResourceRelationList.add(addResourceRelation(
                                IndexSourceType.KnowledgeType.getName(), kn
                                        .getIdentifier(),
                                IndexSourceType.InstructionalObjectiveType
                                        .getName(), identifier, relationType));
                    }
                } else {
                    knowledgeOrderList.add(skvm.getKnowledgeId());
                    titleMap.put(skvm.getKnowledgeId(),
                            skvm.getKnowledgeTitle());
                    // 新增知识点与教学目标的关系数据
                    createResourceRelationList.add(addResourceRelation(
                            IndexSourceType.KnowledgeType.getName(), skvm
                                    .getKnowledgeId(),
                            IndexSourceType.InstructionalObjectiveType
                                    .getName(), identifier, relationType));
                }
            }
        } else {
            for (SuiteKnowledgesViewModel skvm : knowledges) {
                knowledgeOrderList.add(skvm.getKnowledgeId());
                titleMap.put(skvm.getKnowledgeId(), skvm.getKnowledgeTitle());
                // 新增知识点与教学目标的关系数据
                createResourceRelationList.add(addResourceRelation(
                        IndexSourceType.KnowledgeType.getName(),
                        skvm.getKnowledgeId(),
                        IndexSourceType.InstructionalObjectiveType.getName(),
                        identifier, relationType));
            }
        }
        return null;
    }

    private List<String> queryResourceCategory4SL(String resource) {
        List<String> returnList = new ArrayList<String>();
        String sql = "SELECT identifier FROM resource_categories where primary_category = 'instructionalobjectives' and resource = '"
                + resource + "' and category_code = 'SL'";
        List<Map<String, Object>> list = jt.queryForList(sql);
        if (CollectionUtils.isNotEmpty(list)) {
            for (Map<String, Object> map : list) {
                returnList.add((String) map.get("identifier"));
            }
        }
        return returnList;
    }

    public List<ResourceCategory> addApplicablePeriodCategoryList(
            String resource, List<String> applicablePeriod) {
        List<ResourceCategory> rcList = new ArrayList<ResourceCategory>();
        if (CollectionUtils.isNotEmpty(applicablePeriod)) {
            try {
                Category c = null;
                if (etCategory.existKey("category_SL")) {
                    c = etCategory.get("category_SL", Category.class);
                }
                if (c == null) {
                    Category category = new Category();
                    category.setNdCode("SL");
                    c = categoryRepository.getByExample(category);
                    if (c != null) {
                        etCategory.set("category_SL", c, 1l, TimeUnit.DAYS);
                    } else {
                        throw new LifeCircleException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "LC/CHECK_PARAM_VALID_FAIL", "SL维度不存在");
                    }
                }
                if (c != null) {
                    for (String string : applicablePeriod) {
                        if (string.startsWith("SL")) {
                            CategoryData cd = null;
                            if (etCategoryData.existKey("categoryData_"
                                    + string)) {
                                cd = etCategoryData.get("categoryData_"
                                        + string, CategoryData.class);
                            }
                            if (cd == null) {
                                CategoryData categoryData = new CategoryData();
                                categoryData.setNdCode(string);
                                cd = categoryDataRepository
                                        .getByExample(categoryData);
                                if (cd != null) {
                                    etCategoryData.set(
                                            "categoryData_" + string, cd, 1l,
                                            TimeUnit.DAYS);
                                } else {
                                    throw new LifeCircleException(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            "LC/CHECK_PARAM_VALID_FAIL", string
                                            + "维度数据不存在");
                                }
                            }
                            ResourceCategory rc = new ResourceCategory();
                            rc.setIdentifier(UUID.randomUUID().toString());
                            rc.setPrimaryCategory(IndexSourceType.InstructionalObjectiveType
                                    .getName());
                            rc.setResource(resource);
                            rc.setTaxonname(cd.getShortName());
                            rc.setCategoryCode(c.getNdCode());
                            rc.setCategoryName(c.getShortName());
                            rc.setTaxoncode(string);
                            rc.setTaxoncodeid(cd.getIdentifier());
                            rcList.add(rc);
                        }
                    }
                }
            } catch (EspStoreException e) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                        e.getMessage());
            }

        }
        return rcList;
    }

    /**
     * 添加资源分类维度数据（带KC维度数据）
     *
     * @param resType
     * @param resource
     * @param list
     * @return
     */
    private List<ResourceCategory> addResourceCategoryList(String resType,
                                                           String resource, List<ResClassificationModel> list) {
        List<ResourceCategory> rcList = new ArrayList<ResourceCategory>();
        if (IndexSourceType.KnowledgeType.getName().equals(resType)) {
            if (CollectionUtils.isNotEmpty(list)) {
                for (ResClassificationModel rcm : list) {
                    if (rcm.getTaxoncode().startsWith("KC")) {
                        ResourceCategory tmp = BeanMapperUtils.beanMapper(rcm,
                                ResourceCategory.class);
                        tmp.setIdentifier(UUID.randomUUID().toString());
                        tmp.setResource(resource);
                        tmp.setPrimaryCategory(resType);
                        rcList.add(tmp);
                    }
                }
            }
        }
        rcList.add(addResourceCategory(resType, resource));
        return rcList;
    }

    /**
     * 添加资源分类维度数据
     *
     * @param resType
     * @param resource
     * @return
     */
    private ResourceCategory addResourceCategory(String resType, String resource) {
        ResourceCategory rc = new ResourceCategory();
        rc.setIdentifier(UUID.randomUUID().toString());
        rc.setPrimaryCategory(resType);
        rc.setResource(resource);
        rc.setCategoryCode("$R");
        rc.setCategoryName("resourcetype");
        if (IndexSourceType.KnowledgeType.getName().equals(resType)) {
            // 知识点
            rc.setTaxoncode("$RA0205");
            rc.setTaxonname("知识点");
            rc.setTaxoncodeid("b40911a4-5ab0-469f-aab5-b5128b9f88fd");
        } else if (IndexSourceType.InstructionalObjectiveType.getName().equals(
                resType)) {
            // 教学目标
            rc.setTaxoncode("$RA0204");
            rc.setTaxonname("教学目标");
            rc.setTaxoncodeid("c869acfe-b6c3-415f-abb8-97384c04c500");
        }
        return rc;
    }

    /**
     * 添加资源覆盖范围数据
     *
     * @param resType
     * @param resource
     * @return
     */
    private ResCoverage addResCoverage(String resType, String resource) {
        ResCoverage rc = new ResCoverage();
        rc.setIdentifier(UUID.randomUUID().toString());
        rc.setResource(resource);
        rc.setResType(resType);
        rc.setStrategy("OWNER");
        rc.setTarget("nd");
        rc.setTargetTitle("nd资源库");
        rc.setTargetType("Org");
        return rc;
    }

    /**
     * 添加知识点数据
     *
     * @param title
     * @param creator
     * @return
     */
    private Chapter addKnowledge(String title, String creator) {
        Chapter kn = new Chapter();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        kn.setIdentifier(UUID.randomUUID().toString());
        kn.setmIdentifier(kn.getIdentifier());
        kn.setTitle(title);
        kn.setCreator(creator);
        kn.setLanguage("zh_cn");
        kn.setVersion("v0.6");
        kn.setPrimaryCategory("knowledges");
        kn.setCreateTime(ts);
        kn.setLastUpdate(ts);
        kn.setStatus("ONLINE");
        kn.setParent("$SB0100");
        kn.setTeachingMaterial("$SB0100");
        kn.setLeft(-2);
        kn.setRight(-1);
        kn.setPrimaryCategory(IndexSourceType.KnowledgeType.getName());
        return kn;
    }

    /**
     * 创建知识点与教学目标的关系模型
     *
     * @return
     */
    private ResourceRelation addResourceRelation(String resType,
                                                 String sourceUuid, String targetType, String target) {
        return addResourceRelation(resType, sourceUuid, targetType, target,
                "ASSOCIATE");
    }

    private ResourceRelation addResourceRelation(String resType,
                                                 String sourceUuid, String targetType, String target,
                                                 String relationType) {
        ResourceRelation rr = new ResourceRelation();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        rr.setIdentifier(UUID.randomUUID().toString());
        rr.setResType(resType);
        rr.setSourceUuid(sourceUuid);
        rr.setResourceTargetType(targetType);
        rr.setTarget(target);
        rr.setEnable(true);
        rr.setOrderNum(1);
        rr.setSortNum(5001f);
        rr.setCreateTime(ts);
        rr.setLastUpdate(ts);
        rr.setStatus(LifecycleStatus.CREATED.getCode());
        rr.setRelationType(relationType);
        return rr;
    }

    private ResourceRelation addResourceRelation(String resType,
                                                 String sourceUuid, String targetType, String target,
                                                 String relationType, float sortNum) {
        ResourceRelation rr = new ResourceRelation();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
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
        return rr;
    }

    /**
     * 错误返回值封装
     *
     * @param identifier
     * @param title
     * @param message
     * @return
     */
    private Map<String, Object> getReturnInfo(String identifier, String title,
                                              String description, String customProperties, String message) {
        Map<String, Object> returnMap = new HashMap<String, Object>();
        returnMap.put("identifier", identifier);
        returnMap.put("title", title);
        if (StringUtils.isNotEmpty(description)) {
            returnMap.put("description", description);
        } else {
            returnMap.put("description", "");
        }
        if (StringUtils.isNotEmpty(customProperties)) {
            returnMap.put("custom_properties",
                    ObjectUtils.fromJson(customProperties, Map.class));
        } else {
            returnMap.put("custom_properties", new HashMap<String, Object>());
        }

        returnMap.put("message", message);
        return returnMap;
    }

    @Override
    public Map<String, Object> queryListByKnTitle(String title, String suiteId) {
        Map<String, Object> returnMap = new LinkedHashMap<String, Object>();
        // 1、根据知识点title查询知识点信息
        if (StringUtils.isNotEmpty(title)) {
            List<String> titles = new ArrayList<String>();
            titles.add(title);

            List<Map<String, Object>> knTitleList = instructionalobjectiveDao
                    .queryKnowledgeListByTitles(titles);
            if (CollectionUtils.isNotEmpty(knTitleList)) {
                // 2、根据知识点id查找教学目标类型、教学目标、子教学目标相关数据
                Map<String, Object> knMap = knTitleList.get(0);
                List<Map<String, Object>> dataList = instructionalobjectiveDao
                        .queryByKnId((String) knMap.get("identifier"));
                if (CollectionUtils.isNotEmpty(dataList)) {
                    // 教学目标类型id集合
                    List<String> otIdList = new ArrayList<String>();
                    boolean otFlag = false;

                    if (StringUtils.isNotEmpty(suiteId)) {
                        otFlag = true;

                        // 2.1、根据套件递归查询子套件
                        List<Map<String, Object>> suiteList = new LinkedList<Map<String, Object>>();
                        int num = 1;
                        educationRelationService.recursiveSuiteDirectory(
                                suiteList, suiteId, num);

                        // 2.2、根据套件查询教学目标类型
                        Set<String> sids = new HashSet<String>();
                        sids.add(suiteId);
                        for (Map<String, Object> m : suiteList) {
                            sids.add((String) m.get("identifier"));
                        }
                        ListViewModel<RelationForQueryViewModel> modelList = educationRelationService
                                .batchQueryResourcesByDB(
                                        IndexSourceType.AssetType.getName(),
                                        sids,
                                        IndexSourceType.AssetType.getName(),
                                        null, null,
                                        RelationType.ASSOCIATE.getName(),
                                        "0,500", "$RA0503", false, true, false);
                        if (CollectionUtils.isNotEmpty(modelList.getItems())) {
                            for (RelationForQueryViewModel tmp : modelList
                                    .getItems()) {
                                otIdList.add(tmp.getIdentifier());
                            }
                        }
                    }

                    // 3、动态拼接教学目标title
                    Collection<Map.Entry<String, String>> idWithTitles = new LinkedList<Map.Entry<String, String>>();

                    returnMap.put("knowledge", knMap);
                    List<Map<String, Object>> itemsList = new ArrayList<Map<String, Object>>();
                    List<String> otIds = new ArrayList<String>();
                    Set<String> objIds = new HashSet<String>();

                    for (Map<String, Object> map : dataList) {
                        Map<String, Object> itemMap = new HashMap<String, Object>();

                        // 教学目标类型id
                        String otId = (String) map.get("ot_id");
                        // 教学目标类型title
                        String otTitle = (String) map.get("ot_title");
                        // 教学目标类型描述
                        String otDesc = (String) map.get("ot_desc");
                        // 教学目标自定义属性
                        String otCust = (String) map.get("ot_cust");
                        // 教学目标id
                        String objId = (String) map.get("obj_id");
                        // 教学目标title
                        String objTitle = (String) map.get("obj_title");
                        // 教学目标描述
                        String objDesc = (String) map.get("obj_desc");
                        // 子教学目标描述
                        String subObjDesc = (String) map.get("sub_obj_desc");

                        // 根据套件id来过滤教学目标类型
                        if (otFlag) {
                            if (!otIdList.contains(otId)) {
                                continue;
                            }
                        }

                        Map.Entry<String, String> entry = new DefaultMapEntry(
                                objId, objTitle);
                        idWithTitles.add(entry);

                        if (!otIds.contains(otId)) {
                            otIds.add(otId);
                            objIds.add(objId);
                            Map<String, Object> otMap = new HashMap<String, Object>();
                            otMap.put("identifier", otId);
                            otMap.put("title", otTitle);
                            otMap.put("description", otDesc);
                            if (StringUtils.isNotEmpty(otCust)) {
                                otMap.put("custom_properties",
                                        ObjectUtils.fromJson(otCust, Map.class));
                            } else {
                                otMap.put("custom_properties",
                                        new HashMap<String, Object>());
                            }

                            Map<String, Object> objMap = new HashMap<String, Object>();
                            objMap.put("identifier", objId);
                            objMap.put("title", objTitle);
                            objMap.put("description", objDesc);

                            List<String> subDescList = new ArrayList<String>();
                            if (StringUtils.isNotEmpty(subObjDesc)) {
                                subDescList.add(subObjDesc);
                            }
                            objMap.put("versions", subDescList);

                            itemMap.put("objective_type", otMap);

                            List<Map<String, Object>> objList = new ArrayList<Map<String, Object>>();
                            objList.add(objMap);
                            itemMap.put("objectives", objList);
                            itemsList.add(itemMap);
                        } else {
                            if (!objIds.contains(objId)) {
                                objIds.add(objId);
                                // 找出教学目标类型对应的itemMap
                                for (Map<String, Object> itMap : itemsList) {
                                    if (itMap.get("objective_type") != null) {
                                        Map<String, Object> otMap = (Map) itMap
                                                .get("objective_type");
                                        if (otId.equals((String) otMap
                                                .get("identifier"))) {
                                            List<Map<String, Object>> objList = (List) itMap
                                                    .get("objectives");
                                            Map<String, Object> objMap = new HashMap<String, Object>();
                                            objMap.put("identifier", objId);
                                            objMap.put("title", objTitle);
                                            objMap.put("description", objDesc);

                                            List<String> subDescList = new ArrayList<String>();
                                            if (StringUtils
                                                    .isNotEmpty(subObjDesc)) {
                                                subDescList.add(subObjDesc);
                                            }
                                            objMap.put("versions", subDescList);
                                            objList.add(objMap);
                                        }
                                    }
                                }
                            } else {
                                if (StringUtils.isNotEmpty(subObjDesc)) {
                                    for (Map<String, Object> itMap : itemsList) {
                                        if (itMap.get("objective_type") != null) {
                                            Map<String, Object> otMap = (Map) itMap
                                                    .get("objective_type");
                                            if (otId.equals((String) otMap
                                                    .get("identifier"))) {
                                                List<Map<String, Object>> objList = (List) itMap
                                                        .get("objectives");
                                                for (Map<String, Object> map2 : objList) {
                                                    if (objId
                                                            .equals((String) map2
                                                                    .get("identifier"))) {
                                                        List<String> versions = (List) map2
                                                                .get("versions");
                                                        if (!versions
                                                                .contains(subObjDesc)) {
                                                            versions.add(subObjDesc);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }

                    // 动态拼接教学目标title
                    if (CollectionUtils.isNotEmpty(idWithTitles)) {
                        Map<String, String> obTitleMap = getInstructionalObjectiveTitle(idWithTitles);
                        for (Map<String, Object> mm : itemsList) {
                            if (mm.get("objectives") != null) {
                                List<Map<String, Object>> tmpList = (List) mm
                                        .get("objectives");
                                if (CollectionUtils.isNotEmpty(tmpList)) {
                                    for (Map<String, Object> map2 : tmpList) {
                                        if (obTitleMap
                                                .containsKey((String) map2
                                                        .get("identifier"))) {
                                            String tmpTitle = obTitleMap
                                                    .get((String) map2
                                                            .get("identifier"));
                                            map2.put("title", tmpTitle);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 查找知识点（部分教学目标关联多个知识点）
                    ListViewModel<RelationForQueryViewModel> knowledgeList = educationRelationService
                            .batchQueryResourcesByDB(
                                    IndexSourceType.InstructionalObjectiveType
                                            .getName(), objIds,
                                    IndexSourceType.KnowledgeType.getName(),
                                    null, null, RelationType.ASSOCIATE
                                            .getName(), "0,500", null, true,
                                    true, false);
                    if (CollectionUtils.isNotEmpty(knowledgeList.getItems())) {
                        for (RelationForQueryViewModel tmpVm : knowledgeList
                                .getItems()) {
                            // 教学目标id
                            String obId = tmpVm.getSid();
                            // 知识点id
                            String identifier = tmpVm.getIdentifier();
                            // 知识点title
                            String knTitle = tmpVm.getTitle();
                            Map<String, Object> knm = new HashMap<String, Object>();
                            knm.put("identifier", identifier);
                            knm.put("title", knTitle);
                            for (Map<String, Object> mm : itemsList) {
                                if (mm.get("objectives") != null) {
                                    List<Map<String, Object>> tmpList = (List) mm
                                            .get("objectives");
                                    if (CollectionUtils.isNotEmpty(tmpList)) {
                                        for (Map<String, Object> map2 : tmpList) {
                                            if (obId.equals((String) map2
                                                    .get("identifier"))) {
                                                if (map2.get("knowledge_list") == null) {
                                                    map2.put(
                                                            "knowledge_list",
                                                            new ArrayList<Map<String, Object>>());
                                                }
                                                List<Map<String, Object>> kl = (List) map2
                                                        .get("knowledge_list");
                                                kl.add(knm);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    returnMap.put("items", itemsList);
                }
            }
        }
        return returnMap;
    }

    @Override
    public Map<String, Object> queryList4KnowledgeBySuiteId(String suiteId,
                                                            String limit) {
        Map<String, Object> returnMap = new LinkedHashMap<String, Object>();
        // 1、判断limit合法性
        Integer[] result = ParamCheckUtil.checkLimit(limit);

        // 2、根据套件递归查询子套件
        List<Map<String, Object>> suiteList = new LinkedList<Map<String, Object>>();
        int num = 1;
        educationRelationService.recursiveSuiteDirectory(suiteList, suiteId,
                num);

        // 3、根据套件查询教学目标类型
        Set<String> sids = new HashSet<String>();
        sids.add(suiteId);
        for (Map<String, Object> m : suiteList) {
            sids.add((String) m.get("identifier"));
        }
        ListViewModel<RelationForQueryViewModel> modelList = educationRelationService
                .batchQueryResourcesByDB(IndexSourceType.AssetType.getName(),
                        sids, IndexSourceType.AssetType.getName(), null, null,
                        RelationType.ASSOCIATE.getName(), "0,500", "$RA0503",
                        false, true, false);

        // 4、根据教学目标类型查询知识点的分页数据
        List<String> otIds = new ArrayList<String>();
        if (CollectionUtils.isEmpty(modelList.getItems())) {
            return returnMap;
        }
        for (RelationForQueryViewModel vm : modelList.getItems()) {
            otIds.add(vm.getIdentifier());
        }
        List<Map<String, String>> knTitleList = instructionalobjectiveDao
                .queryKnTitleListByObjectiveTypeIds(otIds);

        // 5、根据教学目标类型与知识点查询相关数据
        if (CollectionUtils.isEmpty(knTitleList)) {
            return returnMap;
        }
        int total = knTitleList.size();
        if (result[0] > total) {
            return returnMap;
        }

        int toIndex = (result[0] + result[1] > total) ? total
                : (result[0] + result[1]);
        List<Map<String, String>> subKnTitleList = knTitleList.subList(
                result[0], toIndex);
        List<String> subKnIds = new ArrayList<String>();
        for (Map<String, String> m : subKnTitleList) {
            subKnIds.add(m.get("identifier"));
        }

        // 教学目标列表数据
        List<Map<String, String>> objectiveList = instructionalobjectiveDao
                .queryListByOtIdsAndKnIds(otIds, subKnIds);
        Map<String, List<String>> categoryMap = new HashMap<String, List<String>>();
        if (CollectionUtils.isNotEmpty(objectiveList)) {
            List<String> ioIds = new ArrayList<String>();
            for (Map<String, String> map : objectiveList) {
                String identifier = map.get("identifier");
                ioIds.add(identifier);
            }
            String cSql = "SELECT taxOnCode,resource from resource_categories where primary_category = 'instructionalobjectives' and resource in (:ids) and category_code = 'SL'";
            NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("ids", ioIds);

            List<Map<String, Object>> iList = npjt.query(cSql, params,
                    new RowMapper<Map<String, Object>>() {
                        @Override
                        public Map<String, Object> mapRow(ResultSet rs,
                                                          int rowNum) throws SQLException {
                            Map<String, Object> m = new HashMap<String, Object>();
                            String taxOnCode = rs.getString("taxOnCode");
                            String resource = rs.getString("resource");
                            m.put("taxOnCode", taxOnCode);
                            m.put("resource", resource);
                            return m;
                        }
                    });

            if (CollectionUtils.isNotEmpty(iList)) {
                for (Map<String, Object> map : iList) {
                    if (categoryMap.containsKey((String) map.get("resource"))) {
                        List<String> l = categoryMap.get((String) map
                                .get("resource"));
                        l.add((String) map.get("taxOnCode"));
                    } else {
                        List<String> l = new ArrayList<String>();
                        l.add((String) map.get("taxOnCode"));
                        categoryMap.put((String) map.get("resource"), l);
                    }
                }
            }
        }

        // 6、拼装数据
        returnMap.put("limit", limit);
        returnMap.put("total", total);

        List<Map<String, Object>> itemList = new LinkedList<Map<String, Object>>();
        // 教学目标id的集合，用于查找子教学目标
        Set<String> obIds = new HashSet<String>();

        // 7、动态拼接教学目标title
        // Collection<Map.Entry<String, String>> idWithTitles = new
        // LinkedList<Map.Entry<String,String>>();

        for (Map<String, String> map : subKnTitleList) {
            String knId = map.get("identifier");
            Map<String, Object> knMap = new LinkedHashMap<String, Object>();
            knMap.put("knowledge", map);

            List<Map<String, Object>> knDataList = new ArrayList<Map<String, Object>>();
            for (Map<String, String> map2 : objectiveList) {
                if (knId.equals(map2.get("knId"))) {
                    Map<String, Object> dataMap = new LinkedHashMap<String, Object>();

                    // 教学目标类型id
                    String otId = map2.get("sourceUuid");
                    // 教学目标Id
                    String identifier = map2.get("identifier");
                    obIds.add(identifier);

                    // 教学目标title
                    String title = map2.get("title");
                    // 教学目标description
                    String description = map2.get("description");
                    // 教学目标creator
                    String creator = map2.get("creator");
                    // 教学目标自定义属性customProperties
                    String customProperties = map2.get("customProperties");
                    // 提供商来源
                    String providerSource = map2.get("providerSource");
                    // 备注
                    String keywords = map2.get("keywords");
                    // 状态
                    String status = map2.get("status");

                    // Map.Entry<String,String> entry = new
                    // DefaultMapEntry(identifier,title);
                    // idWithTitles.add(entry);

                    for (RelationForQueryViewModel vm : modelList.getItems()) {
                        if (otId.equals(vm.getIdentifier())) {
                            boolean flag = false;
                            for (Map<String, Object> map6 : knDataList) {
                                if (map6.get("objective_type") != null) {
                                    if (otId.equals(((Map) map6
                                            .get("objective_type"))
                                            .get("identifier"))) {
                                        flag = true;
                                        break;
                                    }
                                }
                            }
                            if (flag) {
                                break;
                            }

                            Map<String, Object> tmp = new HashMap<String, Object>();
                            tmp.put("identifier", otId);
                            tmp.put("title", vm.getTitle());
                            tmp.put("description", vm.getDescription());
                            tmp.put("custom_properties",
                                    vm.getCustomProperties());

                            String tmpSuiteId = vm.getSid();
                            if (tmpSuiteId.equals(suiteId)) {
                                tmp.put("suite_id", suiteId);
                            }

                            for (Map<String, Object> map3 : suiteList) {
                                if (tmpSuiteId.equals((String) map3
                                        .get("identifier"))) {
                                    tmp.put("suite_id", map3.get("identifier"));
                                }
                            }

                            dataMap.put("objective_type", tmp);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(dataMap)) {
                        knDataList.add(dataMap);
                    }

                    for (Map<String, Object> map4 : knDataList) {
                        if (map4.get("objective_type") != null) {
                            if (otId.equals(((Map) map4.get("objective_type"))
                                    .get("identifier"))) {
                                if (map4.get("objectives") == null) {
                                    map4.put(
                                            "objectives",
                                            new ArrayList<Map<String, Object>>());
                                }
                                List<Map<String, Object>> tmpList = (List) map4
                                        .get("objectives");
                                Map<String, Object> map5 = new HashMap<String, Object>();
                                map5.put("identifier", identifier);
                                map5.put("title", title);
                                map5.put("description", description);
                                map5.put("creator", creator);
                                if (StringUtils.isEmpty(providerSource)) {
                                    map5.put("providerSource", "");
                                } else {
                                    map5.put("providerSource", providerSource);
                                }

                                if (StringUtils.isNotEmpty(customProperties)) {
                                    map5.put("custom_properties", ObjectUtils
                                            .fromJson(customProperties,
                                                    Map.class));
                                } else {
                                    map5.put("custom_properties",
                                            new HashMap<String, String>());
                                }
                                if (StringUtils.isNotEmpty(keywords)) {
                                    map5.put("keywords", ObjectUtils.fromJson(
                                            keywords, List.class));
                                } else {
                                    map5.put("keywords",
                                            new ArrayList<String>());
                                }

                                map5.put("versions", new ArrayList<String>());
                                map5.put("status", status);

                                if (categoryMap.containsKey(identifier)) {
                                    map5.put("applicable_period",
                                            categoryMap.get(identifier));
                                } else {
                                    map5.put("applicable_period",
                                            new ArrayList<String>());
                                }
                                tmpList.add(map5);
                            }
                        }
                    }
                }
            }
            knMap.put("datas", knDataList);
            itemList.add(knMap);
        }

        // 动态拼接教学目标title(门户暂不需要)
        // if(CollectionUtils.isNotEmpty(idWithTitles)){
        // Map<String, String> obTitleMap =
        // getInstructionalObjectiveTitle(idWithTitles);
        // for (Map<String,Object> mm : itemList) {
        // List<Map<String,Object>> knDataList = (List)mm.get("datas");
        // for (Map<String, Object> map7 : knDataList) {
        // if(map7.get("objectives") != null){
        // List<Map<String,Object>> tmpList = (List)map7.get("objectives");
        // if(CollectionUtils.isNotEmpty(tmpList)){
        // for (Map<String, Object> map2 : tmpList) {
        // if(obTitleMap.containsKey((String)map2.get("identifier"))){
        // String tmpTitle = obTitleMap.get((String)map2.get("identifier"));
        // map2.put("title", tmpTitle);
        // }
        // }
        // }
        // }
        // }
        // }
        // }

        // 查找子教学目标
        ListViewModel<RelationForQueryViewModel> subInstructionalList = educationRelationService
                .batchQueryResourcesByDB(
                        IndexSourceType.InstructionalObjectiveType.getName(),
                        obIds, IndexSourceType.SubInstructionType.getName(),
                        null, null, RelationType.ASSOCIATE.getName(), "0,500",
                        null, false, true, false);
        if (CollectionUtils.isNotEmpty(subInstructionalList.getItems())) {
            for (RelationForQueryViewModel tmpVm : subInstructionalList
                    .getItems()) {
                // 教学目标id
                String obId = tmpVm.getSid();
                // 子教学目标描述
                String desc = tmpVm.getDescription();
                for (Map<String, Object> mm : itemList) {
                    List<Map<String, Object>> knDataList = (List) mm
                            .get("datas");
                    for (Map<String, Object> map7 : knDataList) {
                        if (map7.get("objectives") != null) {
                            List<Map<String, Object>> tmpList = (List) map7
                                    .get("objectives");
                            if (CollectionUtils.isNotEmpty(tmpList)) {
                                for (Map<String, Object> map2 : tmpList) {
                                    if (obId.equals((String) map2
                                            .get("identifier"))) {
                                        if (map2.get("versions") == null) {
                                            map2.put("versions",
                                                    new ArrayList<String>());
                                        }
                                        List<String> versions = (List) map2
                                                .get("versions");
                                        if (StringUtils.isNotEmpty(desc)
                                                && !versions.contains(desc)) {
                                            versions.add(desc);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 查找知识点（部分教学目标关联多个知识点）
        ListViewModel<RelationForQueryViewModel> knowledgeList = educationRelationService
                .batchQueryResourcesByDB(
                        IndexSourceType.InstructionalObjectiveType.getName(),
                        obIds, IndexSourceType.KnowledgeType.getName(), null,
                        null, RelationType.ASSOCIATE.getName(), "0,500", null,
                        true, true, false);
        if (CollectionUtils.isNotEmpty(knowledgeList.getItems())) {
            for (RelationForQueryViewModel tmpVm : knowledgeList.getItems()) {
                // 教学目标id
                String obId = tmpVm.getSid();
                // 知识点id
                String identifier = tmpVm.getIdentifier();
                // 知识点title
                String title = tmpVm.getTitle();
                // 知识点状态
                String status = tmpVm.getStatus();
                Map<String, Object> knm = new HashMap<String, Object>();
                knm.put("identifier", identifier);
                knm.put("title", title);
                for (Map<String, Object> mm : itemList) {
                    List<Map<String, Object>> knDataList = (List) mm
                            .get("datas");
                    for (Map<String, Object> map7 : knDataList) {
                        if (map7.get("objectives") != null) {
                            List<Map<String, Object>> tmpList = (List) map7
                                    .get("objectives");
                            if (CollectionUtils.isNotEmpty(tmpList)) {
                                for (Map<String, Object> map2 : tmpList) {
                                    if (obId.equals((String) map2
                                            .get("identifier"))) {
                                        if (map2.get("knowledge_list") == null) {
                                            map2.put(
                                                    "knowledge_list",
                                                    new ArrayList<Map<String, Object>>());
                                        }
                                        List<Map<String, Object>> kl = (List) map2
                                                .get("knowledge_list");
                                        kl.add(knm);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        returnMap.put("items", itemList);
        return returnMap;
    }

    /**
     * 根据套件id查询教学目标，并按样例来进行分页
     *
     * @param suiteId
     * @param limit
     * @param isSubSuite 标示传进来的id是否为子套件
     * @return
     */
    @Override
    public Map<String, Object> queryList4SampleBySuiteId(String suiteId,
                                                         String limit, Boolean isSubSuite, int level, String relationType) {
        Map<String, Object> returnMap = new LinkedHashMap<String, Object>();
        returnMap.put("limit", limit);
        returnMap.put("total", 0);
        returnMap.put("suite_model", new HashMap<>());
        returnMap.put("items", new ArrayList<>());
        // 1、判断limit合法性
        Integer[] result = ParamCheckUtil.checkLimit(limit);

        // 2、根据套件递归查询子套件
        List<Map<String, Object>> suiteList = new LinkedList<Map<String, Object>>();
        List<String> rootSuite = new ArrayList<>();
        Map<String, Object> parent = new LinkedHashMap<>();
        Map<String, Object> sonMap = new LinkedHashMap<>();
        List<String> subSuiteList = new ArrayList<>();
        if (!isSubSuite) {
            int num = 1;
            educationRelationService.recursiveSuiteDirectory(suiteList,
                    suiteId, num);
            rootSuite.add(suiteId);
        } else {
            educationRelationService.recursiveRootSuite(suiteId, rootSuite,
                    parent, relationType);

            getSubSuite(suiteId, subSuiteList, sonMap);
            //处理childrenMap
            dealChildrenMap(sonMap, level - 1);

            for (String suiteId1 : subSuiteList) {
                Map<String, Object> map = new HashMap<>();
                map.put("identifier", suiteId1);
                suiteList.add(map);
            }
        }

        // 3、根据套件查询教学目标类型
        Set<String> sids = new HashSet<String>();
        if (!isSubSuite) {
            sids.add(suiteId);
            for (Map<String, Object> m : suiteList) {
                sids.add((String) m.get("identifier"));
            }
        } else {
            sids.add(suiteId);
            if (CollectionUtils.isNotEmpty(subSuiteList)) {
                sids.addAll(subSuiteList);
            }
        }

        ListViewModel<RelationForQueryViewModel> modelList = new ListViewModel<>();
        if (CollectionUtils.isNotEmpty(sids)) {
            modelList = educationRelationService.batchQueryResourcesByDB(
                    IndexSourceType.AssetType.getName(), sids,
                    IndexSourceType.AssetType.getName(), null, null,
                    relationType, "0,1000", "$RA0503",
                    false, true, false);
        }

        // 4、根据教学目标类型查询知识点的分页数据
//        List<String> otIds = new ArrayList<String>();
        if (CollectionUtils.isEmpty(modelList.getItems())) {
            return returnMap;
        }
//        for (RelationForQueryViewModel vm : modelList.getItems()) {
//            otIds.add(vm.getIdentifier());
//        }
        // List<Map<String, String>> knTitleList = instructionalobjectiveDao
        // .queryKnTitleListByObjectiveTypeIds(otIds);

        List<Sample> samples = new ArrayList<>();
        samples = sampleRepository.querySampleBySuiteId(rootSuite.get(0));
        List<String> objectiveIdList = new ArrayList<>();
        List<String> includeList = new ArrayList<String>();
        for (Sample sample : samples) {
//            Map<String, String> knTitle = new HashMap<>();
            List<String> objective1 = new ArrayList<>();
            List<String> objective2 = new ArrayList<>();
            List<String> objective3 = new ArrayList<>();
            List<String> objective4 = new ArrayList<>();
            List<String> objective5 = new ArrayList<>();
            if (StringUtils.isNotEmpty(sample.getKnowledgeId1())) {
                objective1 = resourceRelationRepository
                        .findBySourceIdAndResTypeAndTargetType(sample
                                        .getKnowledgeId1(),
                                IndexSourceType.KnowledgeType.getName(),
                                IndexSourceType.InstructionalObjectiveType
                                        .getName(),
                                relationType);
                ResourceModel rm = ndResourceService.getDetail(IndexSourceType.KnowledgeType.getName(), sample.getKnowledgeId1(), includeList);
                sample.setKnowledgeTitle1(rm.getTitle());
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId2())) {
                objective2 = resourceRelationRepository
                        .findBySourceIdAndResTypeAndTargetType(sample
                                        .getKnowledgeId2(),
                                IndexSourceType.KnowledgeType.getName(),
                                IndexSourceType.InstructionalObjectiveType
                                        .getName(),
                                relationType);
                ResourceModel rm = ndResourceService.getDetail(IndexSourceType.KnowledgeType.getName(), sample.getKnowledgeId2(), includeList);
                sample.setKnowledgeTitle2(rm.getTitle());
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId3())) {
                objective3 = resourceRelationRepository
                        .findBySourceIdAndResTypeAndTargetType(sample
                                        .getKnowledgeId3(),
                                IndexSourceType.KnowledgeType.getName(),
                                IndexSourceType.InstructionalObjectiveType
                                        .getName(),
                                relationType);
                ResourceModel rm = ndResourceService.getDetail(IndexSourceType.KnowledgeType.getName(), sample.getKnowledgeId3(), includeList);
                sample.setKnowledgeTitle3(rm.getTitle());
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId4())) {
                objective4 = resourceRelationRepository
                        .findBySourceIdAndResTypeAndTargetType(sample
                                        .getKnowledgeId4(),
                                IndexSourceType.KnowledgeType.getName(),
                                IndexSourceType.InstructionalObjectiveType
                                        .getName(),
                                relationType);
                ResourceModel rm = ndResourceService.getDetail(IndexSourceType.KnowledgeType.getName(), sample.getKnowledgeId4(), includeList);
                sample.setKnowledgeTitle4(rm.getTitle());
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId5())) {
                objective5 = resourceRelationRepository
                        .findBySourceIdAndResTypeAndTargetType(sample
                                        .getKnowledgeId5(),
                                IndexSourceType.KnowledgeType.getName(),
                                IndexSourceType.InstructionalObjectiveType
                                        .getName(),
                                relationType);
                ResourceModel rm = ndResourceService.getDetail(IndexSourceType.KnowledgeType.getName(), sample.getKnowledgeId5(), includeList);
                sample.setKnowledgeTitle5(rm.getTitle());
            }
            List<String> objectiveIds = new ArrayList<>();
            if (StringUtils.isNotEmpty(sample.getKnowledgeId1())) {
                objectiveIds = objective1;
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId2())) {
                objectiveIds.retainAll(objective2);
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId3())) {
                objectiveIds.retainAll(objective3);
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId4())) {
                objectiveIds.retainAll(objective4);
            }
            if (StringUtils.isNotEmpty(sample.getKnowledgeId5())) {
                objectiveIds.retainAll(objective5);
            }
            objectiveIdList.addAll(objectiveIds);
        }

        // 5、根据教学目标类型与知识点查询相关数据
        if (CollectionUtils.isEmpty(samples)) {
            return returnMap;
        }
        int total = samples.size();
        if (result[0] > total) {
            return returnMap;
        }

        int toIndex = (result[0] + result[1] > total) ? total
                : (result[0] + result[1]);
        List<Sample> subSampleList = samples.subList(result[0], toIndex);
        // 过滤
        if (relationType.equals(RelationType.COPY.getName())) {
            try {
                List<ResourceRelation> relationList = resourceRelationApiService
                        .getByResTypeAndTargetTypeAndSourceId(
                                IndexSourceType.AssetType.getName(),
                                IndexSourceType.InstructionalObjectiveType.getName(), suiteId);
                List<String> instructionalUuidList = new ArrayList<>();
                for (ResourceRelation relation : relationList) {
                    instructionalUuidList.add(relation.getTarget());
                }
                List<String> tempList = new ArrayList<>();
                tempList.addAll(objectiveIdList);
                for (String objectiveId : tempList) {
                    if (!instructionalUuidList.contains(objectiveId)) {
                        objectiveIdList.remove(objectiveId);
                    }
                }
            } catch (EspStoreException e) {
            }
        }
        // 教学目标列表数据
        List<Map<String, Object>> objectiveList = new ArrayList<>();
        List<InstructionalObjective> oList = new ArrayList<>();
        List<ResourceRelation> relationList = null;
        if (CollectionUtils.isNotEmpty(objectiveIdList)) {
            oList = objectiveRepository
                    .getAllByIds(objectiveIdList);

            relationList = resourceRelationRepository
                    .findSourcesByTargetIdsAndResType(objectiveIdList,
                            IndexSourceType.AssetType.getName(),
                            relationType);
            for (int i = relationList.size() - 1; i >= 0; i--) {
                if (relationList.get(i).getSourceUuid().equals(suiteId)) {
                    relationList.remove(i);
                }
            }

        }
        if (CollectionUtils.isEmpty(relationList)) {
            return returnMap;
        }

        for (InstructionalObjective objective : oList) {
            List<String> targetIds = new ArrayList<>();
            targetIds.add(objective.getIdentifier());
            List<String> knIds = resourceRelationRepository
                    .findSourceIdsByTargetIdsAndResType(targetIds,
                            IndexSourceType.KnowledgeType.getName(),
                            relationType);
//            List<String> sourceIds = resourceRelationRepository
//                    .findSourceIdsByTargetIdsAndResType(targetIds,
//                            IndexSourceType.AssetType.getName(),
//                            RelationType.ASSOCIATE.toString());

            Map<String, Object> map = new HashMap<>();
            for (ResourceRelation rr : relationList) {
                if (rr.getTarget().equals(objective.getIdentifier())) {
                    map.put("sourceUuid", rr.getSourceUuid());
                }
            }
            map.put("knIds", knIds);
            map.put("identifier", objective.getIdentifier());
            map.put("title", objective.getTitle());
            map.put("description", objective.getDescription());
            map.put("customProperties", objective.getCustomProperties());
            map.put("keywords", ObjectUtils.fromJson(objective.getDbkeywords(), List.class));
            map.put("providerSource", objective.getProviderSource());
            map.put("creator", objective.getCreator());
            map.put("status", objective.getStatus());

            objectiveList.add(map);
        }


        Map<String, List<String>> categoryMap = new HashMap<String, List<String>>();
        if (CollectionUtils.isNotEmpty(objectiveList)) {
            List<String> ioIds = new ArrayList<String>();
            for (Map<String, Object> objective : objectiveList) {
                String identifier = (String) objective.get("identifier");
                ioIds.add(identifier);
            }
            String cSql = "SELECT taxOnCode,resource from resource_categories where primary_category = 'instructionalobjectives' and resource in (:ids) and category_code = 'SL'";
            NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("ids", ioIds);

            List<Map<String, Object>> iList = npjt.query(cSql, params,
                    new RowMapper<Map<String, Object>>() {
                        @Override
                        public Map<String, Object> mapRow(ResultSet rs,
                                                          int rowNum) throws SQLException {
                            Map<String, Object> m = new HashMap<String, Object>();
                            String taxOnCode = rs.getString("taxOnCode");
                            String resource = rs.getString("resource");
                            m.put("taxOnCode", taxOnCode);
                            m.put("resource", resource);
                            return m;
                        }
                    });

            if (CollectionUtils.isNotEmpty(iList)) {
                for (Map<String, Object> map : iList) {
                    if (categoryMap.containsKey((String) map.get("resource"))) {
                        List<String> l = categoryMap.get((String) map
                                .get("resource"));
                        l.add((String) map.get("taxOnCode"));
                    } else {
                        List<String> l = new ArrayList<String>();
                        l.add((String) map.get("taxOnCode"));
                        categoryMap.put((String) map.get("resource"), l);
                    }
                }
            }
        }

        // 6、拼装数据
        returnMap.put("limit", limit);
        returnMap.put("total", total);
        if (CollectionUtils.isNotEmpty(sonMap)) {
            returnMap.put("suite_model", sonMap);
        }

        List<Map<String, Object>> itemList = new LinkedList<Map<String, Object>>();
        // 教学目标id的集合，用于查找子教学目标
        Set<String> obIds = new HashSet<String>();

        // 7、动态拼接教学目标title
        // Collection<Map.Entry<String, String>> idWithTitles = new
        // LinkedList<Map.Entry<String,String>>();

        for (Sample sample : subSampleList) {
            List<String> knIds = new ArrayList<>();
            String knId1 = sample.getKnowledgeId1();
            String knId2 = sample.getKnowledgeId2();
            String knId3 = sample.getKnowledgeId3();
            String knId4 = sample.getKnowledgeId4();
            String knId5 = sample.getKnowledgeId5();

            if (StringUtils.isNotEmpty(knId1)) {
                knIds.add(knId1);
            }
            if (StringUtils.isNotEmpty(knId2)) {
                knIds.add(knId2);
            }
            if (StringUtils.isNotEmpty(knId3)) {
                knIds.add(knId3);
            }
            if (StringUtils.isNotEmpty(knId4)) {
                knIds.add(knId4);
            }
            if (StringUtils.isNotEmpty(knId5)) {
                knIds.add(knId5);
            }

            Map<String, Object> knMap = new LinkedHashMap<String, Object>();
            knMap.put("sample", sample);

            List<Map<String, Object>> knDataList = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> objective : objectiveList) {
                if (CommonServiceHelper.listEqual(knIds,
                        (List<String>) objective.get("knIds"))) { // FIXME
                    // 没管knid顺序
                    Map<String, Object> dataMap = new LinkedHashMap<String, Object>();

                    // 教学目标类型id
                    String otId = (String) objective.get("sourceUuid");
                    // 教学目标Id
                    String identifier = (String) objective.get("identifier");
                    obIds.add(identifier);

                    // 教学目标title
                    String title = (String) objective.get("title");
                    // 教学目标description
                    String description = (String) objective.get("description");
                    // 教学目标creator
                    String creator = (String) objective.get("creator");
                    // 教学目标自定义属性customProperties
                    String customProperties = (String) objective
                            .get("customProperties");
                    // 提供商来源
                    String providerSource = (String) objective
                            .get("providerSource");
                    // 备注
                    List<String> keywords = (List<String>) objective
                            .get("keywords");
                    // 状态
                    String status = (String) objective.get("status");

                    // Map.Entry<String,String> entry = new
                    // DefaultMapEntry(identifier,title);
                    // idWithTitles.add(entry);

                    for (RelationForQueryViewModel vm : modelList.getItems()) {
                        if (StringUtils.isNotEmpty(otId)
                                && otId.equals(vm.getIdentifier())) {
                            boolean flag = false;
                            for (Map<String, Object> map6 : knDataList) {
                                if (map6.get("objective_type") != null) {
                                    if (otId.equals(((Map) map6
                                            .get("objective_type"))
                                            .get("identifier"))) {
                                        flag = true;
                                        break;
                                    }
                                }
                            }
                            if (flag) {
                                break;
                            }

                            Map<String, Object> tmp = new HashMap<String, Object>();
                            tmp.put("identifier", otId);
                            tmp.put("title", vm.getTitle());
                            tmp.put("description", vm.getDescription());
                            tmp.put("custom_properties",
                                    vm.getCustomProperties());

                            String tmpSuiteId = vm.getSid();
                            if (tmpSuiteId.equals(suiteId)) {
                                tmp.put("suite_id", suiteId);
                            }

                            for (Map<String, Object> map3 : suiteList) {
                                if (tmpSuiteId.equals((String) map3
                                        .get("identifier"))) {
                                    tmp.put("suite_id", map3.get("identifier"));
                                }
                            }

                            dataMap.put("objective_type", tmp);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(dataMap)) {
                        knDataList.add(dataMap);
                    }

                    for (Map<String, Object> map4 : knDataList) {
                        if (map4.get("objective_type") != null) {
                            if (StringUtils.isNotEmpty(otId)
                                    && otId.equals(((Map) map4
                                    .get("objective_type"))
                                    .get("identifier"))) {
                                if (map4.get("objectives") == null) {
                                    map4.put(
                                            "objectives",
                                            new ArrayList<Map<String, Object>>());
                                }
                                List<Map<String, Object>> tmpList = (List) map4
                                        .get("objectives");
                                Map<String, Object> map5 = new HashMap<String, Object>();
                                map5.put("identifier", identifier);
                                map5.put("title", title);
                                map5.put("description", description);
                                map5.put("creator", creator);
                                if (StringUtils.isEmpty(providerSource)) {
                                    map5.put("providerSource", "");
                                } else {
                                    map5.put("providerSource", providerSource);
                                }

                                if (StringUtils.isNotEmpty(customProperties)) {
                                    map5.put("custom_properties", ObjectUtils
                                            .fromJson(customProperties,
                                                    Map.class));
                                } else {
                                    map5.put("custom_properties",
                                            new HashMap<String, String>());
                                }
                                if (CollectionUtils.isNotEmpty(keywords)) {
                                    map5.put("keywords", keywords);
                                } else {
                                    map5.put("keywords",
                                            new ArrayList<String>());
                                }

                                map5.put("versions", new ArrayList<String>());
                                map5.put("status", status);

                                if (categoryMap.containsKey(identifier)) {
                                    map5.put("applicable_period",
                                            categoryMap.get(identifier));
                                } else {
                                    map5.put("applicable_period",
                                            new ArrayList<String>());
                                }
                                tmpList.add(map5);
                            }
                        }
                    }
                }
            }
            knMap.put("datas", knDataList);
            itemList.add(knMap);
        }

        // 动态拼接教学目标title(门户暂不需要)
        // if(CollectionUtils.isNotEmpty(idWithTitles)){
        // Map<String, String> obTitleMap =
        // getInstructionalObjectiveTitle(idWithTitles);
        // for (Map<String,Object> mm : itemList) {
        // List<Map<String,Object>> knDataList = (List)mm.get("datas");
        // for (Map<String, Object> map7 : knDataList) {
        // if(map7.get("objectives") != null){
        // List<Map<String,Object>> tmpList = (List)map7.get("objectives");
        // if(CollectionUtils.isNotEmpty(tmpList)){
        // for (Map<String, Object> map2 : tmpList) {
        // if(obTitleMap.containsKey((String)map2.get("identifier"))){
        // String tmpTitle = obTitleMap.get((String)map2.get("identifier"));
        // map2.put("title", tmpTitle);
        // }
        // }
        // }
        // }
        // }
        // }
        // }

        // 查找子教学目标
        ListViewModel<RelationForQueryViewModel> subInstructionalList = educationRelationService
                .batchQueryResourcesByDB(
                        IndexSourceType.InstructionalObjectiveType.getName(),
                        obIds, IndexSourceType.SubInstructionType.getName(),
                        null, null, relationType, "0,500",
                        null, false, true, false);
        if (CollectionUtils.isNotEmpty(subInstructionalList.getItems())) {
            for (RelationForQueryViewModel tmpVm : subInstructionalList
                    .getItems()) {
                // 教学目标id
                String obId = tmpVm.getSid();
                // 子教学目标描述
                String desc = tmpVm.getDescription();
                for (Map<String, Object> mm : itemList) {
                    List<Map<String, Object>> knDataList = (List) mm
                            .get("datas");
                    for (Map<String, Object> map7 : knDataList) {
                        if (map7.get("objectives") != null) {
                            List<Map<String, Object>> tmpList = (List) map7
                                    .get("objectives");
                            if (CollectionUtils.isNotEmpty(tmpList)) {
                                for (Map<String, Object> map2 : tmpList) {
                                    if (obId.equals((String) map2
                                            .get("identifier"))) {
                                        if (map2.get("versions") == null) {
                                            map2.put("versions",
                                                    new ArrayList<String>());
                                        }
                                        List<String> versions = (List) map2
                                                .get("versions");
                                        if (StringUtils.isNotEmpty(desc)
                                                && !versions.contains(desc)) {
                                            versions.add(desc);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 查找知识点（部分教学目标关联多个知识点）(重复数据放到sample里面  20161222)
//        ListViewModel<RelationForQueryViewModel> knowledgeList = educationRelationService
//                .batchQueryResourcesByDB(
//                        IndexSourceType.InstructionalObjectiveType.getName(),
//                        obIds, IndexSourceType.KnowledgeType.getName(), null,
//                        null, RelationType.ASSOCIATE.getName(), "0,1000", null,
//                        true, true, false);
//        if (CollectionUtils.isNotEmpty(knowledgeList.getItems())) {
//            for (RelationForQueryViewModel tmpVm : knowledgeList.getItems()) {
//                // 教学目标id
//                String obId = tmpVm.getSid();
//                // 知识点id
//                String identifier = tmpVm.getIdentifier();
//                // 知识点title
//                String title = tmpVm.getTitle();
//                // 知识点状态
//                String status = tmpVm.getStatus();
//                Map<String, Object> knm = new HashMap<String, Object>();
//                knm.put("identifier", identifier);
//                knm.put("title", title);
//                for (Map<String, Object> mm : itemList) {
//                    List<Map<String, Object>> knDataList = (List) mm
//                            .get("datas");
//                    for (Map<String, Object> map7 : knDataList) {
//                        if (map7.get("objectives") != null) {
//                            List<Map<String, Object>> tmpList = (List) map7
//                                    .get("objectives");
//                            if (CollectionUtils.isNotEmpty(tmpList)) {
//                                for (Map<String, Object> map2 : tmpList) {
//                                    if (obId.equals((String) map2
//                                            .get("identifier"))) {
//                                        if (map2.get("knowledge_list") == null) {
//                                            map2.put(
//                                                    "knowledge_list",
//                                                    new ArrayList<Map<String, Object>>());
//                                        }
//                                        List<Map<String, Object>> kl = (List) map2
//                                                .get("knowledge_list");
//                                        kl.add(knm);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        //暂时不用回传分层数据
//        if (CollectionUtils.isNotEmpty(sonMap)) {
//            Map<String, Object> map = new HashMap<>();
//            map.put("sonTree", sonMap);
//            itemList.add(map);
//        }
        returnMap.put("items", itemList);
        return returnMap;
    }

    @Override
    public void changeStatus(List<Map<String, Object>> params) {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        // 记录生命周期状态
        List<Contribute> contributeList = new ArrayList<Contribute>();
        // 同步ES集合
        Set<Resource> resSet = new HashSet<Resource>();

        for (Map<String, Object> map : params) {
            // 资源类型
            String resType = (String) map.get("res_type");
            // 用户Id
            String userId = (String) map.get("user_id");
            // 资源items
            List<Map<String, String>> items = (List) map.get("items");
            Set<String> ids = new HashSet<String>();
            // 校验资源是否存在
            if (CollectionUtils.isNotEmpty(items)) {
                for (Map<String, String> map2 : items) {
                    String identifier = map2.get("identifier");
                    ids.add(identifier);
                }
                if (resType.equals("samples")) {
                    List<String> rids = new ArrayList<String>(ids);
                    List<Sample> list;
                    try {
                        list = sampleRepository.getAll(rids);
                        if (list.size() != ids.size()) {
                            for (String id : ids) {
                                boolean f = false;
                                for (Sample rfvm : list) {
                                    if (rfvm.getIdentifier().equals(id)) {
                                        f = true;
                                        break;
                                    }
                                }
                                if (!f) {
                                    throw new LifeCircleException(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            "LC/CHECK_PARAM_VALID_FAIL",
                                            "资源不存在！identifier:" + id);
                                }
                            }
                        }
                    } catch (EspStoreException e) {
                        e.printStackTrace();
                    }
                } else {
                    List<ResourceModel> list = ndResourceService.batchDetail(
                            resType, ids, new ArrayList<String>());
                    if (list.size() != ids.size()) {
                        for (String id : ids) {
                            boolean f = false;
                            for (ResourceModel resourceModel : list) {
                                if (resourceModel.getIdentifier().equals(id)) {
                                    f = true;
                                    break;
                                }
                            }
                            if (!f) {
                                throw new LifeCircleException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "LC/CHECK_PARAM_VALID_FAIL",
                                        "资源不存在！identifier:" + id);
                            }
                        }
                    }
                }
                ResourceRepository<? extends EspEntity> resourceRepository = null;
                if (resType.equals("samples")) {
                    resourceRepository = sampleRepository;
                } else {
                    resourceRepository = commonServiceHelper
                            .getRepository(resType);
                }
                List idList = new ArrayList<String>();
                idList.addAll(ids);
                List<? extends EspEntity> entityList = null;
                try {
                    entityList = resourceRepository.getAll(idList);
                } catch (EspStoreException e1) {
                    e1.printStackTrace();
                }
                List resourceList = new ArrayList();
                if (CollectionUtils.isNotEmpty(entityList)) {
                    for (EspEntity espEntity : entityList) {
                        for (Map<String, String> map2 : items) {
                            String identifier = map2.get("identifier");
                            String status = map2.get("status");
                            String message = map2.get("message");
                            if (espEntity.getIdentifier().equals(identifier)
                                    && resType.equals("samples")) {
                                Sample relation = (Sample) espEntity;
                                relation.setStatus(status);
                                resourceList.add(relation);
                                // 保存资源生命周期及同步到ES
                                contributeList
                                        .add(addContribute(resType, identifier,
                                                status, userId, ts, message));
                                // resSet.add(new Resource(resType,
                                // identifier));
                            } else if (espEntity.getIdentifier().equals(
                                    identifier)) {
                                Education edu = (Education) espEntity;
                                edu.setStatus(status);
                                resourceList.add(edu);
                                // 保存资源生命周期及同步到ES
                                contributeList
                                        .add(addContribute(resType, identifier,
                                                status, userId, ts, message));
                                resSet.add(new Resource(resType, identifier));
                            }
                        }
                    }

                    if (CollectionUtils.isNotEmpty(resourceList)) {
                        try {
                            resourceRepository.batchAdd(resourceList);
                        } catch (EspStoreException e) {
                            throw new LifeCircleException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    LifeCircleErrorMessageMapper.StoreSdkFail
                                            .getCode(), e.getMessage());
                        }
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(contributeList)) {
                try {
                    contributeRepository.batchAdd(contributeList);
                } catch (EspStoreException e) {
                    throw new LifeCircleException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
                            e.getMessage());
                }
            }

            // 同步ES
            esResourceOperation.asynBatchAdd(resSet);
        }
    }

    private Contribute addContribute(String resType, String resource,
                                     String status, String userId, Timestamp ts, String message) {
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
        contribute.setMessage(message);
        return contribute;
    }

    private Sample addSample(String suiteId, List<String> oldknIds, List<String> knId, String userId,
                             Map<String, String> knTitles) {
        Sample sample = new Sample();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        sample.setIdentifier(UUID.randomUUID().toString());
        sample.setAssetId(suiteId);
        if (oldknIds.size() < 2) {
            sample.setKnowledgeId1(knId.get(0));
            sample.setTitle(knTitles.get(oldknIds.get(0)));
        }
        // 要给知识点id进行hashcode排序
        // String temp = "";
        String title = "";
        // for (int i = 0; i < knIds.size() - 1; i++) {
        // for (int j = 0; j < knIds.size() - i - 1; j++) {
        // if (knIds.get(i).hashCode() > knIds.get(j).hashCode()) {
        // temp = knIds.get(i);
        // knIds.set(i, knIds.get(j));
        // knIds.set(j, temp);
        // }
        // }
        // }
        switch (oldknIds.size()) {
            case 2: {
                sample.setKnowledgeId1(knId.get(0));
                sample.setKnowledgeId2(knId.get(1));
                title = knTitles.get(oldknIds.get(0)) + "/"
                        + knTitles.get(oldknIds.get(1));
                sample.setTitle(title);
                break;
            }
            case 3: {
                sample.setKnowledgeId1(knId.get(0));
                sample.setKnowledgeId2(knId.get(1));
                sample.setKnowledgeId3(knId.get(2));
                title = knTitles.get(oldknIds.get(0)) + "/"
                        + knTitles.get(oldknIds.get(1)) + "/"
                        + knTitles.get(oldknIds.get(2));
                sample.setTitle(title);
                break;
            }
            case 4: {
                sample.setKnowledgeId1(knId.get(0));
                sample.setKnowledgeId2(knId.get(1));
                sample.setKnowledgeId3(knId.get(2));
                sample.setKnowledgeId4(knId.get(3));
                title = knTitles.get(oldknIds.get(0)) + "/"
                        + knTitles.get(oldknIds.get(1)) + "/"
                        + knTitles.get(oldknIds.get(2)) + "/"
                        + knTitles.get(oldknIds.get(3));
                sample.setTitle(title);
                break;
            }
            case 5: {
                sample.setKnowledgeId1(knId.get(0));
                sample.setKnowledgeId2(knId.get(1));
                sample.setKnowledgeId3(knId.get(2));
                sample.setKnowledgeId4(knId.get(3));
                sample.setKnowledgeId5(knId.get(4));
                title = knTitles.get(oldknIds.get(0)) + "/"
                        + knTitles.get(oldknIds.get(1)) + "/"
                        + knTitles.get(oldknIds.get(2)) + "/"
                        + knTitles.get(oldknIds.get(3)) + "/"
                        + knTitles.get(oldknIds.get(4));
                sample.setTitle(title);
                break;
            }
        }
        sample.setCreateTime(ts);
        sample.setLastUpdate(ts);
        sample.setCreator(userId);
        sample.setEnable(true);
        sample.setStatus(LifecycleStatus.CREATED.toString());
        return sample;
    }

    @Override
    public List<SuitAndInstructionalObjectiveModel> getSuitAndInstructionalObjective(
            List<String> suitIdList) {

        List<ResultsModel> resultsModelList = getInstructionalObjectiveBySuitId(suitIdList);
        Pattern pattern = Pattern.compile("<span.+?</span>");
        if (CollectionUtils.isNotEmpty(resultsModelList)) {
            for (ResultsModel model : resultsModelList) {
                if (model.getTitle() != null) {
                    String hashString = pattern.matcher(model.getTitle()).replaceAll("【XX】");
                    model.setTitle(hashString);
                    int hashCode = hashString.hashCode();
                    model.setHashCode(hashCode);
                }
            }
        }

        // 计算每个套件对应学习目标的hashCode
        Map<Integer, List<String>> sourceIdMap = new HashMap<Integer, List<String>>();
        // 保存所有的hashCode
        List<Integer> hashCodeList = new ArrayList<Integer>();
        for (int i = 0; i < suitIdList.size(); i++) {
            int totalHashCode = 34;
            boolean flag = false;
            for (int j = 0; j < resultsModelList.size(); j++) {

                if (suitIdList.get(i).equals(
                        resultsModelList.get(j).getSourceId())) {
                    flag = true;
                    totalHashCode += 17 * resultsModelList.get(j).getHashCode();
                }
            }
            if (flag) {

                if (!sourceIdMap.containsKey(totalHashCode)) {
                    List<String> sourceIdList = new ArrayList<String>();
                    sourceIdList.add(suitIdList.get(i));
                    sourceIdMap.put(totalHashCode, sourceIdList);
                    hashCodeList.add(totalHashCode);
                } else {
                    List<String> tempList = sourceIdMap.get(totalHashCode);
                    tempList.add(suitIdList.get(i));
                }
            }
        }
        // 根据hashcode归类，返回结果
        List<SuitAndInstructionalObjectiveModel> list = new ArrayList<SuitAndInstructionalObjectiveModel>();
        for (int k = 0; k < hashCodeList.size(); k++) {
            SuitAndInstructionalObjectiveModel sacwm = new SuitAndInstructionalObjectiveModel();
            List<String> sourceIdList = sourceIdMap.get(hashCodeList.get(k));
            List<String> title = new ArrayList<String>();
            List<Map<String, String>> suitList = new ArrayList<Map<String, String>>();
            List<SuiteModel> suiteModel = new ArrayList<SuiteModel>();
            for (int m = 0; m < sourceIdList.size(); m++) {
                Map<String, String> suitMap = new HashMap<String, String>();
                SuiteModel sm = new SuiteModel();
                for (int n = 0; n < resultsModelList.size(); n++) {
                    if (sourceIdList.get(m).equals(
                            resultsModelList.get(n).getSourceId())) {
                        title.add(resultsModelList.get(n).getTitle());
                        if (!suitMap.containsKey(sourceIdList.get(m))) {
                            suitMap.put(sourceIdList.get(m), resultsModelList
                                    .get(n).getSuiteTitle());
                            suitList.add(suitMap);
                            sm.setIdentifier(sourceIdList.get(m));
                            sm.setDescription(resultsModelList.get(n)
                                    .getSuiteTitle());
                            sm.setSuiteType(resultsModelList.get(n).getRelationType());
                            suiteModel.add(sm);
                        }

                    }
                }
            }
            HashSet<String> hs = new HashSet<String>(title);
            List<String> DuplicateTitle = new ArrayList<String>(hs);
            sacwm.setSuiteList(suiteModel);
            sacwm.setObjectiveTypeTitle(DuplicateTitle);
            list.add(sacwm);
        }

        // 对结果中的suit_list按个数进行排序
        List<SuitAndInstructionalObjectiveModel> sortlist = new ArrayList<SuitAndInstructionalObjectiveModel>();
        while (list.size() > 0) {
            SuitAndInstructionalObjectiveModel maxModel = getMaxCount(list);
            sortlist.add(maxModel);
            list.remove(maxModel);
        }

        return sortlist;
    }

    private static SuitAndInstructionalObjectiveModel getMaxCount(
            List<SuitAndInstructionalObjectiveModel> list) {
        // 记录下标对应的suite_list对应的个数
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0; i < list.size(); i++) {
            map.put(i, list.get(i).getSuiteList().size());
        }
        int max = map.get(0);
        int n = 0;
        for (int j = 0; j < list.size(); j++) {
            if (max < map.get(j)) {
                max = map.get(j);
                n = j;
            }
        }
        return list.get(n);
    }

    private List<ResultsModel> getInstructionalObjectiveBySuitId(
            List<String> suitId) {

        return instructionalobjectiveDao.getInstructionalObjectiveById(suitId);
    }


    /**
     * 通过suiteId查询教学目标类型
     *
     * @author xm
     * @date 2016年11月29日 下午8:50:34
     */


    //TODO 修复学习目标sortNum数据
    @Override
    public List<String> updateSortNum(List<String> objectiveTypeIds, float repairedNum) {
        List<String> objectiveTypeList = new ArrayList<>();
        List<String> repairedId = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(objectiveTypeIds)) {
            objectiveTypeList = objectiveTypeIds;
        } else {
            objectiveTypeList = queryAllObjectiveTypesId();
        }
        if (CollectionUtils.isNotEmpty(objectiveTypeList)) {
            for (String objectiveTypeId : objectiveTypeList) {
                List<ResourceRelation> createRelationList = new ArrayList<>();

                ResourceRelation example = new ResourceRelation();
                example.setRelationType(RelationType.ASSOCIATE.getName());
                example.setSourceUuid(objectiveTypeId);
                example.setResType(IndexSourceType.AssetType.getName());
                example.setResourceTargetType(IndexSourceType.InstructionalObjectiveType.getName());

                List<ResourceRelation> relations = new ArrayList<>();
                try {
                    relations = resourceRelationRepository.getAllByExample(example);
                } catch (EspStoreException e) {
                    e.printStackTrace();
                }
                if (CollectionUtils.isNotEmpty(relations)) {
                    repairObjectiveSortNum(relations, createRelationList, repairedNum);
                    repairedId.add(objectiveTypeId);
                }
                if (CollectionUtils.isNotEmpty(createRelationList)) {
                    try {
                        resourceRelationRepository.batchAdd(createRelationList);
                    } catch (EspStoreException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return repairedId;
    }

    private List<String> queryAllObjectiveTypesId() {
        List<String> objectiveTypesIds = new ArrayList<>();
        Map<String, Object> param = new HashMap<>();
        String sql = "SELECT nd.identifier,nd.description from ndresource nd,resource_categories rc where nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0503' and rc.resource=nd.identifier and nd.enable = 1 order by nd.create_time";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        List<Map<String, Object>> list = npjt.queryForList(sql, param);
        for (Map<String, Object> map : list) {
            objectiveTypesIds.add((String) map.get("identifier"));
        }
        return objectiveTypesIds;
    }

    private void repairObjectiveSortNum(List<ResourceRelation> relations, List<ResourceRelation> createRelationList, float repairedNum) {
        List<String> targetIds = new ArrayList<>();
        for (ResourceRelation relation : relations) {
            targetIds.add(relation.getTarget());
        }
        float max = 0;
        List<Map<String, Object>> sortNumMap = findSortNumOrderByCreateTimeByTargetId(targetIds);
        max = findMaxSortNumInList(sortNumMap, max, repairedNum) + 10;
        int num = 0;
        for (Map<String, Object> map : sortNumMap) {
            if ((float) map.get("sort_num") == repairedNum) {
                num++;
            }
        }
        for (Map<String, Object> map : sortNumMap) {
            if ((float) map.get("sort_num") == repairedNum && num > 1) {
                map.put("sort_num", max);
                max = max + 10;
            }
            for (ResourceRelation relation1 : relations) {
                if (relation1.getIdentifier().equals(map.get("identifier"))) {
                    relation1.setSortNum((Float) map.get("sort_num"));
                    createRelationList.add(relation1);
                }
            }
        }
    }

    //查出sort_num并按照创建时间递增排序
    private List<Map<String, Object>> findSortNumOrderByCreateTimeByTargetId(List<String> targetIds) {
        String cSql = "select rr.sort_num,rr.identifier from resource_relations rr,ndresource nr where rr.res_type = 'assets' and rr.resource_target_type = 'instructionalobjectives' and rr.target = nr.identifier and rr.enable = true and nr.enable = true and rr.target in (:ids) ORDER BY nr.create_time";
        List<Map<String, Object>> result = new ArrayList<>();

        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
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

    private boolean checkSampleList(List<Sample> sampleList, Sample checkSample, int size) {
        switch (size) {
            case 1: {
                for (Sample sample : sampleList) {
                    if (StringUtils.isEmpty(sample.getKnowledgeId1())) {
                        continue;
                    }
                    if (sample.getAssetId().equals(checkSample.getAssetId()) &&
                            (sample.getKnowledgeId1().equals(checkSample.getKnowledgeId1()))) {
                        return false;
                    }
                }
                return true;
            }
            case 2: {
                for (Sample sample : sampleList) {
                    if (StringUtils.isEmpty(sample.getKnowledgeId1()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId2())) {
                        continue;
                    }
                    if (sample.getAssetId().equals(checkSample.getAssetId()) &&
                            (sample.getKnowledgeId1().equals(checkSample.getKnowledgeId1())) &&
                            (sample.getKnowledgeId2().equals(checkSample.getKnowledgeId2()))) {
                        return false;
                    }
                }
                return true;
            }
            case 3: {
                for (Sample sample : sampleList) {
                    if (StringUtils.isEmpty(sample.getKnowledgeId1()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId2()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId3())) {
                        continue;
                    }
                    if (sample.getAssetId().equals(checkSample.getAssetId()) &&
                            (sample.getKnowledgeId1().equals(checkSample.getKnowledgeId1())) &&
                            (sample.getKnowledgeId2().equals(checkSample.getKnowledgeId2())) &&
                            (sample.getKnowledgeId3().equals(checkSample.getKnowledgeId3()))) {
                        return false;
                    }
                }
                return true;
            }
            case 4: {
                for (Sample sample : sampleList) {
                    if (StringUtils.isEmpty(sample.getKnowledgeId1()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId2()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId3()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId4())) {
                        continue;
                    }
                    if (sample.getAssetId().equals(checkSample.getAssetId()) &&
                            (sample.getKnowledgeId1().equals(checkSample.getKnowledgeId1())) &&
                            (sample.getKnowledgeId2().equals(checkSample.getKnowledgeId2())) &&
                            (sample.getKnowledgeId3().equals(checkSample.getKnowledgeId3())) &&
                            (sample.getKnowledgeId4().equals(checkSample.getKnowledgeId4()))) {
                        return false;
                    }
                }
                return true;
            }
            case 5: {
                for (Sample sample : sampleList) {
                    if (StringUtils.isEmpty(sample.getKnowledgeId1()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId2()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId3()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId4()) ||
                            StringUtils.isEmpty(sample.getKnowledgeId5())) {
                        continue;
                    }
                    if (sample.getAssetId().equals(checkSample.getAssetId()) &&
                            (sample.getKnowledgeId1().equals(checkSample.getKnowledgeId1())) &&
                            (sample.getKnowledgeId2().equals(checkSample.getKnowledgeId2())) &&
                            (sample.getKnowledgeId3().equals(checkSample.getKnowledgeId3())) &&
                            (sample.getKnowledgeId4().equals(checkSample.getKnowledgeId4())) &&
                            (sample.getKnowledgeId5().equals(checkSample.getKnowledgeId5()))) {
                        return false;
                    }
                }
                return true;
            }
            default:
                return false;
        }

    }

    //找到学习目标类型与学习目标关系表里最大的sortNum，取不到就从内存取，内存没有就取默认值
    //todo 修复数据是修复5001f的数据
    public float dealObjectivesSortNum(String objectiveTypeId, Map<String, Float> sortNumMap, String relationType) {
        ResourceRelation example = new ResourceRelation();
        example.setRelationType(relationType);
        example.setSourceUuid(objectiveTypeId);
        example.setResourceTargetType(IndexSourceType.InstructionalObjectiveType.getName());

        List<ResourceRelation> relations = new ArrayList<>();
        try {
            relations = resourceRelationRepository.getAllByExample(example);
        } catch (EspStoreException e) {
            e.printStackTrace();
        }

        //第一个 暂时写死为5001 FIXME
        if (CollectionUtils.isEmpty(relations) && CollectionUtils.isNotEmpty(sortNumMap) && sortNumMap.get(example.getSourceUuid()) == null) {
            sortNumMap.put(example.getSourceUuid(), 5001f);
            return 5001;
        }
        if (CollectionUtils.isEmpty(relations) && CollectionUtils.isEmpty(sortNumMap)) {
            sortNumMap.put(example.getSourceUuid(), 5001f);
            return 5001;
        }

        float max = 0;
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

    @Override
    public void changeSampleStatus(String suiteId, String status) {
        if (StringUtils.isNotEmpty(status)) {
            if (!LifecycleStatus.isLegalStatus(status)) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, "", "状态码不对");
            }
        } else {
            status = LifecycleStatus.CREATED.getCode();
        }

        Sample example = new Sample();
        example.setEnable(true);
        example.setAssetId(suiteId);
        try {
            List<Sample> samples = sampleRepository.getAllByExample(example);
            if (CollectionUtils.isNotEmpty(samples)) {
                List<Sample> sampleList = new ArrayList<>();
                for (Sample sample : samples) {
                    sample.setStatus(status);
                    sampleList.add(sample);
                }
                sampleRepository.batchAdd(sampleList);
            }
        } catch (EspStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询最子套件,并归类，并加缓存
     *
     * @author xm
     * @date 2016年11月24日 上午10:27:51
     */
    @Override
    public ListViewModel<ClassifySuitAndInstructionalObjectiveModel> classfiDownSuiteQuery(String words, String limit, String status, String category) {
        Integer limitNum[] = ParamCheckUtil.checkLimit(limit);

        Map<String, List<String>> resultClassifyByDescription = new HashMap<String, List<String>>();

        String key = "";

        if (StringUtils.isNotEmpty(status)) {
            key = "classifyByStatusAndDownSubSuite" + status;
            if (StringUtils.isNotEmpty(category)) {
                key += category;
            }
            resultClassifyByDescription = getclassifyDownSubSuiteListFromRedis(key);
            List<Map<String, String>> downSubSuiteList = new ArrayList<Map<String, String>>();
            if (CollectionUtils.isEmpty(resultClassifyByDescription)) {
                // redis没值，则查数据库,查询最子套件列表
                downSubSuiteList = queryDownSubsuite(status, category);
                // 归类,形成 key（ String"description"）--value（List<String> "identifier"）
                if (!downSubSuiteList.isEmpty()) {
                    resultClassifyByDescription = classifyByDescription(downSubSuiteList);
                } else {
                    ListViewModel<ClassifySuitAndInstructionalObjectiveModel> resultViewModel = new ListViewModel<ClassifySuitAndInstructionalObjectiveModel>();
                    resultViewModel.setLimit(limit);
                    resultViewModel.setTotal((long) 0);
                    resultViewModel.setItems(new ArrayList<ClassifySuitAndInstructionalObjectiveModel>());
                    return resultViewModel;
                }
                // 存到缓存中取，有status的缓存又不一样，注意在这添加，存到不同的位置上面去
                saveClassifyDownSubSuiteResultToRedis(resultClassifyByDescription, key);
            }
        } else {
            key = "classifyDownSubSuite";
            if (StringUtils.isNotEmpty(category)) {
                key += category;
            }
            //缓存，从redis缓存中取值
            resultClassifyByDescription = getclassifyDownSubSuiteListFromRedis(key);
            List<Map<String, String>> downSubSuiteList = new ArrayList<Map<String, String>>();
            if (CollectionUtils.isEmpty(resultClassifyByDescription)) {
                // redis没值，则查数据库,查询最子套件列表
                downSubSuiteList = queryDownSubsuite(status, category);
                // 归类,形成 key（ String"description"）--value（List<String> "identifier"）
                if (!downSubSuiteList.isEmpty()) {
                    resultClassifyByDescription = classifyByDescription(downSubSuiteList);
                } else {
                    ListViewModel<ClassifySuitAndInstructionalObjectiveModel> resultViewModel = new ListViewModel<ClassifySuitAndInstructionalObjectiveModel>();
                    resultViewModel.setLimit(limit);
                    resultViewModel.setTotal((long) 0);
                    resultViewModel.setItems(new ArrayList<ClassifySuitAndInstructionalObjectiveModel>());
                    return resultViewModel;
                }
                // 存到缓存中取，有status的缓存又不一样，注意在这添加，存到不同的位置上面去

                saveClassifyDownSubSuiteResultToRedis(resultClassifyByDescription, key);
            }
        }

        // 根据关键字进行搜索
        if (StringUtils.isEmpty(words)) {
            // 返回查询到的所有的值,对resultClassifyByDescription进行limit
            long size = resultClassifyByDescription.size();
            if (limitNum[0] >= size) {
                // 返回空的值viewmodel
                ListViewModel<ClassifySuitAndInstructionalObjectiveModel> result = new ListViewModel<ClassifySuitAndInstructionalObjectiveModel>();
                result.setTotal((long) resultClassifyByDescription.size());
                result.setLimit(limit);
                result.setItems(new ArrayList<ClassifySuitAndInstructionalObjectiveModel>());
                return result;
            }
            long totalSize = limitNum[0] + limitNum[1];
            int lastIndex = 0;
            if (totalSize > size) {
                lastIndex = (int) size;
            } else {
                lastIndex = (int) totalSize;
            }

            ListViewModel<ClassifySuitAndInstructionalObjectiveModel> resultViewModel = new ListViewModel<ClassifySuitAndInstructionalObjectiveModel>();
            List<ClassifySuitAndInstructionalObjectiveModel> classiySuitList = new ArrayList<ClassifySuitAndInstructionalObjectiveModel>();
            // 遍历map对象
            int i = limitNum[0];
            int j = 0;
            for (String description : resultClassifyByDescription.keySet()) {
                if (j < i) {
                    j++;
                } else {
                    if (j < lastIndex) {
                        ClassifyDownSuite downSuiteTmep = new ClassifyDownSuite();
                        Map<String, List<String>> mapTemp = new HashMap<String, List<String>>();
                        mapTemp.put(description,
                                resultClassifyByDescription.get(description));
                        downSuiteTmep.setClassifyBySuiteMap(mapTemp);
                        ClassifySuitAndInstructionalObjectiveModel csaiom = new ClassifySuitAndInstructionalObjectiveModel();
                        csaiom.setTitle(description);
                        List<SuitAndInstructionalObjectiveModel> group = getSuitAndInstructionalObjective(resultClassifyByDescription.get(description));
                        csaiom.setGroup(group);
                        classiySuitList.add(csaiom);
                        j++;
                    } else {
                        break;
                    }

                }
            }

            resultViewModel.setItems(classiySuitList);
            resultViewModel.setLimit(limit);
            resultViewModel.setTotal((long) resultClassifyByDescription.size());
            return resultViewModel;

        } else {
            ListViewModel<ClassifySuitAndInstructionalObjectiveModel> resultViewModel = new ListViewModel<ClassifySuitAndInstructionalObjectiveModel>();
            List<ClassifySuitAndInstructionalObjectiveModel> classiySuitList = new ArrayList<ClassifySuitAndInstructionalObjectiveModel>();

            // 做模糊查询
            List<Map<String, List<String>>> tempResultList = new ArrayList<Map<String, List<String>>>();
            for (String description : resultClassifyByDescription.keySet()) {
                // 把【】去掉
                String matchedString = Pattern.compile("\\【.+?\\】").matcher(description).replaceAll("");
                // 匹配
                //转义words字符串中特殊字符
                String regexWords = CommonHelper.EscapeSpecial(words);
                String regex = ".*" + regexWords + ".*";
                if (matchedString.matches(regex)) {
                    Map<String, List<String>> mapTemp = new HashMap<String, List<String>>();
                    mapTemp.put(description,
                            resultClassifyByDescription.get(description));
                    tempResultList.add(mapTemp);
                    ClassifySuitAndInstructionalObjectiveModel csaiom = new ClassifySuitAndInstructionalObjectiveModel();
                    csaiom.setTitle(description);
                    List<SuitAndInstructionalObjectiveModel> group = getSuitAndInstructionalObjective(resultClassifyByDescription.get(description));
                    csaiom.setGroup(group);
                    classiySuitList.add(csaiom);
                }
            }

            if (tempResultList.isEmpty()) {
                // 若根据关键字找不到，则返回一个空值
                ListViewModel<ClassifySuitAndInstructionalObjectiveModel> result = new ListViewModel<ClassifySuitAndInstructionalObjectiveModel>();
                result.setTotal((long) 0);
                result.setLimit(limit);
                result.setItems(new ArrayList<ClassifySuitAndInstructionalObjectiveModel>());
                return result;
            }

            long size = tempResultList.size();
            if (limitNum[0] >= size) {
                // 若根据关键字找不到，则返回一个空值
                ListViewModel<ClassifySuitAndInstructionalObjectiveModel> result = new ListViewModel<ClassifySuitAndInstructionalObjectiveModel>();
                result.setTotal((long) tempResultList.size());
                result.setLimit(limit);
                result.setItems(new ArrayList<ClassifySuitAndInstructionalObjectiveModel>());
                return result;
            }
            long totalSize = limitNum[0] + limitNum[1];
            int lastIndex = 0;
            if (totalSize > size) {
                lastIndex = (int) size;
            } else {
                lastIndex = (int) totalSize;
            }

            // 根据limit判断去的description和description对应的list的值
            ListViewModel<ClassifyDownSuite> result = new ListViewModel<ClassifyDownSuite>();

            resultViewModel.setItems(classiySuitList.subList(limitNum[0], lastIndex));
            resultViewModel.setLimit(limit);
            resultViewModel.setTotal((long) tempResultList.size());
            return resultViewModel;
        }
    }

    /**
     * 查询最子套件
     *
     * @author xm
     * @date 2016年11月24日 上午10:27:51
     */
    @Override
    public List<Map<String, String>> queryDownSubsuite() {
        final List<Map<String, String>> downSubsutieList = new ArrayList<Map<String, String>>();
        // 查找最子套件，这个套件放在resouce_relation的source_uuid是没值的，放在target上是有值的

        String querySql = "SELECT nd.identifier as identifier ,nd.description as description,nd.title as suiteName "
                + "FROM ndresource as nd,resource_relations as rr,resource_categories as rc, resource_categories as rc2 "
                + "WHERE nd.primary_category = 'assets' "
                + "and nd.enable=1 and rr.enable=1 "
                + "and nd.identifier=rc.resource "
                + "and rc.taxOnCode='$RA0502' "
                + "and rr.target=nd.identifier "
                + "and rr.relation_type != 'COPY'"
                + "and rr.res_type='assets' "
                + "and rr.source_uuid=rc2.resource "
                + "and rc2.taxOnCode='$RA0502' "
                + "and rr.resource_target_type='assets' "
                + "and not exists(SELECT rr4.identifier "
                + "FROM resource_relations as rr4,resource_categories as rc4 "
                + "WHERE rr4.source_uuid=nd.identifier "
                + "and rr4.res_type='assets' "
                + "and rr4.resource_target_type='assets' "
                + "and rr4.enable=1 "
                + "and rr4.target=rc4.resource "
                + "and rr4.relation_type != 'COPY'"
                + "and rc4.taxOnCode='$RA0502') order by nd.title DESC";

        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
                defaultJdbcTemplate);
        namedJdbcTemplate.query(querySql, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, String> m = new HashMap<String, String>();
                String identifier = rs.getString("identifier");
                String suiteName = rs.getString("suiteName");
                String descriptionTemp = rs.getString("description");
                // 用正则表达式来做替换，如把【强酸A】的结构式替换为【xx】的结构式
                String regex = "\\【.+?\\】";
                String description = "";
                if (StringUtils.isNotEmpty(descriptionTemp)) {
                    description = Pattern.compile(regex)
                            .matcher(descriptionTemp).replaceAll("【XX】");
                }
                m.put("identifier", identifier);
                m.put("description", description);
                m.put("suiteName", suiteName);
                downSubsutieList.add(m);
                return null;
            }
        });

        return downSubsutieList;
    }

    /**
     * 重载queryDownSubsuite()方法
     *
     * @author xm
     * @date 2016年11月29日 下午2:12:44
     */
    @Override
    public List<Map<String, String>> queryDownSubsuite(String status, String category) {
        List<Map<String, String>> ParentSuiteByStatus = new ArrayList<>();
        ParentSuiteByStatus = queryRootParentByStatus(status, category);
        List<String> parentTitle = new ArrayList<String>();
        for (Map<String, String> map : ParentSuiteByStatus) {
            // 例如取出父套件"套件71"中的71
            if (map.get("title") != null && map.get("title").length() > 2) {
                parentTitle.add(map.get("title").substring(2));
            }
        }
        // 查了父类接口后匹配的子类接口
        List<Map<String, String>> allParentStatusAccordDownSuiteList = new ArrayList<Map<String, String>>();
        List<Map<String, String>> allDownSuiteList = queryDownSubsuite();
        for (String list : parentTitle) {
            for (Map<String, String> map : allDownSuiteList) {
                String suiteName = map.get("suiteName");
                String suiteNameFirstNum = new String();
                //subString越界控制,取最子套件的第一个数字如 套件71.1.1则取 71出来
                if (suiteName.length() > 2) {
                    if (suiteName.contains(".") && suiteName.indexOf(".") > 2) {
                        suiteNameFirstNum = suiteName.substring(2, suiteName.indexOf("."));
                    } else {
                        //不符合最子套件命名规则
                        suiteNameFirstNum = suiteName;
                    }
                } else {
                    //不符合最子套件命名规则的
                    suiteNameFirstNum = suiteName;
                }

                if (list.equals(suiteNameFirstNum)) {
                    allParentStatusAccordDownSuiteList.add(map);
                }
            }
        }
        return allParentStatusAccordDownSuiteList;
    }

    /**
     * 归类,形成 key（ String"description"）--value（List<String> "identifier"）
     *
     * @author xm
     * @date 2016年11月24日 上午10:27:51
     */
    @Override
    public Map<String, List<String>> classifyByDescription(List<Map<String, String>> list) {
        Map<String, List<String>> resultclassifyByDescription = new TreeMap<String, List<String>>();
        for (Map<String, String> map : list) {
            String description = map.get("description");
            String identifier = map.get("identifier");
            // 形成 key（ String"description"）--value（List<String> "identifier"）
            if (resultclassifyByDescription.containsKey(description)) {
                resultclassifyByDescription.get(description).add(identifier);
            } else {
                List<String> valueList = new ArrayList<String>();
                valueList.add(identifier);
                resultclassifyByDescription.put(description, valueList);
            }
        }
        return resultclassifyByDescription;

    }

    /**
     * 从缓存中取
     *
     * @author xm
     * @date 2016年11月24日 上午10:28:14
     */
    @Override
    public Map<String, List<String>> getclassifyDownSubSuiteListFromRedis(String key) {
        // key="classifyDownSubSuite"
        String keyQuery = key;
        // 判断key是否存在
        boolean flag = ert.existKey(keyQuery);
        if (!flag) {
            return new HashMap<String, List<String>>();
        }
        // 取出缓存数据
        Map<String, List<String>> redisList = ert.get(keyQuery, Map.class);
        return redisList;
    }

    /**
     * 存到缓存
     *
     * @author xm
     * @date 2016年11月24日 上午10:28:25 void
     */
    @Override
    public void saveClassifyDownSubSuiteResultToRedis(final Map<String, List<String>> map, String key) {
        if (!map.isEmpty()) {
            final String keyQuery = key;
            Thread saveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // 保存到redis
                    ert.set(keyQuery, map);
                    // 缓存数据保存20分钟
                    ert.expire(keyQuery, 20l, TimeUnit.MINUTES);
                }
            });
            executorService.execute(saveThread);

        }

    }


    /**
     * 查询根套件，并用status过滤部分根套件
     *
     * @author xm
     * @date 2016年12月1日 下午5:00:00
     */
    public List<Map<String, String>> queryRootParentByStatus(String status) {

        String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id "
                + "from ndresource nd,resource_categories rc "
                + "where nd.primary_category = 'assets' "
                + "and nd.estatus=:status "
                + "and rc.primary_category='assets' "
                + "and rc.taxOnCode = '$RA0502' "
                + "and rc.resource=nd.identifier "
                + "and nd.enable = 1 "
                + "and not exists (SELECT identifier from resource_relations "
                + "where res_type='assets' "
                + "and resource_target_type = 'assets' "
                + "and target = nd.identifier) order by nd.create_time";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("status", status);

        List<Map<String, String>> ParentSuiteByStatus = npjt.query(sql, params,
                new RowMapper<Map<String, String>>() {
                    @Override
                    public Map<String, String> mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        Map<String, String> m = new HashMap<String, String>();
                        String identifier = rs.getString("identifier");
                        String title = rs.getString("title");
                        String description = rs.getString("description");
                        String status = rs.getString("status");
                        m.put("identifier", identifier);
                        m.put("title", title);
                        m.put("description", description);
                        m.put("status", status);
                        return m;
                    }
                });
        return ParentSuiteByStatus;
    }

    /**
     * 查询根套件，并用status过滤部分根套件
     *
     * @author xm
     * @date 2016年12月1日 下午5:00:00
     */
    public List<Map<String, String>> queryRootParentByStatus(String status, String category) {

        String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id "
                + "from ndresource nd,resource_categories rc "
                + "where nd.primary_category = 'assets' ";
        if (StringUtils.isNotEmpty(status)) {
            sql += "and nd.estatus=:status ";
        }

        sql += "and rc.primary_category='assets' ";
        if (StringUtils.isNotEmpty(category)) {
            sql += "and rc.taxOnCode = :category ";
        } else {
            sql += "and rc.taxOnCode = '$RA0502' ";
        }
        sql += "and rc.resource=nd.identifier and nd.enable = 1 and not exists (SELECT identifier from resource_relations where res_type='assets' and resource_target_type = 'assets' and target = nd.identifier) order by nd.create_time";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("status", status);
        params.put("category", category);

        List<Map<String, String>> ParentSuiteByStatus = npjt.query(sql, params,
                new RowMapper<Map<String, String>>() {
                    @Override
                    public Map<String, String> mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        Map<String, String> m = new HashMap<String, String>();
                        String identifier = rs.getString("identifier");
                        String title = rs.getString("title");
                        String description = rs.getString("description");
                        String status = rs.getString("status");
                        m.put("identifier", identifier);
                        m.put("title", title);
                        m.put("description", description);
                        m.put("status", status);
                        return m;
                    }
                });
        return ParentSuiteByStatus;
    }

    /**
     * 查询根套件
     *
     * @author xm
     * @date 2016年11月29日 下午8:50:14
     */
    @Override
    public List<Map<String, String>> queryRootParent() {
        String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id,nd.create_time as createTime,nd.creator as authorName  "
                + "from ndresource nd,resource_categories rc "
                + "where nd.primary_category = 'assets' "
                + "and rc.primary_category='assets' "
                + "and rc.taxOnCode = '$RA0502' "
                + "and rc.resource=nd.identifier "
                + "and nd.enable = 1 "
                + "and not exists (SELECT identifier from resource_relations "
                + "where res_type='assets' "
                + "and resource_target_type = 'assets' "
                + "and target = nd.identifier) order by nd.create_time";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<String, Object>();

        List<Map<String, String>> rootParentSuite = npjt.query(sql, params,
                new RowMapper<Map<String, String>>() {
                    @Override
                    public Map<String, String> mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        Map<String, String> m = new HashMap<String, String>();
                        String identifier = rs.getString("identifier");
                        String title = rs.getString("title");
                        String description = rs.getString("description");
                        String status = rs.getString("status");
                        String createTime = rs.getString("createTime");
                        String authorName = rs.getString("authorName");
                        m.put("identifier", identifier);
                        m.put("title", title);
                        m.put("description", description);
                        m.put("status", status);
                        m.put("createTime", createTime);
                        m.put("authorName", authorName);
                        return m;
                    }
                });
        return rootParentSuite;
    }


    /**
     * 通过套件列表查询教学目标类型
     *
     * @author xm
     * @date 2016年11月29日 下午8:50:34
     */
    public List<Map<String, String>> queryObjectTypeBySuiteId(List<String> sourceId) {

        final List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        String querySql = " SELECT nd1.identifier as identifier FROM resource_relations as rr1 ,ndresource as nd1 ,resource_categories as rc1 "
                + "where rr1.source_uuid in (:sourceId)  "
                + "and rr1.target=nd1.identifier  "
                + "and rr1.enable=1 "
                + "and rr1.res_type='assets' "
                + "and rr1.resource_target_type='assets' "
                + "and rr1.relation_type='ASSOCIATE'"
                + "and nd1.primary_category='assets' "
                + "and nd1.enable=1 "
                + "and nd1.identifier=rc1.resource "
                + "and rc1.taxOnCode='$RA0503'";

        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
                defaultJdbcTemplate);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("sourceId", sourceId);

        namedJdbcTemplate.query(querySql, params, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, String> m = new HashMap<String, String>();
                String identifier = rs.getString("identifier");

                m.put("identifier", identifier);

                resultList.add(m);
                return null;
            }
        });
        return resultList;

    }


    /**
     * 通过教学目标类型列表查询教学目标
     *
     * @author xm
     * @date 2016年11月29日 下午8:51:46
     */
    public List<Map<String, String>> queryInstructionObjectByObjectTypeId(List<String> sourceId) {
        final List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        String querySql = "SELECT nd.identifier as identifier,count(*) as totalInstructionObject FROM ndresource as nd,resource_relations as rr "
                + "WHERE rr.source_uuid in (:sourceId) "
                + "and rr.target=nd.identifier "
                + "and rr.res_type='assets' "
                + "and rr.resource_target_type='instructionalobjectives' "
                + "and rr.enable=1 "
                + "and nd.enable=1 "
                + "and nd.primary_category='instructionalobjectives' "
                + "and rr.relation_type='ASSOCIATE'";

        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
                defaultJdbcTemplate);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("sourceId", sourceId);

        namedJdbcTemplate.query(querySql, params, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, String> m = new HashMap<String, String>();
                String identifier = rs.getString("identifier");
                String totalInstructionObject = rs.getString("totalInstructionObject");
                m.put("identifier", identifier);
                m.put("totalInstructionObject", totalInstructionObject);
                resultList.add(m);
                return null;
            }
        });
        return resultList;

    }

    /**
     * sample中的知识点关联的教学目标必须在指定的教学目标类型关联的教学目标范围内
     *
     * @author xm
     * @date 2016年12月1日 下午5:01:56
     */
    @Override
    public List<Map<String, String>> queryInstructionObjectByKnIdListAndInsObjType(List<String> knowledgeId, List<String> insobjType) {
        final List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();

        String sqlTemp = "SELECT nd.identifier FROM ndresource as nd,resource_relations as rr";
        StringBuffer tempSqlBuffer = new StringBuffer(sqlTemp);
        Map<String, Object> params = new HashMap<String, Object>();
        //sql的前面一部分
        for (int i = 0; i < knowledgeId.size(); i++) {
            String tempRelationTble = ",resource_relations as rr" + i;
            tempSqlBuffer.append(tempRelationTble);
        }
        //加where那段代码
        tempSqlBuffer.append(" WHERE rr.source_uuid in (:insobjTypeList) "
                + "and rr.res_type='assets' "
                + "and rr.target=nd.identifier "
                + "and rr.resource_target_type='instructionalobjectives' "
                + "and rr.enable=1 "
                + "and nd.enable=1 "
                + "and nd.primary_category='instructionalobjectives'");
        params.put("insobjTypeList", insobjType);
        for (int i = 0; i < knowledgeId.size(); i++) {
            String relationName = "rr" + i;
            tempSqlBuffer.append(" and " + relationName + ".source_uuid=:knid" + relationName + " "
                    + "and " + relationName + ".res_type='knowledges' "
                    + "and " + relationName + ".target=nd.identifier "
                    + "and " + relationName + ".resource_target_type='instructionalobjectives' "
                    + "and " + relationName + ".enable=1");
            params.put("knid" + relationName, knowledgeId.get(i));
        }

        String querySql = tempSqlBuffer.toString();
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
                defaultJdbcTemplate);

        namedJdbcTemplate.query(querySql, params, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, String> m = new HashMap<String, String>();
                String identifier = rs.getString("identifier");

                m.put("identifier", identifier);

                resultList.add(m);
                return null;
            }
        });
        return resultList;

    }


    /**
     * 查询通过教学目标列表查子教学目标
     *
     * @author xm
     * @date 2016年12月1日 下午9:26:05
     */
    @Override
    public List<Map<String, String>> querySubInstructionObjectByInstructionObjectId(List<String> insObjId) {


        final List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        if (CollectionUtils.isEmpty(insObjId)) {
            return resultList;
        }
        String querySql = "SELECT ndresource.identifier  FROM ndresource,resource_relations "
                + "where resource_relations.source_uuid in (:insObjId) "
                + "and resource_relations.target=ndresource.identifier "
                + "and resource_relations.res_type='instructionalobjectives' "
                + "and resource_relations.resource_target_type='subInstruction' "
                + "and resource_relations.enable=1 "
                + "and ndresource.enable=1 "
                + "and ndresource.primary_category='subInstruction'";

        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
                defaultJdbcTemplate);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("insObjId", insObjId);

        namedJdbcTemplate.query(querySql, params, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, String> m = new HashMap<String, String>();
                String identifier = rs.getString("identifier");


                m.put("identifier", identifier);
                resultList.add(m);
                return null;
            }
        });
        return resultList;
    }

    /**
     * 通过知识点id查询知识点的enable
     *
     * @author xm
     * @date 2016年12月1日 下午9:27:20
     */
    @Override
    public List<Map<String, String>> queryResourceByknowledgeId(String knId) {

        final List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        String querySql = "SELECT ndresource.enable FROM ndresource WHERE identifier=:knId ";
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
                defaultJdbcTemplate);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("knId", knId);

        namedJdbcTemplate.query(querySql, params, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, String> m = new HashMap<String, String>();
                String enable = rs.getString("enable");

                m.put("enable", enable);
                resultList.add(m);
                return null;
            }
        });
        return resultList;

    }

    /**
     * 统计根套件下的相关信息
     *
     * @author xm
     * @date 2016年12月1日 下午4:52:25
     */
    @Override
    public List<SuiteStatisticsModel> suiteStatistics() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<SuiteStatisticsModel> resultSuiteStatisticsModelsList = new ArrayList<SuiteStatisticsModel>();
        //查出所有的rootParent
        List<Map<String, String>> rootSuiteList = queryRootParent();

        int num = 1;
        if (CollectionUtils.isNotEmpty(rootSuiteList)) {
            for (Map<String, String> map : rootSuiteList) {
                List<Map<String, Object>> suiteIdList = new ArrayList<Map<String, Object>>();
                SuiteStatisticsModel suiteStatisticsModel = new SuiteStatisticsModel();
                
                Date d=null;
                if (StringUtils.isNotEmpty(map.get("createTime"))) {
                 d = new Date(Long.parseLong(map.get("createTime")));
                 suiteStatisticsModel.setRootSuiteCreatTime(sdf.format(d));
				}else {
					 suiteStatisticsModel.setRootSuiteCreatTime(null);
				}
                
                suiteStatisticsModel.setRootSuiteDescription(map.get("description"));
                suiteStatisticsModel.setRootSuiteAuthorName(map.get("authorName"));
                suiteStatisticsModel.setRootSuiteStatus(LifecycleStatus.getStatusCode(map.get("status")));
                if (StringUtils.isNotEmpty(map.get("title")) && map.get("title").length() >= 3) {
                    suiteStatisticsModel.setRootSuiteName(map.get("title").substring(2));
                } else {
                    suiteStatisticsModel.setRootSuiteName(map.get("title"));
                }

                //获取某个套件下的所有的套件包括它自己
                Map<String, Object> rootSuiteIdentify = new HashMap<String, Object>();
                String identifier = map.get("identifier");
                rootSuiteIdentify.put("identifier", identifier);
                suiteIdList.add(rootSuiteIdentify);
                educationRelationServer.recursiveSuiteDirectory(suiteIdList, identifier, num);
                List<String> objectTypeBySuiteIdList = new ArrayList<String>();
                for (Map<String, Object> suiteIdMap : suiteIdList) {
                    objectTypeBySuiteIdList.add((String) suiteIdMap.get("identifier"));
                }
                List<Map<String, String>> objectTypetotal = new ArrayList<Map<String, String>>();
                //查询出所有的套件对应的教学目标类型数量
                if (CollectionUtils.isNotEmpty(objectTypeBySuiteIdList)) {
                    objectTypetotal = queryObjectTypeBySuiteId(objectTypeBySuiteIdList);
                }
                int totalType = objectTypetotal.size();
                //把学习目标类型数量统计拿到放入suiteStatisticsModel中去
                suiteStatisticsModel.setRootSuiteInsObjTypeTotal(totalType);//大套件+大套件下的小套件所对应的所有的教学目标类型
                //查询这个rootIdentify所对应sample
                List<Sample> samplelistTemp = sampleRepository.querySampleBySuiteId(identifier);
                List<Sample> samplelist = new ArrayList<Sample>();
                //查sampList中对应的knowlege，若有一个是enable！=1的，则这个sample不使用
                for (Sample sample : samplelistTemp) {
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId1())) {
                        String knId = sample.getKnowledgeId1();
                        // 查ndresource表中的这个enable若不为1则，continue，跳出循环
                        List<Map<String, String>> list = queryResourceByknowledgeId(knId);
                        if (CollectionUtils.isNotEmpty(list)) {
                           if (!list.get(0).get("enable").toString().equals("1")) {
                                continue;
                            }	
						}
                       
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId2())) {
                        String knId = sample.getKnowledgeId2();
                        // 查ndresource表中的这个enable若不为1则，continue，跳出循环
                        List<Map<String, String>> list = queryResourceByknowledgeId(knId);
                        if (CollectionUtils.isNotEmpty(list)) {
                           if (!list.get(0).get("enable").toString().equals("1")) {
                                continue;
                            }	
						}
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId3())) {
                        String knId = sample.getKnowledgeId3();
                        // 查ndresource表中的这个enable若不为1则，continue，跳出循环
                        List<Map<String, String>> list = queryResourceByknowledgeId(knId);
                        if (CollectionUtils.isNotEmpty(list)) {
                           if (!list.get(0).get("enable").toString().equals("1")) {
                                continue;
                            }	
						}
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId4())) {
                        String knId = sample.getKnowledgeId4();
                        // 查ndresource表中的这个enable若不为1则，continue，跳出循环
                        List<Map<String, String>> list = queryResourceByknowledgeId(knId);
                        if (CollectionUtils.isNotEmpty(list)) {
                           if (!list.get(0).get("enable").toString().equals("1")) {
                                continue;
                            }	
						}
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId5())) {
                        String knId = sample.getKnowledgeId5();
                        // 查ndresource表中的这个enable若不为1则，continue，跳出循环
                        List<Map<String, String>> list = queryResourceByknowledgeId(knId);
                        if (CollectionUtils.isNotEmpty(list)) {
                           if (!list.get(0).get("enable").toString().equals("1")) {
                                continue;
                            }	
						}
                    }

                    samplelist.add(sample);
                }

                List<SampleInfoModel> tempSampleInfoModellist = new ArrayList<SampleInfoModel>();
                //遍历sampleList，从中去出所有的有关sample的值，并放入到model中去
                for (Sample sample : samplelist) {

                    SampleInfoModel sampleInfoModel = new SampleInfoModel();
                    sampleInfoModel.setSampleCreateTime(sdf.format(new java.sql.Date(sample.getCreateTime().getTime())));
                    sampleInfoModel.setSampleAuthorName(sample.getCreator());
                    sampleInfoModel.setSampleName(sample.getTitle());
                    sampleInfoModel.setSampleStatu(LifecycleStatus.getStatusCode(sample.getStatus()));
                    // 列出这个sample对应的所有的知识点的identify
                    List<String> sampleKnIdList = new ArrayList<String>();
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId1())) {
                        sampleKnIdList.add(sample.getKnowledgeId1());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId2())) {
                        sampleKnIdList.add(sample.getKnowledgeId1());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId3())) {
                        sampleKnIdList.add(sample.getKnowledgeId1());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId4())) {
                        sampleKnIdList.add(sample.getKnowledgeId1());
                    }
                    if (StringUtils.isNotEmpty(sample.getKnowledgeId5())) {
                        sampleKnIdList.add(sample.getKnowledgeId1());
                    }

                    List<String> insObjTypeIdList = new ArrayList<String>();
                    for (Map<String, String> objectTypeInfo : objectTypetotal) {
                        insObjTypeIdList.add(objectTypeInfo.get("identifier"));
                    }
                    List<Map<String, String>> sampleRelationInsObjTypeList = new ArrayList<Map<String, String>>();
                    //用知识点查询教学目标类型的数量，和记录所对应的教学目标类型的identify
                    if (CollectionUtils.isNotEmpty(sampleKnIdList) && CollectionUtils.isNotEmpty(insObjTypeIdList)) {
                        sampleRelationInsObjTypeList = queryInstructionObjectByKnIdListAndInsObjType(sampleKnIdList, insObjTypeIdList);
                    }

                    int insObjSize = sampleRelationInsObjTypeList.size();
                    //从中取这个sample对应的目标类型
                    List<String> validInstructionObjectIdList = new ArrayList<String>();
                    for (Map<String, String> instructionObjectId : sampleRelationInsObjTypeList) {
                        validInstructionObjectIdList.add(instructionObjectId.get("identifier"));
                    }

                    List<Map<String, String>> subInsObjList = new ArrayList<Map<String, String>>();
                    //根据subinstructionobject的列表来查询总数来取值
                    if (CollectionUtils.isNotEmpty(validInstructionObjectIdList)) {
                        subInsObjList = querySubInstructionObjectByInstructionObjectId(validInstructionObjectIdList);
                    }

                    int subInsObjSize = subInsObjList.size();
                    //目标描述的数量 subinsobj+insobj
                    int sampleInsObjAndSubInsObjTotal = insObjSize + subInsObjSize;

                    sampleInfoModel.setSampleInsObjTotal(insObjSize);
                    sampleInfoModel.setSampleInsObjAndSubInsObjTotal(sampleInsObjAndSubInsObjTotal);

                    tempSampleInfoModellist.add(sampleInfoModel);
                }
                suiteStatisticsModel.setSampleInfo(tempSampleInfoModellist);
                resultSuiteStatisticsModelsList.add(suiteStatisticsModel);
            }
        }
        return resultSuiteStatisticsModelsList;
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    @Override
    public void changeSampleStatus() {

        // 1、查询一级的套件目录
        String sql = "SELECT nd.identifier from ndresource nd,resource_categories rc where nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' and rc.resource=nd.identifier and nd.enable = 1 and not exists (SELECT identifier from resource_relations where res_type='assets' and resource_target_type = 'assets' and target = nd.identifier) order by nd.create_time";
        List<Map<String, Object>> list = jt.queryForList(sql);
        List<String> tmpIds = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(list)) {
            for (Map<String, Object> map : list) {
                String identifier = (String) map.get("identifier");
                tmpIds.add(identifier);
            }
        }
        //2.调用接口queryList4KnowledgeBySuiteId
        List<String> sampleIdList = new ArrayList<String>();
        for (String suiteId : tmpIds) {
            WafSecurityHttpClient wafSecurityHttpClient = new WafSecurityHttpClient();
            Map<String, String> urlVariables = new HashMap<String, String>();
            urlVariables.put("suite_id", suiteId);
            urlVariables.put("limit", "(0,1000)");
            urlVariables.put("root_suite", suiteId);
            String url = "http://localhost:8080/esp-lifecycle/v0.6/instructionalobjectives/list/suite/{suite_id}?suite_id=" + suiteId + "&limit=(0,1000)&root_suite=" + suiteId;
            Map<String, Object> returnMap = new HashMap<String, Object>();
            try {
                returnMap = wafSecurityHttpClient.get(url, Map.class, urlVariables);
            } catch (Exception e) {
                LOG.error("调用接口queryList4KnowledgeBySuiteId出错");
                System.out.println("@@@@@@@@@@@@@@@@");
            }
//		   Map<String, Object> returnMap=queryList4SampleBySuiteId(suiteId,"(0,1000)", false);
            List<Map<String, Object>> itemsList = (ArrayList<Map<String, Object>>) returnMap.get("items");
            if (CollectionUtils.isNotEmpty(itemsList)) {
                for (Map<String, Object> itemsMap : itemsList) {
                    List<Map<String, Object>> datasList = (ArrayList<Map<String, Object>>) itemsMap.get("datas");
                    if (CollectionUtils.isEmpty(datasList)) {
                        Sample sample = (Sample) itemsMap.get("sample");
                        if (sample != null) {
                            sampleIdList.add(sample.getIdentifier());
                        }
                    }
                }
            }
        }

        //设置sample的状态
        if (CollectionUtils.isNotEmpty(sampleIdList)) {
            System.out.println(sampleIdList);
            setSampleStatus(sampleIdList);
        }
    }

    private void setSampleStatus(List<String> sampleIdList) {

        String sql = "update samples set enable=0 where identifier in (:identifier)";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("identifier", sampleIdList);
        npjt.update(sql, params);
    }

    //找到给定套件id下面所有的子套件id
    private void getSubSuite(String suiteId, List<String> subList, Map<String, Object> sonMap) {
        String sql = "select nd.identifier,nd.description from ndresource nd ,resource_categories rc , resource_relations rr where rc.taxOnCode ='$RA0502' and nd.identifier = rc.resource and nd.enable = 1 and  rr.source_uuid= :suiteId and nd.identifier = rr.target and rr.enable = 1 and rr.relation_type != 'COPY'";
        NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
        Map<String, Object> params = new HashMap<>();
        params.put("suiteId", suiteId);
        List<Map<String, Object>> list = npjt.query(sql, params, new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Map<String, Object> map = new HashMap<>();
                map.put("identifier", rs.getString("identifier"));
                map.put("description", rs.getString("description"));
                return map;
            }
        });

        sonMap.put("identifier", suiteId);

        if (CollectionUtils.isNotEmpty(list)) {
            List<Map<String, Object>> son = new ArrayList<>();
            for (Map<String, Object> map : list) {
                subList.add((String) map.get("identifier"));

                Map<String, Object> temp = new HashMap<>();
                temp.put("description", (String) map.get("description"));
                son.add(temp);
                sonMap.put("children", son);
                getSubSuite((String) map.get("identifier"), subList, temp);

            }
        }
    }

    @Override
    public Map<String, List<MultiLevelSuiteViewModel>> dealReturnMap(Map<String, Map<String, List<MultilayerClassifySubSuite>>> multiLevelMap, String level) {

        //获取所有套件id
        Set<String> allSuiteSet = new HashSet<>();
        Set<Entry<String, Map<String, List<MultilayerClassifySubSuite>>>> multiLevelEntry = multiLevelMap.entrySet();
        for (Entry<String, Map<String, List<MultilayerClassifySubSuite>>> entry : multiLevelEntry) {
            Map<String, List<MultilayerClassifySubSuite>> entryMap = entry.getValue();
            Set<Entry<String, List<MultilayerClassifySubSuite>>> entrySet = entryMap.entrySet();
            for (Entry<String, List<MultilayerClassifySubSuite>> entry1 : entrySet) {
                for (MultilayerClassifySubSuite suite : entry1.getValue()) {
                    allSuiteSet.add(suite.getIdentify());
                }
            }
        }
        List<String> allSuiteList = new ArrayList<>();
        allSuiteList.addAll(allSuiteSet);

        //取出所有学习目标类型
        List<ResultsModel> allObjTypeList = new ArrayList<>();
        allObjTypeList = instructionalobjectiveDao.getInstructionalObjectiveById(allSuiteList);

        Map<String, List<MultiLevelSuiteViewModel>> resultMap = new LinkedHashMap<>();
        Map<String, List<MultilayerClassifySubSuite>> returnMap = multiLevelMap.get(level);
        Map<MultiLevelSuiteViewModel, Long> hashCodeMap = new HashMap<>();
        Set<String> keySet = returnMap.keySet();

        for (String key : keySet) {
            List<MultiLevelSuiteViewModel> returnList = new ArrayList<>();
            for (MultilayerClassifySubSuite model : returnMap.get(key)) {

                //去map里找相同hashcode，没有就新建一个model放到list里
                boolean needCreated = true;
                long hashCodeOfModel = dealHashCode(model, multiLevelMap, allObjTypeList);
                for (MultiLevelSuiteViewModel modelKey : hashCodeMap.keySet()) {
                    if (hashCodeOfModel == hashCodeMap.get(modelKey)) {
                        Map<String, String> sameModel = new HashMap<>();
                        sameModel.put("identifier", model.getIdentify());
                        sameModel.put("description", model.getRegexBeforeDescription());
                        modelKey.getSuiteList().add(sameModel);
                        needCreated = false;
                    }
                }
                if (needCreated) {

                    MultiLevelSuiteViewModel viewModel = new MultiLevelSuiteViewModel();
                    List<ResultsModel> suiteObjType = getObjTypeList(model.getIdentify(), allObjTypeList);
                    List<String> objTypeDescripton = new ArrayList<>();
                    for (ResultsModel result : suiteObjType) {
                        String description = result.getTitle();
                        description = dealObjectiveTypeDescription(description);
                        objTypeDescripton.add(description);
                    }
                    viewModel.setObjectiveTypeTitle(objTypeDescripton);

                    Map<String, String> firstModel = new HashMap<>();
                    firstModel.put("identifier", model.getIdentify());
                    firstModel.put("description", model.getRegexBeforeDescription());
                    List<Map<String, String>> suiteList = new ArrayList<>();
                    suiteList.add(firstModel);
                    viewModel.setSuiteList(suiteList);

                    List<MultiLevelChildViewModel> children = new ArrayList<>();
                    dealChildren(model.getBeforeIdentifySet(), multiLevelMap, children, level, allObjTypeList);
                    viewModel.setChildren(children);

                    long hashcode = dealHashCode(model, multiLevelMap, allObjTypeList);
                    hashCodeMap.put(viewModel, hashcode);

                    returnList.add(viewModel);
                }
            }

            //排序
            List<MultiLevelSuiteViewModel> returnList1 = sortReturnList(returnList);

            resultMap.put(key, returnList1);
        }
        return resultMap;
    }

    //根据hashcode求和判断是否为相同类型的学习目标类型和子套件
    private long dealHashCode(MultilayerClassifySubSuite model, Map<String, Map<String, List<MultilayerClassifySubSuite>>> multiLevelMap, List<ResultsModel> allObjTypeList) {
        String description = model.getRegexAfterDescrption();
        int levelNum = Integer.valueOf(model.getSuiteLevel());
        List<String> suiteIds = new ArrayList<>();
        long hashCodeSum = 0l;
        //自己的description的hashcode
        hashCodeSum += description.hashCode();

        //子套件的hashcode
        hashCodeSum += sumHashCode(multiLevelMap, model.getBeforeIdentifySet(), String.valueOf(levelNum - 1), suiteIds);

        //学习目标类型的hashcode
        List<ResultsModel> objTypeList = new ArrayList<>();
        for (String suiteId : suiteIds) {
            objTypeList.addAll(getObjTypeList(suiteId, allObjTypeList));
        }
        List<String> objTypeTitleList = new ArrayList<>();
        for (ResultsModel result : objTypeList) {
            String title = result.getTitle();
            String regexTitle = dealObjectiveTypeDescription(title);
            objTypeTitleList.add(dealObjectiveTypeDescription(regexTitle));
        }
        for (String title : objTypeTitleList) {
            hashCodeSum += title.hashCode();
        }

        return hashCodeSum;
    }

    //求所有子套件description的hashcode和
    private long sumHashCode(Map<String, Map<String, List<MultilayerClassifySubSuite>>> map, Set<String> childrenId, String level, List<String> suiteIds) {
        long hashCode = 0l;
        if ("0".equals(level)) {
            return 0;
        }
        Map<String, List<MultilayerClassifySubSuite>> dealMap = map.get(level);
        Set<Entry<String, List<MultilayerClassifySubSuite>>> entrySet = dealMap.entrySet();
        for (Entry<String, List<MultilayerClassifySubSuite>> entry : entrySet) {
            for (MultilayerClassifySubSuite model : entry.getValue()) {
                if (childrenId.contains(model.getIdentify())) {
                    hashCode += model.getRegexAfterDescrption().hashCode();
                    if (CollectionUtils.isNotEmpty(model.getBeforeIdentifySet())) {
                        suiteIds.addAll(model.getBeforeIdentifySet());
                        hashCode += sumHashCode(map, model.getBeforeIdentifySet(), String.valueOf(Integer.valueOf(level) - 1), suiteIds);
                    }
                }
            }
        }

        return hashCode;
    }

    private void dealChildren(Set<String> childrenId, Map<String, Map<String, List<MultilayerClassifySubSuite>>> multiLevelMap, List<MultiLevelChildViewModel> children, String level, List<ResultsModel> allObjTypeList) {
        String childLevel = String.valueOf(Integer.valueOf(level) - 1);
        if ("0".equals(childLevel)) {
            return;
        }
        Map<String, List<MultilayerClassifySubSuite>> dealMap = multiLevelMap.get(childLevel);
        for (String cId : childrenId) {
            MultiLevelChildViewModel childViewModel = new MultiLevelChildViewModel();

            //找到自己的description
            Set<Entry<String, List<MultilayerClassifySubSuite>>> entrySet = dealMap.entrySet();
            for (Entry<String, List<MultilayerClassifySubSuite>> entry : entrySet) {
                for (MultilayerClassifySubSuite model : entry.getValue()) {
                    if (model.getIdentify().equals(cId)) {
                        childViewModel.setSuiteTitle(model.getRegexAfterDescrption());
                        Set<String> childrenId1 = model.getBeforeIdentifySet();
                        if (CollectionUtils.isNotEmpty(childrenId1)) {
                            List<MultiLevelChildViewModel> children1 = new ArrayList<>();
                            dealChildren(childrenId1, multiLevelMap, children1, childLevel, allObjTypeList);
                            if (CollectionUtils.isNotEmpty(children1)) {
                                childViewModel.setChildren(children1);
                            }
                        }
                    }
                }
            }
            //找到学习目标类型
            if (StringUtils.isNotEmpty(childViewModel.getSuiteTitle())) {
                List<ResultsModel> objType = getObjTypeList(cId, allObjTypeList);
                if (CollectionUtils.isNotEmpty(objType)) {
                    List<String> objTypeTitle = new ArrayList<>();
                    for (ResultsModel result : objType) {
                        String description = result.getTitle();
                        description = dealObjectiveTypeDescription(description);
                        objTypeTitle.add(description);
                    }
                    childViewModel.setObjectiveTypeTitle(objTypeTitle);
                }


                //加入list中
                children.add(childViewModel);
            }
        }

    }

    private String dealObjectiveTypeDescription(String description) {
        Pattern pattern = Pattern.compile("<span.+?</span>");
        return pattern.matcher(description).replaceAll("【XX】");
    }

    private List<ResultsModel> getObjTypeList(String suiteId, List<ResultsModel> objTypeList) {
        List<ResultsModel> returnList = new ArrayList<>();
        for (ResultsModel model : objTypeList) {
            if (suiteId.equals(model.getSourceId())) {
                returnList.add(model);
            }
        }
        return returnList;
    }

    private void dealChildrenMap(Map<String, Object> map, int level) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) map.get("children");
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            if ((level == 1 && (children.get(i).get("children") != null && ((List<Map<String, Object>>) children.get(i).get("children")).size() > 0)) ||
                    (level > 1 && (children.get(i).get("children") == null || ((List<Map<String, Object>>) children.get(i).get("children")).size() == 0))) {
                children.remove(i);
            } else {
                dealChildrenMap(children.get(i), level - 1);
                if (StringUtils.isEmpty((String) children.get(i).get("identifier"))) {
                    children.remove(i);
                }
            }
        }
    }

    private List<MultiLevelSuiteViewModel> sortReturnList(List<MultiLevelSuiteViewModel> returnList) {
        List<MultiLevelSuiteViewModel> list = new ArrayList<>();
        while (returnList.size() > 0) {
            MultiLevelSuiteViewModel m = getMaxCountModel(returnList);
            list.add(m);
            returnList.remove(m);
        }
        return list;
    }

    private MultiLevelSuiteViewModel getMaxCountModel(List<MultiLevelSuiteViewModel> list) {
        // 记录下标对应的suite_list对应的个数
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0; i < list.size(); i++) {
            map.put(i, list.get(i).getSuiteList().size());
        }
        int max = map.get(0);
        int n = 0;
        for (int j = 0; j < list.size(); j++) {
            if (max < map.get(j)) {
                max = map.get(j);
                n = j;
            }
        }
        return list.get(n);
    }
}
