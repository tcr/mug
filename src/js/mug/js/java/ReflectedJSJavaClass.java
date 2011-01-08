/**
 * 
 */
package mug.js.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import mug.js.JSEnvironment;
import mug.js.JSFunction;
import mug.js.JSJavaObject;
import mug.js.JSUtils;
import mug.modules.java;

public class ReflectedJSJavaClass extends JSFunction implements JSJavaObject {
	JSEnvironment top;
	public Class javaClass;
	
	public Object getJavaObject() {
		return javaClass;
	}
	
	public ReflectedJSJavaClass(JSEnvironment env, Class javaClass) {
		super(env);
		this.top = env;
		this.javaClass = javaClass;
		
		for (Method m : javaClass.getMethods()) {
			if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers()))
				continue;
			this.set(m.getName(), new ReflectedJSJavaMethod(env, m.getName()));
		}
		for (Class c : javaClass.getClasses()) {
			if (!Modifier.isPublic(c.getModifiers()))
				continue;
			this.set(c.getSimpleName(), new ReflectedJSJavaClass(env, c));
		}
	}
	
	@Override
	public Object instantiate(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
		return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
	}

	@Override
	public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
	{
		// get arguments
		Object[] args = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		
		// iterate methods
		for (Constructor m : javaClass.getConstructors()) {
			// check if this method is matching
			if (!JSJavaUtils.isSupportedFunction(args, m.getParameterTypes()))
				continue;
			// we can call method
			return new ReflectedJSJavaObject(env, m.newInstance(args));
		}
		return null;
	}
	
	@Override
	public Object get(String key) {
		try {
			Field f = javaClass.getField(key);
			return f.get(javaClass);
		} catch (Exception e) {
			return super.get(key);
		}
	}
	
	@Override
	public void set(String key, Object value) {
		try {
			Field f = javaClass.getField(key);
			f.set(javaClass, value);
		} catch (Exception e) {
			super.set(key, value);
		}
	}
}