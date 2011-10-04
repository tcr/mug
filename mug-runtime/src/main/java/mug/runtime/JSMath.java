package mug.runtime;

public class JSMath {
	/**
	 * Environment
	 */

	public static JSObject createObject(JSEnvironment env)
	{
		return new JSObject(env) { {
			defineProperty("random", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.random();
				}
			});
			
			defineProperty("abs", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.abs(JSUtils.asNumber(l0));
				}
			});
			
			defineProperty("pow", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.pow(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
				}
			});
			
			defineProperty("sqrt", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.sqrt(JSUtils.asNumber(l0));
				}
			});
			
			defineProperty("max", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.max(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
				}
			});
			
			defineProperty("min", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.min(JSUtils.asNumber(l0), JSUtils.asNumber(l1));
				}
			});
			
			defineProperty("ceil", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.ceil(JSUtils.asNumber(l0));
				}
			});
			
			defineProperty("floor", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.floor(JSUtils.asNumber(l0));
				}
			});
			
			defineProperty("round", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return Math.round(JSUtils.asNumber(l0));
				}
			});
		} };
	}
}
