package mug.modules;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import mug.js.JSEnvironment;
import mug.js.JSFunction;
import mug.js.JSModule;
import mug.js.JSObject;
import mug.js.JSUtils;
import mug.js.java.*;

public class java extends JSModule {
	final JSEnvironment envv = new JSEnvironment();
	
	JSFunction _import = new JSFunction(envv) {		
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			String qn = JSUtils.asString(l0);
			return new ReflectedJSJavaClass(env, Class.forName(qn));
		}
	};
	
	JSFunction _Proxy = new JSFunction(envv) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
		{
			// coerce path
			String qn = JSUtils.asString(l0);
			final JSObject obj = (JSObject) l1;
			Class javaClass = Class.forName(qn);
			
			return new ReflectedJSJavaObject(env, Proxy.newProxyInstance(javaClass.getClassLoader(),
				new Class[] { javaClass },
				new InvocationHandler() {
					@Override
					public Object invoke(Object ths, Method method, Object[] args) throws Throwable {
						JSObject meth = (JSObject) obj.get(method.getName());
						return JSJavaUtils.coerceJavaType(meth.invoke(ths, args), method.getReturnType());
					}
				}));
		}
	};
	
	// exports library
	final JSObject exports = new JSObject(envv) { {
		set("import", _import);
		set("Proxy", _Proxy);
	} };

	@Override
	public JSObject load() throws Exception {
		// running module returns exports object
		return exports;
	}
}
