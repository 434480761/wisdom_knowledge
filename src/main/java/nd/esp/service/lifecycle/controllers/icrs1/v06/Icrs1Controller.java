package nd.esp.service.lifecycle.controllers.icrs1.v06;

import java.util.ArrayList;
import java.util.List;

import nd.esp.service.lifecycle.models.icrs1.v06.DailyDataModel;
import nd.esp.service.lifecycle.models.icrs1.v06.ResourceTotalModel;
import nd.esp.service.lifecycle.models.icrs1.v06.TextbookModel;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.services.icrs1.v06.Icrs1Service;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.icrs.IcrsResourceType;
import nd.esp.service.lifecycle.utils.CollectionUtils;
import nd.esp.service.lifecycle.vos.icrs1.v06.DailyDataViewModel;
import nd.esp.service.lifecycle.vos.icrs1.v06.ResourceTotalViewModel;
import nd.esp.service.lifecycle.vos.icrs1.v06.TextbookViewModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ibm.icu.text.SimpleDateFormat;

@RestController
@RequestMapping("/v0.6/icrs")
public class Icrs1Controller {

	@Autowired
	private Icrs1Service icrsService;

	/**
	 * 查询本校不同类别资源的产出数量，统计范围为本校全部教师的个人库资源，统计类型包括课件、多媒体、基础习题、趣味题型
	 * 
	 * @author yuzc
	 * @date 2016年9月9日
	 * @param schoolId
	 * @param from_date
	 * @param to_date
	 * @return
	 */
	@RequestMapping(value = "/{school_id}/statistics", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResourceTotalViewModel getResourceStatisticsTotal(
			@PathVariable(value = "school_id") String schoolId,
			@RequestParam(required = false, value = "from_date") String fromDate,
			@RequestParam(required = false, value = "to_date") String toDate) {
		
		// 校验日期是否合法
		if (StringUtils.hasText(fromDate)) {
			isValidDate(fromDate);
		}
		if(StringUtils.hasText(toDate)){
			isValidDate(toDate);
		}
		List<ResourceTotalModel> rtm = icrsService.getResourceTotal(schoolId,
				fromDate, toDate);
		return changeToViewModel(rtm);

	}

	/**
	 * 查询本校资源的日产出数量，统计范围为本校全部教师的个人库资源，统计类型包括课件、多媒体、基础习题、趣味题型。
	 * 
	 * @author yuzc
	 * @date 2016年9月9日
	 * @param schoolId
	 * @param from_date
	 * @param to_date
	 * @return
	 */
	@RequestMapping(value = "/{school_id}/statistics/day", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE }, params = {
			"from_date", "to_date" })
	public List<DailyDataViewModel> getResourceStatisticsByDay(
			@PathVariable(value = "school_id") String schoolId,
			@RequestParam(required = false, value = "res_type") String resType,
			@RequestParam String from_date, @RequestParam String to_date) {

		// 校验日期是否合法
		isValidDate(from_date);
		isValidDate(to_date);
		
		//校验resType和获取对应NDR资源类型
		if(StringUtils.hasText(resType)){
			if(!IcrsResourceType.validType(resType)){
				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.ResourceTypeNotFound);
			}
			
			resType = IcrsResourceType.getCorrespondingType(resType);
		}

		List<DailyDataModel> ddmList = icrsService.getResourceStatisticsByDay(schoolId,
					resType, from_date, to_date);
		
		List<DailyDataViewModel> ddvmList = new ArrayList<DailyDataViewModel>();
		if (CollectionUtils.isNotEmpty(ddmList)) {
			for (DailyDataModel model : ddmList) {
				DailyDataViewModel ddvm = new DailyDataViewModel();
				ddvm.setData(model.getData());
				ddvm.setDate(model.getDate());
				ddvmList.add(ddvm);
			}
		}
		return ddvmList;
	}

	/**
	 * 取得某一个教师上传的资源，所对应的教材列表
	 * 
	 * @author yuzc
	 * @date 2016年9月9日
	 * @param schoolId
	 * @param teacherId
	 * @param resType
	 * @return
	 */
	@RequestMapping(value = "/{school_id}/{teacher_id}/teachingmaterials", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	public List<TextbookViewModel> getTeacherResource(
			@PathVariable(value = "school_id") String schoolId,
			@PathVariable(value = "teacher_id") String teacherId,
			@RequestParam(required = false, value = "res_type") String resType) {
		
		// 校验resType和获取对应NDR资源类型
		if (StringUtils.hasText(resType)) {
			if (!IcrsResourceType.validType(resType)) {
				throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
						LifeCircleErrorMessageMapper.ResourceTypeNotFound);
			}

			resType = IcrsResourceType.getCorrespondingType(resType);
		}
		
		List<TextbookModel> list = icrsService.getTeacherResource(schoolId,
				teacherId, resType);
		List<TextbookViewModel> tvmlist = new ArrayList<TextbookViewModel>();
		if (CollectionUtils.isNotEmpty(list)) {
			for (TextbookModel model : list) {
				TextbookViewModel tvm = new TextbookViewModel();
				tvm.setUuid(model.getUuid());
				tvm.setTitle(model.getTitle());
				tvmlist.add(tvm);
			}
		}
		return tvmlist;
	}

	/**
	 * 校验日期格式
	 * 
	 * @author yuzc
	 * @date 2016年9月12日
	 * @param str
	 * @return
	 */
	public static boolean isValidDate(String str) {

		boolean convertSuccess = true;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		format.setLenient(false);
		try {
			format.parse(str);
		} catch (java.text.ParseException e) {

			throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,
					LifeCircleErrorMessageMapper.DateFormatFail);
		}
		return convertSuccess;
	}

	/**
	 * List<ResourceTotalModel> 转换为 ResourceTotalViewModel
	 * 
	 * @author yuzc
	 * @date 2016年9月12日
	 * @param rtm
	 * @return rtvm
	 */
	public ResourceTotalViewModel changeToViewModel(List<ResourceTotalModel> rtm) {

		ResourceTotalViewModel rtvm = new ResourceTotalViewModel();
		if (CollectionUtils.isNotEmpty(rtm)) {
			for (ResourceTotalModel model : rtm) {
				if(model.getResType().equals(IndexSourceType.AssetType.getName())){
					rtvm.setTotalMultimedia(model.getResTotal());
				}else if(model.getResType().equals(IndexSourceType.SourceCourseWareType.getName())){
					rtvm.setTotalCourseware(model.getResTotal());
				}else if(model.getResType().equals(IndexSourceType.QuestionType.getName())){
					rtvm.setTotalBasicQuestion(model.getResTotal());
				}else if(model.getResType().equals(IndexSourceType.SourceCourseWareObjectType.getName())){
					rtvm.setTotalFunnyQuestion(model.getResTotal());
				}
			}
		}
		
		return rtvm;
	}
}
