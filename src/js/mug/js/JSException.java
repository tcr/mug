package mug.js;

public class JSException extends Exception {
	public Object value;
	
	public JSException(Object value) {
		super(JSUtils.asString(value));
		this.value = value;
	}
}
