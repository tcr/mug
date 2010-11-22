package mug.js;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mug.Modules;

public class JSTopLevel {
	/*
	 * prototypes
	 */
	
	final JSObject objectPrototype = JSObject.createObjectPrototype();
	
	final JSObject functionPrototype = new JSObject(objectPrototype) { {
		// needed to reference self
		JSObject functionPrototype = this;
		
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
	
	final JSObject arrayPrototype = new JSObject(objectPrototype) { {
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
		
		set("sort", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(final JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				// cast to strings
				JSPrimitive[] arr = JSUtils.toJavaArray((JSObject) ths);
				for (int i = 0; i < arr.length; i++)
					arr[i] = new JSString(JSUtils.asString(arr[i]));
				// sort
				final JSObject func = l0 != null ? (JSObject) l0 :
					new JSFunction(functionPrototype) {
						@Override
						public JSPrimitive invoke(final JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
						{
							return new JSNumber(((JSString) l0).value.compareTo(((JSString) l1).value));
						}
					};
				Arrays.sort(arr, new Comparator<JSPrimitive>() {
					@Override
					public int compare(JSPrimitive a, JSPrimitive b) {
						try {
							return (int) JSUtils.asNumber(func.invoke(ths, a, b));
						} catch (Exception e) {
							return 0;
						}
					}
				});
				// return new array
				JSArray out = new JSArray(getArrayPrototype());
				out.append(arr);
				return out;
			}
		});
	} };
	
	final JSObject stringPrototype = new JSObject(objectPrototype) { {
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
		
		set("substring", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				String value = JSUtils.asString(ths);
				int start = (int) JSUtils.asNumber(l0);
				int end = l1 == null ? value.length() : (int) JSUtils.asNumber(l1);
				return new JSString(value.substring(start, end));
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
		
		set("toUpperCase", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive index, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSString(JSUtils.asString(ths).toUpperCase());
			}
		});
		
		set("split", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				Pattern pattern = (l0 instanceof JSRegExp) ? ((JSRegExp) l0).getPattern() : Pattern.compile(Pattern.quote(JSUtils.asString(l0)));
				String[] result = pattern.split(JSUtils.asString(ths));
				JSArray out = new JSArray(arrayPrototype);
				for (int i = 0; i < result.length; i++)
					out.push(new JSString(result[i]));
				return out;
			}
		});
		
		set("match", new JSFunction (functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				//[TODO] if l0 is string, compile
				Pattern pattern = ((JSRegExp) l0).getPattern();
				JSArray out = new JSArray(getArrayPrototype());
				Matcher matcher = pattern.matcher(JSUtils.asString(ths));
				if (!((JSRegExp) l0).isGlobal()) {
					if (!matcher.find())
						return JSAtoms.NULL;
					for (int i = 0; i < matcher.groupCount() + 1; i++)
						out.push(new JSString(matcher.group(i)));
				} else {
					while (matcher.find())
						out.push(new JSString(matcher.group(0)));
					if (out.getLength() == 0)
						return JSAtoms.NULL;
				}
				return out;
			}
		});
	} };
	
	final JSObject numberPrototype = new JSObject(objectPrototype) { {
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
		
		set("toFixed", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				double value = JSUtils.asNumber(ths);
				int fixed = (int) JSUtils.asNumber(l0); 
				DecimalFormat formatter = new DecimalFormat("0" +
					(fixed > 0 ? "." + String.format(String.format("%%0%dd", fixed), 0) : ""));
				return new JSString(formatter.format(value));
			}
		});
	} };
	
	final JSObject booleanPrototype = new JSObject(objectPrototype) { {
	} };
	
	final JSObject regexpPrototype = new JSObject(objectPrototype) { {
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
				if (!((JSRegExp) ths).isGlobal()) {
					if (!matcher.find())
						return JSAtoms.NULL;
					for (int i = 0; i < matcher.groupCount() + 1; i++)
						out.push(new JSString(matcher.group(i)));
				} else {
					while (matcher.find())
						out.push(new JSString(matcher.group(0)));
					if (out.getLength() == 0)
						return JSAtoms.NULL;
				}
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
	
	final JSObject exportsObject = new JSObject(objectPrototype) { };
	
	final JSFunction printFunction = new JSFunction(functionPrototype) {
		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			JSPrimitive[] arguments = JSUtils.arguments(argc, l0, l1, l2, l3, l4, l5, l6, l7, rest);
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0)
					System.out.print(" ");
				System.out.print(JSUtils.asString(arguments[i]));
			}
			System.out.println("");
			return null;
		}
	};
	
	final JSObject mathObject = new JSObject(objectPrototype) { {
		set("abs", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSNumber(Math.abs(JSUtils.asNumber(l0)));
			}
		});
		
		set("pow", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSNumber(Math.pow(JSUtils.asNumber(l0), JSUtils.asNumber(l1)));
			}
		});
		
		set("sqrt", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSNumber(Math.sqrt(JSUtils.asNumber(l0)));
			}
		});
		
		set("max", new JSFunction(functionPrototype) {
			@Override
			public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest) throws Exception
			{
				return new JSNumber(Math.max(JSUtils.asNumber(l0), JSUtils.asNumber(l1)));
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
	
	final JSFunction numberConstructor = new JSFunction(functionPrototype) {
		@Override
		public JSPrimitive invoke(JSPrimitive ths, int argc, JSPrimitive l0, JSPrimitive l1, JSPrimitive l2, JSPrimitive l3, JSPrimitive l4, JSPrimitive l5, JSPrimitive l6, JSPrimitive l7, JSPrimitive[] rest)
				throws Exception {
			return new JSNumber(JSUtils.asNumber(l0));
		}
	};
	
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
	
	public JSObject getObjectPrototype() {
		return objectPrototype;
	}
	
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
	
	JSPrimitive _Number = numberConstructor;
	public JSPrimitive get_Number() { return _Number; }
	public void set_Number(JSPrimitive value) { _Number = value; }
}
