package mug.runtime;

import java.util.regex.Matcher;
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
	
	public Object get(String key) throws Exception {
		if (key.equals("source"))
			return pattern.pattern();
		return super.get(key);
	}

	/**
	 * Environment
	 */

	public static JSObject createPrototype(JSEnvironment env)
	{
		return new JSObject(env) { {
			defineProperty("test", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Pattern pattern = ((JSRegExp) ths).getPattern();
					return pattern.matcher(JSUtils.asString(l0)).find();
				}
			});
			
			defineProperty("exec", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Pattern pattern = ((JSRegExp) ths).getPattern();
					JSArray out = new JSArray(env, 0);
					Matcher matcher = pattern.matcher(JSUtils.asString(l0));
					if (!((JSRegExp) ths).isGlobal()) {
						if (!matcher.find())
							return env.getNullObject();
						for (int i = 0; i < matcher.groupCount() + 1; i++)
							out.push(matcher.group(i));
					} else {
						while (matcher.find())
							out.push(matcher.group(0));
						if (out.getLength() == 0)
							return env.getNullObject();
					}
					return out;
				}
			});
		} };
	}
}
