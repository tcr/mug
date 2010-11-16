package mug.js;

import java.util.regex.Pattern;

public class JSRegExp extends JSObject {
	Pattern pattern;
	boolean global;
	
	public JSRegExp(JSObject proto, Pattern pattern, boolean global) {
		super(proto);
		this.pattern = pattern;
		this.global = global;
	}
	
	public Pattern getPattern() {
		return pattern;
	}
	
	public boolean isGlobal() {
		return global;
	}
}
