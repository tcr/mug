package mug.js;

import mug.js.compiled.*;

abstract public class JSFunction extends JSObject {
	public JSFunction() {
		set_prototype(createProto());
	}

	JSObject actual_prototype;

	public void set_prototype(JSPrimitive v) {
		_prototype = v;
		actual_prototype = (v instanceof JSObject ? (JSObject) v : new JSObject());
	}

	// overrides
	public JSObject createProto() {
		return new JSObject();
	}

	public JSObject createNew() {
		return new JSObject(actual_prototype);
	}

	public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7) throws Exception {
		JSObject obj = createNew();
		JSPrimitive ret;
		if ((ret = invoke(obj, l0, l1, l2, l3, l4, l5, l6, l7)) != null)
			return ret;
		return obj;
	}

	public abstract JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
			throws Exception;
}