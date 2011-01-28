package mug.js;

import java.util.HashMap;

public abstract class JSFunction extends JSObject implements Runnable {	
	public JSFunction(JSEnvironment env) {
		super(env, env.getFunctionPrototype());
		actual_prototype = new JSObject(env);
		_prototype = actual_prototype;
		actual_prototype.defineProperty("constructor", this);
	}
	
	/*
	 * .prototype property caching
	 */

	protected Object _prototype;
	protected JSObject actual_prototype;

	@Override
	public Object get(String key) throws Exception {
		return (key.equals("prototype") && _prototype != null) ? _prototype : super.get(key);
	}

	@Override
	public void set(String key, Object value) throws Exception {
		if (key.equals("prototype")) {
			_prototype = value;
			actual_prototype = value instanceof JSObject ? (JSObject) value : null;
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
	
	@Override
	public Object instantiate(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
		JSObject obj = new JSObject(env, actual_prototype);
		Object ret;
		if ((ret = invoke(obj, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest)) != null)
			return ret;
		return obj;
	}

	public abstract Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
			throws Exception;
	
	/**
	 * JSFunctions supports the Runnable interface.
	 */
	
	public void run() {
		try {
			invoke(null);
		} catch (Exception e) {
		}
	}
}