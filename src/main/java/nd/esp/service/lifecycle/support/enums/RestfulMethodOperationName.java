package nd.esp.service.lifecycle.support.enums;
import nd.esp.service.lifecycle.utils.StringUtils;

public enum RestfulMethodOperationName {
    CREATE("POST","创建资源"),
    UPDATE("PUT","更新资源"),
    PATCH("PATCH","局部更新"),
    DELETE("DELETE","删除资源")
    ;
	  
	public static String getMessageString(String methodName) {
		if (StringUtils.hasText(methodName)) {
			for (RestfulMethodOperationName type : RestfulMethodOperationName.values()) {
				if (type.getName().equals(methodName)) {
					return type.getMessage();
				}
			}
		}
		return null;
	}

	private RestfulMethodOperationName(String name, String message) {
		this.name = name;
		this.message = message;
	}

	private String name;
	private String message;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
