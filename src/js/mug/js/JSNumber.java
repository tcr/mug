package mug.js;

public class JSNumber extends JSObject {
	public double value = 0;

	public JSNumber(JSEnvironment env, double value) {
		super(env, env.getNumberPrototype());
		this.value = value;
	}
}