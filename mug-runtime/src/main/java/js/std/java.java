package js.std;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

import mug.runtime.JSConcurrency;
import mug.runtime.JSEnvironment;
import mug.runtime.JSFunction;
import mug.runtime.JSModule;
import mug.runtime.JSObject;
import mug.runtime.JSUtils;
import mug.runtime.java.JSJavaUtils;
import mug.runtime.java.ReflectedJSJavaClass;
import mug.runtime.java.ReflectedJSJavaObject;

public class java extends JSModule {
	static class Package extends JSObject {
		String qn;
		
		public Package(JSEnvironment env, final String qn) {
			super(env);
			this.qn = qn;
			
			defineProperty("toString", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0,  Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
					return "[Package " + qn + "]";
				}
			});
		}
		
		public Object get(String path) throws Exception {
			if (path.equals("toString"))
				return super.get(path);
			return importMethod(env, qn + "." + path);
		}
	}
	
	static Object importMethod(JSEnvironment env, String qn) {
		try {
			return new ReflectedJSJavaClass(env, Class.forName(qn));
		} catch (ClassNotFoundException e) {
			return new Package(env, qn);
		}
	}
	
	@Override
	public JSObject load(JSEnvironment env) throws Exception
	{
		final JSFunction _import = new JSFunction(env) {		
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return importMethod(env, JSUtils.asString(l0));
			}
		};
		
		final JSFunction _Proxy = new JSFunction(env) {
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
						public Object invoke(Object ths, Method method, Object[] args) throws Throwable {
							JSObject meth = (JSObject) obj.get(method.getName());
							return JSJavaUtils.coerceJavaType(meth.invoke(ths, args), method.getReturnType());
						}
					}));
			}
		};
		
		// exports library
		final JSObject exports = new JSObject(env) { {
			defineProperty("import", _import);
			defineProperty("Proxy", _Proxy);
			
			// default top-level java packages
			String[] pkgs = new String[] { "io", "nio", "lang", "util" };
			for (String pkg : pkgs)
				defineProperty(pkg, importMethod(env, "java." + pkg));
		} };

		// running module returns exports object
		return exports;
	}
}
