package mug.js;

public abstract class JSJavaObject extends JSObject {
	public JSJavaObject(JSEnvironment env) {
		super(env);
	}
	
	public abstract Object getJavaObject();
}
