package mug.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Pattern;

public class JSArray extends JSObject {
	public JSArray(JSEnvironment env, int initialCapacity) {
		super(env, env.getArrayPrototype());
		list = new ArrayList<Object>(initialCapacity);
	}
	
	/*
	 * API functions
	 */
	
	public int getLength() {
		return list.size();
	}
	
	public void append(Object[] arr) {
		for (Object value : arr)
			push(value);
	}
	
	// used by literal constructor
	public void load(Object[] arr) throws Exception {
		for (int i = 0; i < arr.length; i++)
			set(i, arr[i]);
	}
	
	public Object[] toArray() {
		return list.toArray();
	}
	
	/**
	 * Array builtins
	 */
	
	public int push(Object value) {
		list.add(value);
		return list.size();
	}
	
	public Object pop() {
		Object item = list.get(list.size() - 1);
		list.remove(list.size() - 1);
		return item;
	}
	
	public Object shift() {
		Object item = list.get(0);
		list.remove(0);
		return item;
	}
	
	public int unshift(Object value) {
		list.add(0, value);
		return list.size();
	}
	
	/*
	 * accessors
	 */
	
	ArrayList<Object> list;
	
	public Object get(String key) throws Exception {
		// length property
		if (key.equals("length"))
			return new Double(list.size());
		
		// integer index
		int i = 0, len = key.length();
		for (; i < len; i++) {
			char c = key.charAt(i);
			if (c < 48 || c > 57) // isDigit
				break;
		}
		if (i == len)
			try {
				return list.get(Integer.parseInt(key));
			} catch (Exception e) { };
		
		// default
		return super.get(key);
	}
	
	public Object get(int index) throws Exception {
		if (index < 0 || index >= list.size())
			return null;
		return list.get(index);
	}
	
	public Object get(Object key) throws Exception {
		if (key instanceof JSObject)
			key = ((JSObject) key).valueOf();
		
		// integer index
		if (key instanceof Double) {
			double dbl = (Double) key;
			int index = (int) dbl;
			if (index == dbl)
				return get(index);
		}
		//[TODO] instanceof Number
		
		return get(JSUtils.asString(key));
	}
	
	public void set(String key, Object value) throws Exception {
		// length property
		if (key.equals("length")) {
			double dbl = JSUtils.asNumber(value);
			int len = (int) dbl;
			if (dbl == Double.NaN || len != dbl)
				return;
			// to contract, delete hash values
			while (len > list.size())
				list.add(null);
			if (len < list.size())
				list.remove(list.size()-1);
			return;
		}
		
		// integer index
		int index = Integer.parseInt(key);
		if (String.valueOf(index).equals(key)) {
			set(index, value);
			return;
		}
		
		// default
		super.set(key, value);
	}
	
	@Override
	public void set(int index, Object value) throws Exception {
		while (list.size() <= index)
			list.add(null);
		list.set(index, value);
	}
	
	public void set(Object key, Object value) throws Exception {
		if (key instanceof JSObject)
			key = ((JSObject) key).valueOf();
		
		// integer index
		if (key instanceof Double) {
			double dbl = (Double) key;
			int index = (int) dbl;
			if (index == dbl) {
				set(index, value);
				return;	
			}
		}
		//[TODO] instanceof Number
		
		set(JSUtils.asString(key), value);
	}
	
	/**
	 * Environment
	 */
	
