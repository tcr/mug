package mug.js;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class JSArray extends JSObject {
	public JSArray(JSObject proto, int initialCapacity) {
		super(proto);
		list = new ArrayList<JSPrimitive>(initialCapacity);
	}
	
	/*
	 * API functions
	 */
	
	public void push(JSPrimitive value) {
		list.add(value);
	}
	
	public JSPrimitive pop() {
		JSPrimitive item = (JSPrimitive) list.get(list.size() - 1);
		list.remove(list.size() - 1);
		return item;
	}
	
	public int getLength() {
		return list.size();
	}
	
	public void append(JSPrimitive[] arr) {
		for (JSPrimitive value : arr)
			push(value);
	}
	
	// used by literal constructor
	public void load(JSPrimitive[] arr) {
		for (int i = 0; i < arr.length; i++)
			set(i, arr[i]);
	}
	
	/*
	 * accessors
	 */
	
	ArrayList<JSPrimitive> list;
	
	public JSPrimitive get(String key) {
		// length property
		if (key.equals("length"))
			return new JSNumber(list.size());
		
		// integer index
		try {
			int index = ((int) Double.parseDouble(key));
			if (String.valueOf(index).equals(key))
				return (JSPrimitive) list.get(index);
		} catch (Exception e) { };
		
		// default
		return super.get(key);
	}
	
	public JSPrimitive get(JSPrimitive key) {
		if (key instanceof JSObject && ((JSObject) key).getPrimitiveValue() != null)
			key = ((JSObject) key).getPrimitiveValue();
		
		// integer index
		if (key instanceof JSNumber) {
			double dbl = ((JSNumber) key).value;
			int index = (int) dbl;
			if (index == dbl)
				return (JSPrimitive) list.get(index);
		}
		
		return get(JSUtils.asString(key));
	}
	
	public void set(String key, JSPrimitive value) {
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
	
	public void set(JSPrimitive key, JSPrimitive value) {
		if (key instanceof JSObject && ((JSObject) key).getPrimitiveValue() != null)
			key = ((JSObject) key).getPrimitiveValue();
		
		// integer index
		if (key instanceof JSNumber) {
			double dbl = ((JSNumber) key).value;
			int index = (int) dbl;
			if (index == dbl) {
				set(index, value);
				return;	
			}
		}
		
		set(JSUtils.asString(key), value);
	}
	
	public void set(int index, JSPrimitive value) {
		while (list.size() <= index)
			list.add(null);
		list.set(index, value);
	}
}
