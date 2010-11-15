package mug.js;

import mug.Modules;

public class JSTopLevel {
	/*
	 * objects/constructors
	 */	
	
	final JSFunction requireFunction = new JSFunction() {
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			return Modules.getModule(JSUtils.asString(l0)).load();
		}
	};
	
	final JSObject exportsObject = new JSObject() { };
	
	final JSFunction printFunction = new JSFunction() {
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			System.out.println(JSUtils.asString(l0) + " <" + l0 + ">");
			return null;
		}
	};
	
	final JSObject mathObject = new JSObject() { {
		set("sqrt", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
					throws Exception {
				return new JSNumber(Math.sqrt(JSUtils.asNumber(l0)));
			}
		});
	} };
	
	final JSFunction arrayConstructor = new JSFunction() {
		public JSPrimitive instantiate(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
			return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			JSArray obj = new JSArray(arrayPrototype);
		
			// single-argument constructor
			if (argc == 1) {
				obj.set("length", l0);
				return obj;
			}
			// literal declaration
			JSPrimitive[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			for (int i = 0; i < arguments.length; i++)
				obj.set(String.valueOf(i), arguments[i]);
			
			return obj;
		}
	};
	
	/*
	 * prototypes
	 */
	
	final JSObject arrayPrototype = new JSObject() { {
		set("concat", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				// concatenate arrays to new array
				JSArray out = new JSArray(arrayPrototype);
				JSPrimitive[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				for (int j = 0, max = (int) JSUtils.asNumber(ths.get("length")); j < max; j++)
					out.push(ths.get(String.valueOf(j)));
				for (JSPrimitive arg : arguments) {
					if (!(arg instanceof JSObject))
						continue;
					JSObject arr = (JSObject) arg;
					for (int j = 0, max = (int) JSUtils.asNumber(arr.get("length")); j < max; j++)
						out.push(arr.get(String.valueOf(j)));
				}
				return out;
			}
		});
		
		set("push", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				ths.set(String.valueOf((int) ((JSNumber) ths.get("length")).value), l0);
				return ths.get("length");
			}
		});
		
		set("pop", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				int len = (int) ((JSNumber) ths.get("length")).value;
				JSPrimitive out = ths.get(String.valueOf(len-1));
				ths.set("length", new JSNumber(len-1));
				return out;
			}
		});
		
		set("map", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				JSObject func = (JSObject) l0;
				JSArray out = new JSArray(arrayPrototype);
				int len = (int) JSUtils.asNumber(ths.get("length"));
				for (int i = 0; i < len; i++)
					out.push(func.invoke(ths, 2, ths.get(String.valueOf(i)), new JSNumber(i), null, null, null, null, null, null, null));
				return out;
			}
		});
		
		set("join", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				StringBuffer sb = new StringBuffer();
				String delim = (l0 == null || l0 == JSAtoms.NULL) ? "" : JSUtils.asString(l0); 
				int len = (int) JSUtils.asNumber(ths.get("length"));
				for (int i = 0; i < len; i++) {
					if (i > 0)
						sb.append(delim);
					sb.append(JSUtils.asString(ths.get(String.valueOf(i))));
				}
				return new JSString(sb.toString());
			}
		});
	} };
	
	final JSObject stringPrototype = new JSObject() { {
		set("charAt", new JSFunction () {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive index, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSString(String.valueOf(JSUtils.asString(ths.getPrimitiveValue()).charAt((int) JSUtils.asNumber(index))));
			}
		});
		
		set("substr", new JSFunction () {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				int start = (int) JSUtils.asNumber(l0), end = (int) JSUtils.asNumber(l1);
				String value = JSUtils.asString(ths.getPrimitiveValue());
				return new JSString(value.substring(start, end+start));
			}
		});
	} };
	
	final JSObject numberPrototype = new JSObject() { {
	} };
	
	final JSObject booleanPrototype = new JSObject() { {
	} };
	
	/*
	 * prototype accessors
	 */
	
	public JSObject getArrayPrototype() {
		return arrayPrototype;
	}
	
	public JSObject getStringPrototype() {
		return stringPrototype;
	}
	
	public JSObject getNumberPrototype() {
		return numberPrototype;
	}
	
	public JSObject getBooleanPrototype() {
		return booleanPrototype;
	}
	
	/*
	 * scope accessors
	 */
	
	JSPrimitive _require = requireFunction;
	public JSPrimitive get_require() { return _require; }
	public void set_require(JSPrimitive value) { _require = value; }
	
	JSPrimitive _exports = exportsObject;
	public JSPrimitive get_exports() { return _exports; }
	public void set_exports(JSPrimitive value) { _exports = value; }
	
	JSPrimitive _print = printFunction;
	public JSPrimitive get_print() { return _print; }
	public void set_print(JSPrimitive value) { _print = value; }
	
	JSPrimitive _Math = mathObject;
	public JSPrimitive get_Math() { return _Math; }
	public void set_Math(JSPrimitive value) { _Math = value; }

	JSPrimitive _Array = arrayConstructor;
	public JSPrimitive get_Array() { return _Array; }
	public void set_Array(JSPrimitive value) { _Array = value; }
}
