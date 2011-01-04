package mug.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class JSArray extends JSObject {
	public JSArray(JSObject proto, int initialCapacity) {
		super(proto);
		list = new ArrayList<Object>(initialCapacity);
	}
	
	/*
	 * API functions
	 */
	
	public void push(Object value) {
		list.add(value);
	}
	
	public Object pop() {
		Object item = list.get(list.size() - 1);
		list.remove(list.size() - 1);
		return item;
	}
	
	public int getLength() {
		return list.size();
	}
	
	public void append(Object[] arr) {
		for (Object value : arr)
			push(value);
	}
	
	// used by literal constructor
	public void load(Object[] arr) {
		for (int i = 0; i < arr.length; i++)
			set(i, arr[i]);
	}
	
	public Object[] toArray() {
		return list.toArray();
	}
	
	/*
	 * accessors
	 */
	
	ArrayList<Object> list;
	
	public Object get(String key) {
		// length property
		if (key.equals("length"))
			return list.size();
		
		// integer index
		try {
			int index = ((int) Double.parseDouble(key));
			if (String.valueOf(index).equals(key))
				return list.get(index);
		} catch (Exception e) { };
		
		// default
		return super.get(key);
	}
	
	public Object get(int index) {
		return list.get(index);
	}
	
	public Object get(Object key) {
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
	
	public void set(String key, Object value) {
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
	public void set(int index, Object value) {
		while (list.size() <= index)
			list.add(null);
		list.set(index, value);
	}
	
	public void set(Object key, Object value) {
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
}
