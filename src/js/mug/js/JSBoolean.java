package mug.js;

public class JSBoolean extends JSPrimitive {
	public boolean value = false;

	public JSBoolean(boolean value) {
		this.value = value;
	}
	
	public JSObject toObject(JSTopLevel top) {
		JSObject obj = new JSObject(top.getBooleanPrototype());
		obj.setPrimitiveValue(this);
		return obj;
	}
}