package mug.js;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.List;
import java.util.Map;

public class JSON {
	public static Object parse(JSEnvironment env, String json) {
		return toJSValue(env, JSONValue.parse(json));
	}
	
	static Object toJSValue(JSEnvironment env, Object arg) {
		if (arg instanceof String)
			return arg;
		else if (arg instanceof Number)
			return ((Number) arg).doubleValue();
		else if (arg instanceof Boolean)
			return arg;
		else if (arg instanceof List) {
			List list = (List) arg;
			JSArray a = new JSArray(env, list.size());
			for (Object item : ((List) arg).toArray())
				a.push(toJSValue(env, item));
			return a;
		} else if (arg instanceof Map) {
			Map map = (Map) arg;
			JSObject obj = new JSObject(env);
			for (Object key : map.keySet())
				obj.defineProperty((String) key, toJSValue(env, map.get(key)));
			return obj;
		}
		return null;
	}
	
	public static String stringify(Object arg) throws Exception {
		return JSONValue.toJSONString(toJSONValue(arg));
	}
	
	static Object toJSONValue(Object arg) throws Exception {
		// properly format arrays and objects
		if (arg instanceof JSArray) {
			JSONArray json = new JSONArray();
			for (Object item : ((JSArray) arg).toArray())
				json.add(toJSONValue(item));
			return json;
		} else if (arg instanceof JSNull) {
			return null;
		} else if (arg instanceof JSObject) {
			JSONObject json = new JSONObject();
			JSObject obj = (JSObject) arg;
			for (String key : obj.getKeys())
				json.put(key, toJSONValue(obj.get(key)));
			return json;
		}
		return arg;
	}
}
