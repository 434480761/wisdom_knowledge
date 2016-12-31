/* =============================================================
 * Created: [2015年7月2日] by linsm
 * =============================================================
 *
 * Copyright 2014-2015 NetDragon Websoft Inc. All Rights Reserved
 *
 * =============================================================
 */

package nd.esp.service.lifecycle.educommon.services.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import nd.esp.service.lifecycle.educommon.dao.NDResourceDao;
import nd.esp.service.lifecycle.educommon.models.ResRelationModel;
import nd.esp.service.lifecycle.educommon.models.ResourceModel;
import nd.esp.service.lifecycle.educommon.vos.ResourceViewModel;
import nd.esp.service.lifecycle.entity.elasticsearch.Resource;
import nd.esp.service.lifecycle.models.courseware.v06.CoursewareModel;
import nd.esp.service.lifecycle.models.teachingmaterial.v06.TeachingMaterialModel;
import nd.esp.service.lifecycle.models.v06.AssetModel;
import nd.esp.service.lifecycle.models.v06.CourseWareObjectModel;
import nd.esp.service.lifecycle.models.v06.CourseWareObjectTemplateModel;
import nd.esp.service.lifecycle.models.v06.EbookModel;
import nd.esp.service.lifecycle.models.v06.ExaminationPaperModel;
import nd.esp.service.lifecycle.models.v06.HomeworkModel;
import nd.esp.service.lifecycle.models.v06.InstructionalObjectiveModel;
import nd.esp.service.lifecycle.models.v06.KnowledgeBaseModel;
import nd.esp.service.lifecycle.models.v06.KnowledgeModel;
import nd.esp.service.lifecycle.models.v06.LearningPlanModel;
import nd.esp.service.lifecycle.models.v06.LessonModel;
import nd.esp.service.lifecycle.models.v06.LessonPlanModel;
import nd.esp.service.lifecycle.models.v06.QuestionModel;
import nd.esp.service.lifecycle.models.v06.SubInstructionModel;
import nd.esp.service.lifecycle.repository.EspEntity;
import nd.esp.service.lifecycle.repository.ResourceRepository;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.Asset;
import nd.esp.service.lifecycle.repository.model.Category;
import nd.esp.service.lifecycle.repository.model.CategoryData;
import nd.esp.service.lifecycle.repository.model.Chapter;
import nd.esp.service.lifecycle.repository.model.Contribute;
import nd.esp.service.lifecycle.repository.model.Courseware;
import nd.esp.service.lifecycle.repository.model.CoursewareObject;
import nd.esp.service.lifecycle.repository.model.CoursewareObjectTemplate;
import nd.esp.service.lifecycle.repository.model.Ebook;
import nd.esp.service.lifecycle.repository.model.ExaminationPaper;
import nd.esp.service.lifecycle.repository.model.HomeWork;
import nd.esp.service.lifecycle.repository.model.InstructionalObjective;
import nd.esp.service.lifecycle.repository.model.KnowledgeBase;
import nd.esp.service.lifecycle.repository.model.LearningPlan;
import nd.esp.service.lifecycle.repository.model.Lesson;
import nd.esp.service.lifecycle.repository.model.LessonPlan;
import nd.esp.service.lifecycle.repository.model.Question;
import nd.esp.service.lifecycle.repository.model.ResCoverage;
import nd.esp.service.lifecycle.repository.model.ResourceCategory;
import nd.esp.service.lifecycle.repository.model.ResourceRelation;
import nd.esp.service.lifecycle.repository.model.Sample;
import nd.esp.service.lifecycle.repository.model.SubInstruction;
import nd.esp.service.lifecycle.repository.model.TeachingActivities;
import nd.esp.service.lifecycle.repository.model.TeachingMaterial;
import nd.esp.service.lifecycle.repository.sdk.AssetRepository;
import nd.esp.service.lifecycle.repository.sdk.CategoryRepository;
import nd.esp.service.lifecycle.repository.sdk.ChapterRepository;
import nd.esp.service.lifecycle.repository.sdk.Contribute4QuestionDBRepository;
import nd.esp.service.lifecycle.repository.sdk.CoursewareObjectRepository;
import nd.esp.service.lifecycle.repository.sdk.CoursewareObjectTemplateRepository;
import nd.esp.service.lifecycle.repository.sdk.CoursewareRepository;
import nd.esp.service.lifecycle.repository.sdk.EbookRepository;
import nd.esp.service.lifecycle.repository.sdk.ExaminationPaperRepository;
import nd.esp.service.lifecycle.repository.sdk.HomeWorkRepository;
import nd.esp.service.lifecycle.repository.sdk.InstructionalobjectiveRepository;
import nd.esp.service.lifecycle.repository.sdk.KnowledgeBaseRepository;
import nd.esp.service.lifecycle.repository.sdk.LearningPlansRepository;
import nd.esp.service.lifecycle.repository.sdk.LessonPlansRepository;
import nd.esp.service.lifecycle.repository.sdk.LessonRepository;
import nd.esp.service.lifecycle.repository.sdk.QuestionRepository;
import nd.esp.service.lifecycle.repository.sdk.ResCoverage4QuestionDBRepository;
import nd.esp.service.lifecycle.repository.sdk.ResCoverageRepository;
import nd.esp.service.lifecycle.repository.sdk.ResourceCategory4QuestionDBRepository;
import nd.esp.service.lifecycle.repository.sdk.ResourceCategoryRepository;
import nd.esp.service.lifecycle.repository.sdk.ResourceRelationRepository;
import nd.esp.service.lifecycle.repository.sdk.SampleRepository;
import nd.esp.service.lifecycle.repository.sdk.SubInstructionRepository;
import nd.esp.service.lifecycle.repository.sdk.TeachingActivitiesRepository;
import nd.esp.service.lifecycle.repository.sdk.TeachingMaterialRepository;
import nd.esp.service.lifecycle.repository.sdk.TechInfo4QuestionDBRepository;
import nd.esp.service.lifecycle.repository.sdk.TechInfoRepository;
import nd.esp.service.lifecycle.repository.sdk.ToolsRepository;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceV06;
import nd.esp.service.lifecycle.services.elasticsearch.AsynEsResourceService;
import nd.esp.service.lifecycle.support.Constant;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.busi.elasticsearch.ResourceTypeSupport;
import nd.esp.service.lifecycle.support.enums.ResourceNdCode;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.utils.gson.ObjectUtils;
import nd.esp.service.lifecycle.vos.assets.v06.AssetViewModel;
import nd.esp.service.lifecycle.vos.business.assets.ObjectiveTypeViewModel;
import nd.esp.service.lifecycle.vos.coursewareobjects.v06.CourseWareObjectViewModel;
import nd.esp.service.lifecycle.vos.coursewareobjecttemplate.v06.CoursewareObjectTemplateViewModel;
import nd.esp.service.lifecycle.vos.coursewares.v06.CoursewareViewModel;
import nd.esp.service.lifecycle.vos.ebooks.v06.EbookViewModel;
import nd.esp.service.lifecycle.vos.examinationpapers.v06.ExaminationPaperViewModel;
import nd.esp.service.lifecycle.vos.homeworks.v06.HomeworkViewModel;
import nd.esp.service.lifecycle.vos.instructionalobjectives.v06.InstructionalObjectiveViewModel;
import nd.esp.service.lifecycle.vos.knowledgebase.v06.KnowledgeBaseViewModel;
import nd.esp.service.lifecycle.vos.knowledges.v06.KnowledgeViewModel4Out;
import nd.esp.service.lifecycle.vos.learningplans.v06.LearningPlanViewModel;
import nd.esp.service.lifecycle.vos.lessonplans.v06.LessonPlanViewModel;
import nd.esp.service.lifecycle.vos.lessons.v06.LessonViewModel;
import nd.esp.service.lifecycle.vos.questions.v06.QuestionViewModel;
import nd.esp.service.lifecycle.vos.subinstruction.v06.SubInstructionViewModel;
import nd.esp.service.lifecycle.vos.teachingmaterial.v06.TeachingMaterialViewModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 辅助通用接口（各类资源）根据resourceType提供repository model,并且获得各个资源的各个模型类型（entity,model,viewModel),上传、下载
 *
 * @author linsm
 */

