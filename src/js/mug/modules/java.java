package mug.modules;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import mug.js.JSAtoms;
import mug.js.JSBoolean;
import mug.js.JSFunction;
import mug.js.JSModule;
import mug.js.JSNumber;
import mug.js.JSObject;
import mug.js.JSPrimitive;
import mug.js.JSString;
import mug.js.JSTopLevel;
import mug.js.JSUtils;

public class java extends JSModule {
	final JSTopLevel top = new JSTopLevel();
	
	JSFunction _import = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
		{
			String qn = JSUtils.asString(l0);
			return new JSJavaClass(top, Class.forName(qn));
		}
	};
	
	JSFunction _Proxy = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
		{
			// coerce path
			String qn = JSUtils.asString(l0);
			final JSObject obj = (JSObject) l1;
			Class javaClass = Class.forName(qn);
			
			return new JSJavaObject(top.getObjectPrototype(), Proxy.newProxyInstance(javaClass.getClassLoader(),
					new Class[] { javaClass },
					new InvocationHandler() {
						@Override
						public Object invoke(Object ths, Method method, Object[] args) throws Throwable {
							JSObject meth = (JSObject) obj.get(method.getName());
							JSObject l0 = null, l1 = null, l2 = null, l3 = null, l4 = null, l5 = null, l6 = null, l7 = null;
							switch (args.length) {
							case 8: l7 = new JSJavaObject(top.getObjectPrototype(), args[7]);
							case 7: l6 = new JSJavaObject(top.getObjectPrototype(), args[6]);
							case 6: l5 = new JSJavaObject(top.getObjectPrototype(), args[5]);
							case 5: l4 = new JSJavaObject(top.getObjectPrototype(), args[4]);
							case 4: l3 = new JSJavaObject(top.getObjectPrototype(), args[3]);
							case 3: l2 = new JSJavaObject(top.getObjectPrototype(), args[2]);
							case 2: l1 = new JSJavaObject(top.getObjectPrototype(), args[1]);
							case 1: l0 = new JSJavaObject(top.getObjectPrototype(), args[0]);
							}
							return unwrap(meth.invoke(new JSJavaObject(top.getObjectPrototype(), ths)), method.getReturnType());
							//return unwrap(meth.invoke(new JSJavaObject(top.getObjectPrototype(), ths), l0, l1, l2, l3, l4, l5, l6, l7, null), method.getReturnType());
						}
					}));
		}
	};
	
	// exports library
	final JSObject exports = new JSObject(top.getObjectPrototype()) { {
		set("import", _import);
		set("Proxy", _Proxy);
	} };
	
	@Override
	public JSObject load() throws Exception {
		// running module returns exports object
		return exports;
	}
	
	/*
	 * classes
	 */
	
	static public class JSJavaClass extends JSFunction {
		JSTopLevel top;
		public Class javaClass;
		
		public JSJavaClass(JSTopLevel top, Class javaClass) {
			super(top.getFunctionPrototype());
			this.top = top;
			this.javaClass = javaClass;
			
			for (Method m : javaClass.getMethods()) {
				if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers()))
					continue;
				this.set(m.getName(), new JSJavaMethod(top.getFunctionPrototype(), m.getName()));
			}
		}

		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
		{
			// iterate methods
			methodLoop: for (Constructor m : javaClass.getConstructors()) {
				// see if arguments are a match
				JSPrimitive[] primitives = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				Class[] types = m.getParameterTypes();
				Object[] results;
				try {
					results = unwrapArgs(primitives, types);
				} catch (ClassCastException e) {
					continue methodLoop;
				}
				
				// we can call method
				return new JSJavaObject(top.getObjectPrototype(), m.newInstance(results));
			}
			return null;
		}
	};
	
	static public class JSJavaObject extends JSObject {
		public Object javaObject;
		
		public JSJavaObject(JSObject proto, Object javaObject) {
			super(null);
			this.javaObject = javaObject;	
			
			for (Method m : javaObject.getClass().getMethods()) {
				if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers()))
					continue;
				this.set(m.getName(), new JSJavaMethod(proto, m.getName()));
			}
		}
	};
	
	static public class JSJavaMethod extends JSFunction {
		public String javaName;
		
		public JSJavaMethod(JSObject proto, String name) {
			super(proto);
			this.javaName = name;
		}

		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
		{			
			// get parent class
			Class javaClass = ths instanceof JSJavaClass ? ((JSJavaClass) ths).javaClass : ((JSJavaObject) ths).javaObject.getClass();
			// get calling object
			Object prnt = ths instanceof JSJavaClass ? ths : ((JSJavaObject) ths).javaObject;
			// iterate methods
			methodLoop: for (Method m : javaClass.getMethods()) {
				// rudimentary arg length check
				if (!m.getName().equals(javaName))
					continue;

				// see if arguments are a match
				JSPrimitive[] primitives = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				Class[] types = m.getParameterTypes();
				Object[] results;
				try {
					results = unwrapArgs(primitives, types);
				} catch (ClassCastException e) {
					continue methodLoop;
				}
				
				// we can call method
				return wrap(m.invoke(prnt, results));
			}
			
			throw new Exception("No Java method found for " + javaClass.getName() + "::" + javaName);
		}
	};
	
	public static JSPrimitive wrap(Object a) {
		if (a instanceof String)
			return new JSString((String) a);
		if (a == null)
			return null;
		return null;
		//return new JSJavaObject(null, a);
	}
	
	public static Object[] unwrapArgs(JSPrimitive[] primitives, Class[] types) {
		if (primitives.length != types.length)
			throw new ClassCastException("Invalid number of args");
		Object[] results = new Object[types.length];
		for (int i = 0; i < types.length; i++)
			results[i] = primitives[i] == null ? null : unwrap(primitives[i], types[i]);
		return results;		
	}
	
	public static Object unwrap(JSPrimitive p, Class javaClass) {		
		// handle JS primitives first
		if (p instanceof JSString) {
			String value = ((JSString) p).value;
			if (javaClass.equals(String.class))
				return value;
		}
		if (p instanceof JSNumber) {
			double value = ((JSNumber) p).value;
			if (javaClass.equals(Integer.class) || javaClass.equals(int.class))
				return (int) value;
			if (javaClass.equals(Double.class) || javaClass.equals(double.class))
				return value;
			if (javaClass.equals(Float.class) || javaClass.equals(float.class))
				return (float) value;
			if (javaClass.equals(Long.class) || javaClass.equals(long.class))
				return (long) value;
		}
		if (p instanceof JSBoolean) {
			boolean value = ((JSBoolean) p).value;
			if (javaClass.equals(Boolean.class) || javaClass.equals(boolean.class))
				return value;
		}
		if (p instanceof JSJavaObject) {
			Object javaObject = ((JSJavaObject) p).javaObject;
			if (javaClass.isAssignableFrom(javaObject.getClass()))
				return javaClass.cast(javaObject);
		}
		if (p == null || p.equals(JSAtoms.NULL))
			return null;
		
		throw new ClassCastException("Cannot convert JS type " + p.getClass() + " to " + javaClass.getName());
	}
}
