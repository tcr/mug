package mug.js;

import java.util.regex.Pattern;

public class JSRegExp extends JSObject {
	Pattern pattern;
	boolean global;
	
	public JSRegExp(JSEnvironment env, Pattern pattern, boolean global) {
		super(env, env.getRegExpPrototype());
		this.pattern = pattern;
		this.global = global;
	}
	
	/*
	 * api
	 */
	
	public Pattern getPattern() {
		return pattern;
	}
	
	public boolean isGlobal() {
		return global;
	}
	
	/*
	 * object
	 */
	
	@Override
	public Object get(String key) throws Exception {
		if (key.equals("source"))
			return pattern.pattern();
		return super.get(key);
	}
}
