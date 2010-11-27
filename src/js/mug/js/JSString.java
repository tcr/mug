package mug.js;

/**
 * 
 */

public class JSString extends JSObject {
	String value;
	
	public JSString(JSObject proto, String value) {
		super(proto);
		this.value = value;
	}
	
	public Object get(String key) {
		if (key.equals("length"))
			return value.length();
		return super.get(key);
	}
}