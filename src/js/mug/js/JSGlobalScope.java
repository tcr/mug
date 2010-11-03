package mug.js;

public class JSGlobalScope {
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
		public JSPrimitive instantiate(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
			return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			JSArray obj = new JSArray(actual_prototype);
		
			// single-argument constructor
			if (argc == 0) {
				obj.set("length", new JSNumber(argc));
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
	
	/*
	 * nanoTime
	 */
	
	public static JSPrimitive _nanoTime = new JSFunction() {
		@Override
		public JSPrimitive invoke(JSObject ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			return new JSNumber(System.nanoTime());
		}
	};
	
	public JSPrimitive get_nanoTime() { return _nanoTime; }
	public void set_nanoTime(JSPrimitive value) { _nanoTime = value; }
}
