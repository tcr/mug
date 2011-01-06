package mug.js;

/**
 * 
 */

public class JSString extends JSObject {
	String value;
	
	public JSString(JSEnvironment env, String value) {
		super(env, env.getStringPrototype());
		this.value = value;
	}
	
	public Object get(String key) {
		if (key.equals("length"))
			return value.length();
		return super.get(key);
	}
}