/**
 * 
 */
package mug.js.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import mug.js.JSEnvironment;
import mug.js.JSJavaObject;
import mug.js.JSObject;

public class ReflectedJSJavaObject extends JSObject implements JSJavaObject {
	Object javaObject;
	
	public Object getJavaObject() {
		return javaObject;
	}
	
	public ReflectedJSJavaObject(JSEnvironment env, Object javaObject) {
		super(env);
		this.javaObject = javaObject;	
		
		for (Method m : javaObject.getClass().getMethods()) {
			if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers()))
				continue;
			this.set(m.getName(), new ReflectedJSJavaMethod(env, m.getName()));
		}
	}
	
	@Override
	public Object get(String key) {
		try {
			Field f = javaObject.getClass().getField(key);
			return f.get(javaObject);
		} catch (Exception e) {
			return super.get(key);
		}
	}
	
	@Override
	public void set(String key, Object value) {
		try {
			Field f = javaObject.getClass().getField(key);
			f.set(javaObject, value);
		} catch (Exception e) {
			super.set(key, value);
		}
	}
}