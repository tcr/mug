package mug.js;

public class JSValueException extends Exception {
	public Object value;
	
	public JSValueException(Object value) {
		super(JSUtils.asString(value));
		this.value = value;
	}
}
