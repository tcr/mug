package mug.js;

import java.util.HashMap;

/**
 * Object base (without accessors).
 */

public class JSCompiledObject extends JSPrimitive implements JSObject {
	public JSCompiledObject() {
	}
	
	public JSCompiledObject(JSCompiledObject proto) {
		__proto__ = proto;
	}
	
	protected JSCompiledObject __proto__;

	public JSCompiledObject getProto() {
		return __proto__;
	}

	HashMap<String, JSPrimitive> hash;

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
	 * .prototype property
	 */

	protected JSPrimitive _prototype;

	public JSPrimitive get_prototype() {
		if (_prototype != null)
			return _prototype;
		if (__proto__ == null)
			return null;
		return __proto__.get_prototype();
	}
	
	public void set_prototype(JSPrimitive v) {
		_prototype = v;
	}
}