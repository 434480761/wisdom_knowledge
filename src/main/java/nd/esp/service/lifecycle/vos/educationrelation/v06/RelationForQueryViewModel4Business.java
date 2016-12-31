package nd.esp.service.lifecycle.vos.educationrelation.v06;

import java.util.List;
import java.util.Map;

import nd.esp.service.lifecycle.educommon.vos.ResClassificationViewModel;


/**
 * 关系目标检索的viewModel用于智慧知识业务接口
 */
public class RelationForQueryViewModel4Business extends RelationForQueryViewModel{
	private Map<String,List<? extends ResClassificationViewModel>> categories;

	public Map<String,List<? extends ResClassificationViewModel>> getCategories() {
		return categories;
	}

	public void setCategories(Map<String,List<? extends ResClassificationViewModel>> categories) {
		this.categories = categories;
	}
}
