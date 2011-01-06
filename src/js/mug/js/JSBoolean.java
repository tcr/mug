package mug.js;

public class JSBoolean extends JSObject {
	public boolean value = false;

	public JSBoolean(JSEnvironment env, boolean value) {
		super(env, env.getBooleanPrototype());
		this.value = value;
	}
}