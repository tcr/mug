package mug.js;

public class JSGlobalScope {
	public static JSPrimitive _exports = new JSCompiledObject() { };
	
	public JSPrimitive get_exports() { return _exports; }
	public void set_exports(JSPrimitive value) { _exports = value; }
	
	public static JSPrimitive _Math = new JSCompiledObject() { {
		set("sqrt", new JSCompiledFunction() {
			@Override
			public JSPrimitive invoke(JSCompiledObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
					throws Exception {
				return new JSNumber(Math.sqrt(JSUtils.asNumber(l0)));
			}
		});
	} };
	
	public JSPrimitive get_Math() { return _Math; }
	public void set_Math(JSPrimitive value) { _Math = value; }
	
	public static JSPrimitive _Array = new JSCompiledFunction() {
		@Override
		public JSPrimitive invoke(JSCompiledObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
				throws Exception {
			JSCompiledObject obj = new Array_Class();
			if (l0 != null) obj.set("0", l0);
			if (l1 != null) obj.set("1", l1);
			if (l2 != null) obj.set("2", l2);
			if (l3 != null) obj.set("3", l3);
			if (l4 != null) obj.set("4", l4);
			if (l5 != null) obj.set("5", l5);
			if (l6 != null) obj.set("6", l6);
			if (l7 != null) obj.set("7", l7);
			return obj;
		}
		
		class Array_Class extends JSCompiledObject {
			public Array_Class() {
				super();
				super.set("length", new JSNumber(0));
			}
			
			public void set(String key, JSPrimitive value) {
				if (key == "length")
					return;
				super.set(key, value);
				try {
					int index = ((int) Double.parseDouble(key));
					if (index > JSUtils.asNumber(get("length")))
						super.set("length", new JSNumber(index+1));
				} catch (Exception e) { }
			}
		}
	};
	
	public JSPrimitive get_Array() { return _Array; }
	public void set_Array(JSPrimitive value) { _Array = value; }
	
	public static JSPrimitive _nanoTime = new JSCompiledFunction() {
		@Override
		public JSPrimitive invoke(JSCompiledObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
				throws Exception {
			return new JSNumber(System.nanoTime());
		}
	};
	
	public JSPrimitive get_nanoTime() { return _nanoTime; }
	public void set_nanoTime(JSPrimitive value) { _nanoTime = value; }
}
