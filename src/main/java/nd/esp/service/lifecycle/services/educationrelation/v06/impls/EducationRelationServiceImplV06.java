package nd.esp.service.lifecycle.services.educationrelation.v06.impls;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import nd.esp.service.lifecycle.daos.educationrelation.v06.EducationRelationDao;
import nd.esp.service.lifecycle.daos.teachingmaterial.v06.ChapterDao;
import nd.esp.service.lifecycle.educommon.support.RelationType;
import nd.esp.service.lifecycle.models.chapter.v06.ChapterModel;
import nd.esp.service.lifecycle.models.v06.BatchAdjustRelationOrderModel;
import nd.esp.service.lifecycle.models.v06.EducationRelationLifeCycleModel;
import nd.esp.service.lifecycle.models.v06.EducationRelationModel;
import nd.esp.service.lifecycle.models.v06.ResourceRelationResultModel;
import nd.esp.service.lifecycle.repository.Education;
import nd.esp.service.lifecycle.repository.EspEntity;
import nd.esp.service.lifecycle.repository.EspRepository;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.Asset;
import nd.esp.service.lifecycle.repository.model.CategoryData;
import nd.esp.service.lifecycle.repository.model.Chapter;
import nd.esp.service.lifecycle.repository.model.Lesson;
import nd.esp.service.lifecycle.repository.model.ResourceCategory;
import nd.esp.service.lifecycle.repository.model.ResourceRelation;
import nd.esp.service.lifecycle.repository.model.TeachingMaterial;
import nd.esp.service.lifecycle.repository.sdk.AssetRepository;
import nd.esp.service.lifecycle.repository.sdk.CategoryDataRepository;
import nd.esp.service.lifecycle.repository.sdk.ChapterRepository;
import nd.esp.service.lifecycle.repository.sdk.LessonRepository;
import nd.esp.service.lifecycle.repository.sdk.ResourceCategoryRepository;
import nd.esp.service.lifecycle.repository.sdk.ResourceRelationRepository;
import nd.esp.service.lifecycle.repository.sdk.TeachingMaterialRepository;
import nd.esp.service.lifecycle.repository.sdk.impl.ServicesManager;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceForQuestionV06;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceV06;
import nd.esp.service.lifecycle.services.notify.NotifyInstructionalobjectivesService;
import nd.esp.service.lifecycle.services.notify.NotifyReportService;
import nd.esp.service.lifecycle.services.teachingmaterial.v06.ChapterService;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.busi.CommonHelper;
import nd.esp.service.lifecycle.support.enums.ResourceNdCode;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.EduRedisTemplate;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.utils.gson.ObjectUtils;
import nd.esp.service.lifecycle.vos.ListViewModel;
import nd.esp.service.lifecycle.vos.educationrelation.v06.RelationForPathViewModel;
import nd.esp.service.lifecycle.vos.educationrelation.v06.RelationForQueryViewModel;
import nd.esp.service.lifecycle.vos.statics.ResourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("educationRelationServiceV06")
@Transactional
public class EducationRelationServiceImplV06 implements
		EducationRelationServiceV06 {
	private final static Logger LOG = LoggerFactory
			.getLogger(EducationRelationServiceImplV06.class);

	/**
	 * 初始的sortNum值
	 */
	private static final float SORT_NUM_INITIAL_VALUE = 5000f;

	/**
	 * SDK注入
	 */
	@Autowired
	private ResourceRelationRepository resourceRelationRepository;

	@Autowired
	private TeachingMaterialRepository teachingMaterialRepository;

	@Autowired
	private ChapterRepository chapterRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private ResourceCategoryRepository resourceCategoryRepository;

	@Autowired
	private CategoryDataRepository categoryDataRepository;

	@Autowired
	@Qualifier("chapterServiceV06")
	private ChapterService chapterServiceV06;

	@Autowired
	@Qualifier("educationRelationServiceForQuestionV06")
	private EducationRelationServiceForQuestionV06 educationRelationServiceForQuestion;

	@Autowired
	private EducationRelationDao educationRelationDao;

	@Autowired
	private ChapterDao chapterDao;

	@Autowired
	private NotifyInstructionalobjectivesService notifyService;

	@Autowired
	private NotifyReportService nrs;

	@Autowired
	private JdbcTemplate jt;

	@Autowired
	private AssetRepository assetRepository;

	// redis缓存
	@Autowired
	private EduRedisTemplate<List<Map<String, Object>>> ert;
	// 使用线程池
	private final static ExecutorService executorService = CommonHelper
			.getPrimaryExecutorService();

	@Override
	public List<EducationRelationModel> createRelation(
			List<EducationRelationModel> educationRelationModels,
			boolean isCreateWithResource) {
		boolean notifyFlag = true;
		// 待添加的资源关系集合
		List<ResourceRelation> relations4Create = new ArrayList<ResourceRelation>();
		EspEntity resourceEntity = null;
		EspEntity targetEntity = null;
		// 返回的结果集
		List<EducationRelationModel> resultList = new ArrayList<EducationRelationModel>();
		boolean haveExist = false;

		Map<String, List<ResourceRelationResultModel>> map4Total = new HashMap<String, List<ResourceRelationResultModel>>();
		Map<String, Integer> orderNumMap = new HashMap<String, Integer>();
		Map<String, Set<Integer>> isDuplications = new HashMap<String, Set<Integer>>();
		if (isCreateWithResource) {
			int notNullOrderNums = 0;
			for (EducationRelationModel erm : educationRelationModels) {
				if ((erm.getResType().equals(
						IndexSourceType.ChapterType.getName())
						|| erm.getResType().equals(
								IndexSourceType.LessonType.getName()) || erm
						.getResType().equals(
								IndexSourceType.InstructionalObjectiveType
										.getName()))
						&& (erm.getResourceTargetType().equals(
								IndexSourceType.LessonType.getName()) || erm
								.getResourceTargetType()
								.equals(IndexSourceType.InstructionalObjectiveType
										.getName()))) {
					if (erm.getOrderNum() != null) {
						notNullOrderNums++;
						Set<Integer> orderNums = null;
						if (isDuplications.get(erm.getSource()) == null) {
							orderNums = new HashSet<Integer>();
							orderNums.add(erm.getOrderNum());
							isDuplications.put(erm.getSource(), orderNums);
						} else {
							isDuplications.get(erm.getSource()).add(
									erm.getOrderNum());
						}
						if (orderNumMap.get(erm.getSource()) == null
								|| (orderNumMap.get(erm.getSource()) != null && erm
										.getOrderNum() > orderNumMap.get(erm
										.getSource()))) {
							orderNumMap.put(erm.getSource(), erm.getOrderNum());
						}
					}
				}
			}
			if (CollectionUtils.isNotEmpty(isDuplications)) {
				int sum = 0;
				for (Map.Entry<String, Set<Integer>> entry : isDuplications
						.entrySet()) {
					sum += entry.getValue().size();
				}
				if (sum != notNullOrderNums) {

					LOG.error("orderNum不允许重复");

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.OrderNumError
									.getCode(), "orderNum不允许重复");
				}
			}
		}
		float newSortNum = SORT_NUM_INITIAL_VALUE;
		for (EducationRelationModel erm : educationRelationModels) {
			ResourceRelation relation = new ResourceRelation();

			if (StringUtils.isEmpty(erm.getLabel())) {
				erm.setLabel(null);
			}
			if ("knowledgebases".equals(erm.getResourceTargetType())) {
				notifyFlag = false;
			}

			// 判断源资源是否存在，不存在抛出not found异常
			resourceEntity = resourceExist(erm.getResType(), erm.getSource(),
					ResourceType.RESOURCE_SOURCE);
			if (!isCreateWithResource) {// 与资源同时创建时不需要一下操作
				targetEntity = resourceExist(erm.getResourceTargetType(),
						erm.getTarget(), ResourceType.RESOURCE_TARGET);
				EducationRelationModel model4Detail = relationExist(
						erm.getSource(), erm.getTarget(),
						erm.getRelationType(), erm.getLabel());
				if (null != model4Detail) {
					// 获取源资源和目标资源的title值
					model4Detail.setSourceTitle(resourceEntity.getTitle());
					model4Detail.setTargetTitle(targetEntity.getTitle());

					resultList.add(model4Detail);
					haveExist = true;
					break;
				}
			}

			// 生成SDK的入参对象
			relation.setTarget(erm.getTarget());
			relation.setLabel(erm.getLabel());
			relation.setTags(erm.getTags());
			relation.setResType(erm.getResType());
			relation.setResourceTargetType(erm.getResourceTargetType());
			// orderNum仅用于检索显示,默认为0
			relation.setOrderNum(0);
			// 关系类型，默认值是ASSOCIATE
			if (StringUtils.isEmpty(erm.getRelationType())) {
				relation.setRelationType("ASSOCIATE");
			} else {
				relation.setRelationType(erm.getRelationType());
			}
			relation.setSourceUuid(erm.getSource());

			if (erm.getIdentifier() != null) {
				relation.setIdentifier(erm.getIdentifier());
			} else {
				relation.setIdentifier(UUID.randomUUID().toString());
			}
			if (erm.getLifeCycle() == null) {
				relation.setCreator(null);
				relation.setStatus("AUDIT_WAITING");
			} else {
				relation.setCreator(erm.getLifeCycle().getCreator());
				relation.setStatus(erm.getLifeCycle().getStatus());
			}
			relation.setEnable(true);
			// 默认值
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			relation.setCreateTime(timestamp);
			relation.setLastUpdate(timestamp);

			// 处理sortNum,用于排序
			List<ResourceRelationResultModel> list4Total = null;
			if (relation.getResType().equals(
					IndexSourceType.ChapterType.getName())
					&& relation.getResourceTargetType().equals(
							IndexSourceType.QuestionType.getName())) {// 这个判断后期可能需要去掉
				relation.setSortNum(SORT_NUM_INITIAL_VALUE);
			} else {
				// 调用SDK,目的是获取资源关系的总数
				list4Total = map4Total.get(relation.getSourceUuid());
				if (CollectionUtils.isEmpty(list4Total)) {
					newSortNum = SORT_NUM_INITIAL_VALUE;
					list4Total = educationRelationDao
							.getResourceRelationsWithOrder(
									relation.getSourceUuid(),
									relation.getResType());
					if (CollectionUtils.isNotEmpty(list4Total)) {// 说明源资源与目标资源之间没有任何关系
						// 说明已经存在关系,将新增加的资源关系放到最后一个
						newSortNum = list4Total.get(0).getSortNum() == null ? SORT_NUM_INITIAL_VALUE + 10
								: list4Total.get(0).getSortNum() + 10;
						map4Total.put(relation.getSourceUuid(), list4Total);
					} else {
						List<ResourceRelationResultModel> list4TotalTmp = new ArrayList<ResourceRelationResultModel>();
						ResourceRelationResultModel model = new ResourceRelationResultModel();
						model.setOrderNum(relation.getOrderNum());
						model.setSortNum(relation.getSortNum());
						model.setResourceTargetType(relation
								.getResourceTargetType());
						list4TotalTmp.add(model);
						map4Total.put(relation.getSourceUuid(), list4TotalTmp);
					}
				} else {
					newSortNum += 10;
				}

				relation.setSortNum(newSortNum);
			}

			if ((relation.getResType().equals(
					IndexSourceType.ChapterType.getName())
					|| relation.getResType().equals(
							IndexSourceType.LessonType.getName()) || relation
					.getResType().equals(
							IndexSourceType.InstructionalObjectiveType
									.getName()))
					&& (relation.getResourceTargetType().equals(
							IndexSourceType.LessonType.getName()) || relation
							.getResourceTargetType().equals(
									IndexSourceType.InstructionalObjectiveType
											.getName()))) {
				if (erm.getOrderNum() != null) {
					// 判断orderNum是否重复
					boolean isRepeat = false;
					for (ResourceRelationResultModel rr : list4Total) {
						if ((rr.getResourceTargetType().equals(relation
								.getResourceTargetType()))
								&& (rr.getOrderNum() != null
										&& erm.getOrderNum() != null && rr
										.getOrderNum().intValue() == erm
										.getOrderNum().intValue())) {
							isRepeat = true;
							break;
						}
					}

					if (isRepeat) {

						LOG.error("orderNum不允许重复");

						throw new LifeCircleException(
								HttpStatus.INTERNAL_SERVER_ERROR,
								LifeCircleErrorMessageMapper.OrderNumError
										.getCode(), "orderNum不允许重复");
					} else {
						relation.setOrderNum(erm.getOrderNum());
					}
				} else {// 策略:orderNum取已存在的 MAX+1
					// 取到orderNum最大值
					int maxOrderNum = 0;
					if (orderNumMap.get(erm.getSource()) != null) {
						maxOrderNum = orderNumMap.get(erm.getSource());
						if (CollectionUtils.isEmpty(list4Total)) {
							maxOrderNum += 1;
						}
					}
					if (CollectionUtils.isNotEmpty(list4Total)) {
						for (ResourceRelationResultModel rr : list4Total) {
							if (rr.getOrderNum() != null) {
								if ((rr.getResourceTargetType().equals(relation
										.getResourceTargetType()))
										&& (rr.getOrderNum() > maxOrderNum)) {
									maxOrderNum = rr.getOrderNum();
								}
							}
						}
						maxOrderNum += 1;
					}
					relation.setOrderNum(maxOrderNum);
				}
				if (orderNumMap.get(erm.getSource()) == null
						|| relation.getOrderNum() > orderNumMap.get(erm
								.getSource())) {
					orderNumMap.put(erm.getSource(), relation.getOrderNum());
				}
			}

			// 新增源资源与目标资源的创建时间
			relation.setResourceCreateTime(((Education) resourceEntity)
					.getDbcreateTime());
			if (isCreateWithResource) {
				relation.setTargetCreateTime(erm.getTargetCT());
			} else {
				relation.setTargetCreateTime(((Education) targetEntity)
						.getDbcreateTime());
			}
			relations4Create.add(relation);
		}

		List<ResourceRelation> resourceRelations = null;
		try {
			// 调用SDK
			if (CollectionUtils.isNotEmpty(relations4Create)) {
				resourceRelations = resourceRelationRepository
						.batchAdd(relations4Create);
			}
		} catch (EspStoreException e) {

			LOG.error("添加资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.CreateEducationRelationFail
							.getCode(), e.getMessage());
		}

		if (!haveExist && CollectionUtils.isEmpty(resourceRelations)) {

			LOG.error("添加资源关系失败");

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.CreateEducationRelationFail);
		}
		if (notifyFlag) {
			// add by xiezy - 2016.04.15
			// 异步通知智能出题
			if (!isCreateWithResource && !haveExist) {
				if (CollectionUtils.isEmpty(resourceRelations)) {
					for (ResourceRelation rr : resourceRelations) {
						notifyService.asynNotify4Relation(rr.getResType(),
								rr.getSourceUuid(), rr.getResourceTargetType(),
								rr.getTarget());
					}
				}
			}
		}

		// 处理返回结果
		if (!isCreateWithResource) {
			if (CollectionUtils.isNotEmpty(resourceRelations)) {
				for (ResourceRelation resourceRelation : resourceRelations) {
					EducationRelationModel model = new EducationRelationModel();
					model.setIdentifier(resourceRelation.getIdentifier());
					model.setSource(resourceRelation.getSourceUuid());
					if (resourceEntity != null) {
						model.setSourceTitle(resourceEntity.getTitle());
					}
					model.setTarget(resourceRelation.getTarget());
					if (targetEntity != null) {
						model.setTargetTitle(targetEntity.getTitle());
					}
					model.setRelationType(resourceRelation.getRelationType());
					model.setLabel(resourceRelation.getLabel());
					model.setTags(resourceRelation.getTags());
					model.setOrderNum(resourceRelation.getOrderNum());
					model.setResourceTargetType(resourceRelation
							.getResourceTargetType());
					model.setResType(resourceRelation.getResType());
					EducationRelationLifeCycleModel lifeCycleModel = new EducationRelationLifeCycleModel();
					lifeCycleModel.setCreateTime(resourceRelation
							.getCreateTime());
					lifeCycleModel.setLastUpdate(resourceRelation
							.getLastUpdate());
					lifeCycleModel.setEnable(resourceRelation.getEnable());
					lifeCycleModel.setCreator(resourceRelation.getCreator());
					lifeCycleModel.setStatus(resourceRelation.getStatus());
					model.setLifeCycle(lifeCycleModel);

					resultList.add(model);
				}
			}
		}

		return resultList;
	}

	@Override
	public EducationRelationModel updateRelation(String resType,
			String sourceUuid, String rid,
			EducationRelationModel educationRelationModel) {
		// 获取修改前的数据
		ResourceRelation resourceRelation = getRelationByRid(rid);
		if (resourceRelation == null) {
			LOG.error("资源关系不存在");

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.ResourceRelationNotExist);
		}

		// 判断源资源是否存在，不存在抛出not found异常
		EspEntity resourceEntity = resourceExist(resType, sourceUuid,
				ResourceType.RESOURCE_SOURCE);
		EspEntity targetEntity = resourceExist(
				resourceRelation.getResourceTargetType(),
				resourceRelation.getTarget(), ResourceType.RESOURCE_TARGET);

		// 生成SDK的入参对象
		ResourceRelation relation = new ResourceRelation();

		// 可变属性
		// 未传，则保留原值
		if (educationRelationModel.getLifeCycle() == null) {
			relation.setStatus(resourceRelation.getStatus());
		} else {
			relation.setStatus(educationRelationModel.getLifeCycle()
					.getStatus());
		}
		// 未传，则保留原值
		if (CollectionUtils.isEmpty(educationRelationModel.getTags())) {
			relation.setTags(resourceRelation.getTags());
		} else {
			relation.setTags(educationRelationModel.getTags());
		}
		relation.setOrderNum(educationRelationModel.getOrderNum());
		// 未传，则保留原值
		if (StringUtils.isEmpty(educationRelationModel.getRelationType())) {
			relation.setRelationType(resourceRelation.getRelationType());
		} else {
			relation.setRelationType(educationRelationModel.getRelationType());
		}
		// 未传，则保留原值
		if (StringUtils.isEmpty(educationRelationModel.getLabel())) {
			relation.setLabel(resourceRelation.getLabel());
		} else {
			relation.setLabel(educationRelationModel.getLabel());
		}
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		relation.setLastUpdate(timestamp);

		// 不可变属性
		relation.setIdentifier(rid);
		relation.setResourceTargetType(resourceRelation.getResourceTargetType());
		relation.setResType(resourceRelation.getResType());
		relation.setSourceUuid(sourceUuid);
		relation.setCreator(resourceRelation.getCreator());
		relation.setTarget(resourceRelation.getTarget());
		relation.setSortNum(resourceRelation.getSortNum());
		relation.setCreateTime(resourceRelation.getCreateTime());
		relation.setEnable(resourceRelation.getEnable());
		// 新增源资源与目标资源的创建时间
		relation.setResourceCreateTime(resourceRelation.getResourceCreateTime());
		relation.setTargetCreateTime(resourceRelation.getTargetCreateTime());

		// 当relation.getResourceTargetType()为课时或者教学目标时对orderNum进行非重复判断,其他不做处理
		if ((relation.getResType()
				.equals(IndexSourceType.ChapterType.getName())
				|| relation.getResType().equals(
						IndexSourceType.LessonType.getName()) || relation
				.getResType().equals(
						IndexSourceType.InstructionalObjectiveType.getName()))
				&& (relation.getResourceTargetType().equals(
						IndexSourceType.LessonType.getName()) || relation
						.getResourceTargetType().equals(
								IndexSourceType.InstructionalObjectiveType
										.getName()))) {
			// 查出所有的order
			List<ResourceRelationResultModel> list4OrderNum = educationRelationDao
					.getResourceRelations(relation.getSourceUuid(),
							relation.getResType(),
							relation.getResourceTargetType());

			// 判断orderNum是否重复
			boolean isRepeat = false;
			for (ResourceRelationResultModel rr : list4OrderNum) {
				if (rr.getOrderNum() != null
						&& educationRelationModel.getOrderNum() != null
						&& rr.getOrderNum().intValue() == educationRelationModel
								.getOrderNum().intValue()) {
					isRepeat = true;
					break;
				}
			}

			if (isRepeat) {

				LOG.error("orderNum不允许重复");

				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.OrderNumError.getCode(),
						"orderNum不允许重复");
			} else {
				relation.setOrderNum(educationRelationModel.getOrderNum());
			}
		}

		ResourceRelation result = null;
		try {
			// 调用SDK
			result = resourceRelationRepository.update(relation);
		} catch (EspStoreException e) {

			LOG.error("更新资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.UpdateEducationRelationFail
							.getCode(), e.getMessage());
		}

		// 修改资源关系失败
		if (result == null) {

			LOG.error("更新资源关系失败");

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.UpdateEducationRelationFail);
		}

		// 处理返回结果
		EducationRelationModel model = new EducationRelationModel();
		model.setIdentifier(result.getIdentifier());
		model.setSource(result.getSourceUuid());
		if (resourceEntity != null) {
			model.setSourceTitle(resourceEntity.getTitle());
		}
		model.setTarget(result.getTarget());
		if (targetEntity != null) {
			model.setTargetTitle(targetEntity.getTitle());
		}
		model.setRelationType(result.getRelationType());
		model.setLabel(result.getLabel());
		model.setTags(result.getTags());
		model.setOrderNum(result.getOrderNum());
		model.setResourceTargetType(result.getResourceTargetType());
		model.setResType(result.getResType());
		EducationRelationLifeCycleModel lifeCycleModel = new EducationRelationLifeCycleModel();
		lifeCycleModel.setCreateTime(result.getCreateTime());
		lifeCycleModel.setLastUpdate(result.getLastUpdate());
		lifeCycleModel.setEnable(result.getEnable());
		lifeCycleModel.setCreator(result.getCreator());
		lifeCycleModel.setStatus(result.getStatus());
		model.setLifeCycle(lifeCycleModel);

		return model;
	}

	@Override
	public boolean deleteRelation(String rid, String sourceUuid, String resType) {
		// 判断源资源是否存在,不存在将抛出not found的异常
		CommonHelper.resourceExist(resType, sourceUuid,
				ResourceType.RESOURCE_SOURCE);

		ResourceRelation rt = getRelationByRid(rid);

		if (rt == null) {
			LOG.error("资源关系不存在");

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.ResourceRelationNotExist);
		}

		rt.setEnable(false);
		rt.setLastUpdate(new Timestamp(System.currentTimeMillis()));

		try {
			resourceRelationRepository.update(rt);
		} catch (EspStoreException e) {

			LOG.error("更新资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.UpdateLessonFail);
		}

		// add by xiezy - 2016.04.15
		// 异步通知智能出题
		List<ResourceRelation> notifyRelationList = new ArrayList<ResourceRelation>();
		notifyRelationList.add(rt);
		notifyService.asynNotify4RelationOnDelete(notifyRelationList);

		return true;
	}

	@Override
	public boolean deleteRelationByTarget(String resType, String sourceUuid,
			List<String> target, String relationType, boolean reverse) {
		// 判断源资源是否存在,不存在将抛出not found的异常
		CommonHelper.resourceExist(resType, sourceUuid,
				ResourceType.RESOURCE_SOURCE);
		// 用于存放需要删除的资源关系id
		// List<String> deleteIds = new ArrayList<String>();

		// 根据条件查询全部的资源关系,再进行批量删除
		ResourceRelation resourceRelation = new ResourceRelation();
		if (reverse) {
			resourceRelation.setTarget(sourceUuid);
			resourceRelation.setResourceTargetType(resType);
		} else {
			resourceRelation.setSourceUuid(sourceUuid);
			resourceRelation.setResType(resType);
		}
		// 如果relationType不为null和空,则赋值
		if (!StringUtils.isEmpty(relationType)) {
			resourceRelation.setRelationType(relationType);
		}

		// 用于存放查找到的资源关系
		List<ResourceRelation> relationsTotal = new ArrayList<ResourceRelation>();
		List<ResourceRelation> relations = null;

		if (!CollectionUtils.isEmpty(target)) {// 当target有值时,需循环调用sdk找出全部的资源关系id
			for (String tagetId : target) {
				if (reverse) {
					resourceRelation.setSourceUuid(tagetId);
				} else {
					resourceRelation.setTarget(tagetId);
				}

				try {
					// 调用SDK,根据条件查询资源关系
					relations = resourceRelationRepository
							.getAllByExample(resourceRelation);

					if (!CollectionUtils.isEmpty(relations)) {
						// 将找到的资源关系id,放入deleteIds中
						for (ResourceRelation rr : relations) {
							// deleteIds.add(rr.getIdentifier());
							rr.setEnable(false);
							rr.setLastUpdate(new Timestamp(System
									.currentTimeMillis()));
						}
						relationsTotal.addAll(relations);
					}
				} catch (EspStoreException e) {

					LOG.error("target,根据条件获取资源关系失败", e);

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.GetRelationByTargetFail
									.getCode(), e.getMessage());
				}
			}
		} else {// 当target为null或empty时,只需调用一次
			try {
				// 调用SDK,根据条件查询资源关系
				relations = resourceRelationRepository
						.getAllByExample(resourceRelation);

				if (!CollectionUtils.isEmpty(relations)) {
					// 将找到的资源关系id,放入deleteIds中
					for (ResourceRelation rr : relations) {
						// deleteIds.add(rr.getIdentifier());
						rr.setEnable(false);
						rr.setLastUpdate(new Timestamp(System
								.currentTimeMillis()));
					}
					relationsTotal.addAll(relations);
				}
			} catch (EspStoreException e) {

				LOG.error("target,根据条件获取资源关系失败", e);

				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.GetRelationByTargetFail
								.getCode(), e.getMessage());
			}
		}

		try {
			if (CollectionUtils.isEmpty(relationsTotal)) {
				return true;
			}

			// 更新资源关系
			resourceRelationRepository.batchAdd(relationsTotal);
		} catch (EspStoreException e) {

			LOG.error("批量更新资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.UpdateBatchRelationFail
							.getCode(), e.getMessage());
		}

		// add by xiezy - 2016.04.15
		// 异步通知智能出题
		notifyService.asynNotify4RelationOnDelete(relationsTotal);

		return true;
	}

	@Override
	public boolean deleteRelationByTargetType(String resType,
			String sourceUuid, List<String> targetType, String relationType,
			boolean reverse) {
		// 判断源资源是否存在,不存在将抛出not found的异常
		CommonHelper.resourceExist(resType, sourceUuid,
				ResourceType.RESOURCE_SOURCE);
		// 用于存放需要删除的资源关系id
		// List<String> deleteIds = new ArrayList<String>();

		// 根据条件查询全部的资源关系,再进行批量删除
		ResourceRelation resourceRelation = new ResourceRelation();
		if (reverse) {
			resourceRelation.setTarget(sourceUuid);
			resourceRelation.setResourceTargetType(resType);
		} else {
			resourceRelation.setSourceUuid(sourceUuid);
			resourceRelation.setResType(resType);
		}
		// 如果relationType不为null和空,则赋值
		if (!StringUtils.isEmpty(relationType)) {
			resourceRelation.setRelationType(relationType);
		}

		// 用于存放查找到的资源关系
		List<ResourceRelation> relationsTotal = new ArrayList<ResourceRelation>();
		List<ResourceRelation> relations = null;

		if (!CollectionUtils.isEmpty(targetType)) {// 当targetType有值时,需循环调用sdk找出全部的资源关系id
			for (String type : targetType) {
				if (reverse) {
					resourceRelation.setResType(type);
				} else {
					resourceRelation.setResourceTargetType(type);
				}

				try {
					// 调用SDK,根据条件查询资源关系
					relations = resourceRelationRepository
							.getAllByExample(resourceRelation);

					if (!CollectionUtils.isEmpty(relations)) {

						// 将找到的资源关系id,放入deleteIds中
						for (ResourceRelation rr : relations) {
							// deleteIds.add(rr.getIdentifier());
							rr.setEnable(false);
							rr.setLastUpdate(new Timestamp(System
									.currentTimeMillis()));
						}
						relationsTotal.addAll(relations);
					}
				} catch (EspStoreException e) {

					LOG.error("根据条件获取资源关系失败", e);

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.GetRelationByTargetTypeFail
									.getCode(), e.getMessage());
				}
			}
		} else {// 当targetType为null或empty时,只需调用一次
			try {
				// 调用SDK,根据条件查询资源关系
				relations = resourceRelationRepository
						.getAllByExample(resourceRelation);
				if (!CollectionUtils.isEmpty(relations)) {
					// 将找到的资源关系id,放入deleteIds中
					for (ResourceRelation rr : relations) {
						// deleteIds.add(rr.getIdentifier());
						rr.setEnable(false);
						rr.setLastUpdate(new Timestamp(System
								.currentTimeMillis()));
					}
					relationsTotal.addAll(relations);
				}
			} catch (EspStoreException e) {

				LOG.error("根据条件获取资源关系失败", e);

				throw new LifeCircleException(
						HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.GetRelationByTargetTypeFail
								.getCode(), e.getMessage());
			}
		}

		try {
			if (CollectionUtils.isEmpty(relationsTotal)) {
				return true;
			}

			// 更新资源关系
			resourceRelationRepository.batchAdd(relationsTotal);
		} catch (EspStoreException e) {

			LOG.error("批量更新资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.UpdateBatchRelationFail
							.getCode(), e.getMessage());
		}

		// add by xiezy - 2016.04.15
		// 异步通知智能出题
		notifyService.asynNotify4RelationOnDelete(relationsTotal);

		return true;
	}

	@Override
	public List<List<RelationForPathViewModel>> getRelationsByConditions(
			String resType, String sourceUuid, List<String> relationPath,
			boolean reverse, String categoryPattern) {
		// 判断源资源是否存在,不存在将抛出not found的异常
		CommonHelper.resourceExist(resType, sourceUuid,
				ResourceType.RESOURCE_SOURCE);

		// 返回的结果集
		List<List<RelationForPathViewModel>> result = new ArrayList<List<RelationForPathViewModel>>();

		/*
		 * 1.查找resType与课时的资源关系,找到课时的id集合
		 */
		// 用于存放查找到的资源关系
		List<ResourceRelation> relations4Lessons = null;
		// 用于存放课时id
		List<String> lessonIds = new ArrayList<String>();
		// 入参
		ResourceRelation resourceRelation4Lessons = new ResourceRelation();
		if (reverse) {// 进行反向查询 Target->Source
			resourceRelation4Lessons.setTarget(sourceUuid);
			resourceRelation4Lessons.setResourceTargetType(resType);
			// resourceRelation4Lessons.setResType(relationPath.get(0));//relationPath.get(0)
			// == lessons
			resourceRelation4Lessons.setResType("lessons");
		} else {// 不进行反向查询 Source->Target
			resourceRelation4Lessons.setSourceUuid(sourceUuid);
			resourceRelation4Lessons.setResType(resType);
			// resourceRelation4Lessons.setResourceTargetType(relationPath.get(0));
			resourceRelation4Lessons.setResourceTargetType("lessons");
		}

		try {
			// 调用SDK,查找resType与课时的资源关系
			relations4Lessons = resourceRelationRepository
					.getAllByExample(resourceRelation4Lessons);

			// 找出所有的课时id
			if (CollectionUtils.isNotEmpty(relations4Lessons)) {
				for (ResourceRelation rr : relations4Lessons) {
					lessonIds
							.add(reverse ? rr.getSourceUuid() : rr.getTarget());
				}
			}
		} catch (EspStoreException e) {

			LOG.error("根据条件获取资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.GetRelationByTargetTypeFail
							.getCode(), e.getMessage());
		}

		// 如果resType没有对应的课时,返回空集合
		if (lessonIds.size() == 0) {
			return new ArrayList<List<RelationForPathViewModel>>();
		}

		/*
		 * 2.查询课时与教材章节间的关系,获得教材章节的id
		 */
		for (String lessonId : lessonIds) {
			// 用于存放查找到的资源关系
			List<ResourceRelation> relations4Materials = null;
			// 用于存放教材章节的id
			List<String> chapterIds = new ArrayList<String>();
			// 入参
			ResourceRelation resourceRelation4Materials = new ResourceRelation();
			if (reverse) {// 进行反向查询 Target->Source
				resourceRelation4Materials.setTarget(lessonId);
				resourceRelation4Materials.setResourceTargetType(relationPath
						.get(0));
				// resourceRelation4Materials.setResType(relationPath.get(1));//relationPath.get(1)
				// == teachingmaterials
				resourceRelation4Materials.setResType("chapters");
			} else {// 不进行反向查询 Source->Target
				resourceRelation4Materials.setSourceUuid(lessonId);
				resourceRelation4Materials.setResType(relationPath.get(0));
				// resourceRelation4Materials.setResourceTargetType(relationPath.get(1));
				resourceRelation4Materials.setResourceTargetType("chapters");
			}
			try {
				// 调用SDK,查询课时与教材章节间的关系
				relations4Materials = resourceRelationRepository
						.getAllByExample(resourceRelation4Materials);

				// 找出所有的教材章节id
				if (CollectionUtils.isNotEmpty(relations4Materials)) {
					for (ResourceRelation rr : relations4Materials) {
						chapterIds.add(reverse ? rr.getSourceUuid() : rr
								.getTarget());
					}
				}
			} catch (EspStoreException e) {

				LOG.error("根据条件获取资源关系失败", e);

				throw new LifeCircleException(
						HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.GetRelationByTargetTypeFail
								.getCode(), e.getMessage());
			}
			// 如果课时没有对应的教材章节,返回空集合
			if (chapterIds.size() == 0) {
				return new ArrayList<List<RelationForPathViewModel>>();
			}

			// 返回结果的元素
			List<RelationForPathViewModel> item = new ArrayList<RelationForPathViewModel>();

			/*
			 * 3. 1)查找教材和章节父节点 2)处理返回结果
			 */
			boolean isGetDetailSuccess = true;
			for (String chapterId : chapterIds) {
				try {
					// 获取章节详细
					Chapter chapter = chapterRepository.get(chapterId);
					if (chapter == null
							|| chapter.getEnable() == null
							|| !chapter.getEnable()
							|| !ResourceNdCode.chapters.toString().equals(
									chapter.getPrimaryCategory())) {// 章节不存在
						isGetDetailSuccess = false;
						continue;
					}

					// 获取教材详细
					String mid = chapter.getTeachingMaterial();
					TeachingMaterial teachingMaterial = this.getDetail(mid);
					if (teachingMaterial == null) {// 教材不存在
						isGetDetailSuccess = false;
						continue;
					}

					ResourceCategory example = new ResourceCategory();
					example.setResource(mid);
					List<ResourceCategory> resourceCategories = resourceCategoryRepository
							.getAllByExample(example);
					if (CollectionUtils.isEmpty(resourceCategories)) {
						isGetDetailSuccess = false;
						continue;
					}
					List<String> categoryCodes = null;
					for (ResourceCategory resourceCategory : resourceCategories) {
						if (StringUtils.isNotEmpty(resourceCategory
								.getTaxonpath())) {
							String taxonpath = resourceCategory.getTaxonpath();
							categoryCodes = Arrays.asList(taxonpath.split("/"));
							break;
						}
					}

					if (CollectionUtils.isNotEmpty(categoryCodes)) {
						for (String path : categoryCodes) {
							if (StringUtils.isEmpty(path)) {
								isGetDetailSuccess = false;
								break;
							}
						}
					} else {
						isGetDetailSuccess = false;
					}

					if (!isGetDetailSuccess) {
						continue;
					}

					// nd_code集合
					List<String> ndCodes = new ArrayList<String>();

					if (categoryCodes.size() > 1) {
						ndCodes.add(categoryCodes.get(1));
					}
					if (categoryCodes.size() > 2) {
						ndCodes.add(categoryCodes.get(2));
					}
					if (categoryCodes.size() > 3) {
						ndCodes.add(categoryCodes.get(3));
					}
					if (categoryCodes.size() > 4) {
						ndCodes.add(categoryCodes.get(4));
					}
					if (categoryCodes.size() > 5) {
						ndCodes.add(categoryCodes.get(5));
					}

					// 层级计数
					int levelCount = 1;

					// 调用SDK,通过ndCode批量获取分类维度数据
					List<CategoryData> categoryDatas = categoryDataRepository
							.getListWhereInCondition("ndCode", ndCodes);

					if (CollectionUtils.isEmpty(categoryDatas)
							|| categoryDatas.size() != ndCodes.size()) {
						isGetDetailSuccess = false;
						continue;
					}

					// 返回结果1,教材维度块整理
					for (CategoryData categoryData : categoryDatas) {
						RelationForPathViewModel viewModel = new RelationForPathViewModel();
						viewModel.setIdentifier("");
						viewModel.setNdCode(categoryData.getNdCode());
						viewModel.setTitle(categoryData.getTitle());
						viewModel.setLevel(levelCount);
						levelCount++;
						item.add(viewModel);
					}

					List<Chapter> chapterList = chapterDao.getParents(mid,
							chapter.getLeft(), chapter.getRight());

					// 返回结果2,章节块整理
					for (Chapter mcvm : chapterList) {
						ChapterModel chapterModel = chapterServiceV06
								.getChapterDetail(mcvm.getIdentifier());
						if (chapterModel == null) {// 章节不存在
							isGetDetailSuccess = false;
							break;
						}
						RelationForPathViewModel viewModel = new RelationForPathViewModel();
						viewModel.setIdentifier(chapterModel.getIdentifier());
						viewModel.setNdCode("");
						viewModel.setTitle(chapterModel.getTitle());
						viewModel.setLevel(levelCount);
						levelCount++;
						item.add(viewModel);
					}

					// 返回结果3,课时块整理
					Lesson return4Lesson = lessonRepository.get(lessonId);
					if (return4Lesson != null
							&& ResourceNdCode.lessons.toString().equals(
									return4Lesson.getPrimaryCategory())
							&& (return4Lesson.getEnable() == null || return4Lesson
									.getEnable())) {
						RelationForPathViewModel viewModel = new RelationForPathViewModel();
						viewModel.setIdentifier(return4Lesson.getIdentifier());
						viewModel.setNdCode("");
						viewModel.setTitle(return4Lesson.getTitle());
						viewModel.setLevel(levelCount);
						item.add(viewModel);
					} else {
						isGetDetailSuccess = false;
					}

				} catch (EspStoreException e) {

					LOG.error("获取资源之间的关系失败");

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.GetRelationsFail
									.getCode(), e.getMessage());
				}
			}

			if (isGetDetailSuccess) {// 整条线完整,才返回
				result.add(item);
			}

			isGetDetailSuccess = true;
		}

		return result;
	}

	@Override
	public void batchAdjustRelationOrder(String resType, String sourceUuid,
			List<BatchAdjustRelationOrderModel> batchAdjustRelationOrderModels,
			boolean isBusiness, String parent) {
		// 判断源资源是否存在,不存在将抛出not found的异常
		if (!isBusiness) {
			CommonHelper.resourceExist(resType, sourceUuid,
					ResourceType.RESOURCE_SOURCE);
		}

		if (isBusiness && StringUtils.isNotEmpty(parent)) {
			CommonHelper.resourceExist(resType, parent,
					ResourceType.RESOURCE_SOURCE);
			if (CollectionUtils.isNotEmpty(batchAdjustRelationOrderModels)
					&& batchAdjustRelationOrderModels.size() > 1) {
				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						"LC/CHECK_PARAM_VALID_FAIL", "parent暂不支持多个关系处理");
			}
			if (CollectionUtils.isNotEmpty(batchAdjustRelationOrderModels)) {
				BatchAdjustRelationOrderModel tmp = batchAdjustRelationOrderModels
						.get(0);
				ResourceRelation tr = this.getRelationByRid(tmp.getTarget());
				tr.setSourceUuid(parent);
				tr.setSortNum(5000f);
				try {
					resourceRelationRepository.update(tr);
				} catch (EspStoreException e) {
					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
							e.getMessage());
				}
				return;
			}
		}

		if (!CollectionUtils.isEmpty(batchAdjustRelationOrderModels)) {
			// 调整前先校验数据,有问题返回异常,不允许调整
			for (BatchAdjustRelationOrderModel bar : batchAdjustRelationOrderModels) {
				// 判断 需要移动的目标对象 是否存在
				if (this.getRelationByRid(bar.getTarget()) == null) {// 不存在

					LOG.error("需要移动的目标对象--" + bar.getTarget() + "--不存在");

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.TargetRelationNotExist);
				}
				// 判断 移动目的地的靶心对象 是否存在
				if (this.getRelationByRid(bar.getDestination()) == null) {// 不存在

					LOG.error("需要移动的目标对象--" + bar.getTarget()
							+ "--移动目的地的靶心对象--" + bar.getDestination() + "--不存在");

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.DestinationRelationNotExist);
				}
				if ("first".equals(bar.getAt()) || "last".equals(bar.getAt())) {// 移动到第一个位置或者移动到最后
					// 相邻对象adjoin应该为"none"
					if (!"none".equals(bar.getAdjoin())) {
						String message = "first".equals(bar.getAt()) ? "移动到第一个位置"
								: "移动到最后一个";

						LOG.error("需要移动的目标对象--" + bar.getTarget() + "--"
								+ message + "时相邻对象的值应该为none");

						throw new LifeCircleException(
								HttpStatus.INTERNAL_SERVER_ERROR,
								LifeCircleErrorMessageMapper.AdjoinValueError);
					}
				} else if ("middle".equals(bar.getAt())) {// 将目标增加到destination和adjoin中间
					// 判断 相邻对象 是否存在
					if (!"none".equals(bar.getAdjoin())
							&& this.getRelationByRid(bar.getAdjoin()) == null) {// 不存在

						LOG.error("需要移动的目标对象--" + bar.getTarget() + "--相邻对象--"
								+ bar.getAdjoin() + "--不存在");

						throw new LifeCircleException(
								HttpStatus.INTERNAL_SERVER_ERROR,
								LifeCircleErrorMessageMapper.AdjoinRelationNotExist);
					}
				} else {

					LOG.error("需要移动的目标对象--" + bar.getTarget()
							+ "--at只允许为first,last,middle三个值");

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.AtValueError);
				}
			}

			// 需要移动的目标对象
			ResourceRelation targetRelation = null;
			// 移动目的地靶心对象
			ResourceRelation destinationRelation = null;
			// 相邻对象
			ResourceRelation adjoinRelation = null;

			// 数据正确,进行顺序调整
			for (BatchAdjustRelationOrderModel bar4Adjust : batchAdjustRelationOrderModels) {
				if ("first".equals(bar4Adjust.getAt())) {// 移动到第一个位置
					targetRelation = this.getRelationByRid(bar4Adjust
							.getTarget());
					destinationRelation = this.getRelationByRid(bar4Adjust
							.getDestination());

					// 修改targetRelation的sortNum,在destinationRelation的sortNum上-10
					targetRelation
							.setSortNum(destinationRelation.getSortNum() - 10);

				} else if ("last".equals(bar4Adjust.getAt())) {// 移动到最后
					targetRelation = this.getRelationByRid(bar4Adjust
							.getTarget());
					destinationRelation = this.getRelationByRid(bar4Adjust
							.getDestination());

					// 修改targetRelation的sortNum,在destinationRelation的sortNum上+10
					targetRelation
							.setSortNum(destinationRelation.getSortNum() + 10);
				} else {// 将目标增加到destination和adjoin中间
					targetRelation = this.getRelationByRid(bar4Adjust
							.getTarget());
					destinationRelation = this.getRelationByRid(bar4Adjust
							.getDestination());
					adjoinRelation = this.getRelationByRid(bar4Adjust
							.getAdjoin());

					// 修改targetRelation的sortNum,
					// 为(destinationRelation的sortNum + adjoinRelation的sortNum) /
					// 2
					targetRelation
							.setSortNum((destinationRelation.getSortNum() + adjoinRelation
									.getSortNum()) / 2);
				}

				try {
					if (isBusiness) {
						targetRelation.setResType(destinationRelation
								.getResType());
						targetRelation.setSourceUuid(destinationRelation
								.getSourceUuid());
						targetRelation
								.setResourceCreateTime(destinationRelation
										.getResourceCreateTime());
					}
					resourceRelationRepository.update(targetRelation);
				} catch (EspStoreException e) {

					LOG.error("批量调整顺序时更新失败", e);

					throw new LifeCircleException(
							HttpStatus.INTERNAL_SERVER_ERROR,
							LifeCircleErrorMessageMapper.AdjustOrderFail
									.getCode(), e.getMessage());
				}
			}
		}
	}

	public EducationRelationModel relationExist(String sourceId,
			String targetId, String relationType, String label) {
		ResourceRelation example = new ResourceRelation();
		example.setSourceUuid(sourceId);
		example.setTarget(targetId);
		if (StringUtils.isEmpty(relationType)) {
			example.setRelationType("ASSOCIATE");
		} else {
			example.setRelationType(relationType);
		}
		example.setLabel(label);
		example.setEnable(true);

		List<ResourceRelation> resourceRelations = null;
		try {
			resourceRelations = resourceRelationRepository
					.getAllByExample(example);
		} catch (EspStoreException e) {

			LOG.error("获取资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.GetRelationDetailFail
							.getCode(), e.getMessage());
		}

		if (CollectionUtils.isEmpty(resourceRelations)) {
			return null;
		}

		ResourceRelation result = null;

		for (ResourceRelation resourceRelation : resourceRelations) {
			if ((StringUtils.isEmpty(label) && StringUtils
					.isEmpty(resourceRelation.getLabel()))
					|| (StringUtils.isNotEmpty(label) && label
							.equals(resourceRelation.getLabel()))) {
				result = resourceRelation;
				break;
			}
		}

		if (result == null) {
			return null;
		}

		// 处理返回结果
		EducationRelationModel model = new EducationRelationModel();
		model.setIdentifier(result.getIdentifier());
		model.setSource(result.getSourceUuid());
		model.setTarget(result.getTarget());
		model.setRelationType(result.getRelationType());
		model.setLabel(result.getLabel());
		model.setTags(result.getTags());
		model.setOrderNum(result.getOrderNum());
		model.setResourceTargetType(result.getResourceTargetType());
		model.setResType(result.getResType());
		EducationRelationLifeCycleModel lifeCycleModel = new EducationRelationLifeCycleModel();
		lifeCycleModel.setCreateTime(result.getCreateTime());
		lifeCycleModel.setLastUpdate(result.getLastUpdate());
		lifeCycleModel.setEnable(result.getEnable());
		lifeCycleModel.setCreator(result.getCreator());
		lifeCycleModel.setStatus(result.getStatus());
		model.setLifeCycle(lifeCycleModel);

		return model;
	}

	@Override
	public ListViewModel<RelationForQueryViewModel> queryListByResTypeByDB(
			String resType, String sourceUuid, String categories,
			String targetType, String relationType, String limit,
			boolean reverse, String coverage) {
		return queryListByResTypeByDB(resType, sourceUuid, categories,
				targetType, null, null, relationType, limit, reverse, coverage,
				false, false);
	}

	@Override
	public ListViewModel<RelationForQueryViewModel> queryListByResTypeByDB(
			String resType, String sourceUuid, String categories,
			String targetType, String label, String tags, String relationType,
			String limit, boolean reverse, String coverage, boolean isPortal,
			boolean isRecycled) {
		// 判断源资源是否存在,不存在将抛出not found的异常
		if (isRecycled) {
			CommonHelper.resourceExistInRecycled(resType, sourceUuid,
					ResourceType.RESOURCE_SOURCE);
		} else {
			CommonHelper.resourceExist(resType, sourceUuid,
					ResourceType.RESOURCE_SOURCE);
		}

		List<String> sourceUuids = new ArrayList<String>();
		sourceUuids.add(sourceUuid);

		// 返回的结果集
		ListViewModel<RelationForQueryViewModel> listViewModel = educationRelationDao
				.queryResByRelation(resType, sourceUuids, categories,
						targetType, label, tags, relationType, limit, reverse,
						coverage, isPortal, isRecycled);

		return listViewModel;
	}

	@Override
	public ListViewModel<RelationForQueryViewModel> recursionQueryResourcesByDB(
			String resType, String sourceUuid, String categories,
			String targetType, String label, String tags, String relationType,
			String limit, String coverage, boolean isPortal)
			throws EspStoreException {
		// 判断源资源是否存在,不存在将抛出not found的异常
		CommonHelper.resourceExist(resType, sourceUuid,
				ResourceType.RESOURCE_SOURCE);

		/*
		 * 递归查询sourceUuid对应章节下的子章节,得到chapterIds(包含sourceUuid)
		 */
		List<String> chapterIds = new ArrayList<String>();

		// 获取章节对象,目的是为了得到教材id
		Chapter chapter = chapterRepository.get(sourceUuid);

		// 递归查询所有子章节
		List<Chapter> children = chapterDao.getSubTreeByLeftAndRight(
				chapter.getTeachingMaterial(), chapter.getLeft(),
				chapter.getRight());

		// 得到所有需求的章节ids
		if (CollectionUtils.isEmpty(children)) {
			chapterIds.add(sourceUuid);
		} else {
			for (Chapter child : children) {
				if (child.getEnable()) {
					chapterIds.add(child.getIdentifier());
				}
			}
		}

		// 返回的结果集
		// 此处reverse设置为false, 不需要反转
		ListViewModel<RelationForQueryViewModel> listViewModel = educationRelationDao
				.queryResByRelation(resType, chapterIds, categories,
						targetType, label, tags, relationType, limit, false,
						coverage, isPortal, false);

		return listViewModel;
	}

	private TeachingMaterial getDetail(String id) {
		TeachingMaterial teachingMaterial;
		try {
			teachingMaterial = teachingMaterialRepository.get(id);
		} catch (EspStoreException e) {

			LOG.error("资源关系V0.6---获取教材详细出错", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.GetDetailTeachingMaterialFail
							.getCode(), e.getLocalizedMessage());
		}
		if (teachingMaterial == null
				|| !ResourceNdCode.teachingmaterials.toString().equals(
						teachingMaterial.getPrimaryCategory())
				|| (teachingMaterial.getEnable() != null && !teachingMaterial
						.getEnable())) {
			return null;
		}

		return teachingMaterial;
	}

	@Override
	public ListViewModel<RelationForQueryViewModel> batchQueryResourcesByDB(
			String resType, Set<String> sids, String targetType,
			String relationType, String limit) {
		// 返回的结果集

		return this.batchQueryResourcesByDB(resType, sids, targetType, null,
				null, relationType, limit, null, false, false, false);
	}

	@Override
	public ListViewModel<RelationForQueryViewModel> batchQueryResourcesByDB(
			String resType, Set<String> sids, String targetType, String label,
			String tags, String relationType, String limit, String category,
			boolean reverse, boolean isPortal, boolean isRecycled) {
		// 返回的结果集
		ListViewModel<RelationForQueryViewModel> listViewModel = educationRelationDao
				.queryResByRelation(resType, new ArrayList<String>(sids),
						category, targetType, label, tags, relationType, limit,
						reverse, null, isPortal, isRecycled);
		return listViewModel;

	}

	private ResourceRelation getRelationByRid(String rid) {
		// 判断rid对应的资源关系是否存在,不存在抛出异常
		ResourceRelation resourceRelation = null;
		try {
			resourceRelation = resourceRelationRepository.get(rid);
		} catch (EspStoreException e) {

			LOG.error("获取资源关系失败", e);

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.GetRelationDetailFail
							.getCode(), e.getMessage());
		}

		if (resourceRelation == null || !resourceRelation.getEnable()) {
			return null;
		}

		return resourceRelation;
	}

	/**
	 * 判断源资源是否存在
	 *
	 * @param resType
	 *            资源种类
	 * @param resId
	 *            源资源id
	 * @param type
	 *            源资源类型
	 */
	private EspEntity resourceExist(String resType, String resId, String type) {
		EspEntity flag = null;
		try {
			/*
			 * 调用各个资源的获取详细方法,用于判断对应资源是否存在, 若不存在,则抛出异常
			 */
			EspRepository<?> resourceRepository = ServicesManager.get(resType);
			flag = resourceRepository.get(resId);
		} catch (EspStoreException e) {
			if (ResourceType.RESOURCE_SOURCE.equals(type)) {

				LOG.error("源资源:" + resType + "--" + resId + "未找到", e);

				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
						e.getMessage());
			}
			if (ResourceType.RESOURCE_TARGET.equals(type)) {

				LOG.error("目标资源:" + resType + "--" + resId + "未找到", e);

				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.StoreSdkFail.getCode(),
						e.getMessage());
			}
		}

		// 资源不存在,抛出异常
		if (flag == null
				|| ((flag instanceof Education)
						&& ((Education) flag).getEnable() != null && !((Education) flag)
							.getEnable())
				|| !((Education) flag).getPrimaryCategory().equals(resType)) {
			if (ResourceType.RESOURCE_SOURCE.equals(type)) {

				LOG.error("源资源:" + resType + "--" + resId + "未找到");

				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.SourceResourceNotFond
								.getCode(), "源资源:" + resType + "--" + resId
								+ "未找到");
			}
			if (ResourceType.RESOURCE_TARGET.equals(type)) {

				LOG.error("目标资源:" + resType + "--" + resId + "未找到");

				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.TargetResourceNotFond
								.getCode(), "目标资源:" + resType + "--" + resId
								+ "未找到");
			}
		}

		return flag;
	}

	/**
	 * DEMO先演示，逻辑暂时写在service里 add by xuzy 20160629
	 */
	@Override
	public List<Map<String, Object>> queryKnowledgeTree(String uuid) {
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> parentList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> childList = new ArrayList<Map<String, Object>>();
		int upNum = 0;
		int downNum = 0;
		// 查找上级节点
		recursiveKnowledge(parentList, uuid, "up", upNum);

		if (CollectionUtils.isNotEmpty(parentList)) {
			returnList.addAll(parentList);
			for (Map<String, Object> map : parentList) {
				if (uuid.equals((String) map.get("identifier"))) {
					// 找出parent
					String parentId = (String) map.get("parent");
					if (!"ROOT".equals(parentId)) {
						String sql = "SELECT rr.source_uuid as parent,nd.title,nd.estatus as status,nd.identifier from resource_relations rr,ndresource nd where rr.res_type='knowledges' and rr.resource_target_type = 'knowledges' and rr.enable = 1 and rr.source_uuid='"
								+ parentId
								+ "' and nd.primary_category='knowledges' and nd.enable=1 and rr.target = nd.identifier and rr.target != '"
								+ uuid + "'";
						List<Map<String, Object>> tl = jt.queryForList(sql);
						if (CollectionUtils.isNotEmpty(tl)) {
							returnList.addAll(tl);
						}
					}
				}
			}
		}

		// 查找下级节点
		recursiveKnowledge(childList, uuid, "down", downNum);
		if (CollectionUtils.isNotEmpty(childList)) {
			returnList.addAll(childList);
		}
		return returnList;
	}

	/**
	 * 递归查找知识点
	 *
	 * @param list
	 * @param cid
	 * @param type
	 *            用来区分是向上递归还是向下递归
	 * @param num
	 *            递归的层数，主要是避免死循环
	 */
	private void recursiveKnowledge(List<Map<String, Object>> list, String cid,
			String type, int num) {
		if (num > 10) {
			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					"LC/RECURSE_OUT", "递归层数过多，已超过10层");
		}
		num++;
		if ("up".equals(type)) {
			List<Map<String, Object>> tmpList = queryParentKnByChildId(cid);
			if (CollectionUtils.isNotEmpty(tmpList)) {
				for (Map<String, Object> map : tmpList) {
					String parent = (String) map.get("parent");
					if (parent != null && parent.equals(cid)) {
						break;
					}
					list.add(map);
					recursiveKnowledge(list, parent, type, num);
				}
			} else {
				String sql = "SELECT 'ROOT' as parent,nd.title,nd.estatus as status,nd.identifier from ndresource nd where nd.primary_category='knowledges' and nd.identifier = '"
						+ cid + "' and nd.enable = 1";
				List<Map<String, Object>> tl = jt.queryForList(sql);
				if (CollectionUtils.isNotEmpty(tl)) {
					list.addAll(tl);
				}
			}
		} else if ("down".equals(type)) {
			List<Map<String, Object>> tmpList = queryChildKnByParentId(cid);
			if (CollectionUtils.isNotEmpty(tmpList)) {
				for (Map<String, Object> map : tmpList) {
					String childId = (String) map.get("identifier");
					list.add(map);
					recursiveKnowledge(list, childId, type, num);
				}
			}
		}
	}

	private List<Map<String, Object>> queryParentKnByChildId(String cid) {
		String sql = "SELECT rr.source_uuid as parent,nd.title,nd.estatus as status,nd.identifier from resource_relations rr,ndresource nd where rr.res_type='knowledges' and rr.resource_target_type = 'knowledges' and rr.enable = 1 and rr.target='"
				+ cid
				+ "' and nd.primary_category='knowledges' and nd.enable=1 and rr.target = nd.identifier";
		return jt.queryForList(sql);
	}

	private List<Map<String, Object>> queryChildKnByParentId(String pid) {
		String sql = "SELECT rr.source_uuid as parent,nd.title,nd.estatus as status,nd.identifier from resource_relations rr,ndresource nd where rr.res_type='knowledges' and rr.resource_target_type = 'knowledges' and rr.enable = 1 and rr.source_uuid='"
				+ pid
				+ "' and nd.primary_category='knowledges' and nd.enable=1 and rr.target = nd.identifier";
		return jt.queryForList(sql);
	}

	public List<Map<String, Object>> querySuiteDirectory() {

		// 查找redis，是否有相应的缓存数据
		List<Map<String, Object>> redisList = getQuerySuiteDirectoryForRedis();
		if (redisList.isEmpty()) {
			// 缓存没数据，则取数据库数据，并将取出的数据缓存到redis
			return queryFordevAndSaveForRedis();
		} else {
			return redisList;
		}
	}

	@Override
	public List<Map<String, Object>> querySuiteDirectory(
			List<String> categoryList) {
		return queryFordevAndSaveForRedis(categoryList);
	}


	@Override
	public List<Map<String, Object>> querySuiteDirectory(String category,
			String status) {
		return queryFordevAndSaveForRedis(category, status);
	}

	/**
	 * @return List<Map<String,Object>>
	 * @author yzc
	 * @date 2016年11月7日 下午5:16:30
	 * @method queryFordevAndSaveForRedis
	 */
	public List<Map<String, Object>> queryFordevAndSaveForRedis(
			List<String> categoryList) {
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		// 1、查询一级的套件目录
		String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id from ndresource nd,resource_categories rc where nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' and rc.resource=nd.identifier and nd.enable = 1 and not exists (SELECT identifier from resource_relations where res_type='assets' and resource_target_type = 'assets' and target = nd.identifier) order by nd.create_time";
		List<Map<String, Object>> list = jt.queryForList(sql);
		// 获取套件id列表
		List<String> suitIdList = new ArrayList<String>();
		for (Map<String, Object> map : list) {
			String identifier = (String) map.get("identifier");
			suitIdList.add(identifier);
		}
		// 获取套件id列表对应的taxOnCode
		List<Map<String, Object>> resultlist = getCategoryBySuitId(suitIdList);
		// 选择满足条件的suitId列
		List<Map<String, Object>> taxOnCodeList = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < suitIdList.size(); i++) {
			List<String> tempCategoryList = new ArrayList<String>();
			for (Map<String, Object> map : resultlist) {
				if (suitIdList.get(i).equals((String) map.get("identifier"))) {
					tempCategoryList.add((String) map.get("taxOnCode"));
				}
			}
			// 判断tempCategoryList中taxOnCode是否匹配入参的taxOnCode
			if (tempCategoryList.containsAll(categoryList)) {
				// 存在
				for (Map<String, Object> map : list) {
					if (suitIdList.get(i)
							.equals((String) map.get("identifier"))) {
						taxOnCodeList.add(map);
					}
				}
			}
		}
		returnList.addAll(taxOnCodeList);
		// 2、分别向下遍历
		List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
		int num = 1;
		if (CollectionUtils.isNotEmpty(taxOnCodeList)) {
			List<String> tmpIds = new ArrayList<String>();
			for (Map<String, Object> map : taxOnCodeList) {
				String identifier = (String) map.get("identifier");
				tmpIds.add(identifier);
			}
			recursiveSuiteDirectory(subList, tmpIds, num);
		}
		if (CollectionUtils.isNotEmpty(subList)) {
			returnList.addAll(subList);
		}
		// saveResultForRedis(returnList);
		return returnList;
	}

	/**
	 * @return List<Map<String,Object>>
	 * @author xm
	 * @date 2016年11月7日 下午5:16:30
	 * @method queryFordevAndSaveForRedis
	 */
	public List<Map<String, Object>> queryFordevAndSaveForRedis() {
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		// 1、查询一级的套件目录
		String sql = "SELECT nd.identifier,nd.title,nd.description,nd.custom_properties,nd.estatus as status,'root' as parent,'none' as relation_id from ndresource nd,resource_categories rc where nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' and rc.resource=nd.identifier and nd.enable = 1 and not exists (SELECT identifier from resource_relations where res_type='assets' and resource_target_type = 'assets' and target = nd.identifier) order by nd.create_time";
		List<Map<String, Object>> list = jt.queryForList(sql);
		returnList.addAll(list);
		// 2、分别向下遍历
		List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
		int num = 1;
		if (CollectionUtils.isNotEmpty(list)) {
			List<String> tmpIds = new ArrayList<String>();
			for (Map<String, Object> map : list) {
				String identifier = (String) map.get("identifier");
				tmpIds.add(identifier);
			}
			recursiveSuiteDirectory(subList, tmpIds, num);
		}
		if (CollectionUtils.isNotEmpty(subList)) {
			returnList.addAll(subList);
		}
		saveResultForRedis(returnList);

		// 更新所有学科目标的缓存
		List<String> suiteIdList = new ArrayList<String>();
		for (Map<String, Object> map : list) {
			suiteIdList.add((String) map.get("identifier"));
		}
		// 获取一级套件id列表对应的taxOnCode
		List<Map<String, Object>> resultlist = getCategoryBySuitId(suiteIdList);

		List<String> chineseSuiteId = new ArrayList<String>();
		List<String> mathSuiteId = new ArrayList<String>();
		List<String> englishSuiteId = new ArrayList<String>();
		List<String> physicsSuiteId = new ArrayList<String>();
		List<String> chemistrySuiteId = new ArrayList<String>();
		List<String> biologySuiteId = new ArrayList<String>();
		List<String> geographySuiteId = new ArrayList<String>();
		List<String> historySuiteId = new ArrayList<String>();
		List<String> politySuiteId = new ArrayList<String>();

		List<Map<String, Object>> chineseList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> mathList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> englishList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> physicsList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> chemistryList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> biologyList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> geographyList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> historyList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> polityList = new ArrayList<Map<String, Object>>();

		for (Map<String, Object> map : resultlist) {
			if ("$SB0100".equals((String) map.get("taxOnCode"))) {
				chineseSuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				chineseList.add(map);
			} else if ("$SB0200".equals((String) map.get("taxOnCode"))) {
				mathSuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				mathList.add(map);
			} else if ("$SB0300".equals((String) map.get("taxOnCode"))) {
				englishSuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				englishList.add(map);
			} else if ("$SB0400".equals((String) map.get("taxOnCode"))) {
				physicsSuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				physicsList.add(map);
			} else if ("$SB0500".equals((String) map.get("taxOnCode"))) {
				chemistrySuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				chemistryList.add(map);
			} else if ("$SB01100".equals((String) map.get("taxOnCode"))) {
				biologySuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				biologyList.add(map);
			} else if ("$SB01000".equals((String) map.get("taxOnCode"))) {
				geographySuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				geographyList.add(map);
			} else if ("$SB0900".equals((String) map.get("taxOnCode"))) {
				historySuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				historyList.add(map);
			} else if ("$SB0800".equals((String) map.get("taxOnCode"))) {
				politySuiteId.add((String) map.get("identifier"));
				map.remove("taxOnCode");
				polityList.add(map);
			} else {
			}
		}

		//防止一级套件Id重复，做去重处理（一般情况下不会）
		HashSet<String> hsc = new HashSet<String>(chineseSuiteId);
		List<String> chineseSuiteIdRemoved= new ArrayList<String>(hsc);
		List<Map<String, Object>> subchineseList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(chineseList)) {
			recursiveSuiteDirectory(subchineseList, chineseSuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subchineseList)) {
			chineseList.addAll(subchineseList);
		}
		HashSet<String> hsm = new HashSet<String>(mathSuiteId);
		List<String> mathSuiteIdRemoved= new ArrayList<String>(hsm);
		List<Map<String, Object>> subMathList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(mathList)) {
			recursiveSuiteDirectory(subMathList, mathSuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subMathList)) {
			mathList.addAll(subMathList);
		}
		HashSet<String> hse = new HashSet<String>(englishSuiteId);
		List<String> englishSuiteIdRemoved= new ArrayList<String>(hse);
		List<Map<String, Object>> subEnglishList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(englishList)) {
			recursiveSuiteDirectory(subEnglishList, englishSuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subEnglishList)) {
			englishList.addAll(subEnglishList);
		}
		HashSet<String> hsp = new HashSet<String>(physicsSuiteId);
		List<String> physicsSuiteIdRemoved= new ArrayList<String>(hsp);
		List<Map<String, Object>> subPhysicsList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(physicsList)) {
			recursiveSuiteDirectory(subPhysicsList, physicsSuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subPhysicsList)) {
			physicsList.addAll(subPhysicsList);
		}
		HashSet<String> hsch = new HashSet<String>(chemistrySuiteId);
		List<String> chemostrySuiteIdRemoved= new ArrayList<String>(hsch);
		List<Map<String, Object>> subChemistryList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(chemistryList)) {
			recursiveSuiteDirectory(subChemistryList, chemostrySuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subChemistryList)) {
			chemistryList.addAll(subChemistryList);
		}
		HashSet<String> hsb = new HashSet<String>(biologySuiteId);
		List<String> biologySuiteIdRemoved= new ArrayList<String>(hsb);
		List<Map<String, Object>> subBiologyList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(biologyList)) {
			recursiveSuiteDirectory(subBiologyList, biologySuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subBiologyList)) {
			biologyList.addAll(subBiologyList);
		}
		HashSet<String> hsg = new HashSet<String>(geographySuiteId);
		List<String> geographySuiteIdRemoved= new ArrayList<String>(hsg);
		List<Map<String, Object>> subGeographyList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(geographyList)) {
			recursiveSuiteDirectory(subGeographyList, geographySuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subGeographyList)) {
			geographyList.addAll(subGeographyList);
		}
		HashSet<String> hsh = new HashSet<String>(historySuiteId);
		List<String> historySuiteIdRemoved= new ArrayList<String>(hsh);
		List<Map<String, Object>> subHistoryList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(historyList)) {
			recursiveSuiteDirectory(subHistoryList, historySuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subHistoryList)) {
			historyList.addAll(subHistoryList);
		}
		HashSet<String> hspo = new HashSet<String>(politySuiteId);
		List<String> politySuiteIdRemoved= new ArrayList<String>(hspo);
		List<Map<String, Object>> subPolityList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(polityList)) {
			recursiveSuiteDirectory(subPolityList, politySuiteIdRemoved, num);
		}
		if (CollectionUtils.isNotEmpty(subPolityList)) {
			polityList.addAll(subPolityList);
		}
		saveSuiteToRedis(chineseList, "$SB0100");
		saveSuiteToRedis(mathList, "$SB0200");
		saveSuiteToRedis(englishList, "$SB0300");
		saveSuiteToRedis(physicsList, "$SB0400");
		saveSuiteToRedis(chemistryList, "$SB0500");
		saveSuiteToRedis(biologyList, "$SB01100");
		saveSuiteToRedis(geographyList, "$SB01000");
		saveSuiteToRedis(polityList, "$SB0800");
		saveSuiteToRedis(historyList, "$SB0900");

		return returnList;
	}

	/**
	 * @return List<Map<String,Object>>
	 * @author yzc
	 * @date 2016年11月7日 下午5:16:30
	 * @method queryFordevAndSaveForRedis
	 */
	public List<Map<String, Object>> queryFordevAndSaveForRedis(
			String category, String status) {
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		// 1、查询一级的套件目录
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("category", category);
		param.put("status", status);
		LOG.info("查询的SQL参数:" + ObjectUtils.toJson(param));
		String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id from ndresource nd,resource_categories rc,resource_categories rc2 where ";
		if (StringUtils.hasText(status)) {
			sql += "nd.estatus=:status and ";
		}
		sql += "nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' and rc.resource=nd.identifier and nd.enable = 1 and not exists (SELECT identifier from resource_relations where res_type='assets' and resource_target_type = 'assets' and target = nd.identifier) and rc2.primary_category = 'assets' and rc2.taxOnCode=:category and rc2.resource = rc.resource order by nd.create_time";
		NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
		List<Map<String, Object>> list = npjt.queryForList(sql, param);
		returnList.addAll(list);
		// 2、分别向下遍历
		List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
		int num = 1;
		if (CollectionUtils.isNotEmpty(list)) {
			List<String> tmpIds = new ArrayList<String>();
			for (Map<String, Object> map : list) {
				String identifier = (String) map.get("identifier");
				tmpIds.add(identifier);
			}
			recursiveSuiteDirectory(subList, tmpIds, num);
		}
		if (CollectionUtils.isNotEmpty(subList)) {
			returnList.addAll(subList);
		}
		// saveResultForRedis(returnList);
		return returnList;
	}


	public List<Map<String, Object>> getCategoryBySuitId(List<String> suitIdList) {

		final List<Map<String, Object>> resourceList = new ArrayList<Map<String, Object>>();
		Map<String, Object> params = new HashMap<String, Object>();
		if (CollectionUtils.isEmpty(suitIdList)) {
			return null;
		}
		params.put("suit0", suitIdList.get(0));
		String sql = "select nd.identifier,nd.title,nd.custom_properties,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id,rc.taxOnCode from  ndresource nd ,resource_categories rc where rc.resource=nd.identifier and nd.identifier in (:suit0";
		for (int i = 1; i < suitIdList.size(); i++) {
			sql += "," + ":suit" + i;
			params.put("suit" + i, suitIdList.get(i));
		}
		sql += ") order by nd.create_time";
		LOG.info("查询的SQL语句：" + sql.toString());
		LOG.info("查询的SQL参数:" + ObjectUtils.toJson(params));

		NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(
				jt);
		namedJdbcTemplate.query(sql, params, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				Map<String, Object> sm = new HashMap<String, Object>();
				sm.put("identifier", rs.getString("identifier"));
				sm.put("title", rs.getString("title"));
				sm.put("description", rs.getString("description"));
				sm.put("status", rs.getString("status"));
				sm.put("parent", rs.getString("parent"));
				sm.put("relation_id", rs.getString("relation_id"));
				sm.put("taxOnCode", rs.getString("taxOnCode"));
				sm.put("custom_properties",rs.getString("custom_properties"));
				resourceList.add(sm);
				return null;
			}
		});
		return resourceList;
	}

	/**
	 * 根据套件递归查询最顶级套件
	 *
	 * @param suiteId
	 * @return 最顶级套件id
	 */
	@Override
	public void recursiveRootSuite(String suiteId, List<String> rootSuite,
			Map<String, Object> parent,String relationType) {

		ResourceRelation relation = new ResourceRelation();
		relation.setTarget(suiteId);
		relation.setRelationType(relationType);
		relation.setEnable(true);
		relation.setResType(IndexSourceType.AssetType.getName());
		relation.setResourceTargetType(IndexSourceType.AssetType.getName());

		String thisSuite = suiteId;
		try {
			Asset asset = assetRepository.get(thisSuite);
			ResourceRelation example = resourceRelationRepository
					.getByExample(relation);
			if (example != null) {
				suiteId = example.getSourceUuid();

				Map<String, Object> map = new LinkedHashMap<>();
				if (parent != null) {
					parent.put("identifier", thisSuite);
					parent.put("title", asset.getDescription());
					parent.put("parent", map);
				}
				recursiveRootSuite(suiteId, rootSuite, map,relationType);
			} else {
				if(relationType.equals(RelationType.COPY.getName())){
					recursiveRootSuite(suiteId, rootSuite, parent,RelationType.PARENT.getName());
				}if(relationType.equals(RelationType.PARENT.getName())){
					recursiveRootSuite(suiteId, rootSuite, parent,RelationType.ASSOCIATE.getName());
				}else{
					rootSuite.add(suiteId);
					if (parent != null) {
						parent.put("identifier", thisSuite);
						parent.put("title", asset.getDescription());
					}
				}
			}
		} catch (EspStoreException e) {
			e.printStackTrace();
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
		String sql = "SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,nd.custom_properties,rr.source_uuid as parent,rr.identifier as relation_id from resource_relations rr,ndresource nd,resource_categories rc where rr.res_type='assets' and rr.resource_target_type='assets' and rr.relation_type = 'ASSOCIATE' and rr.enable = 1 and nd.primary_category='assets' and nd.enable = 1 and rr.source_uuid in (:pids) and rr.target = nd.identifier and  nd.identifier = rc.resource and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' order by rr.sort_num";

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
						m.put("custom_properties",rs.getString("custom_properties"));
						return m;
					}
				});
		return list;
	}

	/**
	 * 从redis缓存中获取套件的树型目录
	 *
	 * @param resType
	 * @return List<Map<String,Object>>
	 * @author xm
	 * @date 2016年11月7日 下午3:53:58
	 * @method getResult
	 */
	public List<Map<String, Object>> getQuerySuiteDirectoryForRedis() {
		// key="setSuite"
		String key = "setSuite";
		// 判断key是否存在
		boolean flag = ert.existKey(key);
		if (!flag) {
			return new ArrayList<Map<String, Object>>();
		}
		// 根据分页参数取出缓存数据
		List<Map<String, Object>> redisList = ert.get(key, List.class);
		return redisList;
	}

	/**
	 * @param resType
	 * @return void
	 * @author xm
	 * @date 2016年11月7日 下午3:53:48
	 * @method saveResult
	 */
	public void saveResultForRedis(final List<Map<String, Object>> list) {
		if (!list.isEmpty()) {
			final String key = "setSuite";
			Thread saveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					// 保存到redis
					ert.set(key, list);
					// 设置过期时间
					// ert.expire(key, 1l, TimeUnit.MINUTES);
				}
			});
			executorService.execute(saveThread);

		}
	}

	@Override
	public List<Map<String, Object>> querySuiteList(String category,
			String status) {
		if (StringUtils.isNotEmpty(category)) {
			if (StringUtils.isNotEmpty(status)) {
				// 先从缓存中取出数据，然后从中取出满足status的数据
				List<Map<String, Object>> redisList = getQuerySuiteDirectoryFromRedis(category);
				if (redisList.isEmpty()) {
					List<Map<String, Object>> currentList = queryFromDBAndSaveToRedis(category);
					return getQuerySuiteByStatus(currentList, status);
				} else {
					// 从缓存中取到的结果中取出满足status的结果
					return getQuerySuiteByStatus(redisList, status);
				}
			} else {
				// 从缓存中取出数据
				List<Map<String, Object>> redisList = getQuerySuiteDirectoryFromRedis(category);
				if (redisList.isEmpty()) {
					return queryFromDBAndSaveToRedis(category);
				} else {
					return redisList;
				}
				
			}

		} else {
			if (StringUtils.isNotEmpty(status)) {
				// 先从缓存中取出数据，然后从中取出满足status的数据
				List<Map<String, Object>> redisList = getQuerySuiteDirectoryForRedis();
				if (redisList.isEmpty()) {
					List<Map<String, Object>> currentList = queryFordevAndSaveForRedis();
					return getQuerySuiteByStatus(currentList, status);
				} else {
					// 从缓存中取到的结果中取出满足status的结果
					return getQuerySuiteByStatus(redisList, status);
				}

			} else {
				// 查找redis，是否有相应的缓存数据
				List<Map<String, Object>> redisList = getQuerySuiteDirectoryForRedis();
				if (redisList.isEmpty()) {
					// 缓存没数据，则取数据库数据，并将取出的数据缓存到redis
					return queryFordevAndSaveForRedis();
				} else {
					return redisList;
				}
			}
		}
	}

	private List<Map<String, Object>> getQuerySuiteByStatus(
			List<Map<String, Object>> redisList, String status) {

		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> statusList = new ArrayList<Map<String, Object>>();
		// 获取父套件
		for (Map<String, Object> map : redisList) {
			if ("root".equals((String) map.get("parent"))
					&& status.equals((String) map.get("status"))) {
				statusList.add(map);
			}
		}
		returnList.addAll(statusList);
		// 获取父套件下的子套件
		List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
		int num = 1;
		if (CollectionUtils.isNotEmpty(statusList)) {
			List<String> tmpIds = new ArrayList<String>();
			for (Map<String, Object> map : statusList) {
				String identifier = (String) map.get("identifier");
				tmpIds.add(identifier);
			}
			recursiveSuiteDirectory(subList, tmpIds, num);
		}
		if (CollectionUtils.isNotEmpty(subList)) {
			returnList.addAll(subList);
		}
		return returnList;
	}

	private List<Map<String, Object>> queryFromDBAndSaveToRedis(String category) {

		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		Map<String, Object> param = new HashMap<String, Object>();
		// 1、查询一级的套件目录
		String sql = "SELECT nd.identifier,nd.title,nd.description,nd.custom_properties,nd.estatus as status,'root' as parent,'none' as relation_id from ndresource nd,resource_categories rc,resource_categories rc2 where nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0502' and rc.resource=nd.identifier and nd.enable = 1 and not exists (SELECT identifier from resource_relations where res_type='assets' and resource_target_type = 'assets' and target = nd.identifier) and rc2.primary_category = 'assets' and rc2.taxOnCode=:category and rc2.resource = rc.resource order by nd.create_time";
		param.put("category", category);
		LOG.info("查询的SQL参数:" + ObjectUtils.toJson(param));
		NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
		List<Map<String, Object>> list = npjt.queryForList(sql, param);
		returnList.addAll(list);
		// 2、分别向下遍历
		List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
		int num = 1;
		if (CollectionUtils.isNotEmpty(list)) {
			List<String> tmpIds = new ArrayList<String>();
			for (Map<String, Object> map : list) {
				String identifier = (String) map.get("identifier");
				tmpIds.add(identifier);
			}
			recursiveSuiteDirectory(subList, tmpIds, num);
		}
		if (CollectionUtils.isNotEmpty(subList)) {
			returnList.addAll(subList);
		}
		return returnList;
	}

	/**
	 * 根据category缓存数据到redis
	 * 
	 * @param list
	 * @return void
	 * @author yzc
	 * @date 2016年11月30日 下午3:53:48
	 * @method saveSuiteToRedis
	 */
	public void saveSuiteToRedis(final List<Map<String, Object>> list,
			String category) {
		if (!list.isEmpty()) {
			final String key = "suiteWith" + category;
			Thread saveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					ert.set(key, list);
				}
			});
			executorService.execute(saveThread);
		}
	}

	/**
	 * 根据category从redis中取数据
	 * 
	 * @param category
	 * @return List
	 * @author yzc
	 * @date 2016年11月30日 下午3:53:48
	 * @method getQuerySuiteDirectoryFromRedis
	 */
	private List<Map<String, Object>> getQuerySuiteDirectoryFromRedis(
			String category) {
		String key = "suiteWith" + category;
		// 判断key是否存在
		boolean flag = ert.existKey(key);
		if (!flag) {
			return new ArrayList<Map<String, Object>>();
		}
		List<Map<String, Object>> redisList = ert.get(key, List.class);
		return redisList;
	}

	@Override
	public List<Map<String, Object>> querySuiteDirectory(String id) {
		
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		Map<String, Object> param = new HashMap<String, Object>();
		String sql="SELECT nd.identifier,nd.title,nd.description,nd.estatus as status,'root' as parent,'none' as relation_id from ndresource nd,resource_categories rc where nd.identifier=:identifier and nd.primary_category = 'assets' and rc.primary_category='assets' and rc.taxOnCode = '$RA0502'and rc.resource=nd.identifier and nd.enable = 1";
		param.put("identifier", id);
		LOG.info("查询的SQL参数:" + ObjectUtils.toJson(param));
		NamedParameterJdbcTemplate npjt = new NamedParameterJdbcTemplate(jt);
		List<Map<String, Object>> list = npjt.queryForList(sql, param);
		returnList.addAll(list);
		//遍历子套件
		List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
		if (CollectionUtils.isNotEmpty(list)) {
			List<String> tmpIds = new ArrayList<String>();
			for (Map<String, Object> map : list) {
				String identifier = (String) map.get("identifier");
				tmpIds.add(identifier);
			}
			recursiveSuiteDirectory(subList, tmpIds, 1);
		}
		if (CollectionUtils.isNotEmpty(subList)) {
			returnList.addAll(subList);
		}
		return returnList;
	}

}
