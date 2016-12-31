package nd.esp.service.lifecycle.controllers.educationrelation.v06;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import nd.esp.service.lifecycle.educommon.models.ResourceModel;
import nd.esp.service.lifecycle.educommon.services.NDResourceService;
import nd.esp.service.lifecycle.educommon.services.impl.CommonServiceHelper;
import nd.esp.service.lifecycle.educommon.vos.ResourceViewModel;
import nd.esp.service.lifecycle.models.v06.BatchAdjustRelationOrderModel;
import nd.esp.service.lifecycle.models.v06.EducationRelationModel;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceForQuestionV06;
import nd.esp.service.lifecycle.services.educationrelation.v06.EducationRelationServiceV06;
import nd.esp.service.lifecycle.services.instructionalobjectives.v06.InstructionalObjectiveService;
import nd.esp.service.lifecycle.support.Constant;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.busi.CommonHelper;
import nd.esp.service.lifecycle.support.busi.ValidResultHelper;
import nd.esp.service.lifecycle.support.enums.LifecycleStatus;
import nd.esp.service.lifecycle.utils.BeanMapperUtils;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.utils.MessageConvertUtil;
import nd.esp.service.lifecycle.utils.StringUtils;
import nd.esp.service.lifecycle.vos.ListViewModel;
import nd.esp.service.lifecycle.vos.educationrelation.v06.EducationRelationViewModel;
import nd.esp.service.lifecycle.vos.educationrelation.v06.RelationForPathViewModel;
import nd.esp.service.lifecycle.vos.educationrelation.v06.RelationForQueryViewModel;
import nd.esp.service.lifecycle.vos.educationrelation.v06.RelationForQueryViewModel4Business;
import nd.esp.service.lifecycle.vos.statics.CoverageConstant;
import nd.esp.service.lifecycle.vos.valid.CreateEducationRelationDefault;
import nd.esp.service.lifecycle.vos.valid.UpdateEducationRelationDefault;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.nd.gaea.client.http.WafSecurityHttpClient;

/**
 * 教育资源关系Controller(V0.6--增加生命周期)
 * 
 * @author caocr
 *
 */
@RestController
@RequestMapping("/v0.6/{res_type}")
public class EducationRelationControllerV06 {
    private final static Logger LOG = LoggerFactory.getLogger(EducationRelationControllerV06.class);
    
    @Autowired
    @Qualifier("educationRelationServiceV06")
    private EducationRelationServiceV06 educationRelationService;
    
    @Autowired
    @Qualifier("educationRelationServiceForQuestionV06")
    private EducationRelationServiceForQuestionV06 educationRelationServiceForQuestion;

    @Autowired
    private InstructionalObjectiveService instructionalObjectiveService;
    
    @Autowired
    private NDResourceService ndResourceService;
    
    @Autowired
    CommonServiceHelper commonServiceHelper;
    
    @Autowired
    private JdbcTemplate jt;
    
    
    /**
     * 创建资源关系
     * 
     * @param resType                              源资源类型
     * @param sourceUuid                           源资源的id
     * @param educationRelationModel               创建时的入参
     * @param bindingResult                        入参校验的绑定结果
     * @return
     * @since
     */
    @RequestMapping(value = "/{source_uuid}/relations", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public EducationRelationViewModel createRelation(@PathVariable(value="res_type") String resType,@PathVariable(value="source_uuid") String sourceUuid,
                                              @Validated(CreateEducationRelationDefault.class) @RequestBody EducationRelationViewModel educationRelationViewModel,BindingResult bindingResult){
        // 校验入参
        ValidResultHelper.valid(bindingResult,
                                "LC/CREATE_RELATION_PARAM_VALID_FAIL",
                                "EducationRelationControllerV06",
                                "createRelation");

        //数据模型转换
        EducationRelationModel educationRelationModel = BeanMapperUtils.beanMapper(educationRelationViewModel,
                                                                                   EducationRelationModel.class);
        educationRelationModel.setResType(resType);
        educationRelationModel.setSource(sourceUuid);
        
        List<EducationRelationModel> educationRelationModels = new ArrayList<EducationRelationModel>();
        educationRelationModels.add(educationRelationModel);
        // 创建资源关系
        List<EducationRelationModel> resultList = null;
        if (CommonServiceHelper.isQuestionDb(resType)) {
            resultList = educationRelationServiceForQuestion.createRelation(educationRelationModels, false);
        } else {
            resultList = educationRelationService.createRelation(educationRelationModels, false);
        }
        
        if(CollectionUtils.isEmpty(resultList) || resultList.size() != 1){
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.CreateEducationRelationFail);
        }
        
        //返回处理,这里只会有一条记录
        educationRelationViewModel = BeanMapperUtils.beanMapper(resultList.get(0), EducationRelationViewModel.class);
        //不需要显示
        educationRelationViewModel.setResourceTargetType(null);
        
        if (LOG.isInfoEnabled()) {
            LOG.info("资源关系V0.6创建成功,resType:{},sourceId:{},targetType:{},targetId:{},relationType:{}",
                     resType,
                     sourceUuid,
                     educationRelationViewModel.getResourceTargetType(),
                     educationRelationViewModel.getTarget(),
                     educationRelationViewModel.getRelationType());
        }
        
        return educationRelationViewModel;
    }
    
    
    /**
     * 批量创建资源关系
     * 
     * @param resType                              源资源类型
     * @param sourceUuid                           源资源的id
     * @param educationRelationModel               创建时的入参
     * @param bindingResult                        入参校验的绑定结果
     * @return
     * @since
     */
    @RequestMapping(value = "/{source_uuid}/relations/batch", method = RequestMethod.POST, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public List<EducationRelationViewModel> batchCreateRelation(@PathVariable(value="res_type") String resType,@PathVariable(value="source_uuid") String sourceUuid,
                                              @Validated(CreateEducationRelationDefault.class) @RequestBody List<EducationRelationViewModel> educationRelationViewModels,BindingResult bindingResult){
        // 校验入参
        ValidResultHelper.valid(bindingResult,
                                "LC/CREATE_RELATION_PARAM_VALID_FAIL",
                                "EducationRelationControllerV06",
                                "batchCreateRelation");
        List<EducationRelationModel> educationRelationModels = new ArrayList<EducationRelationModel>();
        
        //数据模型转换
        if(CollectionUtils.isNotEmpty(educationRelationViewModels)){
        	for (EducationRelationViewModel educationRelationViewModel : educationRelationViewModels) {
        		if(StringUtils.isEmpty(educationRelationViewModel.getResourceTargetType())){
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    		"LC/CHECK_PARAM_VALID_FAIL",
                            "resourceTargetType不能为空");
        		}
        		if(StringUtils.isEmpty(educationRelationViewModel.getTarget())){
                    throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    		"LC/CHECK_PARAM_VALID_FAIL",
                            "target不能为空");
        		}
        		EducationRelationModel educationRelationModel = BeanMapperUtils.beanMapper(educationRelationViewModel,
                        EducationRelationModel.class);
                educationRelationModel.setResType(resType);
                educationRelationModel.setSource(sourceUuid);
        		educationRelationModels.add(educationRelationModel);
			}
        }
        // 创建资源关系
        List<EducationRelationModel> resultList = null;
        if (CommonServiceHelper.isQuestionDb(resType)) {
            resultList = educationRelationServiceForQuestion.createRelation(educationRelationModels, false);
        } else {
            resultList = educationRelationService.createRelation(educationRelationModels, false);
        }
        
