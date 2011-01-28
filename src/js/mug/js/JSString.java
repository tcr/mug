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
	
	@Override
	public Object get(String key) throws Exception {
		if (key.equals("length"))
			return value.length();
		return super.get(key);
	}
}