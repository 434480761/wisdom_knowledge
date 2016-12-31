package nd.esp.service.lifecycle.support.aop;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import nd.esp.service.lifecycle.educommon.services.impl.CommonServiceHelper;
import nd.esp.service.lifecycle.educommon.vos.ResLifeCycleViewModel;
import nd.esp.service.lifecycle.educommon.vos.ResourceViewModel;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.repository.exception.EspStoreException;
import nd.esp.service.lifecycle.repository.model.Contribute;
import nd.esp.service.lifecycle.repository.sdk.Contribute4QuestionDBRepository;
import nd.esp.service.lifecycle.repository.sdk.ContributeRepository;
import nd.esp.service.lifecycle.support.LifeCircleErrorMessageMapper;
import nd.esp.service.lifecycle.support.LifeCircleException;
import nd.esp.service.lifecycle.support.enums.RestfulMethodOperationName;
import nd.esp.service.lifecycle.utils.StringUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


@Aspect
@Component
@Order(998)
public class AccorFlagToCreateLifecycleAspect {

	private static final Logger LOG = LoggerFactory.getLogger(AccorFlagToCreateLifecycleAspect.class);
	
	public boolean isAddLifeCycle=false;
	@Autowired
	private HttpServletRequest request;

	@Autowired
	CommonServiceHelper commonServiceHelper;	
	
	@Autowired
	private Contribute4QuestionDBRepository contribute4QuestionRepository;
	@Autowired
	private ContributeRepository contributeRepository;
	 	
	//需要哪个就换成哪个
//	@Pointcut("execution(* nd.esp.service.lifecycle.controllers..*Controller*.update*(..)) || "
//			+ "execution(* nd.esp.service.lifecycle.controllers..*Controller*.patch*(..)) || "
//			+ "execution(* nd.esp.service.lifecycle.controllers..*Controller*.create*(..)) || "
//			+ "execution(* nd.esp.service.lifecycle.controllers..*Controller*.delete*(..)")
	
	//create和update（patch）操作
	@Pointcut("execution(* nd.esp.service.lifecycle.controllers..*Controller*.update*(..)) || "
			+ "execution(* nd.esp.service.lifecycle.controllers..*Controller*.patch*(..)) || "
			+ "execution(* nd.esp.service.lifecycle.controllers..*Controller*.create*(..))")
		
	private void aspectjMethod() {

	}
	
	/**
	 * @author xm
	 * @date 2016年11月10日 下午3:10:03
	 * @method beforeAdvice
	 * @param joinPoint
	 */
	@Before("aspectjMethod()")
	public void beforeAdvice(JoinPoint joinPoint) {
		
    // 入参检验，auto_create_contribute=true或者false才行
	  String isAddToContributeStr = request.getParameter("auto_create_contribute");
	  if (StringUtils.isNotEmpty(isAddToContributeStr)) {
		 if ("true".equals(isAddToContributeStr)||"false".equals(isAddToContributeStr)) {
				if ("true".equals(isAddToContributeStr)) {
					isAddLifeCycle=true;
				} 
		 }else{
			 //入参错误，报错auto_create_contribute只能为
			 throw new LifeCircleException(HttpStatus.INTERNAL_SERVER_ERROR,"AccorFlagToCreateLifecycleAspect切面报错", "入参auto_create_contribute格式不对");
		}
	  }
	}

	/**
	 * @author xm
	 * @date 2016年11月10日 下午3:10:10
	 * @method afterReturningAdvice
	 * @param joinPoint
	 * @param result
	 */
	@AfterReturning(value = "aspectjMethod()", returning = "result")
	public void afterReturningAdvice(JoinPoint joinPoint, Object result) {
		if (isAddLifeCycle) {
			String url = request.getRequestURI().toString();
			// 获得resType的值
			String[] urlInfo = url.split("/");
			String resType = "";
			// 用正则的方式来取值，版本后面的肯定就是resType(esp-lifecycle/v0.6/assets/uuid)版本后面存放的都是资源的类型
			String regex = "^v0(.[0-9]+)*$";
			int index = 0;
			for (String string : urlInfo) {
				index++;
				if (Pattern.compile(regex).matcher(string).matches()) {
					if (index <= urlInfo.length - 1) {
						resType = urlInfo[index];
						break;
					}
				}
			}		 
		    //resType做判断是否为空
			if (StringUtils.isNotEmpty(resType)) {
				ResLifeCycleViewModel lifecycle = null;
				if (result != null && result instanceof ResourceViewModel) {
					// 获得lifecycle对象
					ResourceViewModel resourceViewModel = (ResourceViewModel) result;
					lifecycle = resourceViewModel.getLifeCycle();
					// 创建Contribute对象
					Contribute contribute = new Contribute();
					String uuid = UUID.randomUUID().toString();
					contribute.setIdentifier(uuid);					
					Date currentDate = new Date();
					contribute.setContributeTime(new Timestamp(currentDate.getTime()));
					contribute.setLifeStatus(lifecycle.getStatus());
					// 通过request.metchod方法判断呢操作名称
					contribute.setMessage(RestfulMethodOperationName.getMessageString(request.getMethod())+ " :"+ resType);
					contribute.setResource(resourceViewModel.getIdentifier());
					contribute.setResType(resType);
					contribute.setTargetId(request.getParameter("user_id"));
					contribute.setTargetName(request.getParameter("user_name"));
					contribute.setTargetType("User");
					// 根据restype判断存放到哪个数据库中
					
					try {
						if (isQuesRepository(resType)) {							
							commonServiceHelper.saveContributeToQuesDB(contribute);
						} else {
							contributeRepository.add(contribute);
						}
					} catch (EspStoreException e) {
						throw new LifeCircleException(
								HttpStatus.INTERNAL_SERVER_ERROR,
								LifeCircleErrorMessageMapper.StoreSdkFail
										.getCode(), e.getMessage());
					}
				}
			}

		}
	}
	
	/*
	 * 根据restype判断使用的repository
	 */
	public  boolean isQuesRepository(String resType) {
		if (!(resType.equals(IndexSourceType.QuestionType.getName()) || resType
				.equals(IndexSourceType.SourceCourseWareObjectType.getName()))) {
			return false;
		}
		return true;
	}
		
}
