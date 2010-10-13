package mug.js;

import mug.js.compiled.*;
import java.util.HashMap;

/**
 * 
 */

abstract public class JSObjectBase extends JSPrimitive {
	protected JSObject __proto__;

	public JSObject getProto() {
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