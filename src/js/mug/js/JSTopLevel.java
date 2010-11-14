package mug.js;

import mug.Modules;

public class JSTopLevel {
	/*
	 * scope accessors
	 */
	
	public JSPrimitive get_require() { return requireFunction; }
	
	public JSPrimitive get_exports() { return exportsObject; }
	
	public JSPrimitive get_print() { return printFunction; }
	
	public JSPrimitive get_Math() { return mathObject; }

	public JSPrimitive get_Array() { return arrayConstructor; }
	
	/*
	 * prototype accessors
	 */
	
	public JSObject getArrayPrototype() {
		return arrayPrototype;
	}
	
	public JSObject getStringPrototype() {
		return stringPrototype;
	}
	
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
		{
			actual_prototype = getArrayPrototype();
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
	
	/*
	 * prototypes
	 */
	
	final JSObject stringPrototype = new JSObject() { {
		set("charAt", new JSFunction () {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive index,
					JSPrimitive l1, JSPrimitive l2, JSPrimitive l3,
					JSPrimitive l4, JSPrimitive l5, JSPrimitive l6,
					JSPrimitive l7, JSPrimitive[] rest) throws Exception {
				return new JSString(String.valueOf(JSUtils.asString(ths.valueOf()).charAt((int) JSUtils.asNumber(index))));
			}
		});
	} };
	
	final JSObject arrayPrototype = new JSObject() { {
		set("concat", new JSFunction() {
			@Override
			public JSPrimitive invoke(JSObject ths, int argc,
					JSPrimitive l0, JSPrimitive l1, JSPrimitive l2,
					JSPrimitive l3, JSPrimitive l4, JSPrimitive l5,
					JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
					throws Exception {
				// concatenate arrays to new array
				JSObject out = (JSObject) arrayConstructor.instantiate(0, null, null, null, null, null, null, null, null, null);
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
