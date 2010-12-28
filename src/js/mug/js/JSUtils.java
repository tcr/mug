package mug.js;

import java.util.regex.Pattern;

public class JSUtils {
	/*
	 * utilities
	 */
	
	static public boolean isNull(Object value) {
		return value == null || value == JSNull.NULL;
	}
	
	static public Object[] toJavaArray(JSObject arr) {
		int len = (int) JSUtils.asNumber(arr.get("length"));
		Object[] out = new Object[len];
		for (int j = 0; j < len; j++)
			out[j] = arr.get(String.valueOf(j));
		return out;
	}
	
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
			for (int i = 0; i < rest.length; i++)
				System.arraycopy(rest, 0, args, 8, rest.length);
		return args;
	}
	
	/*
	 * conversions
	 */

	static public boolean asBoolean(Object a) {
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
		
		// java types
		if (a instanceof Number)
			return ((Number) a).doubleValue() > 0;
		
		return false;
	}

	static public double asNumber(Object a) {
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

	static public String asString(Object a) {		
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
		
		// java types
		
		return a.toString();
	}
	
	static public JSObject asJSObject(JSTopLevel top, Object a) {
		// js types
		if (a instanceof JSObject)
			return (JSObject) a;
		if (a == null)
			return null;
		if (a instanceof String)
			return new JSString(top.getStringPrototype(), (String) a);
		if (a instanceof Double)
			return new JSNumber(top.getNumberPrototype(), (Double) a);
		if (a instanceof Boolean)
			return new JSBoolean(top.getBooleanPrototype(), (Boolean) a);
		
		// java types
		System.out.println("DEBUG: Converting " + a + " to Java object...");
		return new mug.modules.java.JSJavaObject(top.getObjectPrototype(), a);
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
		if (a instanceof JSObject)
			return "null";
		if (a == null)
			return "undefined";

		// java types
		
		return "object";
	}
	
	/*
	 * operators
	 */
	
	static public Object add(Object a, Object b) {
		//
		if (a instanceof String || b instanceof String)
			return asString(a) + asString(b);
		return new Double(asNumber(a) + asNumber(b));
	}
	
	//[TODO] these shouldn't be so strict
	
	static public boolean testEquality(Object a, Object b) {
		if (a == null && b == null)
			return true;
		if (a == null || b == null)
			return false;
		return a.equals(b);
	}
	
	static public boolean testInequality(Object a, Object b) {
		if (a == null && b == null)
			return false;
		if (a == null || b == null)
			return true;
		return !a.equals(b);
	}
	
	/*	
	static public boolean testStrictEquality(Object a, Object b) {
		if (a == null && b == null)
			return JSAtoms.TRUE;
		if (a instanceof JSNumber && b instanceof JSNumber)
			return ((JSNumber) a).value == ((JSNumber) b).value ? JSAtoms.TRUE : JSAtoms.FALSE;
		//
		return JSAtoms.FALSE;
	}
	
	static public boolean testStrictInequality(JSPrimitive a, JSPrimitive b) {
		if (a == null && b == null)
			return JSAtoms.FALSE;
		if (a instanceof JSNumber && b instanceof JSNumber)
			return ((JSNumber) a).value == ((JSNumber) b).value ? JSAtoms.FALSE : JSAtoms.TRUE;
		//
		return JSAtoms.FALSE;
	}
	*/
	
	// patterns
	
	static public Pattern compilePattern(String expr, String flags) {
		return Pattern.compile(expr,
		    (flags.indexOf('i') != -1 ? Pattern.CASE_INSENSITIVE : 0) +
		    (flags.indexOf('m') != -1 ? Pattern.MULTILINE : 0));
	}
	
	static public boolean isPatternGlobal(String flags) {
		return flags.indexOf('g') != -1;
	}
}
