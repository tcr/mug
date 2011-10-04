package mug.runtime.java;

import java.lang.reflect.Array;

import mug.runtime.JSArray;
import mug.runtime.JSEnvironment;
import mug.runtime.JSNull;
import mug.runtime.JSObject;

public class JSJavaUtils {	
	/**
	 * Checks if the Object array matches the array of types.
	 * Will cast the array to types if possible.
	 */
	
	public static boolean isSupportedFunction(Object[] args, Class[] types) {
		// argument length
		if (args.length != types.length)
			return false;
		// and argument types
		try {
			for (int i = 0; i < types.length; i++)
				args[i] = coerceJavaType(args[i], types[i]);
			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}
	
	/**
	 * Wraps a Java object with a JSObject.
	 */
	
	public static JSJavaObject wrapJavaObject(JSEnvironment env, Object arg) {
		if (arg == null)
			return null;
		if (arg instanceof Class)
			return new ReflectedJSJavaClass(env, (Class) arg);
		if (arg.getClass().isArray())
			return new ReflectedJSJavaArray(env, arg);
		return new ReflectedJSJavaObject(env, arg);
	}
	
	/**
	 * Convert a JS type to a specific Java type, or throw an exception.
	 */
	
	public static Object coerceJavaType(Object arg, Class type) {
		// null
		if (arg == null || arg instanceof JSNull)
			return null;
		
		// js-wrapped object
		if (arg instanceof JSJavaObject) {
			arg = ((JSJavaObject) arg).getJavaObject();
			if (type.isAssignableFrom(arg.getClass()))
				return arg;
		}
		
		// no conversion
		if (type.isAssignableFrom(arg.getClass()))
			return arg;
		
		// booleans
		if (arg instanceof Boolean && type.equals(boolean.class))
			return arg;
		
		// numbers
		//[TODO] expand this
		if (arg instanceof Number) {
			Number num = (Number) arg;
			if (type.equals(int.class) || type.equals(Integer.class))
				return num.intValue();
			if (type.equals(long.class) || type.equals(Long.class))
				return num.longValue();
			if (type.equals(double.class) || type.equals(Double.class))
				return num.doubleValue();
		}
		
		// all choices exhausted
		throw new ClassCastException("Could not convert arg");
	}
	
	/**
	 * Coerce an arbitrary Java object to an accepted JS type.
	 */
	
	public static Object coerceJSTypes(JSEnvironment env, Object arg) {
		if (arg instanceof Character)
			return ((Character) arg).toString();
		if (arg instanceof Number)
			return ((Number) arg).doubleValue();
		if (arg == null ||
			arg instanceof Boolean ||
			arg instanceof String ||
			arg instanceof JSObject)
			return arg;
		return wrapJavaObject(env, arg);
	}
}