public class CommonServiceHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CommonServiceHelper.class);

    @Autowired
    Contribute4QuestionDBRepository contribute4QuestionRepository;
    //素材
    @Autowired
    AssetRepository assetRepository;

    //教案
    @Autowired
    LessonPlansRepository lessonPlansRepository;
    // 学案
    @Autowired
    LearningPlansRepository learningPlansRepository;

    //作业
    @Autowired
    HomeWorkRepository homeWorkRepository;

    //电子教材
    @Autowired
    EbookRepository ebookRepository;

    //习题
    @Autowired
    QuestionRepository questionRepository;

    //教材
    @Autowired
    TeachingMaterialRepository teachingMaterialRepository;

    // 课件颗粒
    @Autowired
    CoursewareObjectRepository coursewareObjectRepository;

    // 学科工具
    @Autowired
    ToolsRepository toolsRepository;

    // 课时
    @Autowired
    LessonRepository lessonRepository;

    // 知识点
    @Autowired
    ChapterRepository chapterRepository;

    //教学目标
    @Autowired
    InstructionalobjectiveRepository instructionalObjectiveRepository;
    //子教学目标
    @Autowired
    SubInstructionRepository subInstructionRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CoursewareRepository coursewareRepository;

    @Autowired
    TeachingActivitiesRepository teachingActivitiesRepository;

    //课件颗粒模板
    @Autowired
    CoursewareObjectTemplateRepository coursewareObjectTemplateRepository;

    @Autowired
    TechInfoRepository techInfoRepository;

    @Autowired
    TechInfo4QuestionDBRepository techInfo4QuestionDBRepository;

    @Autowired
    ResCoverageRepository resCoverageRepository;

    @Autowired
    ResCoverage4QuestionDBRepository resCoverage4QuestionDBRepository;

    @Autowired
    ResourceCategoryRepository resourceCategoryRepository;

    @Autowired
    ResourceCategory4QuestionDBRepository resourceCategory4QuestionDBRepository;

    @Autowired
    KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    ExaminationPaperRepository examinationPaperRepository;

    @Autowired
    private NDResourceDao ndResourceDao;

    @PersistenceContext(unitName = "entityManagerFactory")
    EntityManager em;

    @PersistenceContext(unitName = "questionEntityManagerFactory")
    EntityManager questionEm;

    @Qualifier(value = "defaultJdbcTemplate")
    @Autowired
    private JdbcTemplate defaultJdbcTemplate;

    @Autowired
    @Qualifier("educationRelationServiceV06")
    private EducationRelationServiceV06 educationRelationService;

    @Autowired
    private AsynEsResourceService esResourceOperation;

    @Autowired
    private ResourceRelationRepository resourceRelationRepository;

    Map<String, RepositoryAndModelAndView> repositoryAndModelMap;
    
    @Autowired
    private SampleRepository sampleReopsitory;

    @PostConstruct
    public void postConstruct() {
        repositoryAndModelMap = new HashMap<String, RepositoryAndModelAndView>();
        //素材
        repositoryAndModelMap.put("assets", new RepositoryAndModelAndView(assetRepository, AssetModel.class, AssetViewModel.class, Asset.class, true, true));
        //教案
        repositoryAndModelMap.put("lessonplans", new RepositoryAndModelAndView(lessonPlansRepository, LessonPlanModel.class, LessonPlanViewModel.class, LessonPlan.class, true, true));
        // 学案
        repositoryAndModelMap.put("learningplans", new RepositoryAndModelAndView(learningPlansRepository,
                LearningPlanModel.class,
                LearningPlanViewModel.class,
                LearningPlan.class, true, true));
        //作业
        repositoryAndModelMap.put("homeworks", new RepositoryAndModelAndView(homeWorkRepository,
                HomeworkModel.class,
                HomeworkViewModel.class,
                HomeWork.class, true, true));

        //教材
        repositoryAndModelMap.put("teachingmaterials", new RepositoryAndModelAndView(teachingMaterialRepository,
                TeachingMaterialModel.class,
                TeachingMaterialViewModel.class,
                TeachingMaterial.class, true, true));

        //习题
        repositoryAndModelMap.put("questions", new RepositoryAndModelAndView(questionRepository,
                QuestionModel.class,
                QuestionViewModel.class,
                Question.class, true, true));
        // 课件颗粒
        repositoryAndModelMap.put("coursewareobjects", new RepositoryAndModelAndView(coursewareObjectRepository,
                CourseWareObjectModel.class,
                CourseWareObjectViewModel.class,
                CoursewareObject.class,
                true,
                true));

        // 学科工具
        repositoryAndModelMap.put("tools", new RepositoryAndModelAndView(toolsRepository,
                CourseWareObjectModel.class,
                CourseWareObjectViewModel.class,
                CoursewareObject.class,
                true,
                true));

        // 试卷
        repositoryAndModelMap.put("examinationpapers", new RepositoryAndModelAndView(examinationPaperRepository,
                ExaminationPaperModel.class,
                ExaminationPaperViewModel.class,
                ExaminationPaper.class,
                true,
                true));

        // 习题集
        repositoryAndModelMap.put("exercisesset", new RepositoryAndModelAndView(examinationPaperRepository,
                ExaminationPaperModel.class,
                ExaminationPaperViewModel.class,
                ExaminationPaper.class,
                true,
                true));


        // 课时
        repositoryAndModelMap.put("lessons", new RepositoryAndModelAndView(lessonRepository,
                LessonModel.class,
                LessonViewModel.class,
                Lesson.class,
                false,
                false));

        // 知识点
        repositoryAndModelMap.put("knowledges", new RepositoryAndModelAndView(chapterRepository,
                KnowledgeModel.class,
                KnowledgeViewModel4Out.class,
                Chapter.class,
                false,
                false));

        // 教学目标
        repositoryAndModelMap.put("instructionalobjectives",
                new RepositoryAndModelAndView(instructionalObjectiveRepository,
                        InstructionalObjectiveModel.class,
                        InstructionalObjectiveViewModel.class,
                        InstructionalObjective.class,
                        false,
                        false));

        // 子教学目标
        repositoryAndModelMap.put("subInstruction",
                new RepositoryAndModelAndView(subInstructionRepository,
                        SubInstructionModel.class,
                        SubInstructionViewModel.class,
                        SubInstruction.class,
                        false,
                        false));

        //课件
        repositoryAndModelMap.put("coursewares", new RepositoryAndModelAndView(coursewareRepository,
                CoursewareModel.class,
                CoursewareViewModel.class,
                Courseware.class, true, true));
        // 电子教材
        repositoryAndModelMap.put("ebooks", new RepositoryAndModelAndView(ebookRepository,
                EbookModel.class,
                EbookViewModel.class,
                Ebook.class,
                true,
                true));

        //课件颗粒模板
        repositoryAndModelMap.put("coursewareobjecttemplates", new RepositoryAndModelAndView(coursewareObjectTemplateRepository,
                CourseWareObjectTemplateModel.class,
                CoursewareObjectTemplateViewModel.class,
                CoursewareObjectTemplate.class,
                true,
                true));
        //教辅
        repositoryAndModelMap.put("guidancebooks", new RepositoryAndModelAndView(teachingMaterialRepository,
                TeachingMaterialModel.class,
                TeachingMaterialViewModel.class,
                TeachingMaterial.class,
                false,
                false));

        //元课程
        repositoryAndModelMap.put("metacurriculums", new RepositoryAndModelAndView(teachingMaterialRepository,
                TeachingMaterialModel.class,
                TeachingMaterialViewModel.class,
                TeachingMaterial.class,
                true,
                true));

        //教学活动
        repositoryAndModelMap.put("teachingactivities", new RepositoryAndModelAndView(teachingActivitiesRepository,
                CoursewareModel.class,
                CoursewareViewModel.class,
                TeachingActivities.class, true, true));

        //eduresource
        repositoryAndModelMap.put(Constant.RESTYPE_EDURESOURCE, new RepositoryAndModelAndView(null,
                null,
                ResourceViewModel.class,
                null,
                false,
                false));

        //knowledgebase
        repositoryAndModelMap.put("knowledgebases", new RepositoryAndModelAndView(knowledgeBaseRepository,
                KnowledgeBaseModel.class,
                KnowledgeBaseViewModel.class,
                KnowledgeBase.class, false, false));
    }

    /**
     * 根据资源类型判断是否有上传接口
     *
     * @param resourceType
     * @return
     * @author:xuzy
     * @date:2016年1月26日
     */
    public boolean isUploadable(String resourceType) {
        RepositoryAndModelAndView repositoryAndModel = repositoryAndModelMap.get(resourceType);
        if (repositoryAndModel != null) {
            return repositoryAndModel.isUploadable();
        }
        return false;
    }

    public ResourceRepository<? extends EspEntity> getRepository(String resourceType) {
        RepositoryAndModelAndView repositoryAndModel = repositoryAndModelMap.get(resourceType);
        if (repositoryAndModel != null) {
            return repositoryAndModel.getRepository();
        } else {

            LOG.error(LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage() + resourceType);

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getCode(),
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage()
                            + resourceType);
        }
    }

    public Class<? extends ResourceModel> getModel(String resourceType) {
        RepositoryAndModelAndView repositoryAndModel = repositoryAndModelMap.get(resourceType);
        if (repositoryAndModel != null) {
            return repositoryAndModel.getModel();
        } else {

            LOG.error(LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage() + resourceType);

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getCode(),
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage()
                            + resourceType);
        }
    }

    public Class<? extends ResourceViewModel> getViewClass(String resourceType) {
        RepositoryAndModelAndView repositoryAndModel = repositoryAndModelMap.get(resourceType);
        if (repositoryAndModel != null) {
            return repositoryAndModel.getViewClass();
        } else {

            LOG.error(LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage() + resourceType);

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getCode(),
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage()
                            + resourceType);
        }
    }

    public Class<? extends EspEntity> getBeanClass(String resourceType) {
        RepositoryAndModelAndView repositoryAndModel = repositoryAndModelMap.get(resourceType);
        if (repositoryAndModel != null) {
            return repositoryAndModel.getBeanClass();
        } else {

            LOG.error(LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage() + resourceType);

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getCode(),
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage()
                            + resourceType);
        }
    }

    public void assertUploadable(String resourceType) {
        RepositoryAndModelAndView repositoryAndModel = repositoryAndModelMap.get(resourceType);
        if (repositoryAndModel != null) {
            if (!repositoryAndModel.isUploadable()) {

                LOG.error(LifeCircleErrorMessageMapper.CSResourceTypeNotSupport.getMessage()
                        + resourceType);

                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.CSResourceTypeNotSupport.getCode(),
                        LifeCircleErrorMessageMapper.CSResourceTypeNotSupport.getMessage()
                                + resourceType);
            }

        } else {

            LOG.error(LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage() + resourceType);

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getCode(),
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage()
                            + resourceType);
        }

    }

    public void assertDownloadable(String resourceType) {
        RepositoryAndModelAndView repositoryAndModel = repositoryAndModelMap.get(resourceType);
        if (repositoryAndModel != null) {
            if (!repositoryAndModel.isDownloadable()) {

                LOG.error(LifeCircleErrorMessageMapper.CSResourceTypeNotSupport.getMessage()
                        + resourceType);

                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.CSResourceTypeNotSupport.getCode(),
                        LifeCircleErrorMessageMapper.CSResourceTypeNotSupport.getMessage()
                                + resourceType);
            }

        } else {

            LOG.error(LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage() + resourceType);

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getCode(),
                    LifeCircleErrorMessageMapper.CommonAPINotSupportResourceType.getMessage()
                            + resourceType);
        }

    }

    /**
     * 辅助类，用于绑定LC model 与SDK repository View
     *
     * @author linsm
     */
    private static class RepositoryAndModelAndView {
        ResourceRepository<? extends EspEntity> repository;
        Class<? extends ResourceModel> modelClass;
        Class<? extends ResourceViewModel> viewClass;
        Class<? extends EspEntity> beanClass;
        boolean downloadable;
        boolean uploadable;

        public ResourceRepository<? extends EspEntity> getRepository() {
            return repository;
        }

        public Class<? extends ResourceModel> getModel() {
            return modelClass;
        }

        public boolean isDownloadable() {
            return downloadable;
        }

        public boolean isUploadable() {
            return uploadable;
        }

        public Class<? extends ResourceViewModel> getViewClass() {
            return viewClass;
        }

        public Class<? extends EspEntity> getBeanClass() {
            return beanClass;
        }

        /**
         *
         */
        public RepositoryAndModelAndView(ResourceRepository<? extends EspEntity> repository,
                                         Class<? extends ResourceModel> modelClass,
                                         Class<? extends ResourceViewModel> viewClass,
                                         Class<? extends EspEntity> beanClass,
                                         boolean downloadable,
                                         boolean uploadable) {
            this.repository = repository;
            this.modelClass = modelClass;
            this.viewClass = viewClass;
            this.beanClass = beanClass;
            this.downloadable = downloadable;
            this.uploadable = uploadable;
        }
    }


    /**
     * 获取维度的shortName
     *
     * @param beanListResult
     * @return
     * @author:xuzy
     * @date:2015年8月4日
     */
    public Map<String, String> getCategoryByData(List<CategoryData> beanListResult) {
        Map<String, String> returnMap = new HashMap<String, String>();
        List<String> cList = new ArrayList<String>();
        List<Category> categoryList = new ArrayList<Category>();
        if (CollectionUtils.isNotEmpty(beanListResult)) {
            for (CategoryData categoryData : beanListResult) {
                //维度
                String s = categoryData.getNdCode().substring(0, 2);
                if (!cList.contains(s)) {
                    cList.add(s);
                }
            }
            try {
                categoryList = categoryRepository.getListWhereInCondition("ndCode", cList);
                for (Category category : categoryList) {
                    String ndCode = category.getNdCode();
                    String shortName = category.getShortName();
                    if (StringUtils.isNotEmpty(ndCode)) {
                        returnMap.put(ndCode, shortName);
                    }
                }
            } catch (EspStoreException e) {

                LOG.error(LifeCircleErrorMessageMapper.StoreSdkFail.getMessage(), e);

                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(), e.getLocalizedMessage());
            }
        }
        return returnMap;
    }

    /**
     * 如果是教材或教辅,需要异步删除其相关章节和相关章节下的关系
     * ps:1.该方法使用在NDResourceServiceImpl类的delete方法中
     *    2.在新线程中使用
     *    3.执行的是update SQL语句
     *    *4.如果写在NDResourceServiceImpl中,会导致@Transactional事务不能生效,导致更新时没有事务的错误
     * <p>Create Time: 2016年1月28日   </p>
     * <p>Create author: xiezy   </p>
     * @param mid
     */
