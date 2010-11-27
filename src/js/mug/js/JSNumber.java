package mug.js;

public class JSNumber extends JSObject {
	public double value = 0;

	public JSNumber(JSObject proto, double value) {
		super(proto);
		this.value = value;
	}
}