	public static JSObject createPrototype(JSEnvironment env)
	{
		return new JSObject(env) { {
			defineProperty("concat", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					// concatenate arrays to new array
					JSObject thsObj = JSUtils.asJSObject(env, ths);
					JSArray out = new JSArray(env, 0);
					Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
					for (int j = 0, max = (int) JSUtils.asNumber(thsObj.get("length")); j < max; j++)
						out.push(thsObj.get(String.valueOf(j)));
					for (Object arg : arguments) {
						JSObject arr = JSUtils.asJSObject(env, arg);
						for (int j = 0, max = (int) JSUtils.asNumber(arr.get("length")); j < max; j++)
							out.push(arr.get(String.valueOf(j)));
					}
					return out;
				}
			});
			
			defineProperty("push", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = (JSObject) ths;

					// attempt builtin method
					if (thsObj instanceof JSArray)
						return ((JSArray) thsObj).push(l0);
					// generic
					thsObj.set((int) JSUtils.asNumber(thsObj.get("length")), l0);
					return thsObj.get("length");
				}
			});
			
			defineProperty("pop", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = (JSObject) ths;

					// attempt builtin method
					if (thsObj instanceof JSArray)
						return ((JSArray) thsObj).pop();
					// generic
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					Object out = thsObj.get(len-1);
					thsObj.set("length", len-1);
					return out;
				}
			});
			
			defineProperty("shift", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = JSUtils.asJSObject(env, ths);
					
					// attempt builtin method
					if (thsObj instanceof JSArray)
						return ((JSArray) thsObj).shift();
					// generic
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					Object ret = thsObj.get(0);
					for (int i = 0; i < len-1; i++)
						thsObj.set(i, thsObj.get(i+1));
					thsObj.set("length", len-1);
					return ret;
				}
			});
			
			defineProperty("unshift", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = JSUtils.asJSObject(env, ths);

					// attempt builtin method
					if (thsObj instanceof JSArray)
						return ((JSArray) thsObj).unshift(l0);
					// generic
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					for (int i = len; i > 0; i--)
						thsObj.set(i, thsObj.get(i-1));
					thsObj.set(0, l0);
					thsObj.set("length", len+1);
					return new Double(len+1);
				}
			});
			
			defineProperty("map", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = JSUtils.asJSObject(env, ths);
					JSObject func = JSUtils.asJSObject(env, l0);
					JSArray out = new JSArray(env, 0);
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					for (int i = 0; i < len; i++)
						out.push(func.invoke(ths, thsObj.get(String.valueOf(i)), i));
					return out;
				}
			});
			
			final JSFunction _indexOf = new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = JSUtils.asJSObject(env, ths);
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					for (int i = 0; i < len; i++)
						if (JSUtils.testEquality(thsObj.get(i), l0))
							return new Double(i);
					return new Double(-1);
				}
			};
			defineProperty("indexOf", _indexOf);
			
			final JSFunction _join = new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = JSUtils.asJSObject(env, ths);
					StringBuffer sb = new StringBuffer();
					String delim = (l0 == null || l0.equals(env.getNullObject())) ? "" : JSUtils.asString(l0); 
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					for (int i = 0; i < len; i++) {
						if (i > 0)
							sb.append(delim);
						sb.append(JSUtils.asString(thsObj.get(i)));
					}
					return sb.toString();
				}
			};
			defineProperty("join", _join);
			
			defineProperty("slice", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					JSObject thsObj = JSUtils.asJSObject(env, ths);
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					int start = (int) JSUtils.asNumber(l0);
					int end = l1 == null ? len : (int) JSUtils.asNumber(l1);
					if (end < 0)
						end += len;
					JSArray out = new JSArray(env, 0);
					for (int i = start; i < end; i++)
						out.push(thsObj.get(i));
					return out;
				}
			});
			
			defineProperty("reverse", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					// reverse works in-place
					JSObject thsObj = JSUtils.asJSObject(env, ths);
					int len = (int) JSUtils.asNumber(thsObj.get("length"));
					for (int i = 0; i < Math.floor(len/2); i++) {
						Object a = thsObj.get(i);
						thsObj.set(i, thsObj.get(len-1-i));
						thsObj.set(len-1-i, a);
					}
					return thsObj;
				}
			});
			
			defineProperty("toString", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return _join.invoke(ths, ",");
				}
			});
			
			defineProperty("sort", new JSFunction(env) {
				@Override
				public Object invoke(final Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					// cast to strings
					Object[] arr = JSUtils.coerceJavaArray((JSObject) ths);
					for (int i = 0; i < arr.length; i++)
						arr[i] = JSUtils.asString(arr[i]);
					// sort
					final JSObject func = l0 != null ? (JSObject) l0 :
						new JSFunction(env) {
							@Override
							public Object invoke(final Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
							{
								return ((String) l0).compareTo((String) l1);
							}
						};
					Arrays.sort(arr, new Comparator<Object>() {
						public int compare(Object a, Object b) {
							try {
								return (int) JSUtils.asNumber(func.invoke(ths, a, b));
							} catch (Exception e) {
								return 0;
							}
						}
					});
					// return new array
					JSArray out = new JSArray(env, 0);
					out.append(arr);
					return out;
				}
			});
		} };
	}
}