//    @Transactional
//    public void deleteChaptersAndRelations(String mid){
//        //删除mid相关章节下的关系
//        ndResourceDao.deleteRelationByChapters(mid);
//        //删除mid相关章节
//        ndResourceDao.deleteChapters(mid);
//    }

    /**
     * 根据资源类型获取techInfo的仓储
     *
     * @param resType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ResourceRepository getTechInfoRepositoryByResType(String resType) {
        if (!isQuestionDb(resType)) {
            return techInfoRepository;
        } else {
            return techInfo4QuestionDBRepository;
        }
    }

    /**
     * 根据资源类型获取resourceCategory的仓储
     *
     * @param resType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ResourceRepository getResourceCategoryRepositoryByResType(String resType) {
        if (!isQuestionDb(resType)) {
            return resourceCategoryRepository;
        } else {
            return resourceCategory4QuestionDBRepository;
        }
    }

    /**
     * 根据资源类型获取resourceCoverage的仓储
     *
     * @param resType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ResourceRepository getResCoverageRepositoryByResType(String resType) {
        if (!isQuestionDb(resType)) {
            return resCoverageRepository;
        } else {
            return resCoverage4QuestionDBRepository;
        }
    }

    public static boolean isQuestionDb(String resType) {
        if (!(resType.equals(IndexSourceType.QuestionType.getName()) || resType
                .equals(IndexSourceType.SourceCourseWareObjectType.getName()))) {
            return false;
        }
        return true;
    }

    /**
     * 查询并更新同步表变量
     * <p>
     * update by lsm (add the first query:querySql )
     *
     * @param var
     * @return
     */
    @Transactional
    public int queryAndUpdateSynVariable(int pid) {
        //pre check (add by lsm)
        String querySql = "select value from synchronized_table where pid = " + pid;
        Query query = em.createNativeQuery(querySql);
        Object queryResult = query.getSingleResult();
        if (queryResult != null) {
            int v = (Integer) queryResult;
            if (v == 1) {
                return 0;
            }
        }

        String queryForUpdateSql = "select value from synchronized_table where pid = " + pid + " for update";
        Query queryForUpdate = em.createNativeQuery(queryForUpdateSql);
        Object queryForUpdateResult = queryForUpdate.getSingleResult();
        if (queryForUpdateResult != null) {
            int v = (Integer) queryForUpdateResult;
            if (v == 0) {
                String updateSql = "update synchronized_table set value = 1 where pid = " + pid;
                Query query2 = em.createNativeQuery(updateSql);
                return query2.executeUpdate();
            } else {
                return 0;
            }
        }
        return 1;
    }

    /**
     * 删除关系（源与目标）（设置enable）
     *
     * @param resourceType
     * @param uuid
     */
    @Transactional(value = "transactionManager")
    public void deleteRelation(String resourceType, String uuid) {
        //关系
        String sql = "UPDATE resource_relations SET enable = '0' WHERE (resource_target_type = '" + resourceType
                + "' AND target='" + uuid + "') OR (res_type = '" + resourceType + "' AND source_uuid='" + uuid + "')";
        Query query = em.createNativeQuery(sql);
        query.executeUpdate();
    }

    @Transactional(value = "transactionManager")
    public void deleteRelationById(Collection<String> ids) {
        if (ids.size() > 0) {
            String idStr = "";
            for (String id : ids) {
                idStr += "'" + id + "',";
            }
            idStr = idStr.substring(0, idStr.lastIndexOf(","));
            String sql = "UPDATE resource_relations SET enable = 0 where identifier in(" + idStr + ")";
            Query query = em.createNativeQuery(sql);
            query.executeUpdate();
        } else {
            return;
        }
    }

    /**
     * 删除习题库中的资源关系（源与目标）（设置enable）
     *
     * @param resourceType
     * @param uuid
     */
    @Transactional(value = "questionTransactionManager")
    public void deleteRelation4QuestionDB(String resourceType, String uuid) {
        //关系
        String sql = "UPDATE resource_relations SET enable = '0' WHERE (resource_target_type = '" + resourceType
                + "' AND target='" + uuid + "') OR (res_type = '" + resourceType + "' AND source_uuid='" + uuid + "')";
        Query query = questionEm.createNativeQuery(sql);
        query.executeUpdate();
    }

    /**
     * 存放congtribute数据到question数据库中
     *
     * @param contribute void
     * @author xm
     * @date 2016年11月11日 下午5:50:47
     * @method saveContributeToQuesDB
     */
    @Transactional(value = "questionTransactionManager")
    public void saveContributeToQuesDB(Contribute contribute) {
        Contribute saveContribute = contribute;
        if (saveContribute != null) {
            try {
                contribute4QuestionRepository.add(contribute);
            } catch (EspStoreException e) {
                LOG.error("添加生命周期阶段失败", e);
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.CreateLifecycleFail);
            }
        }

    }

    /**
     * 初始化同步表变量
     *
     * @param pid
     * @return
     */
    @Transactional
    public int initSynVariable(int pid) {
        String sql = "update synchronized_table set value = 0 where pid = " + pid;
        Query query = em.createNativeQuery(sql);
        return query.executeUpdate();
    }

    @Transactional(value = "transactionManager")
    public void batchAddResourceCategory(List<ResourceCategory> rcList) {
        try {
            resourceCategoryRepository.batchAdd(rcList);
        } catch (EspStoreException e) {
            LOG.warn("工具API接口将教材的维度路径copy至目标资源出错", e);
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(), e.getLocalizedMessage());
        }
    }

    @Transactional(value = "questionTransactionManager")
    public void batchAddResourceCategory4Question(List<ResourceCategory> rcList) {
        try {
            resourceCategory4QuestionDBRepository.batchAdd(rcList);
        } catch (EspStoreException e) {
            LOG.warn("工具API接口将教材的维度路径copy至目标资源出错", e);
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(), e.getLocalizedMessage());
        }
    }

    /**
     * 判断维度数据是否是合法的PT维度
     *
     * @param code
     * @author xiezy
     * @date 2016年7月21日
     */
    public void isPublishType(String code) {
        final List<String> resultList = new ArrayList<String>();

        String sql = "SELECT nd_code as nc FROM category_datas WHERE nd_code LIKE 'PT%'";
        defaultJdbcTemplate.query(sql, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                resultList.add(rs.getString("nc"));
                return null;
            }
        });

        if (CollectionUtils.isNotEmpty(resultList)) {
            if (!resultList.contains(code)) {
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "LC/PT_CODE_IS_NOT_EXIST",
                        code + ":不是合法的PT维度");
            }
        } else {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "LC/PT_IS_NOT_EXIST",
                    "PT维度在该环境未录入");
        }
    }

    public Map<String, Object> getRepositoryAndModelMap() {
        return new HashMap<String, Object>(repositoryAndModelMap);
    }

    /**
     * 创建教学目标类型时，自动异步创建教学目标数据
     *
     * @param suiteId 套件id
     * @param otId    教学目标类型id
     */
    @Transactional
    public void autoCreateObjectives(ResourceModel resourceModel) {
        //新增教学目标的集合
        List<InstructionalObjective> ioList = new ArrayList<InstructionalObjective>();
        //新增资源分类维度的集合
        List<ResourceCategory> rcList = new ArrayList<ResourceCategory>();
        //新增资源覆盖范围的集合
        List<ResCoverage> covList = new ArrayList<ResCoverage>();
        //新增资源关系的集合
        List<ResourceRelation> rrList = new ArrayList<ResourceRelation>();
        //同步ES集合
        Set<Resource> resources = new HashSet<Resource>();

        //教学目标类型id
        String otId = resourceModel.getIdentifier();

        //创建者
        String creator = resourceModel.getLifeCycle().getCreator();

        //教学目标类型的自定义属性
        String customProperties = resourceModel.getCustomProperties();
        Map<String, Object> customMap = null;
        if (StringUtils.isEmpty(customProperties)) {
            return;
        } else {
            customMap = ObjectUtils.fromJson(customProperties, Map.class);
        }
        if (customMap == null || !customMap.containsKey("template")) {
            return;
        }
        String template = (String) customMap.get("template");
        if (StringUtils.isEmpty(template)) {
            return;
        }


        //套件id
        String suiteId = null;
        if (CollectionUtils.isNotEmpty(resourceModel.getRelations())) {
            List<ResRelationModel> rrmList = resourceModel.getRelations();
            for (ResRelationModel resRelationModel : rrmList) {
                if (resRelationModel.getSourceType().equals(IndexSourceType.AssetType.getName())) {
                    suiteId = resRelationModel.getSource();
                    break;
                }
            }
        }

        if (StringUtils.isEmpty(suiteId)) {
            return;
        }

        //1、根据套件id查找顶级套件
        String topSuiteId = suiteId;
        String sql = "SELECT nd.identifier FROM ndresource nd,resource_relations rr where nd.primary_category = 'assets' and rr.res_type = 'assets' and rr.resource_target_type = 'assets' and rr.relation_type = 'ASSOCIATE' and nd.enable = 1 and rr.enable = 1 and nd.identifier = rr.source_uuid and rr.target = ?";
        int num = 0;
        while (true) {
            num++;
            if (num > 10) {
                topSuiteId = null;
                break;
            }
            List<Map<String, Object>> topSuiteList = defaultJdbcTemplate.queryForList(sql, topSuiteId);
            if (CollectionUtils.isEmpty(topSuiteList)) {
                break;
            } else {
                Map<String, Object> map = topSuiteList.get(0);
                topSuiteId = (String) map.get("identifier");
            }
        }
        
        int offset = 0;
        int pageSize = 500;
        

        //2、根据顶级套件查找样例
        List<Sample> sampleList = sampleReopsitory.querySampleBySuiteId(topSuiteId);
        if(CollectionUtils.isEmpty(sampleList)){
        	return;
        }
        
        //3、根据样例获取知识点
        List<String> objectiveTypeTitles = new ArrayList<String>();
        List<Map<String,Object>> knowledgeList = new ArrayList<Map<String,Object>>();
        for (Sample sample : sampleList) {
        	List<String> knIdListOrder = new ArrayList<String>();
			String sampleTitle = sample.getTitle();
			String kn1 = sample.getKnowledgeId1();
			String kn2 = sample.getKnowledgeId2();
			String kn3 = sample.getKnowledgeId3();
			String kn4 = sample.getKnowledgeId4();
			String kn5 = sample.getKnowledgeId5();
			if(StringUtils.isNotEmpty(sampleTitle)){
				String[] st = sampleTitle.split("/");
				if(st.length > 1){
					List<String> knIds = new ArrayList<String>();
					if(StringUtils.isNotEmpty(kn1)){
						knIds.add(kn1);
					}
					if(StringUtils.isNotEmpty(kn2)){
						knIds.add(kn2);
					}
					if(StringUtils.isNotEmpty(kn3)){
						knIds.add(kn3);
					}
					if(StringUtils.isNotEmpty(kn4)){
						knIds.add(kn4);
					}
					if(StringUtils.isNotEmpty(kn5)){
						knIds.add(kn5);
					}
					List<Chapter> knList;
					try {
						knList = chapterRepository.getAll(knIds);
					} catch (EspStoreException e) {
						throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(), e.getLocalizedMessage());
					}
					for (String title : st) {
						for (Chapter chapter : knList) {
							if(title.equals(chapter.getTitle())){
								knIdListOrder.add(chapter.getIdentifier());
							}
						}
					}
				}else{
					knIdListOrder.add(kn1);
				}
				String otTitle = generateNewTitle(template, st);
				objectiveTypeTitles.add(otTitle);
				
				Map<String,Object> knowledgeMap = new HashMap<String,Object>();
				knowledgeMap.put("knIdListOrder", knIdListOrder);
				knowledgeMap.put("otTitle", otTitle);
				knowledgeList.add(knowledgeMap);
			}
		}
        

        //4、判断教学目标是否存在
        String sql2 = "SELECT title FROM ndresource where primary_category = 'instructionalobjectives' and enable = 1 and title in (:subList)";
        if (CollectionUtils.isEmpty(objectiveTypeTitles)) {
            return;
        } else {
            offset = 0;
            pageSize = 50;
            NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(defaultJdbcTemplate);
            while (true) {
                if (offset >= objectiveTypeTitles.size()) {
                    break;
                }
                List<String> subList;
                if (objectiveTypeTitles.size() > (offset + pageSize)) {
                    subList = objectiveTypeTitles.subList(offset, offset + pageSize);
                } else {
                    subList = objectiveTypeTitles.subList(offset, objectiveTypeTitles.size());
                }
                offset += pageSize;

                if (CollectionUtils.isEmpty(subList)) {
                    break;
                } else {
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("subList", subList);
                    List<String> iList = npjt.query(sql2, params, new RowMapper<String>() {
                        @Override
                        public String mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            return rs.getString("title");
                        }
                    });

                    if (CollectionUtils.isNotEmpty(iList)) {
                        for (String s : iList) {
                            Iterator<Map<String, Object>> it = knowledgeList.iterator();
                            while (it.hasNext()) {
                                Map<String, Object> m = it.next();
                                if (s.equals(m.get("otTitle"))) {
                                    it.remove();
                                }
                            }
                        }
                    }
                }
            }

        }

        //5、保存教学目标
        //6、保存教学目标与教学目标类型、知识点的关系
        float sortNum = 5000f;
        for (Map<String, Object> map3 : knowledgeList) {
            //知识点id
        	List<String> idList = (List)map3.get("knIdListOrder");
            //教学目标title
            String objTitle = (String)map3.get("otTitle");
            //创建教学目标
            StringBuffer tmpCust = new StringBuffer("{\"knowledges_order\":[");
            for (int i = 0; i < idList.size(); i++) {
            	String identifier = idList.get(i);
            	if(i == 0){
            		tmpCust.append("\"").append(identifier).append("\"");
            	}else{
            		tmpCust.append(",\"").append(identifier).append("\"");
            	}
			}
            tmpCust.append("]}");
            InstructionalObjective tmpIo = addInstructionalObjective(objTitle, creator, tmpCust.toString());
            ResourceCategory tmpRc = addResourceCategory(IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier());
            ResCoverage tmpCov = addResCoverage(IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier());
            for (String identifier : idList) {
            	ResourceRelation rr1 = addResourceRelation(IndexSourceType.KnowledgeType.getName(), identifier, IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier(), 5000f);
            	rrList.add(rr1);
			}
            ResourceRelation rr2 = addResourceRelation(IndexSourceType.AssetType.getName(), otId, IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier(), sortNum++);
            ioList.add(tmpIo);
            rcList.add(tmpRc);
            covList.add(tmpCov);
            rrList.add(rr2);
            Resource res = new Resource(ResourceNdCode.instructionalobjectives.toString(), tmpIo.getIdentifier());
            resources.add(res);
        }

        if (CollectionUtils.isNotEmpty(ioList)) {
            try {
                instructionalObjectiveRepository.batchAdd(ioList);
                resourceCategoryRepository.batchAdd(rcList);
                resCoverageRepository.batchAdd(covList);
                resourceRelationRepository.batchAdd(rrList);
            } catch (EspStoreException e) {
                LOG.error("异步生成教学目标出错！", e);
            }
        }


        //7、同步ES接口
        if (ResourceTypeSupport.isValidEsResourceType(ResourceNdCode.instructionalobjectives.toString())) {
            if (CollectionUtils.isNotEmpty(resources)) {
                esResourceOperation.asynBatchAdd(resources);
            }
        }
    }

    @Transactional
    public void autoCreateObjectives4Business(ObjectiveTypeViewModel objectiveTypeViewModel, String creatorId) {
        //新增教学目标的集合
        List<InstructionalObjective> ioList = new ArrayList<InstructionalObjective>();
        //新增资源分类维度的集合
        List<ResourceCategory> rcList = new ArrayList<ResourceCategory>();
        //新增资源覆盖范围的集合
        List<ResCoverage> covList = new ArrayList<ResCoverage>();
        //新增资源关系的集合
        List<ResourceRelation> rrList = new ArrayList<ResourceRelation>();
        //同步ES集合
        Set<Resource> resources = new HashSet<Resource>();

        //教学目标类型id
        String otId = objectiveTypeViewModel.getIdentifier();

        //创建者
        String creator = creatorId;
        //教学目标类型的自定义属性
        Map<String, Object> customMap = objectiveTypeViewModel.getCustomProperties();
        if (customMap == null || !customMap.containsKey("template")) {
            return;
        }
        String template = (String) customMap.get("template");
        if (StringUtils.isEmpty(template)) {
            return;
        }


        //套件id
        String suiteId = objectiveTypeViewModel.getSuiteId();
        if (StringUtils.isEmpty(suiteId)) {
            return;
        }

        //1、根据套件id查找顶级套件
        String topSuiteId = suiteId;
        String sql = "SELECT nd.identifier FROM ndresource nd,resource_relations rr where nd.primary_category = 'assets' and rr.relation_type = 'ASSOCIATE' and rr.res_type = 'assets' and rr.resource_target_type = 'assets' and nd.enable = 1 and rr.enable = 1 and nd.identifier = rr.source_uuid and rr.target = ?";
        int num = 0;
        while (true) {
            num++;
            if (num > 10) {
                topSuiteId = null;
                break;
            }
            List<Map<String, Object>> topSuiteList = defaultJdbcTemplate.queryForList(sql, topSuiteId);
            if (CollectionUtils.isEmpty(topSuiteList)) {
                break;
            } else {
                Map<String, Object> map = topSuiteList.get(0);
                topSuiteId = (String) map.get("identifier");
            }
        }

        int offset = 0;
        int pageSize = 500;
        
        //2、根据顶级套件查找样例
        List<Sample> sampleList = sampleReopsitory.querySampleBySuiteId(topSuiteId);
        if(CollectionUtils.isEmpty(sampleList)){
        	return;
        }
        
        //3、根据样例获取知识点
        List<String> objectiveTypeTitles = new ArrayList<String>();
        List<Map<String,Object>> knowledgeList = new ArrayList<Map<String,Object>>();
        for (Sample sample : sampleList) {
        	List<String> knIdListOrder = new ArrayList<String>();
			String sampleTitle = sample.getTitle();
			String kn1 = sample.getKnowledgeId1();
			String kn2 = sample.getKnowledgeId2();
			String kn3 = sample.getKnowledgeId3();
			String kn4 = sample.getKnowledgeId4();
			String kn5 = sample.getKnowledgeId5();
			if(StringUtils.isNotEmpty(sampleTitle)){
				String[] st = sampleTitle.split("/");
				if(st.length > 1){
					List<String> knIds = new ArrayList<String>();
					if(StringUtils.isNotEmpty(kn1)){
						knIds.add(kn1);
					}
					if(StringUtils.isNotEmpty(kn2)){
						knIds.add(kn2);
					}
					if(StringUtils.isNotEmpty(kn3)){
						knIds.add(kn3);
					}
					if(StringUtils.isNotEmpty(kn4)){
						knIds.add(kn4);
					}
					if(StringUtils.isNotEmpty(kn5)){
						knIds.add(kn5);
					}
					List<Chapter> knList;
					try {
						knList = chapterRepository.getAll(knIds);
					} catch (EspStoreException e) {
						throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR, LifeCircleErrorMessageMapper.StoreSdkFail.getCode(), e.getLocalizedMessage());
					}
					for (String title : st) {
						for (Chapter chapter : knList) {
							if(title.equals(chapter.getTitle())){
								knIdListOrder.add(chapter.getIdentifier());
							}
						}
					}
				}else{
					knIdListOrder.add(kn1);
				}
				String otTitle = generateNewTitle(template, st);
				objectiveTypeTitles.add(otTitle);
				
				Map<String,Object> knowledgeMap = new HashMap<String,Object>();
				knowledgeMap.put("knIdListOrder", knIdListOrder);
				knowledgeMap.put("otTitle", otTitle);
				knowledgeList.add(knowledgeMap);
			}
		}
        

        //4、判断教学目标是否存在
        String sql2 = "SELECT title FROM ndresource where primary_category = 'instructionalobjectives' and enable = 1 and title in (:subList)";
        if (CollectionUtils.isEmpty(objectiveTypeTitles)) {
            return;
        } else {
            offset = 0;
            pageSize = 50;
            NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(defaultJdbcTemplate);
            while (true) {
                if (offset >= objectiveTypeTitles.size()) {
                    break;
                }
                List<String> subList;
                if (objectiveTypeTitles.size() > (offset + pageSize)) {
                    subList = objectiveTypeTitles.subList(offset, offset + pageSize);
                } else {
                    subList = objectiveTypeTitles.subList(offset, objectiveTypeTitles.size());
                }
                offset += pageSize;

                if (CollectionUtils.isEmpty(subList)) {
                    break;
                } else {
                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("subList", subList);
                    List<String> iList = npjt.query(sql2, params, new RowMapper<String>() {
                        @Override
                        public String mapRow(ResultSet rs, int rowNum)
                                throws SQLException {
                            return rs.getString("title");
                        }
                    });

                    if (CollectionUtils.isNotEmpty(iList)) {
                        for (String s : iList) {
                            Iterator<Map<String, Object>> it = knowledgeList.iterator();
                            while (it.hasNext()) {
                                Map<String, Object> m = it.next();
                                if (s.equals(m.get("otTitle"))) {
                                    it.remove();
                                }
                            }
                        }
                    }
                }
            }

        }

        //5、保存教学目标
        //6、保存教学目标与教学目标类型、知识点的关系
        float sortNum = 5000f;
        for (Map<String, Object> map3 : knowledgeList) {
            //知识点id
        	List<String> idList = (List)map3.get("knIdListOrder");
            //教学目标title
            String objTitle = (String)map3.get("otTitle");
            //创建教学目标
            StringBuffer tmpCust = new StringBuffer("{\"knowledges_order\":[");
            for (int i = 0; i < idList.size(); i++) {
            	String identifier = idList.get(i);
            	if(i == 0){
            		tmpCust.append("\"").append(identifier).append("\"");
            	}else{
            		tmpCust.append(",\"").append(identifier).append("\"");
            	}
			}
            tmpCust.append("]}");
            InstructionalObjective tmpIo = addInstructionalObjective(objTitle, creator, tmpCust.toString());
            ResourceCategory tmpRc = addResourceCategory(IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier());
            ResCoverage tmpCov = addResCoverage(IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier());
            for (String identifier : idList) {
            	ResourceRelation rr1 = addResourceRelation(IndexSourceType.KnowledgeType.getName(), identifier, IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier(), 5000f);
            	rrList.add(rr1);
			}
            ResourceRelation rr2 = addResourceRelation(IndexSourceType.AssetType.getName(), otId, IndexSourceType.InstructionalObjectiveType.getName(), tmpIo.getIdentifier(), sortNum++);
            ioList.add(tmpIo);
            rcList.add(tmpRc);
            covList.add(tmpCov);
            rrList.add(rr2);
            Resource res = new Resource(ResourceNdCode.instructionalobjectives.toString(), tmpIo.getIdentifier());
            resources.add(res);
        }

        if (CollectionUtils.isNotEmpty(ioList)) {
            try {
                instructionalObjectiveRepository.batchAdd(ioList);
                resourceCategoryRepository.batchAdd(rcList);
                resCoverageRepository.batchAdd(covList);
                resourceRelationRepository.batchAdd(rrList);
            } catch (EspStoreException e) {
                LOG.error("异步生成教学目标出错！", e);
            }
        }


        //7、同步ES接口
        if (ResourceTypeSupport.isValidEsResourceType(ResourceNdCode.instructionalobjectives.toString())) {
            if (CollectionUtils.isNotEmpty(resources)) {
                esResourceOperation.asynBatchAdd(resources);
            }
        }
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
        if (IndexSourceType.InstructionalObjectiveType.getName().equals(resType)) {
            //教学目标
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
    private InstructionalObjective addInstructionalObjective(String title, String creator, String customProperties) {
        InstructionalObjective io = new InstructionalObjective();
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        io.setIdentifier(UUID.randomUUID().toString());
        io.setmIdentifier(io.getIdentifier());
        io.setTitle(title);
        io.setCreator(creator);
        io.setLanguage("zh_cn");
        io.setVersion("v0.6");
        io.setCreateTime(ts);
        io.setLastUpdate(ts);
        io.setCustomProperties(customProperties);
        io.setStatus("ONLINE");
        io.setPrimaryCategory(IndexSourceType.InstructionalObjectiveType.getName());
        return io;
    }

    /**
     * 创建知识点与教学目标的关系模型
     *
     * @return
     */
    private ResourceRelation addResourceRelation(String resType, String sourceUuid, String targetType, String target, float sortNum) {
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
        rr.setRelationType("ASSOCIATE");
        return rr;
    }

    /**
     * 根据教学目标类型模板、知识点title生成教学目标title
     *
     * @param reg     教学目标类型模板
     * @param knTitle 知识点title
     * @return
     */
    private static String generateNewTitle(String reg, String[] knTitles) {
        //$underscore0$替换为""
        String reg1 = "\\$underscore[0-9]{1,}\\$";
        //替换  (?:第X段)?为X
        String reg2 = "\\(\\?:第X段\\)\\?";
        //替换$bracket0$为replace
        String reg3 = "\\$bracket.+?\\$";

        Pattern pattern = Pattern.compile(reg1);
        Matcher matcher = pattern.matcher(reg);
        String result = matcher.replaceAll("");

        pattern = Pattern.compile(reg2);
        matcher = pattern.matcher(result);
        result = matcher.replaceAll("(X)");

        pattern = Pattern.compile(reg3);
        for (String tmpTitle : knTitles) {
        	matcher = pattern.matcher(result);
        	result = matcher.replaceFirst(tmpTitle);
		}
        return result;
    }
    
    public static boolean listEqual(List<String> list1, List<String> list2){
        List<String> temp = new ArrayList<String>();
        for (String string : list2) {
            temp.add(string);
        }
        if (list1.size() != temp.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (temp.contains(list1.get(i))) {
                temp.remove(list1.get(i));
            }
        }
        if (temp.size() == 0) {
            return true;
        }
        return false;
    }
}
