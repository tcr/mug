package mug.js;

public class JSNumber extends JSPrimitive {
	public double value = 0;

	public JSNumber(double value) {
		this.value = value;
	}
	
	public JSObject toObject(JSTopLevel top) {
		JSObject obj = new JSObject(top.getNumberPrototype());
		obj.setPrimitiveValue(this);
		return obj;
	}
}