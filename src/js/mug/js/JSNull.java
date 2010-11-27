package mug.js;

public class JSNull extends JSObject {	
	public JSNull() {
		super(null);
	}

	public static JSNull NULL = new JSNull();
}
