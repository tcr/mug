package mug.js;

public class JSBoolean extends JSObject {
	public boolean value = false;

	public JSBoolean(JSObject proto, boolean value) {
		super(proto);
		this.value = value;
	}
}