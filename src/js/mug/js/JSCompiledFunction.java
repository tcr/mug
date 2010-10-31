package mug.js;

abstract public class JSCompiledFunction extends JSCompiledObject implements JSFunction {
	public JSCompiledFunction() {
		set_prototype(createProto());
	}
	
	/*
	 * .prototype property caching
	 */

	JSCompiledObject actual_prototype;

	public void set_prototype(JSPrimitive v) {
		_prototype = v;
		actual_prototype = (v instanceof JSCompiledObject ? (JSCompiledObject) v : new JSCompiledObject());
	}
	
	/*
	 * calling and instantiation
	 */

	public JSCompiledObject createProto() {
		return new JSCompiledObject();
	}

	public JSCompiledObject createNew() {
		return new JSCompiledObject(actual_prototype);
	}

	public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7) throws Exception {
		JSCompiledObject obj = createNew();
		JSPrimitive ret;
		if ((ret = invoke(obj, l0, l1, l2, l3, l4, l5, l6, l7)) != null)
			return ret;
		return obj;
	}
	
	public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7) throws Exception {
		return invoke((JSCompiledObject) ths, l0, l1, l2, l3, l4, l5, l6, l7);
	}

	public abstract JSPrimitive invoke(JSCompiledObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
			throws Exception;
}