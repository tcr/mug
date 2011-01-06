package mug.js;

public class JSException extends JSObject {
	Exception wrappedException;
	
	public JSException(JSEnvironment env) {
		super(env);
	}
	
	/**
	 * Constructor for wrapping Java Exceptions. 
	 */
	
	public JSException(JSEnvironment env, final Exception e) {
		super(env);
		wrappedException = e;
		set("message", e.getMessage());
		set("toString", new JSFunction(env) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
				return e.getMessage();
			}
		});
	}
}
