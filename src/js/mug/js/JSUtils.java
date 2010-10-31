package mug.js;

public class JSUtils {
	/*
	 * conversions
	 */

	static public boolean asBoolean(JSPrimitive a) {
		if (a instanceof JSBoolean)
			return ((JSBoolean) a).value;
		// 
		// 
		// 
		return false;
	}

	static public double asNumber(JSPrimitive a) {
		if (a instanceof JSNumber)
			return ((JSNumber) a).value;
		if (a instanceof JSBoolean)
			return ((JSBoolean) a).value ? 1 : 0;
		// 
		// 
		return Double.NaN;
	}

	static public String asString(JSPrimitive a) {
		if (a instanceof JSString)
			return ((JSString) a).value;
		if (a instanceof JSNumber) {
			double value = ((JSNumber) a).value;
			return (int) value == value ? Integer.toString((int) value) : Double.toString(value);
		}
		if (a instanceof JSBoolean)
			return Boolean.toString(((JSBoolean) a).value);
		// 
		return null;
	}
	
	/*
	 * arithmetic
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
}
