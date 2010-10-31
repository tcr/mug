package mug.js;

public interface JSObject {
	public JSObject getProto();
	
	public JSPrimitive get(String key);
	public void set(String key, JSPrimitive value);
}