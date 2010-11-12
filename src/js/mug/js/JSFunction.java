package mug.js;

import java.util.HashMap;

public abstract class JSFunction extends JSObject {	
	public JSFunction() {
		set("prototype", new JSObject());
	}
	
	/*
	 * .prototype property caching
	 */

	protected JSPrimitive _prototype;
	protected JSObject actual_prototype;

	public JSPrimitive get(String key) {
		return (key.equals("prototype") && _prototype != null) ? _prototype : super.get(key);
	}

	public void set(String key, JSPrimitive value) {
		if (key.equals("prototype")) {
			_prototype = value;
			actual_prototype = value instanceof JSObject ? (JSObject) value : new JSObject();
		} else
			super.set(key, value);
	}
	
	/*
	 * callable
	 */
	
	public JSPrimitive instantiate(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
		JSObject obj = new JSObject(actual_prototype);
		JSPrimitive ret;
		if ((ret = invoke(obj, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest)) != null)
			return ret;
		return obj;
	}

	public abstract JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
			throws Exception;
}