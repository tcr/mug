package mug.js;


/**
 * 
 */

public class JSString extends JSPrimitive {
	public String value = "";

	public JSString(String value) {
		this.value = value;
	}
	
	public JSObject toObject(JSTopLevel top) {
		JSObject obj = new JSObject(top.getStringPrototype());
		obj.setPrimitiveValue(this);
		return obj;
	}
}