package nd.esp.service.lifecycle.services.questions.v06.impls;

import nd.esp.service.lifecycle.educommon.services.NDResourceService;
import nd.esp.service.lifecycle.educommon.vos.constant.IncludesConstant;
import nd.esp.service.lifecycle.models.v06.QuestionModel;
import nd.esp.service.lifecycle.repository.common.IndexSourceType;
import nd.esp.service.lifecycle.services.packaging.v06.PackageService;
import nd.esp.service.lifecycle.services.questions.v06.QuestionServiceV06;
import nd.esp.service.lifecycle.support.DbName;
import nd.esp.service.lifecycle.support.busi.PrePackUtil;
import nd.esp.service.lifecycle.support.enums.ResourceNdCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("questionServiceV06")
@Transactional(value="questionTransactionManager")
public class QuestionServiceImplV06 implements QuestionServiceV06 {
    @Autowired
    private PackageService pacakageService;
    
    @Autowired
    private NDResourceService ndResourceService;
    

    @Override
    public QuestionModel createQuestion(QuestionModel questionModel) {
        QuestionModel rtQuestionModel = (QuestionModel) ndResourceService.create(ResourceNdCode.questions.toString(), questionModel,DbName.QUESTION);
        return rtQuestionModel;
    }

    @Override
    public QuestionModel updateQuestion(QuestionModel questionModel) {
        QuestionModel rtQuestionModel = (QuestionModel)ndResourceService.update(ResourceNdCode.questions.toString(),
                questionModel,DbName.QUESTION);
        return rtQuestionModel;
    }

    @Override
    public QuestionModel patchQuestion(QuestionModel questionModel) {
        return (QuestionModel)ndResourceService.patch(ResourceNdCode.questions.toString(), questionModel, DbName.QUESTION);
    }

}
