package nd.esp.service.lifecycle.vos.v06;
/**
 * 资源标签统计view model
 * @author xuzy
 *
 */
public class ResourceTagViewModel {
	private String tag;
	private int count;
	private String category;
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	
}
