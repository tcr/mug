package mug.runtime;

import java.lang.reflect.Array;
import java.util.regex.Pattern;

public class JSUtils {
	/*
	 * conversions
	 */
	
	/**
	 * Coerces an Object to a JS boolean based on JavaScript truthy values.
	 */

	static public boolean asBoolean(Object a) throws Exception {
		if (a instanceof JSObject)
			a = ((JSObject) a).valueOf();
		
		// js types
		if (a instanceof Boolean)
			return (Boolean) a;
		if (a instanceof Double)
			return (Double) a != 0;
		if (a instanceof String)
			return ((String) a).length() > 0;
		if (a == null)
			return false;
		if (a instanceof JSNull)
			return false;
		if (a instanceof JSObject)
			return true;
		
		// java types
		if (a instanceof Number)
			return ((Number) a).doubleValue() > 0;
		
		return false;
	}
	
	/**
	 * Coerces an Object to a JS Double.
	 */

	static public double asNumber(Object a) throws Exception {
		if (a instanceof JSObject)
			a = ((JSObject) a).valueOf();
		
		// js types
		if (a instanceof Double)
			return (Double) a;
		if (a instanceof Boolean)
			return (Boolean) a ? 1 : 0;
		if (a instanceof String)
			try {
				return Double.parseDouble((String) a);
			} catch (Exception e) {
				return Double.NaN;
			}
		
		// java types
		if (a instanceof Number)
			return ((Number) a).doubleValue();

		return Double.NaN;
	}
	
	/**
	 * Coerces an Object to a JS String.
	 */

	static public String asString(Object a) throws Exception {		
		if (a instanceof JSObject)
			a = ((JSObject) a).valueOf();
		
		// js types
		if (a instanceof String)
			return (String) a;
		if (a instanceof Boolean)
			return (Boolean) a ? "1" : "0";
		if (a instanceof Double) {
			double value = (Double) a;
			int valuei = (int) value;
			return valuei == value ? Integer.toString(valuei) : Double.toString(value);
		}
		if (a == null)
			return "undefined";
		if (a instanceof JSNull)
			return "null";
		if (a instanceof JSObject)
			return ((JSObject) a).toString();
		
		// exhausted possibilities
		return a.toString();
	}
	
	/**
	 * Coerces a generic Object to a JSObject. Autoboxes primitives.
	 */
	
	static public JSObject asJSObject(JSEnvironment env, Object a) {
		// js types
		if (a instanceof JSObject)
			return (JSObject) a;
		if (a == null)
			return null;
		if (a instanceof String)
			return new JSString(env, (String) a);
		if (a instanceof Double)
			return new JSNumber(env, (Double) a);
		if (a instanceof Boolean)
			return new JSBoolean(env, (Boolean) a);
		throw new ClassCastException("Could not convert \"" + a + "\" to JSObject.");
	}
	
	/*
	 * utilities
	 */
	
	/**
	 * Converts a JS array or array-like object to a Java array.
	 */
	
	static public Object[] coerceJavaArray(JSObject arr) throws Exception {
		// actual arrays
		if (arr instanceof JSArray)
			return ((JSArray) arr).toArray();
		// array-like objects
		int len = (int) JSUtils.asNumber(arr.get("length"));
		Object[] out = new Object[len];
		for (int j = 0; j < len; j++)
			out[j] = arr.get(j);
		return out;
	}
	
	/**
	 * Utility method to determine whether a value is null or undefined.
	 */
	
	static public boolean isNull(Object value) {
		return value == null || value instanceof JSNull;
	}
	
	/**
	 * Utility function to get an arguments object from invoke(...) parameters.
	 */
	
	static public Object[] arguments(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) {
		// return an array of arguments
		Object[] args = new Object[argc];
		// add arguments
		switch (argc > 8 ? 8 : argc) {
		case 8: args[7] = l7;
		case 7: args[6] = l6;
		case 6: args[5] = l5;
		case 5: args[4] = l4;
		case 4: args[3] = l3;
		case 3: args[2] = l2;
		case 2: args[1] = l1;
		case 1: args[0] = l0;
		}
		if (rest != null)
			System.arraycopy(rest, 0, args, 8, rest.length);
		return args;
	}
	
	/**
	 * Creates an arguments object from a Java array. Used by scope-level objects.
	 */
	
	static public JSObject createArgumentsObject(JSEnvironment env, Object[] arr) throws Exception {
		JSArray arguments = new JSArray(env, arr.length);
		for (int i = 0; i < arr.length; i++)
			arguments.set(i, arr[i]);
		arguments.defineProperty("length", new Double(arr.length));
		return arguments;
	}
	
	/**
	 * Precompile regexp patterns.
	 */
	
	static public Pattern compilePattern(String expr, String flags) {
		return Pattern.compile(expr,
		    (flags.indexOf('i') != -1 ? Pattern.CASE_INSENSITIVE : 0) +
		    (flags.indexOf('m') != -1 ? Pattern.MULTILINE : 0));
	}
	
	/**
	 * Determine if a pattern is global.
	 */
	
	static public boolean isPatternGlobal(String flags) {
		return flags.indexOf('g') != -1;
	}
	
	/*
	 * operators
	 */
	
	/**
	 * Add operation.
	 */
	
	static public Object add(Object a, Object b) throws Exception {
		//
		if (a instanceof String || b instanceof String)
			return asString(a) + asString(b);
		return new Double(asNumber(a) + asNumber(b));
	}
	
	//[TODO] implement this fully	
	static public boolean testEquality(Object a, Object b) {
		if (a == null && b == null) // undefined == undefined
			return true;
		if ((a == null || b == null) && (a instanceof JSNull || b instanceof JSNull)) // undefined or null
			return true;
		if ((a == null && b instanceof JSNull) || (a == null && b instanceof JSNull)) // anything == undefined
			return true;
		if (a == null || b == null) // anything equals null
			return false;
		return a.equals(b);
	}

	//[TODO] implement this
	static public boolean testStrictEquality(Object a, Object b) {
		return testEquality(a, b);
	}
	
	static public String typeof(Object a) {
		// js types
		if (a instanceof Boolean)
			return "boolean";
		if (a instanceof Double)
			return "number";
		if (a instanceof String)
			return "string";
		if (a instanceof JSFunction)
			return "function";
		if (a instanceof JSNull)
			return "null";
		if (a == null)
			return "undefined";
		if (a instanceof JSObject)
			return "object";

		// exhausted possibilities		
		return "object";
	}
}
