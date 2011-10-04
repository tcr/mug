/**
 * 
 */
package mug.runtime.java;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import js.std.java;

import mug.runtime.JSEnvironment;
import mug.runtime.JSFunction;
import mug.runtime.JSUtils;

public class ReflectedJSJavaMethod extends JSFunction {
	Class javaClass;
	String javaName;
	ArrayList<Method> methods = new ArrayList();
	
	public void addMethod(Method m) {
		methods.add(m);
	}
	
	public ReflectedJSJavaMethod(JSEnvironment env, Class javaClass, String javaName) {
		super(env);
		this.javaClass = javaClass;
		this.javaName = javaName;
	}

	@Override
	public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
	{
		// get calling object
		Object thisObj = ths instanceof ReflectedJSJavaClass ? ths : ((ReflectedJSJavaObject) ths).javaObject;
			
		// get arguments
		Object[] args = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		// iterate methods
		for (Method m : methods) {
			// check if this method is matching
			Object[] coercedArgs = args.clone();
			if (!JSJavaUtils.isSupportedFunction(coercedArgs, m.getParameterTypes()))
				continue;
			// we can call method
			return JSJavaUtils.coerceJSTypes(env, m.invoke(thisObj, coercedArgs));
		}
		
		// no matching classes found
		StringBuilder primtypes = new StringBuilder();
		for (Object prim : args) {
			if (primtypes.length() > 0)
				primtypes.append(", ");
			primtypes.append(prim != null ? prim.getClass() : "null");
		}
		throw new Exception("No Java method found for " + javaClass.getName() + "::" + javaName + "(" + primtypes.toString() + ")");
	}
}