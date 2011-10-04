package mug.runtime;

import java.text.DecimalFormat;

public class JSNumber extends JSObject {
	public double value = 0;

	public JSNumber(JSEnvironment env, double value) {
		super(env, env.getNumberPrototype());
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
					if (ths instanceof JSNumber)
						return ((JSNumber) ths).value;
					return Double.NaN;
				}
			});

			defineProperty("toString", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					int base = (int) JSUtils.asNumber(l0);
					double value = JSUtils.asNumber(ths);
					switch (base) {
					case 2: return Integer.toBinaryString((int) value);
					case 8:	return Integer.toOctalString((int) value);
					case 16: return Integer.toHexString((int) value);
					}
					return Double.toString(value);
				}
			});
			
			defineProperty("toFixed", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					double value = JSUtils.asNumber(ths);
					int fixed = (int) JSUtils.asNumber(l0); 
					DecimalFormat formatter = new DecimalFormat("0" +
						(fixed > 0 ? "." + String.format(String.format("%%0%dd", fixed), 0) : ""));
					return formatter.format(value);
				}
			});
		} };
	}
}