        //返回处理,这里只会有一条记录
        List<EducationRelationViewModel> returnList = new ArrayList<EducationRelationViewModel>();
        if(CollectionUtils.isNotEmpty(resultList)){
        	for (EducationRelationModel erm : resultList) {
        		EducationRelationViewModel ervm = BeanMapperUtils.beanMapper(erm, EducationRelationViewModel.class);
        		returnList.add(ervm);
			}
        }
        return returnList;
    }
    
    /**
     * 修改资源关系
     * 
     * @param resType                                       源资源类型
     * @param sourceUuid                                    源资源的id
     * @param rid                                           资源关系id
     * @param educationRelationForUpdateModel               修改时传入的参数
     * @param bindingResult                                 入参校验的绑定结果
     * @return
     */
    @RequestMapping(value = "/{source_uuid}/relations/{rid}", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public EducationRelationViewModel updateRelation(@PathVariable(value="res_type") String resType,@PathVariable(value="source_uuid") String sourceUuid, 
            @PathVariable(value="rid") String rid,@Validated(UpdateEducationRelationDefault.class) @RequestBody EducationRelationViewModel educationRelationViewModel,BindingResult bindingResult) {
        //校验入参
        ValidResultHelper.valid(bindingResult,
                                "LC/UPDATE_RELATION_PARAM_VALID_FAIL",
                                "EducationRelationControllerV06",
                                "updateRelation");
        
        // 数据模型转换
        EducationRelationModel educationRelationModel = BeanMapperUtils.beanMapper(educationRelationViewModel,
                                                                                   EducationRelationModel.class);
        
        //修改资源关系
        if (CommonServiceHelper.isQuestionDb(resType)) {
            educationRelationModel = educationRelationServiceForQuestion.updateRelation(resType, sourceUuid, rid, educationRelationModel);
        } else {
            educationRelationModel = educationRelationService.updateRelation(resType, sourceUuid, rid, educationRelationModel);
        }
        
        educationRelationViewModel = BeanMapperUtils.beanMapper(educationRelationModel, EducationRelationViewModel.class);
        // 不需要显示
        educationRelationViewModel.setResourceTargetType(null);
        
        if(LOG.isInfoEnabled()){
            LOG.info(
                    "资源关系V0.6修改成功,resType:{},sourceId:{},targetType:{},targetId:{},relationType:{}",
                    resType, sourceUuid,
                    educationRelationViewModel.getResourceTargetType(),
                    educationRelationViewModel.getTarget(),
                    educationRelationViewModel.getRelationType());
        }
        
        return educationRelationViewModel;
    }
    
    /**
     * 删除资源关系   
     * 
     * @param rid      资源关系id
     * @return
     */
    @RequestMapping(value = "/{source_uuid}/relations/{rid}", method = RequestMethod.DELETE)
    public @ResponseBody Map<String, String> deleteRelation(
            @PathVariable(value="res_type") String resType,@PathVariable(value="source_uuid") String sourceUuid,
            @PathVariable(value="rid") String rid) {
        boolean flag = true;
        if (CommonServiceHelper.isQuestionDb(resType)) {
            flag = educationRelationServiceForQuestion.deleteRelation(rid, sourceUuid, resType);
        } else {
            flag = educationRelationService.deleteRelation(rid, sourceUuid, resType);
        }
        if (!flag) {
            return MessageConvertUtil
                    .getMessageString(LifeCircleErrorMessageMapper.DeleteEducationRelationFail);
        }
        
        if(LOG.isInfoEnabled()){
            LOG.info("资源关系V0.6删除关系成功,rid:{}",rid);
        }
        
        return MessageConvertUtil
                .getMessageString(LifeCircleErrorMessageMapper.DeleteEducationRelationSuccess);
    }
    
    /**
     * 条件删除资源之间的关系  
     * 
     * @param resType          源资源类型
     * @param sourceUuid       源资源id
     * @param target           目标对象的id集合
     * @param relationType     关系类型
     * @param reverse          指定查询的关系是否进行反向查询。如果reverse参数不携带，默认是从s->t查询，如果reverse=true，反向查询T->S
     * @return
     */
    @RequestMapping(value = "/{source_uuid}/relations", method = RequestMethod.DELETE)
    public @ResponseBody Map<String, String> deleteRelationByTarget(
           @PathVariable(value="res_type") String resType,@PathVariable(value="source_uuid") String sourceUuid,
           @RequestParam(required=false)List<String> target,
           @RequestParam(required=false,value="relation_type")String relationType,
           @RequestParam(required=false) String reverse){
        boolean reverseBoolean=false;
        if("true".equals(reverse)){
            reverseBoolean=true;
        }
        
        boolean flag = true;
        boolean flag1 = true;
        boolean flag2 = true;
        if (reverseBoolean) {//当反转时，调两次接口
            flag1 = educationRelationServiceForQuestion.deleteRelationByTarget(resType,
                                                                               sourceUuid,
                                                                               target,
                                                                               relationType,
                                                                               reverseBoolean);
            flag2 = educationRelationService.deleteRelationByTarget(resType,
                                                                    sourceUuid,
                                                                    target,
                                                                    relationType,
                                                                    reverseBoolean);
            flag = flag1 && flag2;
        } else {
            if (CommonServiceHelper.isQuestionDb(resType)) {
                flag = educationRelationServiceForQuestion.deleteRelationByTarget(resType,
                                                                                  sourceUuid,
                                                                                  target,
                                                                                  relationType,
                                                                                  reverseBoolean);
            } else {
                flag = educationRelationService.deleteRelationByTarget(resType,
                                                                       sourceUuid,
                                                                       target,
                                                                       relationType,
                                                                       reverseBoolean);
            }
        }
        
        if (!flag) {
            return MessageConvertUtil
                    .getMessageString(LifeCircleErrorMessageMapper.DeleteBatchRelationFail);
        }
        return MessageConvertUtil
                .getMessageString(LifeCircleErrorMessageMapper.DeleteBatchRelationSuccess);
    }
    
    /**
     * 根据目标类型删除资源之间的关系  
     * 
     * @param resType          源资源类型
     * @param sourceUuid       源资源id
     * @param targetType       目标对象类型
     * @param relationType     关系类型
     * @param reverse          指定查询的关系是否进行反向查询。如果reverse参数不携带，默认是从s->t查询，如果reverse=true，反向查询T->S
     * @return
     */
    @RequestMapping(value = "/{source_uuid}/relations", method = RequestMethod.DELETE,params={"target_type"})
    public @ResponseBody Map<String, String> deleteRelationByTargetType(
           @PathVariable(value="res_type") String resType,@PathVariable(value="source_uuid") String sourceUuid,
           @RequestParam(value="target_type")List<String> targetType,
           @RequestParam(required=false,value="relation_type")String relationType,
           @RequestParam(required=false) String reverse){
        boolean reverseBoolean=false;
        if("true".equals(reverse)){
            reverseBoolean=true;
        }
        
        boolean flag = true;
        boolean flag1 = true;
        boolean flag2 = true;
        if (reverseBoolean) {//当反转时，调两次接口
            flag1 = educationRelationServiceForQuestion.deleteRelationByTargetType(resType,
                                                                                   sourceUuid,
                                                                                   targetType,
                                                                                   relationType,
                                                                                   reverseBoolean);
            flag2 = educationRelationService.deleteRelationByTargetType(resType,
                                                                        sourceUuid,
                                                                        targetType,
                                                                        relationType,
                                                                        reverseBoolean);
            flag = flag1 && flag2;
        } else {
            if (CommonServiceHelper.isQuestionDb(resType)) {
                flag = educationRelationServiceForQuestion.deleteRelationByTargetType(resType,
                                                                                      sourceUuid,
                                                                                      targetType,
                                                                                      relationType,
                                                                                      reverseBoolean);
            } else {
                flag = educationRelationService.deleteRelationByTargetType(resType,
                                                                           sourceUuid,
                                                                           targetType,
                                                                           relationType,
                                                                           reverseBoolean);
            }
        }
        if (!flag) {
            return MessageConvertUtil
                    .getMessageString(LifeCircleErrorMessageMapper.DeleteBatchRelationFail);
        }
        return MessageConvertUtil
                .getMessageString(LifeCircleErrorMessageMapper.DeleteBatchRelationSuccess);
    }
    
    /**
     * 获取资源之间的关系
     *     
     * @param id                   源资源的id标识
     * @param relationPath         查询的关系路径
     * @param reverse              关系是否进行反转
     * @param categoryPattern      分类维度的应用模式
     * @return
     */
    @RequestMapping(value = "/{id}/relations", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
    public @ResponseBody List<List<RelationForPathViewModel>> getRelationsByConditions(
           @PathVariable(value="res_type") String resType,@PathVariable String id,
           @RequestParam(value="relation_path")String relationPath,
           @RequestParam(required=false) boolean reverse,
           @RequestParam(required=false,value="category_pattern")String categoryPattern){
        List<List<RelationForPathViewModel>> resultList = 
                educationRelationService.getRelationsByConditions(resType, id, Arrays.asList(relationPath.split("/")), reverse, categoryPattern);
                
        return resultList;
    }
    
    /**
     * 批量修改资源关系的顺序  
     * 
     * @param resType          元资源的类型
     * @param sourceUuid       源资源的id
     * @param target           需要移动的目标对象
     * @param destination      移动目的地靶心对象
     * @param adjoin           相邻对象的id，如果在第一个和最后一个的时候，不存在相邻对象，传入为none。
     * @param at               移动的方向标识，first是移动到第一个位置，last是将这个关系增加到列表的最后，middle是将目标增加到destination和adjoin中间。
     */
    @RequestMapping(value = "/{source_uuid}/relations/order", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public void batchAdjustRelationOrder(@PathVariable(value = "res_type") String resType,
                                         @PathVariable(value = "source_uuid") String sourceUuid,
                                         @Valid @RequestBody List<BatchAdjustRelationOrderModel> batchAdjustRelationOrderModels,
                                         BindingResult bindingResult) {
        // 调用service
        if (CommonServiceHelper.isQuestionDb(resType)) {
            educationRelationServiceForQuestion.batchAdjustRelationOrder(resType,
                                                                         sourceUuid,
                                                                         batchAdjustRelationOrderModels);
        } else {
            educationRelationService.batchAdjustRelationOrder(resType, sourceUuid, batchAdjustRelationOrderModels, false,null);
        }
    }
    
    /**
     * 智慧知识业务接口 - 关系移动
     * @author xiezy
     * @date 2016年11月14日
     * @param resType
     * @param batchAdjustRelationOrderModels
     * @param bindingResult
     */
    @RequestMapping(value = "/relations/order/move/business", method = RequestMethod.PUT, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE })
    public void batchAdjustRelationOrder4Business(@PathVariable(value = "res_type") String resType,
                                         @Valid @RequestBody List<BatchAdjustRelationOrderModel> batchAdjustRelationOrderModels,
                                         BindingResult bindingResult,
                                         @RequestParam(required=false,value="parent") String parent,
                                         @RequestParam(required=false,defaultValue="false",value="refresh_suite") boolean refreshSuite) {
    	
    	educationRelationService.batchAdjustRelationOrder(resType, null, batchAdjustRelationOrderModels, true,parent);
    	
    	//异步更新套件目录缓存
    	if(refreshSuite){
    		CommonHelper.getPrimaryExecutorService().execute(new Runnable() {
    			@Override
    			public void run() {
    				educationRelationService.queryFordevAndSaveForRedis();
    			}
    		});
    	}
    }
    
    /**
     * 关系目标资源检索 
     * <p>Create Time: 2015年5月18日   </p>
     * @param resType          源资源类型
     * @param sourceUuid       源资源id
     * @param categories       分类维度数据
     * @param targetType       目标资源类型
     * @param label            资源关系标识
     * @param tags             资源关系标签
     * @param relationType     关系类型
     * @param limit            分页参数，第一个值为记录索引参数，第二个值为偏移量
     * @param reverse          指定查询的关系是否进行反向查询。如果reverse参数不携带，默认是从s->t查询，如果reverse=true，反向查询T->S
     * @param recursion        是否根据源资源进行递归查询,举例：通过给定的章节id，递归查询其子章节下的所有知识点信息(新增参数)
     * @param ctType           指定覆盖范围的查询类型，具体选值是Org，Role，User，Time，Space，Group
     * @param ct               指定覆盖范围的查询类型，可以是VIEW，PLAY，SHAREING，REPORTING,COPY，NONE
     * @param cTarget          指定查询覆盖范围目标的具体值
     * @return
     */
    @RequestMapping(value = "/{source_uuid}/targets", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
    public @ResponseBody ListViewModel<RelationForQueryViewModel> searchByResType(@PathVariable(value = "res_type") String resType,
                                                                                  @PathVariable(value = "source_uuid") String sourceUuid,
                                                                                  @RequestParam(required = false) String categories,
                                                                                  @RequestParam(value = "target_type") String targetType,
                                                                                  @RequestParam(required = false) String label,
                                                                                  @RequestParam(required = false, value="relation_tags") String tags,
                                                                                  @RequestParam(value = "relation_type") String relationType,
                                                                                  @RequestParam String limit,
                                                                                  @RequestParam(required = false) String reverse,
                                                                                  @RequestParam(required = false) String recursion,
                                                                                  @RequestParam(required = false, value = "ct_type") String ctType,
                                                                                  @RequestParam(required = false) String ct,
                                                                                  @RequestParam(required = false, value = "ct_target") String cTarget,
                                                                                  @RequestParam(required=false,value="recycled",defaultValue="false") boolean isRecycled,
                                                                                  HttpServletRequest request) {
        if (StringUtils.isEmpty(targetType)) {

            LOG.error("目标资源类型必须要传值，不能为空");

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                          LifeCircleErrorMessageMapper.CheckTargetTypeIsNull);
        }
        
        //FIXME 临时方案,后期需要考虑去掉,可能会影响查询效率
        boolean isPortal = false;
        String bsyskey = request.getHeader(Constant.BSYSKEY);
        if(StringUtils.isNotEmpty(bsyskey) && bsyskey.equals(Constant.BSYSKEY_PORTAL)){
        	isPortal = true;
        }
        
        //覆盖范围参数处理
        if(StringUtils.isEmpty(ctType)){
            ctType = "*";
        }else{
            if(!CoverageConstant.isCoverageTargetType(ctType,true)){
                
                LOG.error("覆盖范围类型不在可选范围内");
                
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.CoverageTargetTypeNotExist);
            }
        }
        if(StringUtils.isEmpty(ct)){
            ct = "*";
        }else{
            if(!CoverageConstant.isCoverageStrategy(ct,true)){
                
                LOG.error("资源操作类型不在可选范围内");
                
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.CoverageStrategyNotExist);
            }
        }
        if(StringUtils.isEmpty(cTarget)){
            cTarget = "*";
        }
        String coverage = ctType + "/" + cTarget + "/" + ct;
        
        if(coverage.equals("*/*/*")){
            coverage = null;
        }
        
        //反向查询boolean,默认为false
        boolean reverseBoolean = false;
        if("true".equals(reverse)){
            reverseBoolean = true;
        }
        //递归查询boolean,默认为false
        boolean recursionBoolean = false;
        if("true".equals(recursion)){
            recursionBoolean = true;
        }
        
        limit = CommonHelper.checkLimitMaxSize(limit);
        
        
        ListViewModel<RelationForQueryViewModel> listViewModel = null;
        try {
            if(!recursionBoolean){
                listViewModel = educationRelationService.queryListByResTypeByDB(
                                   resType, sourceUuid, categories, targetType, label, tags, relationType, limit, reverseBoolean, coverage, isPortal, isRecycled);
            }else if(IndexSourceType.ChapterType.getName().equals(resType)){
                listViewModel = educationRelationService.recursionQueryResourcesByDB(
                        resType, sourceUuid, categories, targetType, label, tags, relationType, limit, coverage, isPortal);
            }else{
                
                LOG.error("递归查询res_type目前仅支持chapters");
                
                throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                        LifeCircleErrorMessageMapper.RelationSupportTypeError);
            }
        } catch (EspStoreException e) {
            LOG.error("通过资源关系获取资源列表失败",e);
            
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.GetEducationRelationListFail.getCode(),e.getMessage());
        }
        if (null == listViewModel.getItems()) {
            return listViewModel;
        }
        // 如果是教学目标，则根据教学目标类型与知识点设置title
        if ((!"true".equals(reverse) && targetType.equals(IndexSourceType.InstructionalObjectiveType.getName()))
                || ("true".equals(reverse) && resType.equals(IndexSourceType.InstructionalObjectiveType.getName()))) {

            Collection<Map.Entry<String, String>> ids = Collections2.transform(listViewModel.getItems(), new Function<RelationForQueryViewModel, Map.Entry<String, String>>() {
                @Nullable
                @Override
                public Map.Entry<String, String> apply(RelationForQueryViewModel model) {
                    return new HashMap.SimpleEntry<>(model.getIdentifier(), model.getTitle());
                }
            });

            Map<String, String> result = instructionalObjectiveService.getInstructionalObjectiveTitle(ids);

            for (RelationForQueryViewModel model : listViewModel.getItems()) {
                String title = result.get(model.getIdentifier());
                model.setTitle(null == title ? model.getTitle():title);
            }

        }

        return listViewModel;
    }
    
    /**
     * 在有些情景下，单个的获取源资源的目标资源列表的接口，业务系统使用起来过于频繁。此时业务方提出需要能够进行设置批量的源资源ID，
     * 通过源资源的ID快速的查询目标资源的列表。
                 1.接口提供设置源资源ID的列表进行批量查询
                 2.接口提供设置关系的类型
                 3.接口提供设置目标资源的类型	
     * <p>Create Time: 2015年10月19日   </p>
     * <p>Create author: xiezy   </p>
     * @param resType 源资源类型
     * @param sids 源资源id，可批量
     * @param targetType 目标资源类型
     * @param label            资源关系标识
     * @param tags             资源关系标签
     * @param relationType 关系类型
     * @param limit 分页参数
     */
    @RequestMapping(value = "/resources/relations/targets/bulk", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
    public @ResponseBody ListViewModel<RelationForQueryViewModel> batchQueryResources(@PathVariable(value="res_type") String resType,
            @RequestParam(value="sid") Set<String> sids,
            @RequestParam(value="target_type") String targetType,
            @RequestParam(required = false) String label,
            @RequestParam(required = false, value="relation_tags") String tags,
            @RequestParam(required=false,value="relation_type") String relationType,
            @RequestParam String limit,
            @RequestParam(required=false) boolean reverse,
            @RequestParam(required=false) String category,
            @RequestParam(required=false,value="recycled",defaultValue="false") boolean isRecycled,
            HttpServletRequest request){
    	
        if (StringUtils.isEmpty(targetType)) {

            LOG.error("目标资源类型必须要传值，不能为空");

            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                                          LifeCircleErrorMessageMapper.CheckTargetTypeIsNull);
        }
        
        limit = CommonHelper.checkLimitMaxSize(limit);
        
        //FIXME 临时方案,后期需要考虑去掉,可能会影响查询效率
        boolean isPortal = false;
        String bsyskey = request.getHeader(Constant.BSYSKEY);
        if(StringUtils.isNotEmpty(bsyskey) && bsyskey.equals(Constant.BSYSKEY_PORTAL)){
        	isPortal = true;
        }
        
//        return educationRelationService.batchQueryResources(resType, sids, targetType, relationType, limit);
        ListViewModel<RelationForQueryViewModel> modelList = educationRelationService.batchQueryResourcesByDB(resType, sids, targetType, label, tags, relationType, limit,category,reverse, isPortal, isRecycled);

        if (null == modelList.getItems()) {
            return modelList;
        }

        // 如果是教学目标，则根据教学目标类型与知识点设置title
        if ((reverse && resType.equals(IndexSourceType.InstructionalObjectiveType.getName()))
                || (!reverse && targetType.equals(IndexSourceType.InstructionalObjectiveType.getName()))) {

            Collection<Map.Entry<String, String>> idWithTitles = Collections2.transform(modelList.getItems(), new Function<RelationForQueryViewModel, Map.Entry<String, String>>() {
                @Nullable
                @Override
                public Map.Entry<String, String> apply(RelationForQueryViewModel model) {
                    return new HashMap.SimpleEntry<>(model.getIdentifier(), model.getTitle());
                }
            });

            Map<String, String> result = instructionalObjectiveService.getInstructionalObjectiveTitle(idWithTitles);

            for (RelationForQueryViewModel model : modelList.getItems()) {
                String title = result.get(model.getIdentifier());
                model.setTitle(null == title ? model.getTitle():title);
            }

        }

        return modelList;
    }
    
    /**
     * 业务批量查询关系接口，多返回CG属性
     */
    @RequestMapping(value = "/resources/relations/targets/bulk4Business", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
    public @ResponseBody ListViewModel<RelationForQueryViewModel4Business> batchQueryResources4Business(@PathVariable(value="res_type") String resType,
            @RequestParam(value="sid") Set<String> sids,
            @RequestParam(value="target_type") String targetType,
            @RequestParam(required = false) String label,
            @RequestParam(required = false, value="relation_tags") String tags,
            @RequestParam(required=false,value="relation_type") String relationType,
            @RequestParam String limit,
            @RequestParam(required=false) boolean reverse,
            @RequestParam(required=false) String category,
            @RequestParam(required=false,value="recycled",defaultValue="false") boolean isRecycled,
            HttpServletRequest request){
    		ListViewModel<RelationForQueryViewModel4Business> returnValue = new ListViewModel<RelationForQueryViewModel4Business>();
    		ListViewModel<RelationForQueryViewModel> list = batchQueryResources(resType, sids, targetType, label, tags, relationType, limit, reverse, category, isRecycled, request);
    		returnValue.setLimit(list.getLimit());
    		returnValue.setTotal(list.getTotal());
    		if(list != null && CollectionUtils.isNotEmpty(list.getItems())){
    			List<RelationForQueryViewModel4Business> businessList = new LinkedList<RelationForQueryViewModel4Business>();
    			Set<String> uuidSet = new HashSet<String>();
	    		for (RelationForQueryViewModel tmp : list.getItems()) {
					uuidSet.add(tmp.getIdentifier());
				}
	    		List<String> includeList = new ArrayList<String>();
	        	includeList.add("CG");
	            List<ResourceModel> l = ndResourceService.batchDetail(targetType, uuidSet, includeList);
            	for (RelationForQueryViewModel tmp : list.getItems()) {
            		for (ResourceModel resourceModel : l) {
            			ResourceViewModel rvm = CommonHelper.changeToView(resourceModel,targetType,includeList,commonServiceHelper);
						if(rvm.getIdentifier().equals(tmp.getIdentifier())){
							RelationForQueryViewModel4Business model = BeanMapperUtils.beanMapper(tmp, RelationForQueryViewModel4Business.class);
							model.setCategories(rvm.getCategories());
							businessList.add(model);
						}
            		}
				}
            	returnValue.setItems(businessList);
	    	}
    		return returnValue;
    }
    
    /**
     * 根据知识点id获取上级节点（直到一级知识点为止）
     * 根据知识点id获取同级节点
     * @param knowledgeId
     * @return
     */
    @RequestMapping(value = "/tree/{uuid}", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
    public List<Map<String,Object>> queryKnowledgeTree(@PathVariable(value="res_type") String resType,@PathVariable(value="uuid") String knowledgeId){
    	if(IndexSourceType.KnowledgeType.getName().equals(resType)){
    		return educationRelationService.queryKnowledgeTree(knowledgeId);
    	}
    	return null;
    }
    
    /**
     * 查询套件目录树
     * @return
     */
//    @RequestMapping(value = "/tree/suiteDirectory", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
//    public List<Map<String,Object>> querySuiteDirectoryTree(@PathVariable(value="res_type") String resType,
//    		@RequestParam(value = "nd_code", required = false) String categoryList,
//    		@RequestParam(value = "status", required = false )String status){
//    	//对status做判断
//    	if (StringUtils.hasText(status)){
//		    if (!LifecycleStatus.isLegalStatus(status)) {
//            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    LifeCircleErrorMessageMapper.StatusIsNotExist.getCode(),
//                    "root套件的" + status + "--status必须为有效值");
//           }
//    	}  	
//    	
//    	if(IndexSourceType.AssetType.getName().equals(resType)){			
//    		if(StringUtils.hasText(categoryList)){
//    			return educationRelationService.querySuiteDirectory(categoryList,status);
//        	}
//    		return educationRelationService.querySuiteDirectory();
//    	}
//    	return null;
//    }
    
    /**
     * 查询套件目录树
     * @param resType           资源类型
     * @param categoryList   套件学科分类维度
     * @param status             资源状态
     * @return
     */
    @RequestMapping(value = "/tree/suiteDirectory", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
    public List<Map<String,Object>> querySuiteList(
    		@PathVariable(value="res_type") String resType,
    		@RequestParam(value = "nd_code", required = false) String categoryList,
    		@RequestParam(value = "status", required = false )String status){
    	//对status做判断
    	if (StringUtils.hasText(status)){
		    if (!LifecycleStatus.isLegalStatus(status)) {
            throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    LifeCircleErrorMessageMapper.StatusIsNotExist.getCode(),
                    "root套件的" + status + "--status必须为有效值");
           }
    	}  	
    	if(IndexSourceType.AssetType.getName().equals(resType)){			
    		
    		return educationRelationService.querySuiteList(categoryList,status);
    	}
    	return null;
    }
    
    /**
     * 根据套件id查询套件下所有套件树
     * @param resType           资源类型
     * @param categoryList   套件学科分类维度
     * @return
     */
    @RequestMapping(value="/tree/suite/{id}",method=RequestMethod.GET,produces={MediaType.APPLICATION_JSON_VALUE})
    public List<Map<String,Object>> querySuiteListBySuiteId(@PathVariable String id){
    	
    	//检验id是否合法
    	if(!CommonHelper.checkUuidPattern(id)){
    		   throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
                       LifeCircleErrorMessageMapper.CheckIdentifierFail);
    	}
    	 
    	return educationRelationService.querySuiteDirectory(id);
    }
    
    /**
     * 刷新redis缓存
     * @return
     */
    @RequestMapping(value="/refresh/redis",method=RequestMethod.GET,produces={MediaType.APPLICATION_JSON_VALUE})
    public Map<String,String> refreshRedis(){ 	
    	educationRelationService.queryFordevAndSaveForRedis();
    	Map<String,String>map=new HashMap<String, String>();
    	map.put("process_state","缓存刷新成功");
    	map.put("process_code", "RefreshRedisSuccess");
		return map;
    	 
    }
     
    
    /**
     * 查询ND学习目标库中已有的套件的学习目标类型的数量和样例中学习目标的数量，并导出为excel
     * @return
     */
    @RequestMapping(value = "/tree/suiteDirectoryNumPrint", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
    public List<Map<String, Object>> querySuiteDirectoryTreeNumPrintExcel(@PathVariable(value="res_type") String resType){
    	     	   
    	WafSecurityHttpClient wafSecurityHttpClient = new WafSecurityHttpClient();
    	//查找出所有的套件
    	String qurUrl=Constant.LIFE_CYCLE_API_URL + "/assets/tree/suiteDirectory";
    	List<Map<String,Object>> suiteList = wafSecurityHttpClient.get(qurUrl, List.class);
    	//查找根套件（如套件1）
    	List<Map<String,Object>> rootSuiteList = new ArrayList<Map<String,Object>>();
    	for (Map<String, Object> map : suiteList) {
    		Map<String, Object> tempMap = new HashMap<String, Object>();
			if ("root".equals(map.get("parent"))) {
				tempMap.put("identifier", map.get("identifier"));
				tempMap.put("title", map.get("title"));
				rootSuiteList.add(tempMap);
			}
		}
    	
    	
    	//查找某套件下所有套件，包括它本身，如（套件1，套件1.1，套件1.2）（套件2，套件2.1，套件2.2）
    	List<List<Map<String,Object>>> rootSubSuiteList=new ArrayList<List<Map<String,Object>>>();  
    
    	for (Map<String, Object> rootSuiteMap : rootSuiteList) {   //循环的次数也就是size了，那假设一个i，每次循环加1
    		String identify = (String) rootSuiteMap.get("identifier");
    		String title = (String) rootSuiteMap.get("title");
    		
    		List<Map<String,Object>> tempList = new ArrayList<Map<String,Object>>();
   	       		  		
    		// 正则  ^套件+根套件数字+(.[0-9])*$,若为这种就可以匹配并加进来,注意java要加\\
    		 String regex="^套件"+title.substring(2)+"(\\.[0-9]+)*$";
    		
			 for (Map<String, Object> map : suiteList) {				 
				String strRegex = (String) map.get("title");				 
				if (Pattern.compile(regex).matcher(strRegex).find()) {      //改一下判断条件，正则匹配了就可以了，改下if判断
					Map<String, Object> tempMap=new HashMap<String, Object>();
					tempMap.put("identifier", map.get("identifier"));
					tempMap.put("title", strRegex);
					tempMap.put("parenttitle", title);
		
					tempList.add(tempMap);
				}		
			}
			if (!tempList.isEmpty()) {
				rootSubSuiteList.add(tempList); 
			}	
		}
    	
    	//查询记录套件的total和items里面的identifier，如套件1下面的所有（1.1）
    	List<Map<String,Object>> suiteTempletListMaps = new ArrayList<Map<String,Object>>();    	
    	for (List<Map<String, Object>> list : rootSubSuiteList) {
    		boolean isRun=false;
    		qurUrl = Constant.LIFE_CYCLE_API_URL+"/assets/resources/relations/targets/bulk?target_type=assets&relation_type=ASSOCIATE&limit=(0,20)&reverse=false&category=$RA0503";
        	StringBuffer qurUrlBuffer =new StringBuffer(qurUrl);
        	String parenttitle=(String) list.get(0).get("parenttitle");
        	
        	Map<String, Object> tempMap = new HashMap<String, Object>();;
			for (Map<String, Object> map : list) {				
				String sid=(String)map.get("identifier");
				if (!sid.isEmpty()) {
				qurUrlBuffer.append("&sid="+sid);
				isRun=true;
				}								
			}
			
			String templetSql=qurUrlBuffer.toString();
			ListViewModel<ResourceViewModel> suiteTempletList=null;
			List<ResourceViewModel> itemList=null;
			
			if (isRun) {
				suiteTempletList = wafSecurityHttpClient.get(templetSql,ListViewModel.class);
				itemList = suiteTempletList.getItems();
				tempMap.put("insobjtotal", suiteTempletList.getTotal());
				tempMap.put("items", itemList);
			}else {
				tempMap.put("items", new ArrayList<HashMap<String, Object>>());
				tempMap.put("insobjtotal", "0");
			}	      
			tempMap.put("parenttitle", parenttitle);
			suiteTempletListMaps.add(tempMap);	
						
		}
    	
    	//items来查找instrucionObjecttives
    	List<Map<String,Object>> resultList = new ArrayList<Map<String,Object>>();  
    	
		for (Map<String, Object> map : suiteTempletListMaps) {
			
			boolean isRun=false;
			@SuppressWarnings("unchecked")
			List<HashMap<String, Object>> itemsList = (List<HashMap<String, Object>>) (map.get("items"));
		
			String insobjtotal =map.get("insobjtotal")+"";
			
			Map<String, Object> tempMap = new HashMap<String, Object>();

			qurUrl = Constant.LIFE_CYCLE_API_URL
					+ "/assets/resources/relations/targets/bulk?target_type=instructionalobjectives&relation_type=ASSOCIATE&limit=(0,20)&reverse=false";
			StringBuffer qurUrlBuffer = new StringBuffer(qurUrl);

			for (HashMap<String, Object> itemMap : itemsList) {
				String sid = (String) itemMap.get("identifier");
				if (!sid.isEmpty()) {
				qurUrlBuffer.append("&sid=" + sid);	
				isRun=true;
				}
				
			}
            String resultSql=qurUrlBuffer.toString();
            ListViewModel<ResourceViewModel> insObjeList=null;
            if (isRun) {
            	 insObjeList = wafSecurityHttpClient.get(resultSql, ListViewModel.class);
            	 tempMap.put("insobjtypetotal", insObjeList.getTotal());
            }else {
            	tempMap.put("insobjtypetotal", "0");
			}	
    			tempMap.put("parenttitle", map.get("parenttitle"));
    			tempMap.put("insobjtotal", insobjtotal);   			
    			resultList.add(tempMap);						
		}
    	   	   	
		try {
			writeToExcel(resultList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resultList;    	    	
    }
    
       	
    /**
     * 将查询的一级套件下的套件信息写到excel文件中
     * @return
     */
    public static void writeToExcel(List<Map<String,Object>> list) throws IOException{
    	//创建一个EXCEL  
		Workbook wb = new HSSFWorkbook();
		// 创建一个SHEET
		Sheet sheet1 = wb.createSheet("套件下套件数目");
		String[] title = { "套件名称", "教学目标数量","教学目标类型数量" };
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
		Map<String, Object> tempMap = null;
		for (int j = 0; j < list.size(); j++) {
			tempMap = list.get(j);
			Row rowTemp = sheet1.createRow((short)(j+1));  
	        //下面是填充数据  
			String idenString=(String)tempMap.get("parenttitle");
			String insobjtotalString=tempMap.get("insobjtotal")+"";
			String insobjtypetotalString=tempMap.get("insobjtypetotal")+"";
			rowTemp.createCell(0).setCellValue(idenString);  
			rowTemp.createCell(1).setCellValue(insobjtotalString);
			rowTemp.createCell(2).setCellValue(insobjtypetotalString); 	
	 }
		 FileOutputStream fileOut = new FileOutputStream("D:/桌面/test.xls");  
	     wb.write(fileOut); 
	     wb.close();
	     fileOut.close();
  }
    
    
    
       
}
