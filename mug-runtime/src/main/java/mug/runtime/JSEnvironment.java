package mug.runtime;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSEnvironment {
	/*
	 * prototypes
	 */
	
	final JSObject objectPrototype = new JSObject(this);
	
	public JSObject getObjectPrototype() {
		return objectPrototype;
	}
	
	final JSNull nullObject = new JSNull(this);
	
	public JSNull getNullObject() {
		return nullObject;
	}
	
	final JSObject functionPrototype = JSFunction.createPrototype(this);
	
	public JSObject getFunctionPrototype() {
		return functionPrototype;
	}
	
	// populate object prototype here
	{
		JSObject.populatePrototype(this, objectPrototype);
	}
	
	final JSObject arrayPrototype = JSArray.createPrototype(this);
	
	public JSObject getArrayPrototype() {
		return arrayPrototype;
	}
	
	final JSObject stringPrototype = JSString.createPrototype(this);
	
	public JSObject getStringPrototype() {
		return stringPrototype;
	}
	
	final JSObject numberPrototype = JSNumber.createPrototype(this);
	
	public JSObject getNumberPrototype() {
		return numberPrototype;
	}
	
	final JSObject booleanPrototype = JSBoolean.createPrototype(this);
	
	public JSObject getBooleanPrototype() {
		return booleanPrototype;
	}
	
	final JSObject regexpPrototype = JSRegExp.createPrototype(this);
	
	public JSObject getRegExpPrototype() {
		return regexpPrototype;
	}
	
	final JSObject datePrototype = JSDate.createPrototype(this);
	
	public JSObject getDatePrototype() {
		return datePrototype;
	}
	
	/*
	 * objects/functions
	 */	
	
	static class CustomSecurityManager extends SecurityManager {
		public String getCallerClassName(int callStackDepth) {
			return getClassContext()[callStackDepth].getName();
		}
	}

	private final static CustomSecurityManager securityManager = new CustomSecurityManager();

	public String getCallerClassName(int callStackDepth) {
		return securityManager.getCallerClassName(callStackDepth);
	}
	
	final JSFunction requireFunction = new JSFunction(JSEnvironment.this) {
		String[] defaultpaths;
		{
			if (System.getProperty("mug.require.path") == null)
				defaultpaths = new String[] { "js.std", "" };
			else
				defaultpaths = (System.getProperty("mug.module.path") + ":").split(":");
		}
		
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			// get originating package
			String callingPath = getCallerClassName(3).replaceFirst("\\.[a-zA-Z_\\$0-9]+$", "").replaceAll("\\.", "/");
			
			// searched package
			String[] modulepaths = defaultpaths.clone();
			// check module path
			String moduleName = JSUtils.asString(l0);
			if (moduleName.charAt(0) == '.') {
				// relative path
				modulepaths = new String[] { "" };
				moduleName = callingPath + "/" + moduleName;
			}
			moduleName = moduleName.replaceAll("/\\./", "/").replaceAll("[^/]+/\\.\\./", "");
			moduleName = moduleName.replaceFirst("\\.js$", "").replaceAll("-", "_").replaceAll("/", ".");
			
			// iterating through module paths
			for (String loc : modulepaths) {
				try {
					String search = loc.equals("") ? moduleName : loc + "." + moduleName;
					Class mdClass = JSModule.class.getClassLoader().loadClass(search);
					if (!JSModule.class.isAssignableFrom(mdClass))
						continue;
					JSModule module = (JSModule) mdClass.newInstance();
					return module.load(JSEnvironment.this);
				} catch (ClassNotFoundException e) {
				}
			}
			// exhausted all possibilities
			throw new ClassNotFoundException("Did not find a module \"" + moduleName + "\" in require path.");
		}
	};
	
	final JSObject consoleObject = new JSObject(this) { {
		defineProperty("log", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				for (int i = 0; i < arguments.length; i++) {
					if (i > 0)
						System.out.print(" ");
					System.out.print(arguments[i]);
				}
				System.out.println("");
				return null;
			}
		});
	} }; 
	
	final JSFunction printFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0)
					System.out.print(" ");
				System.out.print(JSUtils.asString(arguments[i]));
			}
			System.out.println("");
			return null;
		}
	};
	
	final JSFunction parseIntFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			int radix = argc > 1 ? (int) JSUtils.asNumber(l1) : 10;
			return (double) Integer.parseInt(JSUtils.asString(l0), radix);
		}
	};
	
	final JSFunction parseFloatFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return Double.parseDouble(JSUtils.asString(l0));
		}
	};
	
	final JSFunction isNaNFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return Double.isNaN(JSUtils.asNumber(l0));
		}
	};
	
	final JSFunction isFiniteFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return !Double.isInfinite(JSUtils.asNumber(l0));
		}
	};
	
	final JSObject mathObject = JSMath.createObject(this);
	
	final JSObject jsonObject = new JSObject(this) { {
		defineProperty("stringify", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSON.stringify(l0);
			}
		});
		
		defineProperty("parse", new JSFunction(JSEnvironment.this) {
			@Override
			public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
			{
				return JSON.parse(JSEnvironment.this, JSUtils.asString(l0));
			}
		});
	} };
	
	final JSFunction setTimeoutFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSObject callback = JSUtils.asJSObject(JSEnvironment.this, l0);
			long milliseconds = (long) JSUtils.asNumber(l1);
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			Object[] passedArgs = new Object[Math.max(arguments.length - 2, 0)];
			if (arguments.length > 2)
				System.arraycopy(arguments, 2, passedArgs, 0, arguments.length - 2);
			return JSConcurrency.setTimeout(callback, passedArgs, milliseconds);
		}
	};
	
	final JSFunction setIntervalFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSObject callback = JSUtils.asJSObject(JSEnvironment.this, l0);
			long milliseconds = (long) JSUtils.asNumber(l1);
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			Object[] passedArgs = new Object[Math.max(arguments.length - 2, 0)];
			if (arguments.length > 2)
				System.arraycopy(arguments, 2, passedArgs, 0, arguments.length - 2);
			return JSConcurrency.setInterval(callback, passedArgs, milliseconds);
		}
	};
	
	final JSFunction clearTimeoutFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSConcurrency.clearTimeout((long) JSUtils.asNumber(l0));
			return null;
		}
	};
	
	final JSFunction clearIntervalFunction = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			JSConcurrency.clearInterval((long) JSUtils.asNumber(l0));
			return null;
		}
	};
	
	final JSArray argumentsObject = new JSArray(this, 0);
	
	/*
	 * constructors
	 */
	
	final JSFunction objectConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return JSUtils.asJSObject(JSEnvironment.this, l0);
		}
		
		{
			setPrototype(getObjectPrototype());
		}
	};
	
	final JSFunction functionConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			throw new Exception("Function constructor not implemented.");
		}
		
		{
			setPrototype(getFunctionPrototype());
		}
	};
	
	final JSFunction booleanConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return JSUtils.asBoolean(l0);
		}
		
		{
			setPrototype(getBooleanPrototype());
		}
	};
	
	final JSFunction numberConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return JSUtils.asNumber(l0);
		}
		
		{
			setPrototype(getNumberPrototype());
		}
	};
	
	final JSFunction stringConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return JSUtils.asString(l0);
		}
		
		{
			setPrototype(getStringPrototype());
		}
	};
	
	final JSFunction arrayConstructor = new JSFunction(JSEnvironment.this) {
		public Object instantiate(int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest)
				throws Exception {
			
			// single-argument number constructor
			if (argc == 1 && l0 instanceof Double) {
				int length = (int) JSUtils.asNumber(l0);
				JSArray arr = new JSArray(JSEnvironment.this, length);
				for (int i = 0; i < length; i++)
					arr.push(null);
				return arr;
			}
			
			// literal declaration
			Object[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			JSArray arr = new JSArray(JSEnvironment.this, arguments.length);
			for (int i = 0; i < arguments.length; i++)
				arr.push(arguments[i]);
			return arr;
		}
		
		{
			setPrototype(getArrayPrototype());
		}
	};
	{
		getArrayPrototype().defineProperty("constructor", arrayConstructor);
	};
	
	final JSFunction dateConstructor = new JSFunction(JSEnvironment.this) {
		@Override
		public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception {
			if (argc == 1) {
				//[TODO]
			}
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			return new JSDate(JSEnvironment.this, cal);
		}
		
		{
			setPrototype(getDatePrototype()); 
		}
	};
	
	/*
	 * top-level scope accessors 
	 */

	public Object get_require() { return requireFunction; }
	public Object get_print() { return printFunction; }
	public Object get_console() { return consoleObject; }
	public Object get_parseFloat() { return parseFloatFunction; }
	public Object get_parseInt() { return parseIntFunction; }
	public Object get_isNaN() { return isNaNFunction; }
	public Object get_isFinite() { return isFiniteFunction; }
	public Object get_Math() { return mathObject; }
	public Object get_JSON() { return jsonObject; }
	public Object get_Object() { return objectConstructor; }
	public Object get_Function() { return functionConstructor; }
	public Object get_Boolean() { return booleanConstructor; }
	public Object get_String() { return stringConstructor; }
	public Object get_Array() { return arrayConstructor; }
	public Object get_Number() { return numberConstructor; }
	public Object get_Date() { return dateConstructor; }
	public Object get_Error() { return null; }
	public Object get_SyntaxError() { return null; }
	public Object get_RegExp() { return null; }
	public Object get_setTimeout() { return setTimeoutFunction; }
	public Object get_setInterval() { return setIntervalFunction; }
	public Object get_clearTimeout() { return clearTimeoutFunction; }
	public Object get_clearInterval() { return clearIntervalFunction; }
	public Object get_arguments() { return argumentsObject; } 

	/*
	 * constructor
	 */
	
	String[] commandLineArgs;
	
	public JSEnvironment() {
		this(new String[] { });
	}
	
	public JSEnvironment(String[] args) {
		commandLineArgs = args;
		
		// load arguments
		try {
			argumentsObject.load(args);
		} catch (Exception e) { }
	}
	
	public String[] getCommandLineArgs() {
		return commandLineArgs;
	}
}
