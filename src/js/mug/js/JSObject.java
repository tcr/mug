package mug.js;

import java.util.HashMap;
import java.util.Set;

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
	
	public JSPrimitive get(JSPrimitive key) {
		return get(JSUtils.asString(key));
	}

	public void set(String key, JSPrimitive value) {
		if (hash == null)
			hash = new HashMap<String, JSPrimitive>();
		hash.put(key, value);
	}
	
	public void set(JSPrimitive key, JSPrimitive value) {
		set(JSUtils.asString(key), value);
	}
	
	public String[] getKeys() {
		Set<String> set = hash.keySet();
		String[] out = new String[set.size()];
		set.toArray(out);
		return out;
	}
	
	/*
	 * instantiate
	 */

	public JSPrimitive instantiate(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
		// objects that can instantiate overwrite this method
		throw new Exception("Cannot instantiate non-callable object.");
	}
	
	final public JSPrimitive instantiate() throws Exception {
		return instantiate(0, null, null, null, null, null, null, null, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0) throws Exception {
		return instantiate(1, l0, null, null, null, null, null, null, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1) throws Exception {
		return instantiate(2, l0, l1, null, null, null, null, null, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2) throws Exception {
		return instantiate(3, l0, l1, l2, null, null, null, null, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3) throws Exception {
		return instantiate(4, l0, l1, l2, l3, null, null, null, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4) throws Exception {
		return instantiate(5, l0, l1, l2, l3, l4, null, null, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5) throws Exception {
		return instantiate(6, l0, l1, l2, l3, l4, l5, null, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6) throws Exception {
		return instantiate(7, l0, l1, l2, l3, l4, l5, l6, null, null);
	}
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7) throws Exception {
		return instantiate(8, l0, l1, l2, l3, l4, l5, l6, l7, null);
	}	
	
	final public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
		return instantiate(rest.length + 8, l0, l1, l2, l3, l4, l5, l6, l7, rest);
	}	
	
	/*
	 * invoke
	 */

	public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
		// objects that can invoke overwrite this method
		throw new Exception("Cannot invoke non-callable object.");
	}
	
	final public JSPrimitive invoke(JSPrimitive ths) throws Exception {
		return invoke(ths, 0, null, null, null, null, null, null, null, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0) throws Exception {
		return invoke(ths, 1, l0, null, null, null, null, null, null, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1) throws Exception {
		return invoke(ths, 2, l0, l1, null, null, null, null, null, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2) throws Exception {
		return invoke(ths, 3, l0, l1, l2, null, null, null, null, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3) throws Exception {
		return invoke(ths, 4, l0, l1, l2, l3, null, null, null, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4) throws Exception {
		return invoke(ths, 5, l0, l1, l2, l3, l4, null, null, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5) throws Exception {
		return invoke(ths, 6, l0, l1, l2, l3, l4, l5, null, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6) throws Exception {
		return invoke(ths, 7, l0, l1, l2, l3, l4, l5, l6, null, null);
	}
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7) throws Exception {
		return invoke(ths, 8, l0, l1, l2, l3, l4, l5, l6, l7, null);
	}	
	
	final public JSPrimitive invoke(JSPrimitive ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
		return invoke(ths, rest.length + 8, l0, l1, l2, l3, l4, l5, l6, l7, rest);
	}	
	
	/*
	 * cyclic prototype
	 */
	
	public static JSObject createObjectPrototype() {
		JSObject obj = new JSObject();
		return obj;
	}
}