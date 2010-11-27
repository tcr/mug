package mug.js;

import java.util.HashMap;
import java.util.Set;

/**
 * Object base (without accessors).
 */

public class JSObject {
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
	
	public Object valueOf() {
		Object valueOf = get("valueOf");
		if (valueOf instanceof JSObject)
			try {
				return ((JSObject) valueOf).invoke(this);
			} catch (Exception e) {
				return null;
			}
		return null;
	}
	
	/*
	 * hash
	 */

	protected HashMap<String, Object> hash;

	public Object get(String key) {
		Object ret = null;
		if ((hash == null || (ret = hash.get(key)) == null) && __proto__ != null)
			return __proto__.get(key);
		return ret;
	}
	
	public Object get(int key) {
		return get(String.valueOf(key));
	}
	
	public Object get(Object key) {
		return get(JSUtils.asString(key));
	}

	public void set(String key, Object value) {
		if (hash == null)
			hash = new HashMap<String, Object>();
		hash.put(key, value);
	}
	
	public void set(int key, Object value) {
		set(String.valueOf(key), value);
	}
	
	public void set(Object key, Object value) {
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

	public Object instantiate(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
		// objects that can instantiate overwrite this method
		throw new Exception("Cannot instantiate non-callable object.");
	}
	
	final public Object instantiate() throws Exception {
		return instantiate(0, null, null, null, null, null, null, null, null, null);
	}
	
	final public Object instantiate(Object l0) throws Exception {
		return instantiate(1, l0, null, null, null, null, null, null, null, null);
	}
	
	final public Object instantiate(Object l0, Object l1) throws Exception {
		return instantiate(2, l0, l1, null, null, null, null, null, null, null);
	}
	
	final public Object instantiate(Object l0, Object l1, Object l2) throws Exception {
		return instantiate(3, l0, l1, l2, null, null, null, null, null, null);
	}
	
	final public Object instantiate(Object l0, Object l1, Object l2, Object l3) throws Exception {
		return instantiate(4, l0, l1, l2, l3, null, null, null, null, null);
	}
	
	final public Object instantiate(Object l0, Object l1, Object l2, Object l3, Object l4) throws Exception {
		return instantiate(5, l0, l1, l2, l3, l4, null, null, null, null);
	}
	
	final public Object instantiate(Object l0, Object l1, Object l2, Object l3, Object l4, Object l5) throws Exception {
		return instantiate(6, l0, l1, l2, l3, l4, l5, null, null, null);
	}
	
	final public Object instantiate(Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6) throws Exception {
		return instantiate(7, l0, l1, l2, l3, l4, l5, l6, null, null);
	}
	
	final public Object instantiate(Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7) throws Exception {
		return instantiate(8, l0, l1, l2, l3, l4, l5, l6, l7, null);
	}	
	
	final public Object instantiate(Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
		return instantiate(rest.length + 8, l0, l1, l2, l3, l4, l5, l6, l7, rest);
	}	
	
	/*
	 * invoke
	 */

	public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
		// objects that can invoke overwrite this method
		throw new Exception("Cannot invoke non-callable object.");
	}
	
	final public Object invoke(Object ths) throws Exception {
		return invoke(ths, 0, null, null, null, null, null, null, null, null, null);
	}
	
	final public Object invoke(Object ths, Object l0) throws Exception {
		return invoke(ths, 1, l0, null, null, null, null, null, null, null, null);
	}
	
	final public Object invoke(Object ths, Object l0, Object l1) throws Exception {
		return invoke(ths, 2, l0, l1, null, null, null, null, null, null, null);
	}
	
	final public Object invoke(Object ths, Object l0, Object l1, Object l2) throws Exception {
		return invoke(ths, 3, l0, l1, l2, null, null, null, null, null, null);
	}
	
	final public Object invoke(Object ths, Object l0, Object l1, Object l2, Object l3) throws Exception {
		return invoke(ths, 4, l0, l1, l2, l3, null, null, null, null, null);
	}
	
	final public Object invoke(Object ths, Object l0, Object l1, Object l2, Object l3, Object l4) throws Exception {
		return invoke(ths, 5, l0, l1, l2, l3, l4, null, null, null, null);
	}
	
	final public Object invoke(Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5) throws Exception {
		return invoke(ths, 6, l0, l1, l2, l3, l4, l5, null, null, null);
	}
	
	final public Object invoke(Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6) throws Exception {
		return invoke(ths, 7, l0, l1, l2, l3, l4, l5, l6, null, null);
	}
	
	final public Object invoke(Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7) throws Exception {
		return invoke(ths, 8, l0, l1, l2, l3, l4, l5, l6, l7, null);
	}	
	
	final public Object invoke(Object ths, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
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