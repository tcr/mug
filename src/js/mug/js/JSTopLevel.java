package mug.js;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mug.Modules;

public class JSTopLevel {
	/*
	 * prototypes
	 */
	
	final JSObject functionPrototype = new JSObject() { {
		set("apply", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				JSPrimitive[] args = JSUtils.toJavaArray((JSObject) l1);
				rest = null;
				if (args.length > 8) {
					rest = new JSPrimitive[args.length - 8];
					System.arraycopy(args, 8, rest, 0, args.length-8);
				}
				return thsObj.invoke(
						l0 instanceof JSObject ? (JSObject) l0 : null,
						args.length,
						args.length > 0 ? args[0] : null,
						args.length > 1 ? args[1] : null,
						args.length > 2 ? args[2] : null,
						args.length > 3 ? args[3] : null,
						args.length > 4 ? args[4] : null,
						args.length > 5 ? args[5] : null,
						args.length > 6 ? args[6] : null,
						args.length > 7 ? args[7] : null,
						rest);
			}
		});
	} };
	
	final JSObject arrayPrototype = new JSObject() { {
		set("concat", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				// concatenate arrays to new array
				JSObject thsObj = (JSObject) ths;
				JSArray out = new JSArray(arrayPrototype);
				JSPrimitive[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
				for (int j = 0, max = (int) JSUtils.asNumber(thsObj.get("length")); j < max; j++)
					out.push(thsObj.get(String.valueOf(j)));
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
		
		set("push", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				thsObj.set(String.valueOf((int) ((JSNumber) thsObj.get("length")).value), l0);
				return thsObj.get("length");
			}
		});
		
		set("pop", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				int len = (int) ((JSNumber) thsObj.get("length")).value;
				JSPrimitive out = thsObj.get(String.valueOf(len-1));
				thsObj.set("length", new JSNumber(len-1));
				return out;
			}
		});
		
		set("map", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				JSObject func = (JSObject) l0;
				JSArray out = new JSArray(arrayPrototype);
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				for (int i = 0; i < len; i++)
					out.push(func.invoke(ths, 2, thsObj.get(String.valueOf(i)), new JSNumber(i), null, null, null, null, null, null, null));
				return out;
			}
		});
		
		set("join", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				StringBuffer sb = new StringBuffer();
				String delim = (l0 == null || l0 == JSAtoms.NULL) ? "" : JSUtils.asString(l0); 
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				for (int i = 0; i < len; i++) {
					if (i > 0)
						sb.append(delim);
					sb.append(JSUtils.asString(thsObj.get(String.valueOf(i))));
				}
				return new JSString(sb.toString());
			}
		});
		
		set("slice", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				JSObject thsObj = (JSObject) ths;
				int len = (int) JSUtils.asNumber(thsObj.get("length"));
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? len : (int) JSUtils.asNumber(l1);
				if (end < 0)
					end += len + 1;
				JSArray out = new JSArray(arrayPrototype);
				for (int i = start; i < end; i++)
					out.push(thsObj.get(String.valueOf(i)));
				return out;
			}
		});
	} };
	
	final JSObject stringPrototype = new JSObject() { {
		set("charAt", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSString(String.valueOf(JSUtils.asString(ths).charAt((int) JSUtils.asNumber(l0))));
			}
		});
		
		set("charCodeAt", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSNumber((int) JSUtils.asString(ths).charAt((int) JSUtils.asNumber(l0)));
			}
		});
		
		set("replace", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				String value = JSUtils.asString(l1);
				if (l0 instanceof JSRegExp) {
					JSRegExp regexp = (JSRegExp) l0;
					Matcher matcher = regexp.getPattern().matcher(JSUtils.asString(ths));
					return new JSString(regexp.isGlobal() ? matcher.replaceAll(value) : matcher.replaceFirst(value));
				} else {
					String match = JSUtils.asString(l0);
					return new JSString(JSUtils.asString(ths).replaceFirst(Pattern.quote(match), value));
				}
			}
		});
		
		set("substr", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? value.length() : ((int) JSUtils.asNumber(l1)) + start;
				return new JSString(value.substring(start, end));
			}
		});
		
		set("indexOf", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				String search = JSUtils.asString(l0);
				return new JSNumber(value.indexOf(search));
			}
		});
		
		set("toLowerCase", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive index, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSString(JSUtils.asString(ths).toLowerCase());
			}
		});
	} };
	
	final JSObject numberPrototype = new JSObject() { {
		set("toString", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				int base = (int) JSUtils.asNumber(l0);
				double value = JSUtils.asNumber(ths);
				switch (base) {
				case 2: return new JSString(Integer.toBinaryString((int) value));
				case 8:	return new JSString(Integer.toOctalString((int) value));
				case 16: return new JSString(Integer.toHexString((int) value));
				}
				return new JSString(Double.toString(value));
			}
		});
	} };
	
	final JSObject booleanPrototype = new JSObject() { {
	} };
	
	final JSObject regexpPrototype = new JSObject() { {
		set("test", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				Pattern pattern = ((JSRegExp) ths).getPattern();
				return pattern.matcher(JSUtils.asString(l0)).find() ? JSAtoms.TRUE : JSAtoms.FALSE;
			}
		});
		
		set("exec", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				Pattern pattern = ((JSRegExp) ths).getPattern();
				JSArray out = new JSArray(getArrayPrototype());
				Matcher matcher = pattern.matcher(JSUtils.asString(l0));
				matcher.find();
				for (int i = 0; i < matcher.groupCount() + 1; i++)
					out.push(new JSString(matcher.group(i)));
				return out;
			}
		});
	} };
	
	/*
	 * objects/constructors
	 */	
	
	final JSFunction requireFunction = new JSFunction(functionPrototype) {
		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			return Modules.getModule(JSUtils.asString(l0)).load();
		}
	};
	
	final JSObject exportsObject = new JSObject() { };
	
	final JSFunction printFunction = new JSFunction(functionPrototype) {
		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			//System.out.println(JSUtils.asString(l0) + " <" + l0 + ">");
			System.out.println(JSUtils.asString(l0));
			return null;
		}
	};
	
	final JSObject mathObject = new JSObject() { {
		set("sqrt", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSNumber(Math.sqrt(JSUtils.asNumber(l0)));
			}
		});
		
		set("floor", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSNumber(Math.floor(JSUtils.asNumber(l0)));
			}
		});
	} };
	
	final JSFunction arrayConstructor = new JSFunction(functionPrototype) {
		public JSPrimitive instantiate(int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception {
			return invoke(null, argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
		}
		
		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
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
	
	public JSObject getFunctionPrototype() {
		return functionPrototype;
	}
	
	public JSObject getRegExpPrototype() {
		return regexpPrototype;
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
