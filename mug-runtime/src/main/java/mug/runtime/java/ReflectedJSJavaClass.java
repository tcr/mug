/**
 * 
 */
package mug.runtime.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import mug.runtime.JSEnvironment;
import mug.runtime.JSFunction;
import mug.runtime.JSUtils;

public class ReflectedJSJavaClass extends JSFunction implements JSJavaObject {
	protected JSEnvironment env;
	protected Class javaClass;
	
	public Object getJavaObject() {
		return javaClass;
	}
	
	public ReflectedJSJavaClass(final JSEnvironment env, final Class javaClass) {
		super(env);
		this.env = env;
		this.javaClass = javaClass;
		
		// build method objects
		HashMap<String, ReflectedJSJavaMethod> meths = new HashMap();
		// only public classes
		if (Modifier.isPublic(javaClass.getModifiers())) {
			for (Method m : javaClass.getMethods()) {
				// only visible methods
				if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers()))
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
		
		// inner classes
		for (Class c : javaClass.getClasses()) {
			if (!Modifier.isPublic(c.getModifiers()))
				continue;
			this.defineProperty(c.getSimpleName(), new ReflectedJSJavaClass(env, c));
		}
		
		// toString method
		defineProperty("toString", new JSFunction(env) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return "[Class " + javaClass.getName() + "]";
			}
		});
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
	public Object get(String key) throws Exception {
		try {
			Field f = javaClass.getField(key);
			return f.get(javaClass);
		} catch (Exception e) {
			return super.get(key);
		}
	}
	
	@Override
	public void set(String key, Object value) throws Exception {
		try {
			Field f = javaClass.getField(key);
			f.set(javaClass, value);
		} catch (Exception e) {
			super.set(key, value);
		}
	}
}