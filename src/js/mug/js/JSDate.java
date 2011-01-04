package mug.js;

import java.util.Calendar;

/**
 * 
 */

public class JSDate extends JSObject {
	Calendar value;
	
	public JSDate(JSObject proto, Calendar value) {
		super(proto);
		this.value = value;
	}
}