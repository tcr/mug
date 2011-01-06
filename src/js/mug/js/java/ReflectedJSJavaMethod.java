/**
 * 
 */
package mug.js.java;

import java.lang.reflect.Method;

import mug.js.JSEnvironment;
import mug.js.JSFunction;
import mug.js.JSUtils;
import mug.modules.java;

public class ReflectedJSJavaMethod extends JSFunction {
	public String javaName;
	
	public ReflectedJSJavaMethod(JSEnvironment env, String name) {
		super(env);
		this.javaName = name;
	}

	@Override
	public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
	{
		// get parent class
		Class javaClass = ths instanceof ReflectedJSJavaClass ? ((ReflectedJSJavaClass) ths).javaClass : ((ReflectedJSJavaObject) ths).javaObject.getClass();
		// get calling object
		Object prnt = ths instanceof ReflectedJSJavaClass ? ths : ((ReflectedJSJavaObject) ths).javaObject;
		// get arguments
		Object[] args = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		
		// iterate methods
		for (Method m : javaClass.getMethods()) {
			// check if this method is matching
			if (!javaName.equals(m.getName()))
				continue;
			if (!JSJavaUtils.isSupportedFunction(args, m.getParameterTypes()))
				continue;
			// we can call method
			return JSJavaUtils.coerceJSTypes(env, m.invoke(prnt, args));
		}
		
		StringBuilder primtypes = new StringBuilder();
		for (Object prim : args) {
			if (primtypes.length() > 0)
				primtypes.append(", ");
			primtypes.append(prim != null ? prim.getClass() : "null");
		}
		throw new Exception("No Java method found for " + javaClass.getName() + "::" + javaName + "(" + primtypes.toString() + ")");
	}
}