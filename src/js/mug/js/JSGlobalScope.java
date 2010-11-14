package mug.js;

import mug.Modules;

public class JSGlobalScope {
	/*
	 * require
	 */
	
	public static JSPrimitive _require = new JSFunction() {
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			return Modules.getModule(JSUtils.asString(l0)).load();
		}
	};
	
	public JSPrimitive get_require() { return _require; }
	public void set_require(JSPrimitive value) { _require = value; }
	
	/*
	 * exports
	 */
	
	public static JSPrimitive _exports = new JSObject() { };
	
	public JSPrimitive get_exports() { return _exports; }
	public void set_exports(JSPrimitive value) { _exports = value; }
	
	/*
	 * print
	 */
	
	public static JSPrimitive _print = new JSFunction() {
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			System.out.println(JSUtils.asString(l0) + " <" + l0 + ">");
			return null;
		}
	};
	
	public JSPrimitive get_print() { return _print; }
	public void set_print(JSPrimitive value) { _print = value; }
	
	/*
	 * Math
	 */
	
	public static JSPrimitive _Math = new JSObject() { {
		set("sqrt", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
					throws Exception {
				return new JSNumber(Math.sqrt(JSUtils.asNumber(l0)));
			}
		});
	} };
	
	public JSPrimitive get_Math() { return _Math; }
	public void set_Math(JSPrimitive value) { _Math = value; }
	
	/*
	 * Array
	 */
	
	public static JSPrimitive _Array = new JSFunction() {
		{
			actual_prototype = new JSObject() { {
				set("concat", new JSFunction() {
					@Override
					public JSPrimitive invoke(JSObject ths, int argc,
							JSPrimitive l0, JSPrimitive l1, JSPrimitive l2,
							JSPrimitive l3, JSPrimitive l4, JSPrimitive l5,
							JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
							throws Exception {
						// concatenate arrays to new array
						JSObject out = (JSObject) ((JSFunction) _Array).instantiate(0, null, null, null, null, null, null, null, null, null);
						JSPrimitive[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
						int k = append(out, 0, ths);
						for (int i = 0; i < arguments.length; i++) {
							if (!(arguments[i] instanceof JSObject))
								continue;
							k = append(out, k, (JSObject) arguments[i]);
						}
						return out;
					}
					
					int append(JSObject to, int len, JSObject from) {
						for (int j = 0; j < JSUtils.asNumber(from.get("length")); j++) {
							to.set(String.valueOf(len++), from.get(String.valueOf(j)));
						}
						return len;
					}
				});
				
				set("push", new JSFunction() {
					@Override
					public JSPrimitive invoke(JSObject ths, int argc,
							JSPrimitive l0, JSPrimitive l1, JSPrimitive l2,
							JSPrimitive l3, JSPrimitive l4, JSPrimitive l5,
							JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
							throws Exception {
						ths.set(String.valueOf((int) ((JSNumber) ths.get("length")).value), l0);
						return ths.get("length");
					}
				});
				
				set("pop", new JSFunction() {
					@Override
					public JSPrimitive invoke(JSObject ths, int argc,
							JSPrimitive l0, JSPrimitive l1, JSPrimitive l2,
							JSPrimitive l3, JSPrimitive l4, JSPrimitive l5,
							JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
							throws Exception {
						int len = (int) ((JSNumber) ths.get("length")).value;
						JSPrimitive out = ths.get(String.valueOf(len-1));
						ths.set("length", new JSNumber(len-1));
						return out;
					}
				});
			} };
		}
		
		public JSPrimitive instantiate(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
			return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			JSArray obj = new JSArray(actual_prototype);
		
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
	
	public JSPrimitive get_Array() { return _Array; }
	public void set_Array(JSPrimitive value) { _Array = value; }
}
