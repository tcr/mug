package mug.runtime;

public class JSBoolean extends JSObject {
	public boolean value = false;

	public JSBoolean(JSEnvironment env, boolean value) {
		super(env, env.getBooleanPrototype());
		this.value = value;
	}
	
	/**
	 * Environment
	 */

	public static JSObject createPrototype(JSEnvironment env)
	{
		return new JSObject(env) { {
			defineProperty("valueOf", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					if (ths instanceof JSBoolean)
						return ((JSBoolean) ths).value;
					return false;
				}
			});
		} };
	}
}