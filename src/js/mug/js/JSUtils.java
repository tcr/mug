package mug.js;

import java.util.regex.Pattern;

public class JSUtils {
	/*
	 * utilities
	 */
	
	static public boolean isNull(JSPrimitive value) {
		return value == null || value == JSAtoms.NULL;
	}
	
	static public JSPrimitive[] toJavaArray(JSObject arr) {
		int len = (int) JSUtils.asNumber(arr.get("length"));
		JSPrimitive[] out = new JSPrimitive[len];
		for (int j = 0; j < len; j++)
			out[j] = arr.get(String.valueOf(j));
		return out;
	}
	
	static public JSPrimitive[] arguments(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) {
		// return an array of arguments
		JSPrimitive[] args = new JSPrimitive[argc];
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

	static public boolean asBoolean(JSPrimitive a) {
		if (a instanceof JSObject && ((JSObject) a).getPrimitiveValue() != null) 
			a = ((JSObject) a).getPrimitiveValue();
		
		if (a instanceof JSBoolean)
			return ((JSBoolean) a).value;
		// 
		// 
		if (a instanceof JSObject)
			return true;
		return false;
	}

	static public double asNumber(JSPrimitive a) {
		if (a instanceof JSObject && ((JSObject) a).getPrimitiveValue() != null)
			a = ((JSObject) a).getPrimitiveValue();
		
		if (a instanceof JSNumber)
			return ((JSNumber) a).value;
		if (a instanceof JSBoolean)
			return ((JSBoolean) a).value ? 1 : 0;
		if (a instanceof JSString)
			try {
				return Double.parseDouble(((JSString) a).value);
			} catch (Exception e) {
				return Double.NaN;
			}
		// 
		// 
		return Double.NaN;
	}

	static public String asString(JSPrimitive a) {
		if (a instanceof JSObject && ((JSObject) a).getPrimitiveValue() != null)
			a = ((JSObject) a).getPrimitiveValue();
		
		if (a instanceof JSString)
			return ((JSString) a).value;
		if (a instanceof JSNumber) {
			double value = ((JSNumber) a).value;
			return (int) value == value ? Integer.toString((int) value) : Double.toString(value);
		}
		if (a instanceof JSBoolean)
			return Boolean.toString(((JSBoolean) a).value);
		if (a instanceof JSFunction)
			return "function () { }";
		if (a instanceof JSObject)
			return "[object Object]";
		if (a instanceof JSNull)
			return "null";
		if (a == null)
			return "undefined";
		return null;
	}
	
	/*
	 * operators
	 */
	
	static public JSPrimitive add(JSPrimitive a, JSPrimitive b) {
		//
		if (a instanceof JSNumber && b instanceof JSNumber)
			return new JSNumber(((JSNumber) a).value + ((JSNumber) b).value);
		return new JSString(asString(a) + asString(b));
	}
	
	//[TODO] these shouldn't be so strict
	
	static public JSBoolean testEquality(JSPrimitive a, JSPrimitive b) {
		if (a == null && b == null)
			return JSAtoms.TRUE;
		if (a instanceof JSNumber && b instanceof JSNumber)
			return ((JSNumber) a).value == ((JSNumber) b).value ? JSAtoms.TRUE : JSAtoms.FALSE;
		//
		return JSAtoms.FALSE;
	}
	
	static public JSBoolean testInequality(JSPrimitive a, JSPrimitive b) {
		if (a == null && b == null)
			return JSAtoms.FALSE;
		if (a instanceof JSNumber && b instanceof JSNumber)
			return ((JSNumber) a).value == ((JSNumber) b).value ? JSAtoms.FALSE : JSAtoms.TRUE;
		//
		return JSAtoms.FALSE;
	}
	
	static public JSBoolean testStrictEquality(JSPrimitive a, JSPrimitive b) {
		if (a == null && b == null)
			return JSAtoms.TRUE;
		if (a instanceof JSNumber && b instanceof JSNumber)
			return ((JSNumber) a).value == ((JSNumber) b).value ? JSAtoms.TRUE : JSAtoms.FALSE;
		//
		return JSAtoms.FALSE;
	}
	
	static public JSBoolean testStrictInequality(JSPrimitive a, JSPrimitive b) {
		if (a == null && b == null)
			return JSAtoms.FALSE;
		if (a instanceof JSNumber && b instanceof JSNumber)
			return ((JSNumber) a).value == ((JSNumber) b).value ? JSAtoms.FALSE : JSAtoms.TRUE;
		//
		return JSAtoms.FALSE;
	}

	static public JSString typeof(JSPrimitive a) {
		if (a instanceof JSString)
			return new JSString("string");
		if (a instanceof JSNumber)
			return new JSString("number");
		if (a instanceof JSBoolean)
			return new JSString("boolean");
		if (a instanceof JSNull)
			return new JSString("null");
		if (a == null)
			return new JSString("undefined");
		if (a instanceof JSFunction)
			return new JSString("function");
		if (a instanceof JSObject)
			return new JSString("object");
		return null;
	}
	
	static public Pattern compilePattern(String expr, String flags) {
		return Pattern.compile(expr,
		    (flags.indexOf('i') != -1 ? Pattern.CASE_INSENSITIVE : 0) +
		    (flags.indexOf('m') != -1 ? Pattern.MULTILINE : 0));
	}
	
	static public boolean isPatternGlobal(String flags) {
		return flags.indexOf('g') != -1;
	}
}
