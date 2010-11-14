package mug.js;

import java.util.regex.Pattern;

public class JSRegExp extends JSObject {
	Pattern pattern;
	boolean global;
	
	public JSRegExp(String expr, String flags) {
		pattern = Pattern.compile(expr,
			(flags.indexOf('r') != -1 ? Pattern.CASE_INSENSITIVE : 0));
		global = flags.indexOf('g') != -1;
		
		set("test", new JSFunction () {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0,
					JSPrimitive l1, JSPrimitive l2, JSPrimitive l3,
					JSPrimitive l4, JSPrimitive l5, JSPrimitive l6,
					JSPrimitive l7, JSPrimitive[] rest) throws Exception {
				Pattern pattern = ((JSRegExp) ths).getPattern();
				return pattern.matcher(JSUtils.asString(l0)).find() ? JSAtoms.TRUE : JSAtoms.FALSE;
			}
		});
	}
	
	public Pattern getPattern() {
		return pattern;
	}
	
	public boolean isGlobal() {
		return global;
	}
}
