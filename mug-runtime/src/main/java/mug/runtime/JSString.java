package mug.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 */

public class JSString extends JSObject {
	String value;
	
	public JSString(JSEnvironment env, String value) {
		super(env, env.getStringPrototype());
		this.value = value;
	}
	
	public Object get(String key) throws Exception {
		if (key.equals("length"))
			return value.length();
		return super.get(key);
	}
	
	/**
	 * Environment
	 */

	public static JSObject createPrototype(JSEnvironment env)
	{
		return new JSObject(env) { {
			defineProperty("valueOf", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					if (ths instanceof JSString)
						return ((JSString) ths).value;
					return ths.toString();
				}
			});
			
			defineProperty("charAt", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					int idx = (int) JSUtils.asNumber(l0);
					String str = JSUtils.asString(ths);
					return idx >= 0 && idx < str.length() ? String.valueOf(str.charAt(idx)) : "";
				}
			});
			
			defineProperty("charCodeAt", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					int idx = (int) JSUtils.asNumber(l0);
					String str = JSUtils.asString(ths);
					return idx >= 0 && idx < str.length() ? (double) str.charAt(idx) : Double.NaN;
				}
			});
			
			defineProperty("replace", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					String value = JSUtils.asString(l1);
					if (l0 instanceof JSRegExp) {
						JSRegExp regexp = (JSRegExp) l0;
						Matcher matcher = regexp.getPattern().matcher(JSUtils.asString(ths));
						return regexp.isGlobal() ? matcher.replaceAll(value) : matcher.replaceFirst(value);
					} else {
						String match = JSUtils.asString(l0);
						return JSUtils.asString(ths).replaceFirst(Pattern.quote(match), value);
					}
				}
			});
			
			defineProperty("substring", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					String value = JSUtils.asString(ths);
					if (value.length() == 0)
						return "";
					int start = (int) JSUtils.asNumber(l0);
					if (start < 0)
						start = 0;
					int end = l1 == null ? value.length() : (int) JSUtils.asNumber(l1);
					if (end >= value.length())
						end = value.length()-1;
					if (end < start)
						end = start;
					return value.substring(start, end);
				}
			});
			
			defineProperty("substr", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					String value = JSUtils.asString(ths);
					int start = (int) JSUtils.asNumber(l0);
					int end = l1 == null ? value.length() : ((int) JSUtils.asNumber(l1)) + start;
					return value.substring(start, end);
				}
			});
			
			defineProperty("indexOf", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					String value = JSUtils.asString(ths);
					String search = JSUtils.asString(l0);
					int start = argc > 1 ? (int) JSUtils.asNumber(l1) : 0;
					return new Double(value.indexOf(search, start));
				}
			});
			
			defineProperty("toLowerCase", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object index, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return JSUtils.asString(ths).toLowerCase();
				}
			});
			
			defineProperty("toUpperCase", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object index, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return JSUtils.asString(ths).toUpperCase();
				}
			});
			
			defineProperty("split", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Pattern pattern = (l0 instanceof JSRegExp) ? ((JSRegExp) l0).getPattern() : Pattern.compile(Pattern.quote(JSUtils.asString(l0)));
					String[] result = pattern.split(JSUtils.asString(ths), -1);
					// specific hack for empty strings...
					if (pattern.pattern().equals("\\Q\\E")) {
						String str = JSUtils.asString(ths);
						result = new String[str.length()];
						for (int i = 0; i < str.length(); i++)
							result[i] = String.valueOf(str.charAt(i));
					}
					JSArray out = new JSArray(env, 0);
					for (int i = 0; i < result.length; i++)
						out.push(result[i]);
					return out;
				}
			});
			
			defineProperty("match", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					//[TODO] if l0 is string, compile
					Pattern pattern = ((JSRegExp) l0).getPattern();
					JSArray out = new JSArray(env, 0);
					Matcher matcher = pattern.matcher(JSUtils.asString(ths));
					if (!((JSRegExp) l0).isGlobal()) {
						if (!matcher.find())
							return env.getNullObject();
						for (int i = 0; i < matcher.groupCount() + 1; i++)
							out.push(matcher.group(i));
					} else {
						while (matcher.find())
							out.push(matcher.group(0));
						if (out.getLength() == 0)
							return env.getNullObject();
					}
					return out;
				}
			});
		} };
	}
}