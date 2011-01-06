package mug.js;

import java.util.HashMap;

public abstract class JSFunction extends JSObject {	
	public JSFunction(JSEnvironment env) {
		super(env, env.getFunctionPrototype());
		set("prototype", new JSObject(env));
		actual_prototype.set("constructor", this);
	}
	
	/*
	 * .prototype property caching
	 */

	protected Object _prototype;
	protected JSObject actual_prototype;

	public Object get(String key) {
		return (key.equals("prototype") && _prototype != null) ? _prototype : super.get(key);
	}

	public void set(String key, Object value) {
		if (key.equals("prototype")) {
			_prototype = value;
			if (value instanceof JSObject)
				actual_prototype = (JSObject) value;
		} else
			super.set(key, value);
	}
	
	/* 
	 * inheritance
	 */
	
	@Override
	public boolean hasInstance(Object arg) {
		if (actual_prototype != null && arg instanceof JSObject) {
			JSObject v = (JSObject) arg;
			while ((v = v.getProto()) != null)
				if (actual_prototype.equals(v))
					return true;
		}
		return false;
	}
	
	/*
	 * callable
	 */
	
	public Object instantiate(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
		JSObject obj = new JSObject(env, actual_prototype);
		Object ret;
		if ((ret = invoke(obj, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest)) != null)
			return ret;
		return obj;
	}

	public abstract Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
			throws Exception;
}