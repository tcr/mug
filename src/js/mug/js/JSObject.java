package mug.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


import mug.js.StringMap.Property;

import org.json.simple.JSONObject;

/**
 * Object base (without accessors).
 */

public class JSObject {
	protected JSEnvironment env;
	
	public JSEnvironment getEnvironment() {
		return env;
	}
	
	/**
	 * Default object constructor. When the JSEnvironment is first created,
	 * it has no object prototype; thus the prototype of the object prototype
	 * itself is null.
	 */
	
	public JSObject(JSEnvironment env) {
		this.env = env;
		__proto__ = env.getObjectPrototype();
	}
	
	/**
	 * Construct an object with a specific prototype.
	 */
	
	public JSObject(JSEnvironment env, JSObject proto) {
		this.env = env;
		__proto__ = proto;
	}
	
	/**
	 * JSObject prototype.
	 */
	
	protected JSObject __proto__;

	public JSObject getProto() {
		return __proto__;
	}
	
	/*
	 * hash
	 */

	protected StringMap hash;
	
	/*
	 * [[Get]]
	 */
	
	// static reference

	public Object get(String key) throws Exception {
		Property prop = null;
		if ((hash == null || (prop = hash.findProperty(key)) == null) && __proto__ != null)
			return __proto__.get(key);
		return prop == null ? null :
			prop.get == null ? prop.value :
			prop.get.invoke(this);
	}
	
	// array reference
	
	public Object get(int key) throws Exception {
		return get(String.valueOf(key));
	}
	
	// dynamic reference
	
	public Object get(Object key) throws Exception {
		return get(JSUtils.asString(key));
	}
	
	/*
	 * [[Put]]
	 */
	
	// static reference

	public void set(String key, Object value) throws Exception {
		if (hash == null)
			hash = new StringMap();
		if (hash.getProperty(key).set == null)
			hash.getProperty(key).value = value;
		else
			hash.getProperty(key).set.invoke(this, value);
	}
	
	// array reference
	
	public void set(int key, Object value) throws Exception {
		set(String.valueOf(key), value);
	}
	
	// dynamic reference
	
	public void set(Object key, Object value) throws Exception {
		set(JSUtils.asString(key), value);
	}
	
	/*
	 * [[Delete]]
	 */
	
	public boolean delete(String key) throws Exception {
		return hash.deletePropertyIfConfigurable(key);
	}
	
	public boolean delete(Object key) throws Exception {
		return hash.deletePropertyIfConfigurable(JSUtils.asString(key));
	}

	
	/*
	 * Helper functions
	 */
	
	public String[] getKeys() {
		if (hash == null)
			return new String[0];
		
		ArrayList<String> keys = new ArrayList();
		Iterator<String> iter = hash.getEnumerableKeys();
		while (iter.hasNext())
			keys.add(iter.next());
		String[] arr = new String[keys.size()];
		keys.toArray(arr);
		return arr;
			
		/*
		Set<String> set = hash.keySet();
		String[] out = new String[set.size()];
		set.toArray(out);
		return out;
		*/
	}
	
	public boolean hasOwnProperty(String prop) {
		return hash != null && (hash.findProperty(prop) != null);
	}
	
	public Object valueOf() throws Exception {
		Object valueOf = null;
		try {
			valueOf = get("valueOf");
		} catch (Exception e1) {
		}
		if (valueOf instanceof JSObject)
			try {
				return ((JSObject) valueOf).invoke(this);
			} catch (Exception e) {
			}
		return this;
	}
	
	public String toString() {
		Object toString = null;
		try {
			toString = get("toString");
		} catch (Exception e1) {
		}
		if (toString instanceof JSObject)
			try {
				return JSUtils.asString(((JSObject) toString).invoke(this));
			} catch (Exception e) {
			}
		return "[object Object]";
	}
	
	public boolean hasInstance(Object v) {
		return false;
	}
	
	/*
	 * property definition
	 */
	
	public void defineProperty(String key, Object value) {
		defineProperty(key, value, true, true, true, null, null);
	}
	
	public void defineProperty(String key, Object value, boolean writable, boolean enumerable, boolean configurable) {
		defineProperty(key, value, writable, enumerable, configurable, null, null);
	}
	
	public void defineProperty(String key, Object value, boolean writable, boolean enumerable, boolean configurable, JSObject get, JSObject set) {
		if (hash == null)
			hash = new StringMap();
		StringMap.Property p = hash.getProperty(key);
		p.value = value;
		p.writable = writable;
		p.enumerable = enumerable;
		p.configurable = configurable;
		p.get = get;
		p.set = set;
	}
	
	public void defineGetter(String key, JSObject get) {
		if (hash == null)
			hash = new StringMap();
		StringMap.Property p = hash.getProperty(key);
		p.get = get;
	}
	
	public void defineSetter(String key, JSObject set) {
		if (hash == null)
			hash = new StringMap();
		StringMap.Property p = hash.getProperty(key);
		p.set = set;
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
	
	final public Object invoke(Object ths, Object[] args) throws Exception {
		Object l0 = null, l1 = null, l2 = null, l3 = null, l4 = null, l5 = null, l6 = null, l7 = null;
		switch (args.length) {
		case 8: l7 = args[7];
		case 7: l6 = args[6];
		case 6: l5 = args[5];
		case 5: l4 = args[4];
		case 4: l3 = args[3];
		case 3: l2 = args[2];
		case 2: l1 = args[1];
		case 1: l0 = args[0];
		}
		Object[] rest = null;
		if (args.length > 8) {
			rest = new Object[Math.max(args.length - 7, 0)];
			System.arraycopy(args, 8, rest, 0, rest.length);
		}
		return invoke(ths, args.length, l0, l1, l2, l3, l4, l5, l6, l7, rest);
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
	
	final public Object instantiate(Object[] args) throws Exception {
		Object l0 = null, l1 = null, l2 = null, l3 = null, l4 = null, l5 = null, l6 = null, l7 = null;
		switch (args.length) {
		case 8: l7 = args[7];
		case 7: l6 = args[6];
		case 6: l5 = args[5];
		case 5: l4 = args[4];
		case 4: l3 = args[3];
		case 3: l2 = args[2];
		case 2: l1 = args[1];
		case 1: l0 = args[0];
		}
		Object[] rest = null;
		if (args.length > 8) {
			rest = new Object[Math.max(args.length - 7, 0)];
			System.arraycopy(args, 8, rest, 0, rest.length);
		}
		return instantiate(args.length, l0, l1, l2, l3, l4, l5, l6, l7, rest);
	}
}