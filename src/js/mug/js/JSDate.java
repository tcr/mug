package mug.js;

import java.util.Calendar;

/**
 * 
 */

public class JSDate extends JSObject {
	Calendar value;
	
	public JSDate(JSEnvironment env, Calendar value) {
		super(env, env.getDatePrototype());
		this.value = value;
	}
}