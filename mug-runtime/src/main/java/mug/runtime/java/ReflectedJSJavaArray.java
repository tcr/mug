/**
 * 
 */
package mug.runtime.java;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import mug.runtime.JSArray;
import mug.runtime.JSEnvironment;
import mug.runtime.JSObject;
import mug.runtime.JSUtils;

public class ReflectedJSJavaArray extends ReflectedJSJavaObject {
	public ReflectedJSJavaArray(JSEnvironment env, Object javaObject) {
		super(env, javaObject);
	}

	@Override
	public Object get(int key) throws Exception {
		return Array.get(javaObject, key);
	}
	
	public Object get(Object key) throws Exception {
		if (key instanceof JSObject)
			key = ((JSObject) key).valueOf();
		
		// integer index
		if (key instanceof Double) {
			double dbl = (Double) key;
			int index = (int) dbl;
			if (index == dbl)
				return Array.get(javaObject, index);
		}
		//[TODO] instanceof Number
		
		return get(JSUtils.asString(key));
	}
	
	@Override
	public void set(int index, Object value) throws Exception {
		Array.set(javaObject, index, value);
	}
	
	public void set(Object key, Object value) throws Exception {
		if (key instanceof JSObject)
			key = ((JSObject) key).valueOf();
		
		// integer index
		if (key instanceof Double) {
			double dbl = (Double) key;
			int index = (int) dbl;
			if (index == dbl) {
				Array.set(javaObject, index, value);
				return;	
			}
		}
		//[TODO] instanceof Number
		
		set(JSUtils.asString(key), value);
	}
}