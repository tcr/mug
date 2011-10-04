package mug.runtime;

public class JSValueException extends Exception {
	public Object value;
	
	public JSValueException(Object value) {
		super(getObjectMessage(value));
		this.value = value;
	}
	
	static String getObjectMessage(Object value) {
		try {
			return JSUtils.asString(value);
		} catch (Exception e) {
			return "";
		}	
	}
}
