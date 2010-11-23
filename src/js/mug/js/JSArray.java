package mug.js;

import java.util.ArrayList;
import java.util.regex.Pattern;

import cern.colt.map.OpenIntObjectHashMap;

public class JSArray extends JSObject {
	public JSArray(JSObject proto) {
		super(proto);
	}
	
	/*
	 * API functions
	 */
	
	public void push(JSPrimitive value) {
		list.put(list.size(), value);
	}
	
	public JSPrimitive pop() {
		JSPrimitive item = (JSPrimitive) list.get(list.size()-1);
		list.removeKey(list.size()-1);
		return item;
	}
	
	public int getLength() {
		return list.size();
	}
	
	public void append(JSPrimitive[] arr) {
		for (JSPrimitive value : arr)
			push(value);
	}
	
	/*
	 * length property
	 */
	
	OpenIntObjectHashMap list = new OpenIntObjectHashMap();
	
	public JSPrimitive get(String key) {
		if (key.equals("length"))
			return new JSNumber(list.size());
		
		// try index
		int index = ((int) Double.parseDouble(key));
		if (String.valueOf(index).equals(key))
			return (JSPrimitive) list.get(index);
		
		// else
		return super.get(key);
	}
	
	public void set(String key, JSPrimitive value) {
		if (key.equals("length")) {
			double dbl = JSUtils.asNumber(value);
			int len = (int) dbl;
			if (dbl == Double.NaN || len != dbl)
				return;
			// to contract, delete hash values
			if (len < list.size())
				System.out.println("can't resize smaller :(");
				//list = new ArrayList(list.subList(0, len));
			return;
		}
		
		// try index
		int index = Integer.parseInt(key);
		if (String.valueOf(index).equals(key)) {
			list.put(index, value);
			return;
		}
		
		// else
		super.set(key, value);
	}
}
