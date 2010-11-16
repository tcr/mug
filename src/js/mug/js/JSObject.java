package mug.js;

import java.util.HashMap;

/**
 * Object base (without accessors).
 */

public class JSObject extends JSPrimitive {
	// private constructor for prototypes
	private JSObject() {
	}
	
	public JSObject(JSObject proto) {
		__proto__ = proto;
	}
	
	protected JSObject __proto__;

	public JSObject getProto() {
		return __proto__;
	}
	
	/*
	 * methods
	 */
	
	public JSObject toObject(JSTopLevel top) {
		return this;
	}
	
	JSPrimitive value = null;
	
	public void setPrimitiveValue(JSPrimitive value) {
		this.value = value;
	}
	
	public JSPrimitive getPrimitiveValue() {
		return value;
	}
	
	/*
	 * hash
	 */

	protected HashMap<String, JSPrimitive> hash;

	public JSPrimitive get(String key) {
		JSPrimitive ret = null;
		if ((hash == null || (ret = hash.get(key)) == null) && __proto__ != null)
			return __proto__.get(key);
		return ret;
	}

	public void set(String key, JSPrimitive value) {
		if (hash == null)
			hash = new HashMap<String, JSPrimitive>();
		hash.put(key, value);
	}
	
	/*
	 * callable
	 */

	public JSPrimitive instantiate(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
		throw new Exception("Cannot instantiate non-callable object.");
	}

	public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
		throw new Exception("Cannot invoke non-callable object.");
	}
	
	/*
	 * cyclic prototype
	 */
	
	public static JSObject createObjectPrototype() {
		JSObject obj = new JSObject();
		obj.__proto__ = obj;
		return obj;
	}
}