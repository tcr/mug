import mug.js.*;
import mug.js.compiled.*;

public class Container {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// create global scope
		JSScriptScope global = new JSScriptScope();
		global.set_Math(MATH);
		//global.set_Array(ARRAY);
		global.set_print(PRINT);
		JSScript script = new JSScript();
		
		// time and execute
		long start = System.nanoTime();
		System.out.println("###START");
		script.execute(global);
		System.out.println("###END (time: " + ((System.nanoTime() - start) / 1000000.0) + " milliseconds)");
	}

	static JSFunction PRINT = new JSFunction() {
		@Override
		public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
				throws Exception {
			System.out.println(JSUtils.asString(l0) + " <" + l0 + ">");
			return null;
		}
	};
	
	static JSObject MATH = new JSObject() { {
		set("sqrt", new JSFunction () {
			@Override
			public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
					throws Exception {
				return new JSNumber(Math.sqrt(JSUtils.asNumber(l0)));
			}
		});
	} };
	
	static JSObject ARRAY = new JSFunction () {
		@Override
		public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
				throws Exception {
			JSObject obj = new ARRAY_CLASS();
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
		
		class ARRAY_CLASS extends JSObject {
			public ARRAY_CLASS() {
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
	
	static JSFunction NANOTIME = new JSFunction () {
		@Override
		public JSPrimitive invoke(JSObject ths, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7)
				throws Exception {
			return new JSNumber(System.nanoTime());
		}
	};
}