/**
 * 
 */
package mug.runtime.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import mug.runtime.JSEnvironment;
import mug.runtime.JSObject;

public class ReflectedJSJavaObject extends JSObject implements JSJavaObject {
	protected Object javaObject;
	protected Class javaClass;
	
	public Object getJavaObject() {
		return javaObject;
	}
	
	public ReflectedJSJavaObject(JSEnvironment env, Object javaObject) {
		super(env);
		this.javaObject = javaObject;
		this.javaClass = javaObject.getClass();

		// get list of classes and interfaces
		Class[] interfaces = javaClass.getInterfaces();
		Class[] classes = new Class[1 + interfaces.length];
		classes[0] = javaClass;
		System.arraycopy(interfaces, 0, classes, 1, interfaces.length);
		// build method objects
		HashMap<String, ReflectedJSJavaMethod> meths = new HashMap();
		for (Class cls : classes) {
			// only public classes
			if (!Modifier.isPublic(cls.getModifiers()))
				continue;
			
			for (Method m : cls.getMethods()) {
				// only visible methods
				if (!Modifier.isPublic(m.getModifiers()))
					continue;
				// if method hasn't been constructed yet, add it
				if (!meths.containsKey(m.getName())) {
					meths.put(m.getName(), new ReflectedJSJavaMethod(env, javaClass, m.getName()));
					this.defineProperty(m.getName(), meths.get(m.getName()));
				}
				// add method
				meths.get(m.getName()).addMethod(m);
			}
		}
	}
	
	@Override
	public Object get(String key) throws Exception {
		try {
			Field f = javaObject.getClass().getField(key);
			return f.get(javaObject);
		} catch (Exception e) {
			return super.get(key);
		}
	}
	
	@Override
	public void set(String key, Object value) throws Exception {
		try {
			Field f = javaObject.getClass().getField(key);
			f.set(javaObject, value);
		} catch (Exception e) {
			super.set(key, value);
		}
	}
}