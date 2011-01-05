package mug.js;

public abstract class JSJavaObject extends JSObject {
	public JSJavaObject(JSObject proto) {
		super(proto);
	}
	
	public abstract Object getJavaObject();
}
