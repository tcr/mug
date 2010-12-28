package mug.modules;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import mug.js.JSBoolean;
import mug.js.JSFunction;
import mug.js.JSModule;
import mug.js.JSNumber;
import mug.js.JSObject;
import mug.js.JSString;
import mug.js.JSTopLevel;
import mug.js.JSUtils;

public class java extends JSModule {
	final JSTopLevel top = new JSTopLevel();
	
	JSFunction _import = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			String qn = JSUtils.asString(l0);
			return new JSJavaClass(top, Class.forName(qn));
		}
	};
	
	JSFunction _Proxy = new JSFunction (top.getFunctionPrototype()) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
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
							return castObject(meth.invoke(ths, args), method.getReturnType());
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
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			// get arguments
			Object[] args = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			
			// iterate methods
			for (Constructor m : javaClass.getConstructors()) {
				// check if this method is matching
				if (!isSupportedFunction(args, m.getParameterTypes()))
					continue;
				// we can call method
				return new JSJavaObject(top.getObjectPrototype(), m.newInstance(args));
			}
			return null;
		}
		
		@Override
		public Object get(String key) {
			try {
				Field f = javaClass.getField(key);
				return f.get(javaClass);
			} catch (Exception e) {
				return super.get(key);
			}
		}
		
		@Override
		public void set(String key, Object value) {
			try {
				Field f = javaClass.getField(key);
				f.set(javaClass, value);
			} catch (Exception e) {
				super.set(key, value);
			}
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
		
		@Override
		public Object get(String key) {
			try {
				Field f = javaObject.getClass().getField(key);
				return f.get(javaObject);
			} catch (Exception e) {
				return super.get(key);
			}
		}
		
		@Override
		public void set(String key, Object value) {
			try {
				Field f = javaObject.getClass().getField(key);
				f.set(javaObject, value);
			} catch (Exception e) {
				super.set(key, value);
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
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			// get parent class
			Class javaClass = ths instanceof JSJavaClass ? ((JSJavaClass) ths).javaClass : ((JSJavaObject) ths).javaObject.getClass();
			// get calling object
			Object prnt = ths instanceof JSJavaClass ? ths : ((JSJavaObject) ths).javaObject;
			// get arguments
			Object[] args = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			
			// iterate methods
			for (Method m : javaClass.getMethods()) {
				// check if this method is matching
				if (!javaName.equals(m.getName()))
					continue;
				if (!isSupportedFunction(args, m.getParameterTypes()))
					continue;
				// we can call method
				return m.invoke(prnt, args);
			}
			
			StringBuilder primtypes = new StringBuilder();
			for (Object prim : args) {
				if (primtypes.length() > 0)
					primtypes.append(", ");
				primtypes.append(prim != null ? prim.getClass() : "null");
			}
			throw new Exception("No Java method found for " + javaClass.getName() + "::" + javaName + "(" + primtypes.toString() + ")");
		}
	};
	
	/**
	 * Checks if the Object array matches the array of types.
	 * Will cast the array to types if possible.
	 */
	
	@SuppressWarnings("unchecked")
	static boolean isSupportedFunction(Object[] args, Class[] types) {
		// argument length
		if (args.length != types.length)
			return false;
		// and argument types
		try {
			for (int i = 0; i < types.length; i++)
				args[i] = castObject(args[i], types[i]);
			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}
	
	static Object castObject(Object arg, Class type) {
		// no conversion
		if (arg == null)
			return null;
		if (type.isAssignableFrom(arg.getClass()))
			return arg;
		
		// js-wrapped object
		if (arg instanceof JSJavaObject) {
			arg = ((JSJavaObject) arg).javaObject;
			if (type.isAssignableFrom(arg.getClass()))
				return arg;
		}
		
		// booleans
		if (arg instanceof Boolean && type.equals(boolean.class))
			return arg;
		
		// numbers
		//[TODO] expand this
		if (arg instanceof Number) {
			Number num = (Number) arg;
			if (type.equals(int.class) || type.equals(Integer.class))
				return new Integer(num.intValue());
		}
		
		// all choices exhausted
		throw new ClassCastException("Could not convert arg");
	}
}
