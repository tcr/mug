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
		public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
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
			public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
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
		public JSPrimitive instantiate(JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7) throws Exception {
			return invoke(null, l0, l1, l2, l3, l4, l5, l6, l7);
		}
		
		@Override
		public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
				throws Exception {
			JSArray obj = new JSArray(actual_prototype);
			if (l0 != null) obj.push(l0);
			if (l1 != null) obj.set("1", l1);
			if (l2 != null) obj.set("2", l2);
			if (l3 != null) obj.set("3", l3);
			if (l4 != null) obj.set("4", l4);
			if (l5 != null) obj.set("5", l5);
			if (l6 != null) obj.set("6", l6);
			if (l7 != null) obj.set("7", l7);
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
		public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
				throws Exception {
			return new JSNumber(System.nanoTime());
		}
	};
	
	public JSPrimitive get_nanoTime() { return _nanoTime; }
	public void set_nanoTime(JSPrimitive value) { _nanoTime = value; }
}
