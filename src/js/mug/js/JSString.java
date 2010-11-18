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
		return new JSStringObject(top.getStringPrototype(), this);
	}
	
	public static class JSStringObject extends JSObject {
		public JSStringObject(JSObject proto, JSString value) {
			super(proto);
			setPrimitiveValue(value);
		}
		
		public JSPrimitive get(String key) {
			if (key.equals("length"))
				return new JSNumber(((JSString) this.getPrimitiveValue()).value.length());
			return super.get(key);
		}
	}
}