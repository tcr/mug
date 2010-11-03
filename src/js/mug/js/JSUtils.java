package mug.js;

public class JSUtils {
	/*
	 * API
	 */
	
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
